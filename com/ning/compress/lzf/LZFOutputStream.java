package com.ning.compress.lzf;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.WritableByteChannel;

import com.ning.compress.BufferRecycler;
import com.ning.compress.lzf.util.ChunkEncoderFactory;

/**
 * Decorator {@link OutputStream} implementation that will compress
 * output using LZF compression algorithm, given uncompressed input
 * to write. Its counterpart is {@link LZFInputStream}; although
 * in some ways {@link LZFCompressingInputStream} can be seen
 * as the opposite.
 *
 * @author jon hartlaub
 * @author Tatu Saloranta
 *
 * @see LZFInputStream
 * @see LZFCompressingInputStream
 */
public class LZFOutputStream extends FilterOutputStream implements WritableByteChannel
{
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = LZFChunk.MAX_CHUNK_LEN;

    private final ChunkEncoder _encoder;
    private final BufferRecycler _recycler;

    protected byte[] _outputBuffer;
    protected int _position = 0;

    /**
     * Configuration setting that governs whether basic 'flush()' should
     * first complete a block or not.
     *<p>
     * Default value is 'true'
     */
    protected boolean _cfgFinishBlockOnFlush = true;

    /**
     * Flag that indicates if we have already called '_outputStream.close()'
     * (to avoid calling it multiple times)
     */
    protected boolean _outputStreamClosed;

    /*
    ///////////////////////////////////////////////////////////////////////
    // Construction, configuration
    ///////////////////////////////////////////////////////////////////////
     */

    public LZFOutputStream(final OutputStream outputStream)
    {
        this(ChunkEncoderFactory.optimalInstance(DEFAULT_OUTPUT_BUFFER_SIZE), outputStream);
    }

    public LZFOutputStream(final ChunkEncoder encoder, final OutputStream outputStream)
    {
        this(encoder, outputStream, DEFAULT_OUTPUT_BUFFER_SIZE, encoder._recycler);
    }

    public LZFOutputStream(final OutputStream outputStream, final BufferRecycler bufferRecycler)
    {
        this(ChunkEncoderFactory.optimalInstance(bufferRecycler), outputStream, bufferRecycler);
    }

    public LZFOutputStream(final ChunkEncoder encoder, final OutputStream outputStream, final BufferRecycler bufferRecycler)
    {
        this(encoder, outputStream, DEFAULT_OUTPUT_BUFFER_SIZE, bufferRecycler);
    }

    public LZFOutputStream(final ChunkEncoder encoder, final OutputStream outputStream,
			               final int bufferSize, BufferRecycler bufferRecycler)
    {
        super(outputStream);
        _encoder = encoder;
		if (bufferRecycler==null) {
			bufferRecycler = _encoder._recycler;
		}
        _recycler = bufferRecycler;
        _outputBuffer = bufferRecycler.allocOutputBuffer(bufferSize);
        _outputStreamClosed = false;
    }

