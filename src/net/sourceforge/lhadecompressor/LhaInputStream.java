package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * This class implements an input stream filter for reading data in the lzh
 * file format stream.<br>
 * <p>
 * <strong>SAMPLE CODE:</strong> reads datas from lha file format stream.<br>
 * 
 * <pre>
 * InputStream in;
 * 
 * LzhInputStream lin = new LzhInputStream(in);
 * for (LzhEntry entry = lin.getNextEntry();
 *   entry != null;
 *   entry = lin.getNextEntry()) {
 * 
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class LhaInputStream extends FilterInputStream {
	private LhaDecoderInputStream dis;
	private String encoding;
	private LhaEntry entry;
	private boolean closed;

	/**
	 * Creates a new lha input stream.
	 * 
	 * @param in
	 *            the actual input stream
	 */
	public LhaInputStream(InputStream in) {
		this(in, LhaEntry.HD_STR_ENCODING);
	}

	/**
	 * Creates a new lha input stream.
	 * 
	 * @param in
	 *            the actual input stream
	 * @param encoding
	 *            character encoding name
	 */
	public LhaInputStream(InputStream in, String encoding) {
		super(in);

		this.encoding = encoding;
		this.dis = null;
		this.entry = null;
		this.closed = true;
	}

	/**
	 * Reads the next lha entry and positions stream at the beginning of the
	 * entry data.
	 * 
	 * @param name
	 *            the name of the entry
	 * @return the LzhEntry just read
	 * @throws LhaException
	 *             if a lha format error has occurred
	 * @throws IOException
	 *             if an I/O error has occured
	 */
	public LhaEntry getNextEntry() throws LhaException, IOException {
		if (entry != null)
			dis.closeEntry();

		LhaEntryReader hr = new LhaEntryReader(encoding) {
			protected int _read(byte[] b) throws IOException {
				return (in.read(b));
			}
		};

		entry = hr.readHeader();

		if (entry != null) {
			dis = new LhaDecoderInputStream(in, entry);
			closed = false;
		} else {
			dis = null;
			closed = true;
		}

		return (entry);
	}

	/**
	 * Closes the current input stream.
	 * 
	 * @throws IOException
	 *             if an I/O error has occured
	 */
	final private void ensureOpen() throws IOException {
		if (closed)
			throw (new IOException("Stream closed"));
	}

	/**
	 * Returns 0 after EOF has reached for the current input stream, otherwise
	 * always return 1. Programs should not count on this method to return the
	 * actual number of bytes that could be read without blocking.
	 * 
	 * @return 1 before EOF and 0 after EOF has reached for input stream.
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	public int available() throws IOException {
		ensureOpen();

		return (dis.available());
	}

	/**
	 * Reads from the current input stream.
	 * 
	 * @param pos
	 *            the offset in lha file
	 * @return the next byte of data, or <code>-1</code> if the end of the
	 *         stream is reached
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	public int read() throws IOException {
		ensureOpen();

		return (dis.read());
	}

	/**
	 * Reads from the current input stream into an array of bytes.
	 * 
	 * @param pos
	 *            the offset in lha file
	 * @param b
	 *            the buffer into which the data is read
	 * @param off
	 *            the start offset in array <code>b</code> at which the data
	 *            is written
	 * @param len
	 *            the maximum number of bytes to read
	 * @return the total number of bytes read into the buffer, or
	 *         <code>-1</code> if there is no more data because the end of the
	 *         stream has been reached
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		ensureOpen();

		return (dis.read(b, off, len));
	}

	/**
	 * Returns 0 after EOF has reached for the current input stream, otherwise
	 * always return 1. Programs should not count on this method to return the
	 * actual number of bytes that could be read without blocking.
	 * 
	 * @param n
	 *            the number of bytes to skip
	 * @return the actual number of bytes skipped
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	public long skip(long n) throws IOException {
		ensureOpen();

		return (dis.skip(n));
	}

	/**
	 * Closes the current input stream.
	 * 
	 * @throws IOException
	 *             if an I/O error has occured
	 */
	public void close() throws IOException {
		if (dis != null) {
			closed = true;

			dis.close();
			dis = null;
		}

		super.close();
	}
}