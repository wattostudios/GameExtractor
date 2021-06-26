package com.ning.compress.lzf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ning.compress.BufferRecycler;
import com.ning.compress.lzf.util.ChunkDecoderFactory;

/**
 * Decorator {@link InputStream} implementation used for reading compressed data
 * and uncompressing it on the fly, such that reads return uncompressed
 * data. Its direct counterpart is {@link LZFOutputStream}; but there is
 * also {@link LZFCompressingInputStream} which does reverse of this class.
 * 
 * @author Tatu Saloranta
 * 
 * @see com.ning.compress.lzf.util.LZFFileInputStream
 * @see com.ning.compress.lzf.LZFCompressingInputStream
 */
public class LZFInputStream extends InputStream
{
    /**
     * Underlying decoder in use.
     */
    protected final ChunkDecoder _decoder;
    
    /**
     * Object that handles details of buffer recycling
     */
    protected final BufferRecycler _recycler;

    /**
     * stream to be decompressed
     */
    protected final InputStream _inputStream;

    /**
     * Flag that indicates if we have already called 'inputStream.close()'
     * (to avoid calling it multiple times)
     */
    protected boolean _inputStreamClosed;
    
    /**
     * Flag that indicates whether we force full reads (reading of as many
     * bytes as requested), or 'optimal' reads (up to as many as available,
     * but at least one). Default is false, meaning that 'optimal' read
     * is used.
     */
    protected boolean _cfgFullReads = false;
	
    /**
     * the current buffer of compressed bytes (from which to decode)
     * */
    protected byte[] _inputBuffer;
	
    /**
     * the buffer of uncompressed bytes from which content is read
     * */
    protected byte[] _decodedBytes;
	
    /**
     * The current position (next char to output) in the uncompressed bytes buffer.
     */
    protected int _bufferPosition = 0;
    
    /**
     * Length of the current uncompressed bytes buffer
     */
    protected int _bufferLength = 0;

    /*
    ///////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////
     */

    public LZFInputStream(final InputStream inputStream) throws IOException
    {
        this(inputStream, false);
    }

    public LZFInputStream(final ChunkDecoder decoder, final InputStream in)
        throws IOException
    {
        this(decoder, in, BufferRecycler.instance(), false);
    }
    
    /**
     * @param in Underlying input stream to use
     * @param fullReads Whether {@link #read(byte[])} should try to read exactly
     *   as many bytes as requested (true); or just however many happen to be
     *   available (false)
     */
    public LZFInputStream(final InputStream in, boolean fullReads) throws IOException
    {
        this(ChunkDecoderFactory.optimalInstance(), in, BufferRecycler.instance(), fullReads);
    }

    public LZFInputStream(final ChunkDecoder decoder, final InputStream in, boolean fullReads)
        throws IOException
    {
        this(decoder, in, BufferRecycler.instance(), fullReads);
    }

    public LZFInputStream(final InputStream inputStream, final BufferRecycler bufferRecycler) throws IOException
    {
        this(inputStream, bufferRecycler, false);
    }

    /**
     * @param in Underlying input stream to use
     * @param fullReads Whether {@link #read(byte[])} should try to read exactly
     *   as many bytes as requested (true); or just however many happen to be
     *   available (false)
	 * @param bufferRecycler Buffer recycler instance, for usages where the
	 *   caller manages the recycler instances
     */
    public LZFInputStream(final InputStream in, final BufferRecycler bufferRecycler, boolean fullReads) throws IOException
    {
        this(ChunkDecoderFactory.optimalInstance(), in, bufferRecycler, fullReads);
    }

	public LZFInputStream(final ChunkDecoder decoder, final InputStream in, final BufferRecycler bufferRecycler, boolean fullReads)
        throws IOException
    {
        super();
        _decoder = decoder;
        _recycler = bufferRecycler;
        _inputStream = in;
        _inputStreamClosed = false;
        _cfgFullReads = fullReads;

        _inputBuffer = bufferRecycler.allocInputBuffer(LZFChunk.MAX_CHUNK_LEN);
        _decodedBytes = bufferRecycler.allocDecodeBuffer(LZFChunk.MAX_CHUNK_LEN);
    }

    /**
     * Method that can be used define whether reads should be "full" or
     * "optimal": former means that full compressed blocks are read right
     * away as needed, optimal that only smaller chunks are read at a time,
     * more being read as needed.
     */
    public void setUseFullReads(boolean b) {
        _cfgFullReads = b;
    }
    
    /*
    ///////////////////////////////////////////////////////////////////////
    // InputStream impl
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Method is overridden to report number of bytes that can now be read
     * from decoded data buffer, without reading bytes from the underlying
     * stream.
     * Never throws an exception; returns number of bytes available without
     * further reads from underlying source; -1 if stream has been closed, or
     * 0 if an actual read (and possible blocking) is needed to find out.
     */
    @Override
    public int available()
    {
        if (_inputStreamClosed) { // javadocs suggest 0 for closed as well (not -1)
            return 0;
        }
        int left = (_bufferLength - _bufferPosition);
        return (left <= 0) ? 0 : left;
    }
    
