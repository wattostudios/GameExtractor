/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.io.buffer;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_RGSSAD_RGSSAD;
import org.watto.io.converter.ByteConverter;

/***********************************************************************************************
Reading and Buffering data that comes from an <i>ExporterPlugin</i> data source. Used for thumbnail
previews, among other things.
***********************************************************************************************/
public class ExporterByteBuffer implements ManipulatorBuffer {

  ExporterPlugin exporter;

  Resource resource;

  /** The buffer size **/
  int bufferSize = 2048;

  /** The virtual pointer location in the file **/
  long filePointer = 0;

  /** The buffer **/
  byte[] buffer = new byte[bufferSize];

  /** The read position in the buffer **/
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

      //if (bufferLevel + length >= bufferSize) {
      if (bufferLevel + length > bufferSize) {

        // Move the remaining buffered data to the start of the buffer
        int remainingBufferSize = bufferSize - bufferLevel;
        System.arraycopy(buffer, bufferLevel, buffer, 0, remainingBufferSize);

        //fill the rest of the buffer with fresh data from the file
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
          filePointer++;
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
  Not Supported
  ***********************************************************************************************/
  @Override
  public void checkWrite(int length) {
    // Writing not supported
  }

  /***********************************************************************************************
  Closes the file
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
      filePointer++;
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
  Not Supported
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
  Gets the current position in this file. Data will be read from this point.
  @return the current position in the file
  ***********************************************************************************************/
  @Override
  public long getPointer() {
    try {

      if (bufferLevel < 0) {
        // not sure how it gets in to this situation, but sometimes the bufferLevel is like -6224859, so we need to cater for it here
        // so that it's use in seek() will hit the correct if/else statement (usually to close/open the move forward from the beginning)
        return filePointer - bufferSize;
      }
      return filePointer - bufferSize + bufferLevel;

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
  
  ***********************************************************************************************/
  @Override
  public int peek() {
    try {

      checkFill(1);

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
          checkFill(length);
          readLevel += sizeToRead;
        }

      }

      return requestedLength;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }
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

      /*
      long newPosition = offset - filePointer;
      
      if (newPosition >= 0 && newPosition < bufferSize) {
        bufferLevel = (int) newPosition;
      }
      else {
        seek(offset);
      }
      */

      seek(offset);

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
      // 3.16 put this here, as closing/opening isn't always that quick, so if we can shortcut it here by moving the buffer back a little, that's much better
      int difference = (int) (getPointer() - offset);
      if (difference <= bufferLevel) {
        bufferLevel -= difference;
      }
      else {
        // take the exporter right back to the beginning of the file
        //exporter.close();
        //exporter.open(resource);
        exporter.closeAndReopen(resource); // 3.14 implemented this so exporters that decompress the full file might retain it between reloads.
        fill(); // does a flush() as part of this.
      }
    }
    else if (offset > getPointer()) {
      // seeking to a later spot in the file, so just skip over some bytes
      int skipAmount = (int) (offset - getPointer());

      if (bufferLevel + skipAmount < bufferSize) {
        // the buffer already contains the data for this point - only a small skip
        bufferLevel += skipAmount;
      }
      else {
        // the buffer isn't big enough for the skip, so need to find the right place in the file, and read in more data

        // we have already read in some buffer - we don't need to read that again
        skipAmount -= (bufferSize - bufferLevel);

        // now we're at the end of the buffer, so read some actual data from the file, until we reach the offset
        for (int i = 0; i < skipAmount; i++) {
          boolean available = exporter.available();
          if (!available) {
          }
          else {
            exporter.read();
          }
          filePointer++;
        }

        // now we're at the right place, so fill the buffer
        fill();
      }

    }
    else {
      // seeking to an earlier spot in the file

      // see if we can just move the buffer back a little...
      int difference = (int) (getPointer() - offset);
      if (difference <= bufferLevel) {
        bufferLevel -= difference;
      }
      else {
        // nope, can't move back in the buffer, so re-open (start again at 0) then skip some bytes
        exporter.close();
        exporter.open(resource);

        filePointer = 0;
        bufferLevel = 0;

        // read to the right place
        for (int i = 0; i < offset; i++) {
          boolean available = exporter.available();
          if (!available) {
          }
          else {
            exporter.read();
          }
          filePointer++;
        }

        // fill the buffer
        fill();

      }
    }

  }

  /***********************************************************************************************
  Sets the size of the buffer.
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
  Not Supported
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

      int newBufferPos = length + bufferLevel;
      if (newBufferPos <= bufferSize) {
        // skip is within the existing buffer
        bufferLevel = newBufferPos;
      }
      else {
        // skip goes beyond the buffer, so we need to seek to a new offset, and fill the buffer 
        seek(getPointer() + length);
      }

      return length;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return 0;
    }
  }

  /***********************************************************************************************
  Not Supported
  ***********************************************************************************************/
  @Override
  public void write(byte[] source) {
    // Writing not supported
  }

  /***********************************************************************************************
  Not Supported
  ***********************************************************************************************/
  @Override
  public void write(byte[] source, int offset, int length) {
    // Writing not supported
  }

  /***********************************************************************************************
  Not Supported
  ***********************************************************************************************/
  @Override
  public void write(int source) {
    // Writing not supported
  }
}