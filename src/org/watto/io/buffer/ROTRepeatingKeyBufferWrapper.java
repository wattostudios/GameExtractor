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
import org.watto.io.converter.ByteConverter;

/***********************************************************************************************
A class that sits between a <code>ManipulatorBuffer</code> and a <code>Manipulator</code> class.
Each byte is ROTd by a value as they are read or written from the <code>ManipulatorBuffer</code>.
***********************************************************************************************/
public class ROTRepeatingKeyBufferWrapper implements ManipulatorBuffer {

  /** the buffer **/
  ManipulatorBuffer buffer;

  int[] rotKey = new int[0];

  public int getCurrentKeyPos() {
    return currentKeyPos;
  }

  public void setCurrentKeyPos(int currentKeyPos) {
    this.currentKeyPos = currentKeyPos;
  }

  int keyLength = 0;

  int currentKeyPos = 0;

  /***********************************************************************************************
  Wraps this class around a <code>buffer</code>, and assigns the <code>xorValue</code>
  @param buffer the <code>ManipulatorBuffer</code> that reads and writes the data
  @param xorValue the value to XOR against
  ***********************************************************************************************/
  public ROTRepeatingKeyBufferWrapper(ManipulatorBuffer buffer, int[] rotKey) {
    this.buffer = buffer;
    this.rotKey = rotKey;
    this.keyLength = rotKey.length;
    this.currentKeyPos = 0;
  }