    /**
     * Method for defining whether call to {@link #flush} will also complete
     * current block (similar to calling {@link #finishBlock()}) or not.
     */
    public LZFOutputStream setFinishBlockOnFlush(boolean b) {
        _cfgFinishBlockOnFlush = b;
        return this;
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // OutputStream impl
    ///////////////////////////////////////////////////////////////////////
     */

    @Override
    public void write(final int singleByte) throws IOException
    {
        checkNotClosed();
        if (_position >= _outputBuffer.length) {
            writeCompressedBlock();
        }
        _outputBuffer[_position++] = (byte) singleByte;
    }

    @Override
    public void write(final byte[] buffer, int offset, int length) throws IOException
    {
        checkNotClosed();

        final int BUFFER_LEN = _outputBuffer.length;

        // simple case first: empty _outputBuffer and "big" input buffer: write first full blocks, if any, without copying
        while (_position == 0 && length >= BUFFER_LEN) {
            _encoder.encodeAndWriteChunk(buffer, offset, BUFFER_LEN, out);
            offset += BUFFER_LEN;
            length -= BUFFER_LEN;
        }

        // simple case first: buffering only (for trivially short writes)
        int free = BUFFER_LEN - _position;
        if (free > length) {
            System.arraycopy(buffer, offset, _outputBuffer, _position, length);
            _position += length;
            return;
        }
        // otherwise, copy whatever we can, flush
        System.arraycopy(buffer, offset, _outputBuffer, _position, free);
        offset += free;
        length -= free;
        _position += free;
        writeCompressedBlock();

        // then write intermediate full blocks, if any, without copying:
        while (length >= BUFFER_LEN) {
            _encoder.encodeAndWriteChunk(buffer, offset, BUFFER_LEN, out);
            offset += BUFFER_LEN;
            length -= BUFFER_LEN;
        }

        // and finally, copy leftovers in buffer, if any
        if (length > 0) {
            System.arraycopy(buffer, offset, _outputBuffer, 0, length);
        }
        _position = length;
    }

    public void write(final InputStream in) throws IOException {
        writeCompressedBlock(); // will flush _outputBuffer
        int read;
        while ((read = in.read(_outputBuffer)) >= 0) {
            _position = read;
            writeCompressedBlock();
        }
    }

    public void write(final FileChannel in) throws IOException {
        MappedByteBuffer src = in.map(MapMode.READ_ONLY, 0, in.size());
        write(src);
    }

    @Override
    public synchronized int write(final ByteBuffer src) throws IOException {
        int r = src.remaining();
        if (r <= 0) {
            return r;
        }
        writeCompressedBlock(); // will flush _outputBuffer
        if (src.hasArray()) {
            // direct compression from backing array
            write(src.array(), src.arrayOffset(), src.limit() - src.arrayOffset());
        } else {
            // need to copy to heap array first
            while (src.hasRemaining()) {
                int toRead = Math.min(src.remaining(), _outputBuffer.length);
                src.get(_outputBuffer, 0, toRead);
                _position = toRead;
                writeCompressedBlock();
            }
        }
        return r;
    }

    @Override
    public void flush() throws IOException
    {
        checkNotClosed();
        if (_cfgFinishBlockOnFlush && _position > 0) {
            writeCompressedBlock();
        }
        super.flush();
    }

    @Override
    public boolean isOpen() {
        return ! _outputStreamClosed;
    }

    @Override
    public void close() throws IOException
    {
        if (!_outputStreamClosed) {
            if (_position > 0) {
                writeCompressedBlock();
            }
            super.close(); // will flush beforehand
            _encoder.close();
            _outputStreamClosed = true;
            byte[] buf = _outputBuffer;
            if (buf != null) {
                _outputBuffer = null;
                _recycler.releaseOutputBuffer(buf);
            }
        }
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Additional public methods
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to find underlying {@link OutputStream} that
     * we write encoded LZF encoded data into, after compressing it.
     * Will never return null; although underlying stream may be closed
     * (if this stream has been closed).
     */
    public OutputStream getUnderlyingOutputStream() {
        return out;
    }

    /**
     * Accessor for checking whether call to "flush()" will first finish the
     * current block or not.
     */
    public boolean getFinishBlockOnFlush() {
        return _cfgFinishBlockOnFlush;
    }

    /**
     * Method that can be used to force completion of the current block,
     * which means that all buffered data will be compressed into an
     * LZF block. This typically results in lower compression ratio
     * as larger blocks compress better; but may be necessary for
     * network connections to ensure timely sending of data.
     */
    public LZFOutputStream finishBlock() throws IOException
    {
        checkNotClosed();
        if (_position > 0) {
            writeCompressedBlock();
        }
        return this;
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Compress and write the current block to the OutputStream
     */
    protected void writeCompressedBlock() throws IOException
    {
        int left = _position;
        _position = 0;
        int offset = 0;

        while (left > 0) {
            int chunkLen = Math.min(LZFChunk.MAX_CHUNK_LEN, left);
            _encoder.encodeAndWriteChunk(_outputBuffer, offset, chunkLen, out);
            offset += chunkLen;
            left -= chunkLen;
        }
    }

    protected void checkNotClosed() throws IOException
    {
        if (_outputStreamClosed) {
            throw new IOException(getClass().getName()+" already closed");
        }
    }
}
