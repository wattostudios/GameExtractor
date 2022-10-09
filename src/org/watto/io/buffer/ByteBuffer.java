////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.io.buffer;

import org.watto.ErrorLogger;
import org.watto.array.ArrayResizer;
import org.watto.io.converter.ByteConverter;

/***********************************************************************************************
A class that reads, writes, and buffers data from a <code>byte[]</code> data source.
***********************************************************************************************/
public class ByteBuffer implements ManipulatorBuffer {

  /** The size of the buffer **/
  int bufferSize = 0;

  /** The buffer **/
  byte[] buffer = new byte[0];

  /** The read position in the buffer **/
  int bufferLevel = 0;

  /***********************************************************************************************
  Constructor for extended classes
  ***********************************************************************************************/
  protected ByteBuffer() {
  }

  /***********************************************************************************************
  Creates the buffer for the data <code>array</code>
  @param array the data to buffer
  ***********************************************************************************************/
  public ByteBuffer(byte[] array) {
    this.buffer = array;
    this.bufferSize = buffer.length;
  }

  /***********************************************************************************************
  Creates the buffer of the given size
  @param bufferSize the size of the buffer array
  ***********************************************************************************************/
  public ByteBuffer(int bufferSize) {
    this.bufferSize = bufferSize;
    this.buffer = new byte[bufferSize];
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be read from the buffer. If not, the buffer
  is moved forward and re-filled to allow <code>length</code> bytes to be read.
  @param length the length of data to be read from the buffer
  ***********************************************************************************************/
  @Override
  public void checkFill(int length) {
    // N/A - the buffer is always fully available
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be written to the buffer. If not, the buffer
  is written to disk and cleared out, to allow <code>length</code> bytes to be written.
  @param length the length of data to be written to the buffer
  ***********************************************************************************************/
  @Override
  public void checkWrite(int length) {
    // N/A - the buffer is always fully available
  }

  /***********************************************************************************************
  Closes the file. If the file is writable, it performs a forceWrite() to flush the buffer to disk.
  ***********************************************************************************************/
  @Override
  public void close() {
    // N/A - the buffer is always fully available
  }

  /***********************************************************************************************
  Flushes out the buffer and refills it by reading from the file
  ***********************************************************************************************/
  @Override
  public void fill() {
    // N/A - the buffer is always fully available
  }

  /***********************************************************************************************
  Empties the buffer, discarding all data in it.
  ***********************************************************************************************/
  @Override
  public void flush() {
    // N/A - the buffer is always fully available
  }

  /***********************************************************************************************
  Writes all the buffered data to disk, and flushes the buffer.
  ***********************************************************************************************/
  @Override
  public void forceWrite() {
    // N/A - the buffer is always fully available
  }

  /***********************************************************************************************
  Gets a pointer to the buffer
  ***********************************************************************************************/
  public byte[] getBuffer() {
    return buffer;
  }

  /***********************************************************************************************
  Copies <code>length</code> bytes of data from the buffer, and returns it. This does not move
  any file pointers.
  @param length the length of data to copy
  @return the data from the buffer
  ***********************************************************************************************/
  @Override
  public byte[] getBuffer(int length) {
    try {
      int readSize = length;
      if (bufferLevel + length >= bufferSize) {
        readSize = bufferSize - bufferLevel;
      }

      byte[] bytes = new byte[readSize];
      System.arraycopy(buffer, bufferLevel, bytes, 0, readSize);
      return bytes;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /***********************************************************************************************
  Gets the position of the pointer in the buffer
  @return the pointer position in the buffer
  ***********************************************************************************************/
  @Override
  public int getBufferLevel() {
    return bufferLevel;
  }

  /***********************************************************************************************
  Gets the size of the buffer
  @return the size of the buffer
  ***********************************************************************************************/
  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  /***********************************************************************************************
  Gets the current position in this file. Data will be read or written from this point.
  @return the current position in the file
  ***********************************************************************************************/
  @Override
  public long getPointer() {
    return bufferLevel;
  }

  /***********************************************************************************************
  Increases the size of the buffer when writing
  @param increase the length to increase the buffer by
  ***********************************************************************************************/
  void increaseSize(int increase) {
    if (bufferLevel + increase > bufferSize) {
      buffer = ArrayResizer.resize(buffer, bufferLevel + increase);
      bufferSize = buffer.length;
    }
  }

  /***********************************************************************************************
  Is this buffer open for reading or writing?
  @return true if the buffer is open, false otherwise
  ***********************************************************************************************/
  @Override
  public boolean isOpen() {
    return true;
  }

  /***********************************************************************************************
  Gets the length of the file
  @return the length of the file
  ***********************************************************************************************/
  @Override
  public long length() {
    return bufferSize;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public int peek() {
    try {
      int readData = ByteConverter.unsign(buffer[bufferLevel]);
      return readData;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }
  }

  /***********************************************************************************************
  Reads a single byte from the buffer
  @return the byte
  ***********************************************************************************************/
  @Override
  public int read() {
    try {
      int readData = buffer[bufferLevel];
      bufferLevel++;
      return readData;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }
  }

  /***********************************************************************************************
  Reads a number of bytes from the buffer into the <code>destination</code> array
  @param destination the array that data is read in to
  @return the number of bytes that were read into the array
  ***********************************************************************************************/
  @Override
  public int read(byte[] destination) {
    return read(destination, 0, destination.length);
  }

  /***********************************************************************************************
  Reads <code>length</code> bytes of data from the buffer into the <code>offset</code> position
  in the <code>destination</code> array
  @param destination the array that data is read in to
  @param offset the offset in the <code>destination</code> array where the data is read in to
  @param length the number of bytes to read into the array
  ***********************************************************************************************/
  @Override
  public int read(byte[] destination, int offset, int length) {
    int remaining = (int) remainingLength();
    if (length > remaining) {
      length = remaining;
    }

    int readLevel = offset;

    System.arraycopy(buffer, bufferLevel, destination, readLevel, length);
    bufferLevel += length;

    return length;
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. If the <code>offset</code> is in the buffer, it
  moves the buffer pointer rather than reloading the whole buffer
  @param offset the offset to seek to in the file
  @see seek(long)
  ***********************************************************************************************/
  @Override
  public void relativeSeek(long offset) {
    seek(offset);
  }

  /***********************************************************************************************
  Gets the number of bytes left to read in the file. In other words, the length between the
  current pointer and the end of the file
  @return the number of bytes remaining
  ***********************************************************************************************/
  @Override
  public long remainingLength() {
    return bufferSize - bufferLevel;
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. The whole buffer is flushed and re-read from the
  new <code>offset</code>, so is inefficient for small jumps.
  @param offset the offset to seek to in the file
  @see relativeSeek(long)
  ***********************************************************************************************/
  @Override
  public void seek(long offset) {
    bufferLevel = (int) offset;
  }

  /***********************************************************************************************
  Sets the size of the buffer.
  <br><br>
  <b>WARNING:</b> If this file is opened as <i>writable</i>, this method will <code>forceWrite()</code>
   any data in the buffer to the current position in the file. If you don't want this, make sure
   you <code>flush()</code> before running this method.
   @param length the new length of the buffer
  ***********************************************************************************************/
  @Override
  public void setBufferSize(int length) {
    // N/A - the buffer always encompasses the whole String
  }

  /***********************************************************************************************
  Sets the length of the file. If the file is smaller than this length, the file size is increased.
  If the file is larger than this size, the file size is not changed.
  <br>
  <i>Note:</i> Setting the length does not force the file to be this length, it only sets the
  minimum size of the file. If you write data past the length of the file, the file will become
  longer.
  @param length the new length of the file
  ***********************************************************************************************/
  @Override
  public void setLength(long length) {
    buffer = ArrayResizer.resize(buffer, (int) length);
    if (bufferLevel >= length) {
      bufferLevel = (int) length - 1;
    }
  }

  /***********************************************************************************************
  Skips over <code>length</code> bytes in the buffer
  @param length the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  @Override
  public int skip(int length) {
    bufferLevel += length;
    return length;
  }

  /***********************************************************************************************
  Writes an array of data into the buffer
  @param source the data to write to the buffer
  ***********************************************************************************************/
  @Override
  public void write(byte[] source) {
    int writeSize = source.length;
    increaseSize(writeSize);
    System.arraycopy(source, 0, buffer, bufferLevel, writeSize);
    bufferLevel += writeSize;
  }

  /***********************************************************************************************
  Writes <code>length</code> bytes of data from the <code>offset</code> in the <code>source</code>
  array into the buffer
  @param source the data to write to the buffer
  @param offset the offset in the <code>source</code> to start reading from
  @param length the length of data to write
  ***********************************************************************************************/
  @Override
  public void write(byte[] source, int offset, int length) {
    increaseSize(length);
    System.arraycopy(source, offset, buffer, bufferLevel, length);
    bufferLevel += length;
  }

  /***********************************************************************************************
  Writes a single byte of data to the buffer
  @param source the byte to write
  ***********************************************************************************************/
  @Override
  public void write(int source) {
    increaseSize(1);
    buffer[bufferLevel] = (byte) source;
    bufferLevel++;
  }
}