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
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_RGSSAD_RGSSAD;

/***********************************************************************************************
A class that reads, writes, and buffers data from a <code>byte[]</code> data source.
***********************************************************************************************/
public class ExporterByteBuffer implements ManipulatorBuffer {

  ExporterPlugin exporter;

  Resource resource;

  /** The buffer size **/
  int bufferSize = 2048;

  /** whether the buffer is writable or not **/
  boolean writable = false;

  /** The virtual pointer location in the file **/
  long filePointer = 0;

  /** The buffer **/
  byte[] buffer = new byte[bufferSize];

  /** The read and write position in the buffer **/
  int bufferLevel = 0;

  /***********************************************************************************************
  Constructor for extended classes
  ***********************************************************************************************/
  protected ExporterByteBuffer() {
  }

  /***********************************************************************************************
  Creates the buffer for the data <code>array</code>
  @param array the data to buffer
  ***********************************************************************************************/
  public ExporterByteBuffer(Resource resource) {
    this.resource = resource;

    this.exporter = resource.getExporter();

    /*
    // V3.10 this setting is no longer relevant, as we now have a Task_QuickBMSBulkExport which is really fast 
    if (this.exporter instanceof Exporter_QuickBMS_Decompression && !(Settings.getBoolean("AllowQuickBMSThumbnails"))) {
      // Exclude this exporter from being used for thumbnail generation
      this.exporter = Exporter_Default.getInstance();
    }
    */

    if (exporter instanceof Exporter_Custom_RGSSAD_RGSSAD) {
      if (resource.getExtension().equals("png")) {
        bufferSize = (int) resource.getDecompressedLength();
        buffer = new byte[bufferSize];
      }
    }

    exporter.open(resource);

    fill();
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be read from the buffer. If not, the buffer
  is moved forward and re-filled to allow <code>length</code> bytes to be read.
  @param length the length of data to be read from the buffer
  ***********************************************************************************************/
  @Override
  public void checkFill(int length) {
    try {

      if (bufferLevel + length >= bufferSize) {
        filePointer += bufferLevel;
        //seek(filePointer);

        // Move the remaining buffered data to the start of the buffer
        int remainingBufferSize = bufferSize - bufferLevel;
        System.arraycopy(buffer, bufferLevel, buffer, 0, remainingBufferSize);
        // fill the rest of the buffer with fresh data from the file

        while (remainingBufferSize < bufferSize) {
          boolean available = exporter.available(); // important - some exporters only load the bytes from the file when calling this (eg ZLib_CompressedSizeOnly)
          if (!available) { // TODO 3.03 ADDED THIS IF-ELSE BIT - CHECK THUMBNAILS STILL WORK WELL!
            buffer[remainingBufferSize] = 0; // just nullify the rest of the buffer
            remainingBufferSize++;
          }
          else {
            buffer[remainingBufferSize] = (byte) exporter.read();
            remainingBufferSize++;
          }
        }

        // reset the bufferLevel
        bufferLevel = 0;
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be written to the buffer. If not, the buffer
  is written to disk and cleared out, to allow <code>length</code> bytes to be written.
  @param length the length of data to be written to the buffer
  ***********************************************************************************************/
  @Override
  public void checkWrite(int length) {
    // Writing not supported
  }

  /***********************************************************************************************
  Closes the file. If the file is writable, it performs a forceWrite() to flush the buffer to disk.
  ***********************************************************************************************/
  @Override
  public void close() {
    exporter.close();
  }

  /***********************************************************************************************
  Flushes out the buffer and refills it by reading from the file
  ***********************************************************************************************/
  @Override
  public void fill() {
    flush();

    for (int i = 0; i < bufferSize; i++) {
      boolean available = exporter.available(); // important - some exporters only load the bytes from the file when calling this (eg ZLib_CompressedSizeOnly)
      if (!available) { // TODO 3.03 ADDED THIS IF-ELSE BIT - CHECK THUMBNAILS STILL WORK WELL!
        buffer[i] = 0; // just nullify the rest of the buffer
      }
      else {
        buffer[i] = (byte) exporter.read();
      }
    }

  }

  /***********************************************************************************************
  Empties the buffer, discarding all data in it.
  ***********************************************************************************************/
  @Override
  public void flush() {
    buffer = new byte[bufferSize];
    bufferLevel = 0;
  }

  /***********************************************************************************************
  Writes all the buffered data to disk, and flushes the buffer.
  ***********************************************************************************************/
  @Override
  public void forceWrite() {
    // Writing not supported
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

      if (length > bufferSize) {
        return buffer;
      }
      else {
        byte[] smallBuffer = new byte[length];
        System.arraycopy(buffer, 0, smallBuffer, 0, length);
        return smallBuffer;
      }

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
    try {

      return filePointer + bufferLevel;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
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
    return resource.getDecompressedLength();
  }

  /***********************************************************************************************
  Reads a single byte from the buffer
  @return the byte
  ***********************************************************************************************/
  @Override
  public int read() {
    try {

      checkFill(1);

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
    try {

      // TODO 3.03 added all this bit, so that it returns a real value (at the end) instead of "0", and so that the
      // remaining length is returned if, for some reason, it requested more than that
      int requestedLength = length;
      long remainingLength = remainingLength();
      if (remainingLength < requestedLength) {
        if (remainingLength < 0) {
          remainingLength = -1; // -1 means EOF for some things like the JPEG viewer
        }
        requestedLength = (int) remainingLength;
      }

      checkFill(length);

      int readLevel = offset;

      if (length <= bufferSize) {
        System.arraycopy(buffer, bufferLevel, destination, readLevel, length);
        bufferLevel += length;
      }
      else {
        while (length > 0) {
          int sizeToRead = length;
          if (length > bufferSize) {
            sizeToRead = bufferSize;
          }
          System.arraycopy(buffer, bufferLevel, destination, readLevel, sizeToRead);
          bufferLevel += sizeToRead;

          length -= sizeToRead;
          checkFill(length); // TODO 3.03 changed from checkFill(sizeToRead); to checkFill(length); 
          readLevel += sizeToRead;
        }

      }

      //return length; // TODO 3.03 change to below
      return requestedLength;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }

    /*
    try {
      byte[] bytes = new byte[length];
    
      int readSize = 0;
      while (readSize < length) {
        //if (!exporter.available()) {
        //  break;
        //}
        bytes[readSize] = (byte) exporter.read();
        readSize++;
      }
    
      int copySize = length;
      if (readSize < copySize) {
        copySize = readSize;
      }
    
      System.arraycopy(bytes, 0, destination, offset, copySize);
      return copySize;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return 0;
    }
    */
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. If the <code>offset</code> is in the buffer, it
  moves the buffer pointer rather than reloading the whole buffer
  @param offset the offset to seek to in the file
  @see seek(long)
  ***********************************************************************************************/
  @Override
  public void relativeSeek(long offset) {
    try {

      long newPosition = offset - filePointer;

      if (newPosition >= 0 && newPosition < bufferSize) {
        bufferLevel = (int) newPosition;
      }
      else {
        seek(offset);
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Gets the number of bytes left to read in the file. In other words, the length between the
  current pointer and the end of the file
  @return the number of bytes remaining
  ***********************************************************************************************/
  @Override
  public long remainingLength() {
    return length() - getPointer();
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. The whole buffer is flushed and re-read from the
  new <code>offset</code>, so is inefficient for small jumps.
  @param offset the offset to seek to in the file
  @see relativeSeek(long)
  ***********************************************************************************************/
  @Override
  public void seek(long offset) {
    // ONLY "0" OR FORWARD-SEEKING IS AVAILABLE (NO BACKWARDS SEEKING OTHER THAN TO "0")
    if (offset == getPointer()) {
      // already at the right offset, so we don't need to do anything
      return;
    }
    else if (offset == filePointer) {
      // just go back to the start of the buffer
      bufferLevel = 0;
    }
    else if (offset == 0) {
      // take the exporter right back to the beginning of the file
      exporter.close();
      exporter.open(resource);
      fill(); // does a flush() as part of this.
    }
    else if (offset > getPointer()) {
      // seeking to a later spot in the file, so just skip over some bytes
      int skipAmount = (int) (offset - getPointer());

      if (bufferLevel + skipAmount < bufferSize) {
        bufferLevel += skipAmount; // the buffer already contains the data for this point - only a small skip
      }
      else {
        skipAmount = (int) (offset - (filePointer + bufferSize));

        // skip and discard the remaining skip amount
        for (int i = 0; i < skipAmount; i++) {
          //if (exporter.available()) {
          exporter.available(); // important - some exporters only load the bytes from the file when calling this (eg ZLib_CompressedSizeOnly)
          exporter.read();
          //}
          //else {
          //  break;
          //}
        }

        // move the filePointer to the new offset
        filePointer = offset;

        // now that we're at the right spot, fill the buffer
        fill(); // does a flush() as part of this.

      }
    }
    else {
      // seeking to an earlier spot in the file, so re-open (start again at 0) then skip some bytes
      exporter.close();
      exporter.open(resource);

      /*
      // v3.10 fixed this, because it doesn't increment the file pointer, so just do skip() instead (which works) and reset the globals.
      // Also don't want to fill(), as this will be handled properly on the next read()
      for (int i = 0; i < offset; i++) {
        //if (exporter.available()) {
        exporter.available(); // important - some exporters only load the bytes from the file when calling this (eg ZLib_CompressedSizeOnly)
        exporter.read();
        //}
        //else {
        //  break;
        //}
      }
      
      // now that we're at the right spot, fill the buffer
      fill(); // does a flush() as part of this.
      */
      filePointer = 0;
      bufferLevel = 0;
      for (int i = 0; i < offset; i++) {
        //if (exporter.available()) {
        read();
        //}
        //else {
        //  break;
        //}
      }

      //skip((int) offset);

    }

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
    try {

      bufferSize = length;
      flush();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
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
    // N/A
  }

  /***********************************************************************************************
  Skips over <code>length</code> bytes in the buffer
  @param length the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  @Override
  public int skip(int length) {
    try {

      int bufferPos = length + bufferLevel;
      if (bufferPos >= bufferSize) {
        seek(filePointer + bufferPos);
      }
      else {
        bufferLevel += length;
      }

      return length;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return 0;
    }
  }

  /***********************************************************************************************
  Writes an array of data into the buffer
  @param source the data to write to the buffer
  ***********************************************************************************************/
  @Override
  public void write(byte[] source) {
    // Writing not supported
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
    // Writing not supported
  }

  /***********************************************************************************************
  Writes a single byte of data to the buffer
  @param source the byte to write
  ***********************************************************************************************/
  @Override
  public void write(int source) {
    // Writing not supported
  }
}