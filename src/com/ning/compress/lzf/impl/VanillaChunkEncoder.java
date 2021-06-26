package com.ning.compress.lzf.impl;

import com.ning.compress.BufferRecycler;
import com.ning.compress.lzf.ChunkEncoder;
import com.ning.compress.lzf.LZFChunk;

public class VanillaChunkEncoder
    extends ChunkEncoder
{
    /**
     * @param totalLength Total encoded length; used for calculating size
     *   of hash table to use
     */
    public VanillaChunkEncoder(int totalLength) {
        super(totalLength);
    }

    /**
     * Alternate constructor used when we want to avoid allocation encoding
     * buffer, in cases where caller wants full control over allocations.
     */
    protected VanillaChunkEncoder(int totalLength, boolean bogus) {
        super(totalLength, bogus);
    }

    /**
     * @param totalLength Total encoded length; used for calculating size
     *   of hash table to use
	 * @param bufferRecycler The BufferRecycler instance
     */
    public VanillaChunkEncoder(int totalLength, BufferRecycler bufferRecycler) {
        super(totalLength, bufferRecycler);
    }

    /**
     * Alternate constructor used when we want to avoid allocation encoding
     * buffer, in cases where caller wants full control over allocations.
     */
    protected VanillaChunkEncoder(int totalLength, BufferRecycler bufferRecycler, boolean bogus) {
        super(totalLength, bufferRecycler, bogus);
    }

    public static VanillaChunkEncoder nonAllocatingEncoder(int totalLength) {
        return new VanillaChunkEncoder(totalLength, true);
    }
    
    public static VanillaChunkEncoder nonAllocatingEncoder(int totalLength, BufferRecycler bufferRecycler) {
        return new VanillaChunkEncoder(totalLength, bufferRecycler, true);
    }
    
    /*
    ///////////////////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Main workhorse method that will try to compress given chunk, and return
     * end position (offset to byte after last included byte)
     * 
     * @return Output pointer after handling content, such that <code>result - originalOutPost</code>
     *    is the actual length of compressed chunk (without header)
     */
    @Override
    protected int tryCompress(byte[] in, int inPos, int inEnd, byte[] out, int outPos)
    {
        final int[] hashTable = _hashTable;
        ++outPos; // To leave one byte for literal-length indicator
        int seen = first(in, inPos); // past 4 bytes we have seen... (last one is LSB)
        int literals = 0;
        inEnd -= TAIL_LENGTH;
        final int firstPos = inPos; // so that we won't have back references across block boundary
        
        while (inPos < inEnd) {
            byte p2 = in[inPos + 2];
            // next
            seen = (seen << 8) + (p2 & 255);
            int off = hash(seen);
            int ref = hashTable[off];
            hashTable[off] = inPos;
  
            // First expected common case: no back-ref (for whatever reason)
            if (ref >= inPos // can't refer forward (i.e. leftovers)
                    || (ref < firstPos) // or to previous block
                    || (off = inPos - ref) > MAX_OFF
                    || in[ref+2] != p2 // must match hash
                    || in[ref+1] != (byte) (seen >> 8)
                    || in[ref] != (byte) (seen >> 16)) {
                out[outPos++] = in[inPos++];
                literals++;
                if (literals == LZFChunk.MAX_LITERAL) {
                    out[outPos - 33] = (byte) 31; // <= out[outPos - literals - 1] = MAX_LITERAL_MINUS_1;
                    literals = 0;
                    outPos++; // To leave one byte for literal-length indicator
                }
                continue;
            }
            // match
            int maxLen = inEnd - inPos + 2;
            if (maxLen > MAX_REF) {
                maxLen = MAX_REF;
            }
            if (literals == 0) {
                outPos--; // We do not need literal length indicator, go back
            } else {
                out[outPos - literals - 1] = (byte) (literals - 1);
                literals = 0;
            }
            int len = 3;
            // find match length
            while (len < maxLen && in[ref + len] == in[inPos + len]) {
                len++;
            }
            len -= 2;
            --off; // was off by one earlier
            if (len < 7) {
                out[outPos++] = (byte) ((off >> 8) + (len << 5));
            } else {
                out[outPos++] = (byte) ((off >> 8) + (7 << 5));
                out[outPos++] = (byte) (len - 7);
            }
            out[outPos++] = (byte) off;
            outPos++;
            inPos += len;
            seen = first(in, inPos);
            seen = (seen << 8) + (in[inPos + 2] & 255);
            hashTable[hash(seen)] = inPos;
            ++inPos;
            seen = (seen << 8) + (in[inPos + 2] & 255); // hash = next(hash, in, inPos);
            hashTable[hash(seen)] = inPos;
            ++inPos;
        }
        // try offlining the tail
        return _handleTail(in, inPos, inEnd+4, out, outPos, literals);
    }

    private final int _handleTail(byte[] in, int inPos, int inEnd, byte[] out, int outPos,
            int literals)
    {
        while (inPos < inEnd) {
            out[outPos++] = in[inPos++];
            literals++;
            if (literals == LZFChunk.MAX_LITERAL) {
                out[outPos - literals - 1] = (byte) (literals - 1);
                literals = 0;
                outPos++;
            }
        }
        out[outPos - literals - 1] = (byte) (literals - 1);
        if (literals == 0) {
            outPos--;
        }
        return outPos;
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////////////
     */

    private final int first(byte[] in, int inPos) {
        return (in[inPos] << 8) + (in[inPos + 1] & 0xFF);
    }
}