    @Override
    public int read() throws IOException
    {
        if (!readyBuffer()) {
            return -1;
        }
        return _decodedBytes[_bufferPosition++] & 255;
    }
	
    @Override
    public int read(final byte[] buffer) throws IOException
    {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(final byte[] buffer, int offset, int length) throws IOException
    {
        if (length < 1) {
            return 0;
        }
        if (!readyBuffer()) {
            return -1;
        }
        // First let's read however much data we happen to have...
        int chunkLength = Math.min(_bufferLength - _bufferPosition, length);
        System.arraycopy(_decodedBytes, _bufferPosition, buffer, offset, chunkLength);
        _bufferPosition += chunkLength;

        if (chunkLength == length || !_cfgFullReads) {
            return chunkLength;
        }
        // Need more data, then
        int totalRead = chunkLength;
        do {
            offset += chunkLength;
            if (!readyBuffer()) {
                break;
            }
            chunkLength = Math.min(_bufferLength - _bufferPosition, (length - totalRead));
            System.arraycopy(_decodedBytes, _bufferPosition, buffer, offset, chunkLength);
            _bufferPosition += chunkLength;
            totalRead += chunkLength;
        } while (totalRead < length);
        return totalRead;
    }
    
    @Override
    public void close() throws IOException
    {
        _bufferPosition = _bufferLength = 0;
        byte[] buf = _inputBuffer;
        if (buf != null) {
            _inputBuffer = null;
            _recycler.releaseInputBuffer(buf);
        }
        buf = _decodedBytes;
        if (buf != null) {
            _decodedBytes = null;
            _recycler.releaseDecodeBuffer(buf);
        }
        if (!_inputStreamClosed) {
            _inputStreamClosed = true;
            _inputStream.close();
        }
    }

    /**
     * Overridden to implement efficient skipping by skipping full chunks whenever
     * possible.
     */
    @Override
    public long skip(long n) throws IOException
    {
        if (_inputStreamClosed) {
            return -1;
        }
        if (n <= 0L) {
            return n;
        }
        long skipped;

        // if any left to skip, just return that for simplicity
        if (_bufferPosition < _bufferLength) {
            int left = (_bufferLength - _bufferPosition);
            if (n <= left) { // small skip, fulfilled from what we already got
                _bufferPosition += (int) n;
                return n;
            }
            _bufferPosition = _bufferLength;
            skipped = left;
            n -= left;
        } else {
            skipped = 0L;
        }
        // and then full-chunk skipping, if possible
        while (true) {
            int amount = _decoder.skipOrDecodeChunk(_inputStream, _inputBuffer, _decodedBytes, n);
            if (amount >= 0) { // successful skipping of the chunk
                skipped += amount;

                n -= amount;
                if (n <= 0L) {
                    return skipped;
                }
                continue;
            }
            if (amount == -1) { // EOF
                close();
                return skipped;
            }
            // decoded buffer-full, more than max skip
            _bufferLength = -(amount+1);
            skipped += n;
            _bufferPosition = (int) n;
            return skipped;
        }
    }
    
    /*
    ///////////////////////////////////////////////////////////////////////
    // Extended public API
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to find underlying {@link InputStream} that
     * we read from to get LZF encoded data to decode.
     * Will never return null; although underlying stream may be closed
     * (if this stream has been closed).
     */
    public InputStream getUnderlyingInputStream() {
        return _inputStream;
    }

    /**
     * Method that can be called to discard any already buffered input, read
     * from input source.
     * Specialized method that only makes sense if the underlying {@link InputStream}
     * can be repositioned reliably.
     */
    public void discardBuffered()
    {
        _bufferPosition = _bufferLength = 0;
    }

    /**
     * Convenience method that will read and uncompress all data available,
     * and write it using given {@link OutputStream}. This avoids having to
     * make an intermediate copy of uncompressed data which would be needed
     * when doing the same manually.
     * 
     * @param out OutputStream to use for writing content
     * 
     * @return Number of bytes written (uncompressed)
     */
    public int readAndWrite(OutputStream out) throws IOException
    {
        int total = 0;

        while (readyBuffer()) {
            int avail = _bufferLength - _bufferPosition;
            out.write(_decodedBytes, _bufferPosition, avail);
            _bufferPosition += avail; // to ensure it looks like we consumed it all
            total += avail;
        }
        return total;
    }
    
    /*
    ///////////////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Fill the uncompressed bytes buffer by reading the underlying inputStream.
     * 
     * @throws IOException
     * 
     * @return True if there is now at least one byte to read in the buffer; false
     *   if there is no more content to read
     */
    protected boolean readyBuffer() throws IOException
    {
        if (_bufferPosition < _bufferLength) {
            return true;
        }
        if (_inputStreamClosed) {
            return false;
        }
        _bufferLength = _decoder.decodeChunk(_inputStream, _inputBuffer, _decodedBytes);
        if (_bufferLength < 0) {
            close();
            return false;
        }
        _bufferPosition = 0;
        return (_bufferPosition < _bufferLength);
    }
}
