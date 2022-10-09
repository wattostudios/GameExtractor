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

/***********************************************************************************************
 A class that reads, writes, and buffers data from a source. Usually the source is a
 <code>File</code> in the file system.
***********************************************************************************************/
public interface ManipulatorBuffer {

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be read from the buffer. If not, the buffer
  is moved forward and re-filled to allow <code>length</code> bytes to be read.
  @param length the length of data to be read from the buffer
  ***********************************************************************************************/
  public void checkFill(int length);

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be written to the buffer. If not, the buffer
  is written to disk and cleared out, to allow <code>length</code> bytes to be written.
  @param length the length of data to be written to the buffer
  ***********************************************************************************************/
  public void checkWrite(int length);

  /***********************************************************************************************
  Closes the file. If the file is writable, it performs a forceWrite() to flush the buffer to disk.
  ***********************************************************************************************/
  public void close();

  /***********************************************************************************************
  Flushes out the buffer and refills it by reading from the file
  ***********************************************************************************************/
  public void fill();

  /***********************************************************************************************
  Empties the buffer, discarding all data in it.
  ***********************************************************************************************/
  public void flush();

  /***********************************************************************************************
  Writes all the buffered data to disk, and flushes the buffer.
  ***********************************************************************************************/
  public void forceWrite();

  /***********************************************************************************************
   Reads a single byte from the buffer, but doesn't increment any file pointers
   @return the byte at the current point in the buffer
   ***********************************************************************************************/
  public int peek();

  /***********************************************************************************************
  Copies <code>length</code> bytes of data from the buffer, and returns it. This does not move
  any file pointers.
  @param length the length of data to copy
  @return the data from the buffer
  ***********************************************************************************************/
  public byte[] getBuffer(int length);

  /***********************************************************************************************
  Gets the position of the pointer in the buffer
  @return the pointer position in the buffer
  ***********************************************************************************************/
  public int getBufferLevel();

  /***********************************************************************************************
  Gets the size of the buffer
  @return the size of the buffer
  ***********************************************************************************************/
  public int getBufferSize();

  /***********************************************************************************************
  Gets the current position in this file. Data will be read or written from this point.
  @return the current position in the file
  ***********************************************************************************************/
  public long getPointer();

  /***********************************************************************************************
  Is this buffer open for reading or writing?
  @return true if the buffer is open, false otherwise
  ***********************************************************************************************/
  public boolean isOpen();

  /***********************************************************************************************
  Gets the length of the file
  @return the length of the file
  ***********************************************************************************************/
  public long length();

  /***********************************************************************************************
  Reads a single byte from the buffer
  @return the byte
  ***********************************************************************************************/
  public int read();

  /***********************************************************************************************
  Reads a number of bytes from the buffer into the <code>destination</code> array
  @param destination the array that data is read in to
  @return the number of bytes that were read into the array
  ***********************************************************************************************/
  public int read(byte[] destination);

  /***********************************************************************************************
  Reads <code>length</code> bytes of data from the buffer into the <code>offset</code> position
  in the <code>destination</code> array
  @param destination the array that data is read in to
  @param offset the offset in the <code>destination</code> array where the data is read in to
  @param length the number of bytes to read into the array
  ***********************************************************************************************/
  public int read(byte[] destination, int offset, int length);

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. If the <code>offset</code> is in the buffer, it
  moves the buffer pointer rather than reloading the whole buffer
  @param offset the offset to seek to in the file
  @see seek(long)
  ***********************************************************************************************/
  public void relativeSeek(long offset);

  /***********************************************************************************************
  Gets the number of bytes left to read in the file. In other words, the length between the
  current pointer and the end of the file
  @return the number of bytes remaining
  ***********************************************************************************************/
  public long remainingLength();

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. The whole buffer is flushed and re-read from the
  new <code>offset</code>, so is inefficient for small jumps.
  @param offset the offset to seek to in the file
  @see relativeSeek(long)
  ***********************************************************************************************/
  public void seek(long offset);

  /***********************************************************************************************
  Sets the size of the buffer.
  <br><br>
  <b>WARNING:</b> If this file is opened as <i>writable</i>, this method will <code>forceWrite()</code>
   any data in the buffer to the current position in the file. If you don't want this, make sure
   you <code>flush()</code> before running this method.
   @param length the new length of the buffer
  ***********************************************************************************************/
  public void setBufferSize(int length);

  /***********************************************************************************************
  Sets the length of the file. If the file is smaller than this length, the file size is increased.
  If the file is larger than this size, the file size is not changed.
  <br>
  <i>Note:</i> Setting the length does not force the file to be this length, it only sets the 
  minimum size of the file. If you write data past the length of the file, the file will become
  longer.
  @param length the new length of the file 
  ***********************************************************************************************/
  public void setLength(long length);

  /***********************************************************************************************
  Skips over <code>length</code> bytes in the buffer
  @param length the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  public int skip(int length);

  /***********************************************************************************************
  Writes an array of data into the buffer
  @param source the data to write to the buffer
  ***********************************************************************************************/
  public void write(byte[] source);

  /***********************************************************************************************
  Writes <code>length</code> bytes of data from the <code>offset</code> in the <code>source</code>
  array into the buffer
  @param source the data to write to the buffer
  @param offset the offset in the <code>source</code> to start reading from
  @param length the length of data to write
  ***********************************************************************************************/
  public void write(byte[] source, int offset, int length);

  /***********************************************************************************************
  Writes a single byte of data to the buffer
  @param source the byte to write
  ***********************************************************************************************/
  public void write(int source);
}