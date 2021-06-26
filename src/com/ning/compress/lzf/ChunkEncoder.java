/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.ning.compress.lzf;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import com.ning.compress.BufferRecycler;

/**
 * Class that handles actual encoding of individual chunks.
 * Resulting chunks can be compressed or non-compressed; compression
 * is only used if it actually reduces chunk size (including overhead
 * of additional header bytes)
 *<p>
 * Note that instances <b>are stateful</b> and hence
 * <b>not thread-safe</b>; one instance is meant to be used
 * for processing a sequence of chunks where total length
 * is known.
 * 
 * @author Tatu Saloranta (tatu.saloranta@iki.fi)
 */
public abstract class ChunkEncoder
    implements Closeable
{
    // // // Constants
    
    // Beyond certain point we won't be able to compress; let's use 16 bytes as cut-off
    protected static final int MIN_BLOCK_TO_COMPRESS = 16;

    protected static final int MIN_HASH_SIZE = 256;

    // Not much point in bigger tables, with 8k window
    protected static final int MAX_HASH_SIZE = 16384;

    protected static final int MAX_OFF = 1 << 13; // 8k
    protected static final int MAX_REF = (1 << 8) + (1 << 3); // 264

    /**
     * How many tail bytes are we willing to just copy as is, to simplify
     * loop end checks? 4 is bare minimum, may be raised to 8?
     */
    protected static final int TAIL_LENGTH = 4;

    // // // Encoding tables etc

    protected final BufferRecycler _recycler;

    /**
     * Hash table contains lookup based on 3-byte sequence; key is hash
     * of such triplet, value is offset in buffer.
     */
    protected int[] _hashTable;
    
    protected final int _hashModulo;

    /**
     * Buffer in which encoded content is stored during processing
     */
    protected byte[] _encodeBuffer;

    /**
     * Small buffer passed to LZFChunk, needed for writing chunk header
     */
    protected byte[] _headerBuffer;

    /**
	 * Uses a ThreadLocal soft-referenced BufferRecycler instance.
	 * 
     * @param totalLength Total encoded length; used for calculating size
     *   of hash table to use
     */
    protected ChunkEncoder(int totalLength)
    {
        this(totalLength, BufferRecycler.instance());
    }

    /**
     * @param totalLength Total encoded length; used for calculating size
     *   of hash table to use
	 * @param bufferRecycler Buffer recycler instance, for usages where the
	 *   caller manages the recycler instances
     */
    protected ChunkEncoder(int totalLength, BufferRecycler bufferRecycler)
    {
        // Need room for at most a single full chunk
        int largestChunkLen = Math.min(totalLength, LZFChunk.MAX_CHUNK_LEN);       
        int suggestedHashLen = calcHashLen(largestChunkLen);
        _recycler = bufferRecycler;
        _hashTable = bufferRecycler.allocEncodingHash(suggestedHashLen);
        _hashModulo = _hashTable.length - 1;
        // Ok, then, what's the worst case output buffer length?
        // length indicator for each 32 literals, so:
        // 21-Feb-2013, tatu: Plus we want to prepend chunk header in place:
        int bufferLen = largestChunkLen + ((largestChunkLen + 31) >> 5) + LZFChunk.MAX_HEADER_LEN;
        _encodeBuffer = bufferRecycler.allocEncodingBuffer(bufferLen);
    }
	
    /**
     * Alternate constructor used when we want to avoid allocation encoding
     * buffer, in cases where caller wants full control over allocations.
     */
    protected ChunkEncoder(int totalLength, boolean bogus)
    {
        this(totalLength, BufferRecycler.instance(), bogus);
    }

    /**
     * Alternate constructor used when we want to avoid allocation encoding
     * buffer, in cases where caller wants full control over allocations.
     */
    protected ChunkEncoder(int totalLength, BufferRecycler bufferRecycler, boolean bogus)
    {
        int largestChunkLen = Math.max(totalLength, LZFChunk.MAX_CHUNK_LEN);
        int suggestedHashLen = calcHashLen(largestChunkLen);
        _recycler = bufferRecycler;
        _hashTable = bufferRecycler.allocEncodingHash(suggestedHashLen);
        _hashModulo = _hashTable.length - 1;
        _encodeBuffer = null;
    }

    private static int calcHashLen(int chunkSize)
    {
        // in general try get hash table size of 2x input size
        chunkSize += chunkSize;
        // but no larger than max size:
        if (chunkSize >= MAX_HASH_SIZE) {
            return MAX_HASH_SIZE;
        }
        // otherwise just need to round up to nearest 2x
        int hashLen = MIN_HASH_SIZE;
        while (hashLen < chunkSize) {
            hashLen += hashLen;
        }
        return hashLen;
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Method to close once encoder is no longer in use. Note: after calling
     * this method, further calls to {@link #encodeChunk} will fail
     */
    @Override
    public final void close()
    {
        byte[] buf = _encodeBuffer;
        if (buf != null) {
            _encodeBuffer = null;
            _recycler.releaseEncodeBuffer(buf);
        }
        int[] ibuf = _hashTable;
        if (ibuf != null) {
            _hashTable = null;
            _recycler.releaseEncodingHash(ibuf);
        }
    }
    /**
     * Method for compressing (or not) individual chunks
     */
    public LZFChunk encodeChunk(byte[] data, int offset, int len)
    {
        if (len >= MIN_BLOCK_TO_COMPRESS) {
            /* If we have non-trivial block, and can compress it by at least
             * 2 bytes (since header is 2 bytes longer), let's compress:
             */
            int compLen = tryCompress(data, offset, offset+len, _encodeBuffer, 0);
            if (compLen < (len-2)) { // nah; just return uncompressed
                return LZFChunk.createCompressed(len, _encodeBuffer, 0, compLen);
            }
        }
        // Otherwise leave uncompressed:
        return LZFChunk.createNonCompressed(data, offset, len);
    }

    /**
     * Method for compressing individual chunk, if (and only if) it compresses down
     * to specified ratio or less.
     * 
     * @param maxResultRatio Value between 0.05 and 1.10 to indicate maximum relative size of
     *   the result to use, in order to append encoded chunk
     *   
     * @return Encoded chunk if (and only if) input compresses down to specified ratio or less;
     *    otherwise returns null
     */
    public LZFChunk encodeChunkIfCompresses(byte[] data, int offset, int inputLen,
            double maxResultRatio)
    {
        if (inputLen >= MIN_BLOCK_TO_COMPRESS) {
            final int maxSize = (int) (maxResultRatio * inputLen + LZFChunk.HEADER_LEN_COMPRESSED + 0.5);
            int compLen = tryCompress(data, offset, offset+inputLen, _encodeBuffer, 0);
            if (compLen <= maxSize) {
                return LZFChunk.createCompressed(inputLen, _encodeBuffer, 0, compLen);
            }
        }
        return null;
    }
    
    /**
     * Alternate chunk compression method that will append encoded chunk in
     * pre-allocated buffer. Note that caller must ensure that the buffer is
     * large enough to hold not just encoded result but also intermediate
     * result; latter may be up to 4% larger than input; caller may use
     * {@link LZFEncoder#estimateMaxWorkspaceSize(int)} to calculate
     * necessary buffer size.
     * 
     * @return Offset in output buffer after appending the encoded chunk
     */
    public int appendEncodedChunk(final byte[] input, final int inputPtr, final int inputLen,
            final byte[] outputBuffer, final int outputPos)
    {
        if (inputLen >= MIN_BLOCK_TO_COMPRESS) {
            /* If we have non-trivial block, and can compress it by at least
             * 2 bytes (since header is 2 bytes longer), use as-is
             */
            final int compStart = outputPos + LZFChunk.HEADER_LEN_COMPRESSED;
            final int end = tryCompress(input, inputPtr, inputPtr+inputLen, outputBuffer, compStart);
            final int uncompEnd = (outputPos + LZFChunk.HEADER_LEN_NOT_COMPRESSED) + inputLen;
            if (end < uncompEnd) { // yes, compressed by at least one byte
                final int compLen = end - compStart;
                LZFChunk.appendCompressedHeader(inputLen, compLen, outputBuffer, outputPos);
                return end;
            }
        }
        // Otherwise append as non-compressed chunk instead (length + 5):
        return LZFChunk.appendNonCompressed(input, inputPtr, inputLen, outputBuffer, outputPos);
    }

    /**
     * Method similar to {@link #appendEncodedChunk}, but that will <b>only</b> append
     * encoded chunk if it compresses down to specified ratio (also considering header that
     * will be needed); otherwise will
     * return <code>-1</code> without appending anything.
     * 
     * @param maxResultRatio Value between 0.05 and 1.10 to indicate maximum relative size of
     *   the result to use, in order to append encoded chunk
     * 
     * @return Offset after appending compressed chunk, if compression produces compact
     *    enough chunk; otherwise -1 to indicate that no compression resulted.
     */
    public int appendEncodedIfCompresses(final byte[] input, double maxResultRatio,
            final int inputPtr, final int inputLen,
            final byte[] outputBuffer, final int outputPos)
    {
        if (inputLen >= MIN_BLOCK_TO_COMPRESS) {
            final int compStart = outputPos + LZFChunk.HEADER_LEN_COMPRESSED;
            final int end = tryCompress(input, inputPtr, inputPtr+inputLen, outputBuffer, compStart);
            final int maxSize = (int) (maxResultRatio * inputLen + LZFChunk.HEADER_LEN_COMPRESSED + 0.5);

            if (end <= (outputPos + maxSize)) { // yes, compressed enough, let's do this!
                final int compLen = end - compStart;
                LZFChunk.appendCompressedHeader(inputLen, compLen, outputBuffer, outputPos);
                return end;
            }
        }
        return -1;
    }

    /**
     * Method for encoding individual chunk, writing it to given output stream.
     */
    public void encodeAndWriteChunk(byte[] data, int offset, int len, OutputStream out)
        throws IOException
    {
        if (len >= MIN_BLOCK_TO_COMPRESS) {
            // If we have non-trivial block, and can compress it by at least
            // 2 bytes (since header is 2 bytes longer), let's compress:
            int compEnd = tryCompress(data, offset, offset+len, _encodeBuffer, LZFChunk.HEADER_LEN_COMPRESSED);
            final int compLen = compEnd - LZFChunk.HEADER_LEN_COMPRESSED;
            if (compLen < (len-2)) { // yes, compressed block is smaller (consider header is 2 bytes longer)
                LZFChunk.appendCompressedHeader(len, compLen, _encodeBuffer, 0);
                out.write(_encodeBuffer, 0, compEnd);
                return;
            }
        }
        // Otherwise leave uncompressed:
        byte[] headerBuf = _headerBuffer;
        if (headerBuf == null) {
            _headerBuffer = headerBuf = new byte[LZFChunk.MAX_HEADER_LEN];
        }
        LZFChunk.writeNonCompressedHeader(len, out, headerBuf);
        out.write(data, offset, len);
    }

    /**
     * Method for encoding individual chunk, writing it to given output stream,
     * if (and only if!) it compresses enough.
     *
     * @return True if compression occurred and chunk was written; false if not.
     */
    public boolean encodeAndWriteChunkIfCompresses(byte[] data, int offset, int inputLen,
            OutputStream out, double resultRatio)
        throws IOException
    {
        if (inputLen >= MIN_BLOCK_TO_COMPRESS) {
            int compEnd = tryCompress(data, offset, offset+inputLen, _encodeBuffer, LZFChunk.HEADER_LEN_COMPRESSED);
            final int maxSize = (int) (resultRatio * inputLen + LZFChunk.HEADER_LEN_COMPRESSED + 0.5);
            if (compEnd <= maxSize) { // yes, down to small enough
                LZFChunk.appendCompressedHeader(inputLen, compEnd-LZFChunk.HEADER_LEN_COMPRESSED,
                        _encodeBuffer, 0);
                out.write(_encodeBuffer, 0, compEnd);
                return true;
            }
        }
        return false;
    }
    
	public BufferRecycler getBufferRecycler() {
		return _recycler;
	}

    /*
    ///////////////////////////////////////////////////////////////////////
    // Abstract methods for sub-classes
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Main workhorse method that will try to compress given chunk, and return
     * end position (offset to byte after last included byte).
     * Result will be "raw" encoded contents <b>without</b> chunk header information:
     * caller is responsible for prepending header, if it chooses to use encoded
     * data; it may also choose to instead create an uncompressed chunk.
     * 
     * @return Output pointer after handling content, such that <code>result - originalOutPost</code>
     *    is the actual length of compressed chunk (without header)
     */
    protected abstract int tryCompress(byte[] in, int inPos, int inEnd, byte[] out, int outPos);

    /*
    ///////////////////////////////////////////////////////////////////////
    // Shared helper methods
    ///////////////////////////////////////////////////////////////////////
     */

    protected final int hash(int h) {
        // or 184117; but this seems to give better hashing?
        return ((h * 57321) >> 9) & _hashModulo;
        // original lzf-c.c used this:
        //return (((h ^ (h << 5)) >> (24 - HLOG) - h*5) & _hashModulo;
        // but that didn't seem to provide better matches
    }
}
