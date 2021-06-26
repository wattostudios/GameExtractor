package com.ning.compress.lzf.parallel;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ning.compress.lzf.LZFChunk;

/**
 * Decorator {@link OutputStream} implementation that will compress
 * output using LZF compression algorithm, given uncompressed input
 * to write. Its counterpart is {@link com.ning.compress.lzf.LZFInputStream}; although
 * in some ways {@link com.ning.compress.lzf.LZFCompressingInputStream} can be seen
 * as the opposite.
 * <p>
 * This class uses a parallel implementation to make use of all available cores,
 * modulo system load.
 *
 * @author Tatu Saloranta
 * @author C&eacute;drik Lime
 *
 * @see com.ning.compress.lzf.LZFInputStream
 * @see com.ning.compress.lzf.LZFCompressingInputStream
 * @see com.ning.compress.lzf.LZFOutputStream
 */
public class PLZFOutputStream extends FilterOutputStream implements WritableByteChannel
{
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = LZFChunk.MAX_CHUNK_LEN;

    protected byte[] _outputBuffer;
    protected int _position = 0;

    /**
     * Flag that indicates if we have already called '_outputStream.close()'
     * (to avoid calling it multiple times)
     */
    protected boolean _outputStreamClosed;

    private BlockManager blockManager;
    private final ExecutorService compressExecutor;
    private final ExecutorService writeExecutor;
    volatile Exception writeException = null;


    /*
    ///////////////////////////////////////////////////////////////////////
    // Construction, configuration
    ///////////////////////////////////////////////////////////////////////
     */

    public PLZFOutputStream(final OutputStream outputStream) {
        this(outputStream, DEFAULT_OUTPUT_BUFFER_SIZE, getNThreads());
    }

    protected PLZFOutputStream(final OutputStream outputStream, int nThreads) {
        this(outputStream, DEFAULT_OUTPUT_BUFFER_SIZE, nThreads);
    }

    protected PLZFOutputStream(final OutputStream outputStream, final int bufferSize, int nThreads) {
        super(outputStream);
        _outputStreamClosed = false;
        compressExecutor = new ThreadPoolExecutor(nThreads, nThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()); // unbounded
        ((ThreadPoolExecutor)compressExecutor).allowCoreThreadTimeOut(true);
        writeExecutor = Executors.newSingleThreadExecutor(); // unbounded
        blockManager = new BlockManager(nThreads * 2, bufferSize); // this is where the bounds will be enforced!
        _outputBuffer = blockManager.getBlockFromPool();
    }

    protected static int getNThreads() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        OperatingSystemMXBean jmx = ManagementFactory.getOperatingSystemMXBean();

        if (jmx != null) {
            int loadAverage = (int) jmx.getSystemLoadAverage();
            if (nThreads > 1 && loadAverage >= 1) {
                nThreads = Math.max(1, nThreads - loadAverage);
            }
        }
        return nThreads;
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // OutputStream impl
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * {@inheritDoc}
     * WARNING: using this method will lead to very poor performance!
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

        // then write intermediate full blocks, if any:
        while (length >= BUFFER_LEN) {
            System.arraycopy(buffer, offset, _outputBuffer, 0, BUFFER_LEN);
            _position = BUFFER_LEN;
            writeCompressedBlock();
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


    /**
     * This <code>flush</code> method does nothing.
     */
    @Override
    public void flush() throws IOException
    {
        checkNotClosed();
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
            byte[] buf = _outputBuffer;
            if (buf != null) {
                assert _position == 0;
                blockManager.releaseBlockToPool(_outputBuffer);
                _outputBuffer = null;
            }
            writeExecutor.shutdown();
            try {
                writeExecutor.awaitTermination(1, TimeUnit.HOURS);
                // at this point compressExecutor should have no pending tasks: cleanup ThreadLocal's
                // we don't know how many threads; go to the max for now. This will change once we get a proper configuration bean.
                int maxThreads = Runtime.getRuntime().availableProcessors();
                Collection<CompressTask> cleanupTasks = new ArrayList<CompressTask>(maxThreads);
                for (int i = 0; i < maxThreads; ++i) {
                    cleanupTasks.add(new CompressTask(null, -1, -1, null));
                }
                compressExecutor.invokeAll(cleanupTasks);
                compressExecutor.shutdown();
                compressExecutor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new IOException(e);
            } finally {
                super.flush();
                super.close();
                _outputStreamClosed = true;
                compressExecutor.shutdownNow();
                writeExecutor.shutdownNow();
                blockManager = null;
                checkWriteException();
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
        if (_position == 0) {
            return;
        }
        Future<LZFChunk> lzfFuture = compressExecutor.submit(new CompressTask(_outputBuffer, 0, _position, blockManager));
        writeExecutor.execute(new WriteTask(out, lzfFuture, this));
        _outputBuffer = blockManager.getBlockFromPool();
        _position = 0;
        checkWriteException();
    }

    protected void checkWriteException() throws IOException {
        if (writeException != null) {
            IOException ioe = (writeException instanceof IOException) ? (IOException) writeException : new IOException(writeException);
            writeException = null;
            throw ioe;
        }
    }

    protected void checkNotClosed() throws IOException
    {
        if (_outputStreamClosed) {
            throw new IOException(getClass().getName()+" already closed");
        }
    }
}