  /***********************************************************************************************
  Wraps this class around a <code>buffer</code>, and assigns the <code>xorValue</code>
  @param buffer the <code>ManipulatorBuffer</code> that reads and writes the data
  @param xorValue the value to XOR against
  ***********************************************************************************************/
  public ROTRepeatingKeyBufferWrapper(ManipulatorBuffer buffer, int[] rotKey, int keyPos) {
    this.buffer = buffer;
    this.rotKey = rotKey;
    this.keyLength = rotKey.length;
    this.currentKeyPos = keyPos;
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be read from the buffer. If not, the buffer
  is moved forward and re-filled to allow <code>length</code> bytes to be read.
  @param length the length of data to be read from the buffer
  ***********************************************************************************************/
  @Override
  public void checkFill(int length) {
    buffer.checkFill(length);
  }

  /***********************************************************************************************
  Checks to see whether <code>length</code> bytes can be written to the buffer. If not, the buffer
  is written to disk and cleared out, to allow <code>length</code> bytes to be written.
  @param length the length of data to be written to the buffer
  ***********************************************************************************************/
  @Override
  public void checkWrite(int length) {
    buffer.checkWrite(length);
  }

  /***********************************************************************************************
  Closes the file. If the file is writable, it performs a forceWrite() to flush the buffer to disk.
  ***********************************************************************************************/
  @Override
  public void close() {
    buffer.close();
  }

  /***********************************************************************************************
  Flushes out the buffer and refills it by reading from the file
  ***********************************************************************************************/
  @Override
  public void fill() {
    buffer.fill();
  }

  /***********************************************************************************************
  Empties the buffer, discarding all data in it.
  ***********************************************************************************************/
  @Override
  public void flush() {
    buffer.flush();
  }

  /***********************************************************************************************
  Writes all the buffered data to disk, and flushes the buffer.
  ***********************************************************************************************/
  @Override
  public void forceWrite() {
    buffer.forceWrite();
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
      byte[] bytes = buffer.getBuffer(length);
      for (int i = 0; i < bytes.length; i++) {
        int byteValue = ByteConverter.unsign(bytes[i]) + rotKey[currentKeyPos++];
        if (byteValue < 0) {
          byteValue = 256 + byteValue;
        }
        else if (byteValue >= 256) {
          byteValue -= 256;
        }
        bytes[i] = (byte) byteValue;
        
        if (currentKeyPos >= keyLength) {
          currentKeyPos = 0;
        }
      }
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
    return buffer.getBufferLevel();
  }

  /***********************************************************************************************
  Gets the size of the buffer
  @return the size of the buffer
  ***********************************************************************************************/
  @Override
  public int getBufferSize() {
    return buffer.getBufferSize();
  }

  /***********************************************************************************************
  Gets the current position in this file. Data will be read or written from this point.
  @return the current position in the file
  ***********************************************************************************************/
  @Override
  public long getPointer() {
    return buffer.getPointer();
  }

  /***********************************************************************************************
  Is this buffer open for reading or writing?
  @return true if the buffer is open, false otherwise
  ***********************************************************************************************/
  @Override
  public boolean isOpen() {
    return buffer.isOpen();
  }

  /***********************************************************************************************
  Gets the length of the file
  @return the length of the file
  ***********************************************************************************************/
  @Override
  public long length() {
    return buffer.length();
  }

  /***********************************************************************************************
  Reads a single byte from the buffer
  @return the byte
  ***********************************************************************************************/
  @Override
  public int peek() {
    int value = buffer.read();

    int byteValue = value + rotKey[currentKeyPos++];
    if (byteValue < 0) {
      byteValue = 256 + byteValue;
    }
    else if (byteValue >= 256) {
      byteValue -= 256;
    }
    int returnByte = byteValue;
    
    if (currentKeyPos >= keyLength) {
      currentKeyPos = 0;
    }
    return returnByte;
  }

  /***********************************************************************************************
  Reads a single byte from the buffer
  @return the byte
  ***********************************************************************************************/
  @Override
  public int read() {
    int value = buffer.read();
    //if (value == -1) {
    //  return value;
    //}

    int byteValue = value + rotKey[currentKeyPos++];
    if (byteValue < 0) {
      byteValue = 256 + byteValue;
    }
    else if (byteValue >= 256) {
      byteValue -= 256;
    }
    int returnByte =  byteValue;
    
    if (currentKeyPos >= keyLength) {
      currentKeyPos = 0;
    }
    return returnByte;
  }

  /***********************************************************************************************
  Reads a number of bytes from the buffer into the <code>destination</code> array
  @param destination the array that data is read in to
  @return the number of bytes that were read into the array
  ***********************************************************************************************/
  @Override
  public int read(byte[] destination) {
    int length = buffer.read(destination);
    for (int i = 0; i < length; i++) {
      
      int byteValue = ByteConverter.unsign(destination[i]) + rotKey[currentKeyPos++];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      else if (byteValue >= 256) {
        byteValue -= 256;
      }
      destination[i] = (byte) byteValue;
      
      if (currentKeyPos >= keyLength) {
        currentKeyPos = 0;
      }
    }
    return length;
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
    int bufferLength = buffer.read(destination, offset, length);
    for (int i = 0; i < bufferLength; i++) {
      
      int byteValue = ByteConverter.unsign(destination[offset+i]) + rotKey[currentKeyPos++];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      else if (byteValue >= 256) {
        byteValue -= 256;
      }
      destination[offset+i] = (byte) byteValue;
      
      if (currentKeyPos >= keyLength) {
        currentKeyPos = 0;
      }
    }
    return bufferLength;
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. If the <code>offset</code> is in the buffer, it
  moves the buffer pointer rather than reloading the whole buffer
  @param offset the offset to seek to in the file
  @see seek(long)
  ***********************************************************************************************/
  @Override
  public void relativeSeek(long offset) {
    buffer.relativeSeek(offset);
  }

  /***********************************************************************************************
  Gets the number of bytes left to read in the file. In other words, the length between the
  current pointer and the end of the file
  @return the number of bytes remaining
  ***********************************************************************************************/
  @Override
  public long remainingLength() {
    return buffer.remainingLength();
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the file. The whole buffer is flushed and re-read from the
  new <code>offset</code>, so is inefficient for small jumps.
  @param offset the offset to seek to in the file
  @see relativeSeek(long)
  ***********************************************************************************************/
  @Override
  public void seek(long offset) {
    buffer.seek(offset);
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
    buffer.setBufferSize(length);
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
    buffer.setLength(length);
  }

  /***********************************************************************************************
  Skips over <code>length</code> bytes in the buffer
  @param length the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  @Override
  public int skip(int length) {
    // Move the keypos
    if (currentKeyPos + length <= keyLength) {
      currentKeyPos += length;
      if (currentKeyPos >= keyLength) {
        currentKeyPos = 0;
      }
    }
    else {
      int remainingLength = length;

      // add the amount to fill the remaining keyLength
      int frontPiece = keyLength - currentKeyPos;
      remainingLength -= frontPiece;

      // Now work out how much overflow when we remove all the keyLength multiples from the length
      int backPiece = remainingLength % keyLength;
      currentKeyPos = backPiece;
    }

    // Now move the buffer
    return buffer.skip(length);
  }

  /***********************************************************************************************
  Writes an array of data into the buffer
  @param source the data to write to the buffer
  ***********************************************************************************************/
  @Override
  public void write(byte[] source) {
    for (int i = 0; i < source.length; i++) {
      
      int byteValue = ByteConverter.unsign(source[i]) + rotKey[currentKeyPos++];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      else if (byteValue >= 256) {
        byteValue -= 256;
      }
      source[i] = (byte) byteValue;
      
      if (currentKeyPos >= keyLength) {
        currentKeyPos = 0;
      }
    }
    buffer.write(source);
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
    for (int i = 0; i < length; i++) {
      
      int byteValue = ByteConverter.unsign(source[offset+i]) + rotKey[currentKeyPos++];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      else if (byteValue >= 256) {
        byteValue -= 256;
      }
      source[offset+i] = (byte) byteValue;
      
      if (currentKeyPos >= keyLength) {
        currentKeyPos = 0;
      }
    }
    buffer.write(source, offset, length);
  }

  /***********************************************************************************************
  Writes a single byte of data to the buffer
  @param source the byte to write
  ***********************************************************************************************/
  @Override
  public void write(int source) {
    
    int byteValue = source + rotKey[currentKeyPos++];
    if (byteValue < 0) {
      byteValue = 256 + byteValue;
    }
    else if (byteValue >= 256) {
      byteValue -= 256;
    }
    
    buffer.write(byteValue);
    if (currentKeyPos >= keyLength) {
      currentKeyPos = 0;
    }
  }
}