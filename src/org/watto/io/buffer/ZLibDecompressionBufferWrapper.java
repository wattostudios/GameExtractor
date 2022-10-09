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

import java.util.zip.InflaterInputStream;
import org.watto.ErrorLogger;
import org.watto.io.stream.ManipulatorBufferInputStream;

/***********************************************************************************************
A class that sits between a <code>ManipulatorBuffer</code> and a <code>Manipulator</code> class.
When reading from the <code>ManipulatorBuffer</code>, the data is decompressed with ZLib
compression before being passed back to the reading class.
***********************************************************************************************/
public class ZLibDecompressionBufferWrapper implements ManipulatorBuffer {

  /** the buffer **/
  ManipulatorBuffer buffer;

  /** the stream used for decompression **/
  InflaterInputStream decompressionStream;

  /***********************************************************************************************
  Wraps this class around a <code>buffer</code>, and assigns the <code>xorValue</code>
  @param buffer the <code>ManipulatorBuffer</code> that reads and writes the data
  ***********************************************************************************************/
  public ZLibDecompressionBufferWrapper(ManipulatorBuffer buffer) {
    this.buffer = buffer;
    this.decompressionStream = new InflaterInputStream(new ManipulatorBufferInputStream(buffer));
  }

  /***********************************************************************************************
  Is there more data available to read from the <i>decompressionStream</i>?
  @return <b>true</b>  if there is at least 1 byte of data remaining in the <i>decompressionStream</i><br />
          <b>false</b> if the end of the stream has been reached
  ***********************************************************************************************/
  public boolean available() {
    try {
      return decompressionStream.available() > 0;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return false;
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be read from the buffer. If not, the buffer
  is moved forward and re-filled to allow <code>length</code> bytes to be read.
  @param length the length of data to be read from the buffer
  ***********************************************************************************************/
  public void checkFill(int length) {
    //buffer.checkFill(length);
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be written to the buffer. If not, the buffer
  is written to disk and cleared out, to allow <code>length</code> bytes to be written.
  @param length the length of data to be written to the buffer
  ***********************************************************************************************/
  public void checkWrite(int length) {
    //buffer.checkWrite(length);
  }

  /***********************************************************************************************
  Closes the file. If the file is writable, it performs a forceWrite() to flush the buffer to disk.
  ***********************************************************************************************/
  public void close() {
    try {
      decompressionStream.close();
      //buffer.close();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Flushes out the buffer and refills it by reading from the file
  ***********************************************************************************************/
  public void fill() {
    //buffer.fill();
  }

  /***********************************************************************************************
  Empties the buffer, discarding all data in it.
  ***********************************************************************************************/
  public void flush() {
    //buffer.flush();
  }

  /***********************************************************************************************
  Writes all the buffered data to disk, and flushes the buffer.
  ***********************************************************************************************/
  public void forceWrite() {
    //buffer.forceWrite();
  }

  /***********************************************************************************************
  Copies <code>length</code> bytes of data from the buffer, and returns it. This does not move
  any file pointers.
  @param length the length of data to copy
  @return the data from the buffer
  ***********************************************************************************************/
  public byte[] getBuffer(int length) {
    /*
    try {
      byte[] bytes = buffer.getBuffer(length);
      for (int i = 0;i < bytes.length;i++) {
        bytes[i] ^= xorValue;
      }
      return bytes;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
    */
    return null;
  }

  /***********************************************************************************************
  Gets the position of the pointer in the buffer
  @return the pointer position in the buffer
  ***********************************************************************************************/
  public int getBufferLevel() {
    return buffer.getBufferLevel();
  }

  /***********************************************************************************************
  Gets the size of the buffer
  @return the size of the buffer
  ***********************************************************************************************/
  public int getBufferSize() {
    return buffer.getBufferSize();
  }

  /***********************************************************************************************
  Gets the current position in this file. Data will be read or written from this point.
  @return the current position in the file
  ***********************************************************************************************/
  public long getPointer() {
    return buffer.getPointer();
  }

  /***********************************************************************************************
  Is this buffer open for reading or writing?
  @return true if the buffer is open, false otherwise
  ***********************************************************************************************/
  public boolean isOpen() {
    return buffer.isOpen();
  }

  /***********************************************************************************************
  Gets the length of the file
  @return the length of the file
  ***********************************************************************************************/
  public long length() {
    return buffer.length();
  }

  /***********************************************************************************************
  Reads a single byte from the buffer
  @return the byte
  ***********************************************************************************************/
  public int read() {
    try {
      return decompressionStream.read();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return 0;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public int peek() {
    return 0;
  }

  /***********************************************************************************************
  Reads a number of bytes from the buffer into the <code>destination</code> array
  @param destination the array that data is read in to
  @return the number of bytes that were read into the array
  ***********************************************************************************************/
  public int read(byte[] destination) {
    try {
      return decompressionStream.read(destination);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return 0;
  }

  /***********************************************************************************************
  Reads <code>length</code> bytes of data from the buffer into the <code>offset</code> position
  in the <code>destination</code> array
  @param destination the array that data is read in to
  @param offset the offset in the <code>destination</code> array where the data is read in to
  @param length the number of bytes to read into the array
  ***********************************************************************************************/
  public int read(byte[] destination, int offset, int length) {
    try {
      return decompressionStream.read(destination, offset, length);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return 0;
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. If the <code>offset</code> is in the buffer, it
  moves the buffer pointer rather than reloading the whole buffer
  @param offset the offset to seek to in the file
  @see seek(long)
  ***********************************************************************************************/
  public void relativeSeek(long offset) {
    //buffer.relativeSeek(offset);
  }

  /***********************************************************************************************
  Gets the number of bytes left to read in the file. In other words, the length between the
  current pointer and the end of the file
  @return the number of bytes remaining
  ***********************************************************************************************/
  public long remainingLength() {
    return buffer.remainingLength();
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. The whole buffer is flushed and re-read from the
  new <code>offset</code>, so is inefficient for small jumps.
  @param offset the offset to seek to in the file
  @see relativeSeek(long)
  ***********************************************************************************************/
  public void seek(long offset) {
    //buffer.seek(offset);
  }

  /***********************************************************************************************
  Sets the size of the buffer.
  <br><br>
  <b>WARNING:</b> If this file is opened as <i>writable</i>, this method will <code>forceWrite()</code>
   any data in the buffer to the current position in the file. If you don't want this, make sure
   you <code>flush()</code> before running this method.
   @param length the new length of the buffer
  ***********************************************************************************************/
  public void setBufferSize(int length) {
    //buffer.setBufferSize(length);
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
  public void setLength(long length) {
    //buffer.setLength(length);
  }

  /***********************************************************************************************
  Skips over <code>length</code> bytes in the buffer
  @param length the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  public int skip(int length) {
    try {
      return (int) decompressionStream.skip(length);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return 0;
  }

  /***********************************************************************************************
  Writes an array of data into the buffer
  @param source the data to write to the buffer
  ***********************************************************************************************/
  public void write(byte[] source) {
    /*
    for (int i = 0;i < source.length;i++) {
      source[i] ^= xorValue;
    }
    buffer.write(source);
    */
  }

  /***********************************************************************************************
  Writes <code>length</code> bytes of data from the <code>offset</code> in the <code>source</code>
  array into the buffer
  @param source the data to write to the buffer
  @param offset the offset in the <code>source</code> to start reading from
  @param length the length of data to write
  ***********************************************************************************************/
  public void write(byte[] source, int offset, int length) {
    /*
    for (int i = 0;i < length;i++) {
      source[offset + i] ^= xorValue;
    }
    buffer.write(source,offset,length);
    */
  }

  /***********************************************************************************************
  Writes a single byte of data to the buffer
  @param source the byte to write
  ***********************************************************************************************/
  public void write(int source) {
    //buffer.write(source ^ xorValue);
  }
}