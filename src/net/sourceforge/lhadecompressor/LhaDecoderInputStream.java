/*
	LzhDecoderInputStream.java
	
	package		lha
	class		LzhDecompressInputStream
	
	Copyright (c) 2001-2008 Nobuyasu SUEHIRO All Rights Reserved.
 */

package net.sourceforge.lhadecompressor;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Checksum;

/**
 * This class implements an input stream for reading datas with lha
 * decoder.<br>
 * 
 * Supports decompression methods are lhd, lh0, lh1, lh4, lh5, lh6, lh7, lz4, lz5, lzs.<br>
 * 
 * This class is NOT THREAD SAFE.<br>
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class LhaDecoderInputStream extends InputStream {

  final private static int SIZE_SKIP_BUFFER = 512;

  protected LhaDecoder decoder;

  protected LhaEntry entry;

  protected Checksum crc;

  protected byte[] skipBuffer;

  protected long entryCount;

  /**
   * Creates a new lha input stream.
   * 
   * @param in
   *            the actual input stream
   * @param entry
   *            the lha entry of current input stream
   */
  public LhaDecoderInputStream(InputStream in, LhaEntry entry) throws LhaException,
      IOException {
    this.entry = entry;
    this.crc = new CRC16();
    this.skipBuffer = new byte[SIZE_SKIP_BUFFER];

    this.entryCount = entry.getOriginalSize();
    this.decoder = createDecoder(in, this.entryCount, entry.getMethod());

    crc.reset();
  }

  public LhaDecoderInputStream(InputStream in, long decompLength, String method) throws LhaException,
      IOException {
    this.crc = new CRC16();
    this.skipBuffer = new byte[SIZE_SKIP_BUFFER];

    this.entryCount = decompLength;
    this.decoder = createDecoder(in, this.entryCount, method);

    crc.reset();
  }

  /**
   * Gets the checksum of input stream.
   * 
   * @return the checksum of input stream
   */
  public Checksum getChecksum() {
    return (crc);
  }

  /**
   * Returns 0 after EOF has reached for the current entry data, otherwise
   * always return 1. Programs should not count on this method to return the
   * actual number of bytes that could be read without blocking.
   * 
   * @return 1 before EOF and 0 after EOF has reached for input stream.
   * @throws IOException
   *             if an I/O error has occurred
   */
  public int available() throws IOException {
    if (decoder == null) {
      return (0);
    }

    return (entryCount > 0 ? 1 : 0);
  }

  /**
   * Reads from the input stream.
   * 
   * @param pos
   *            the offset in lha file
   * @return the next byte of data, or <code>-1</code> if the end of the
   *         stream is reached
   * @throws IOException
   *             if an I/O error has occurred
   */
  public int read() throws IOException {
    if (decoder == null) {
      return (-1);
    }

    byte[] b = new byte[1];
    int n = decoder.read(b, 0, 1);
    if (n > 0) {
      crc.update(b[0]);
      --entryCount;
    }

    return (b[0]);
  }

  /**
   * Reads from the input stream into an array of bytes.
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
    if (decoder == null) {
      return (-1);
    }

    int n = decoder.read(b, off, len);
    if (n > 0) {
      crc.update(b, off, n);
      entryCount -= n;
    }

    return (n);
  }

  /**
   * Returns 0 after EOF has reached for the input stream, otherwise always
   * return 1. Programs should not count on this method to return the actual
   * number of bytes that could be read without blocking.
   * 
   * @param n
   *            the number of bytes to skip
   * @return the actual number of bytes skipped
   * @throws IOException
   *             if an I/O error has occurred
   */
  public long skip(long n) throws IOException {
    if (n <= 0) {
      return (0);
    }

    if (n > Integer.MAX_VALUE) {
      n = Integer.MAX_VALUE;
    }
    int total = 0;
    while (total < n) {
      int len = (int) n - total;
      if (len > skipBuffer.length) {
        len = skipBuffer.length;
      }

      len = read(skipBuffer, 0, len);
      if (len == -1) {
        break;
      }
      else {
        crc.update(skipBuffer, 0, len);
      }

      total += len;
    }

    entryCount -= total;
    return (total);
  }

  /**
   * Closes the input stream.
   * 
   * @throws IOException
   *             if an I/O error has occured
   */
  public void close() throws IOException {
    if (decoder != null) {
      decoder.close();
      decoder = null;
      entry = null;
      crc = null;
    }
  }

  /**
   * Reads out the current input stream, check crc, closes the current input
   * stream.
   * 
   * @throws IOException
   *             if an I/O error has occured
   */
  public void closeEntry() throws LhaException, IOException {
    long skipCount = skip(entryCount);
    if (entryCount != skipCount) {
      //throw new LhaException("Data length not matched");
    }

    if ((entry.hasCRC()) && (entry.getCRC() != crc.getValue())) {
      //throw new LhaException("Data crc is not matched");
    }

    close();
  }

  /**
   * Creates a new decoder for input stream.
   */
  private static LhaDecoder createDecoder(InputStream in, long originalSize, String method) throws LhaException, IOException {
    if (method.equals(LhaEntry.METHOD_SIG_LHD)) {
      return (new LhdDecoder());
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH0)) {
      return (new NocompressDecoder(in, originalSize));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH1)) {
      return (new Lh1Decoder(in, originalSize));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH2)) {
      throw (new LhaException("Unsupported method: " + method));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH3)) {
      throw (new LhaException("Unsupported method: " + method));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH4)) {
      return (new Lh4Decoder(in, originalSize, 12, 14, 4));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH5)) {
      return (new Lh4Decoder(in, originalSize, 13, 14, 4));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH6)) {
      return (new Lh4Decoder(in, originalSize, 15, 16, 5));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LH7)) {
      return (new Lh4Decoder(in, originalSize, 16, 17, 5));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LZS)) {
      return (new LzsDecoder(in, originalSize));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LZ4)) {
      return (new NocompressDecoder(in, originalSize));
    }
    else if (method.equals(LhaEntry.METHOD_SIG_LZ5)) {
      return (new Lz5Decoder(in, originalSize));
    }

    throw (new LhaException("Unknown method: " + method));
  }
}