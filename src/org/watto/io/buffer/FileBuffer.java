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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.watto.ErrorLogger;
import org.watto.io.DirectoryBuilder;
import org.watto.io.FilenameChecker;
import org.watto.io.converter.ByteConverter;

/***********************************************************************************************
 * A class that sits between the file system and a <code>Manipulator</code> class. Performs read,
 * write, and buffering.
 ***********************************************************************************************/
public class FileBuffer implements ManipulatorBuffer {

  /** The buffer size **/
  int bufferSize = 2048;

  /** whether the buffer is writable or not **/
  boolean writable = false;

  /** The virtual pointer location in the file **/
  long filePointer = 0;

  /** The interface to read and write from the file **/
  RandomAccessFile raf;

  /** The buffer **/
  byte[] buffer = new byte[bufferSize];

  /** The read and write position in the buffer **/
  int bufferLevel = 0;

  /** The file being manipulated **/
  File file;

  /***********************************************************************************************
   * Opens the <code>file</code>
   * @param file the <code>File</code> to open
   * @param writable whether the buffer allows writing or not
   ***********************************************************************************************/
  public FileBuffer(File file, boolean writable) {
    this(file, writable, 2048);
  }

  /***********************************************************************************************
   * Opens the <code>file</code>, and sets the <code>bufferSize</code>
   * @param file the <code>File</code> to open
   * @param writable whether the buffer allows writing or not
   * @param bufferSize the size of the buffer
   ***********************************************************************************************/
  public FileBuffer(File file, boolean writable, int bufferSize) {
    try {

      this.writable = writable;
      this.bufferSize = bufferSize;

      if (writable) {
        // remove funny characters from the filename
        this.file = FilenameChecker.correctFilename(file);

        // build the directory if it doesn't exist
        DirectoryBuilder.buildDirectory(file, false);

        // create and open the file
        raf = new RandomAccessFile(file, "rw");
      }
      else {
        this.file = file;
        raf = new RandomAccessFile(file, "r");
      }

      buffer = new byte[bufferSize];

      if (!writable) {
        fill();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Checks to see whether <code>length</code> bytes can be read from the buffer. If not, the
   * buffer is moved forward and re-filled to allow <code>length</code> bytes to be read.
   * @param length the length of data to be read from the buffer
   ***********************************************************************************************/
  @Override
  public void checkFill(int length) {
    try {

      if (bufferLevel + length >= bufferSize) {
        filePointer += bufferLevel;
        seek(filePointer);
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Checks to see whether <code>length</code> bytes can be written to the buffer. If not, the
   * buffer is written to disk and cleared out, to allow <code>length</code> bytes to be written.
   * @param length the length of data to be written to the buffer
   ***********************************************************************************************/
  @Override
  public void checkWrite(int length) {
    try {

      if (bufferLevel + length >= bufferSize) {
        forceWrite();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Closes the file. If the file is writable, it performs a forceWrite() to flush the buffer to
   * disk.
   ***********************************************************************************************/
  @Override
  public void close() {
    try {

      if (writable) {
        forceWrite();
      }

      raf.close();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Flushes out the buffer and refills it by reading from the file
   ***********************************************************************************************/
  @Override
  public void fill() {
    try {

      flush();
      raf.read(buffer);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Empties the buffer, discarding all data in it.
   ***********************************************************************************************/
  @Override
  public void flush() {
    buffer = new byte[bufferSize];
    bufferLevel = 0;
  }

  /***********************************************************************************************
   * Writes all the buffered data to disk, and flushes the buffer.
   ***********************************************************************************************/
  @Override
  public void forceWrite() {
    try {

      if (writable && bufferLevel > 0) {
        raf.write(buffer, 0, bufferLevel);
      }

      filePointer += bufferLevel;
      flush();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Copies <code>length</code> bytes of data from the buffer, and returns it. This does not move
   * any file pointers.
   * @param length the length of data to copy
   * @return the data from the buffer
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
   * Gets the position of the pointer in the buffer
   * @return the pointer position in the buffer
   ***********************************************************************************************/
  @Override
  public int getBufferLevel() {
    return bufferLevel;
  }

  /***********************************************************************************************
   * Gets the size of the buffer
   * @return the size of the buffer
   ***********************************************************************************************/
  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  /***********************************************************************************************
   * Gets the <code>File</code> path that is used for reading and writing
   * @return the file path
   ***********************************************************************************************/
  public File getFile() {
    return file;
  }

  /***********************************************************************************************
   * Gets the current position in this file. Data will be read or written from this point.
   * @return the current position in the file
   ***********************************************************************************************/
  @Override
  public long getPointer() {
    try {

      if (writable) {
        return raf.getFilePointer() + bufferLevel;
      }
      else {
        return filePointer + bufferLevel;
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the underlying <code>RandomAccessFile</code> that performs the reading and writing
   * @return the <code>RandomAccessFile</code>
   ***********************************************************************************************/
  public RandomAccessFile getRandomAccessFile() {
    return raf;
  }

  /***********************************************************************************************
   * Gets the value at <code>position</code> in the current buffer. This does not move any
   * pointers.
   * @param position the position in the buffer
   * @return the value at the <code>position</code> in the buffer
   * @throws java.io.IOException
   ***********************************************************************************************/
  public int getValueAtBufferPos(int position) throws IOException {
    try {
      return buffer[position];
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      throw new IOException("No value at buffer position " + position);
    }
  }

  /***********************************************************************************************
   * Is this buffer open for reading or writing?
   * @return true if the buffer is open, false otherwise
   ***********************************************************************************************/
  @Override
  public boolean isOpen() {
    return raf.getChannel().isOpen();
  }

  /***********************************************************************************************
   * Gets the length of the file
   * @return the length of the file
   ***********************************************************************************************/
  @Override
  public long length() {
    try {

      if (writable) {
        return raf.length() + bufferLevel;
      }
      else {
        return raf.length();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }
  }

  /***********************************************************************************************
   * Reads a single byte from the buffer, but doesn't increment any file pointers
   * @return the byte at the current point in the buffer
   ***********************************************************************************************/
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
   * Reads a single byte from the buffer
   * @return the byte
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
   * Reads a number of bytes from the buffer into the <code>destination</code> array
   * @param destination the array that data is read in to
   * @return the number of bytes that were read into the array
   ***********************************************************************************************/
  @Override
  public int read(byte[] destination) {
    return read(destination, 0, destination.length);
  }

  /***********************************************************************************************
   * Reads <code>length</code> bytes of data from the buffer into the <code>offset</code>
   * position in the <code>destination</code> array
   * @param destination the array that data is read in to
   * @param offset the offset in the <code>destination</code> array where the data is read in to
   * @param length the number of bytes to read into the array
   ***********************************************************************************************/
  @Override
  public int read(byte[] destination, int offset, int length) {
    try {

      int lengthToRead = length;

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
          checkFill(sizeToRead);
          readLevel += sizeToRead;
        }

      }

      return lengthToRead;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }
  }

  /***********************************************************************************************
   * Seeks to the <code>offset</code> in the file. If the <code>offset</code> is in the buffer,
   * it moves the buffer pointer rather than reloading the whole buffer <br>
   * What happens is the <i>offset</i> is checked to see if that position is in the current
   * buffer - if it is then the file pointer is incremented without needing to read anything from
   * the hard drive. If the buffer does not cover the <i>offset</i>, the <i>seek(offset)</i>
   * method is called normally. This is in contrast to the normal <i>seek(offset)</i> method that
   * always reads from the hard drive regardless of where the offset is. <br>
   * This method is significantly faster than <i>seek(offset)</i> in applications where many
   * seeks of small distance are required - however it will be very slightly slower for
   * applications where seeking is over lengths greater than the size of the buffer - therefore
   * it is important that the buffer size is large enough to make relative seeking feasable (ie
   * larger than the distance between oldOffset and newOffset).
   * @param offset the offset to seek to
   * @param offset the offset to seek to in the file
   * @see seek(long)
   ***********************************************************************************************/
  @Override
  public void relativeSeek(long offset) {
    try {

      if (writable) {
        forceWrite();
      }

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
   * Gets the number of bytes left to read in the file. In other words, the length between the
   * current pointer and the end of the file
   * @return the number of bytes remaining
   ***********************************************************************************************/
  @Override
  public long remainingLength() {
    return length() - getPointer();
  }

  /***********************************************************************************************
   * Seeks to the <code>offset</code> in the file. The whole buffer is flushed and re-read from
   * the new <code>offset</code>, so is inefficient for small jumps.
   * @param offset the offset to seek to in the file
   * @see relativeSeek(long)
   ***********************************************************************************************/
  @Override
  public void seek(long offset) {
    try {

      if (offset == getPointer()) {
        // already at the right offset, so we don't need to do anything
        return;
      }
      //else if (offset == filePointer){
      //  // the buffer starts at this offset, so just reset the bufferLevel
      //  bufferLevel = 0;
      //  return;
      //  }

      if (writable) {
        forceWrite();
        raf.seek(offset);
        filePointer = offset;
        flush();
      }
      else {
        raf.seek(offset);
        filePointer = offset;
        fill();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Sets the size of the buffer. <br>
   * <br>
   * <b>WARNING:</b> If this file is opened as <i>writable</i>, this method will
   * <code>forceWrite()</code> any data in the buffer to the current position in the file. If you
   * don't want this, make sure you <code>flush()</code> before running this method.
   * @param length the new length of the buffer
   ***********************************************************************************************/
  @Override
  public void setBufferSize(int length) {
    try {

      if (writable) {
        forceWrite();
      }

      bufferSize = length;
      flush();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Sets the length of the file. If the file is smaller than this length, the file size is
   * increased. If the file is larger than this size, the file size is not changed. <br>
   * <i>Note:</i> Setting the length does not force the file to be this length, it only sets the
   * minimum size of the file. If you write data past the length of the file, the file will
   * become longer.
   * @param length the new length of the file
   ***********************************************************************************************/
  @Override
  public void setLength(long length) {
    try {

      raf.setLength(length);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Skips over <code>length</code> bytes in the buffer
   * @param length the number of bytes to skip
   * @return the number of skipped bytes
   ***********************************************************************************************/
  @Override
  public int skip(int length) {
    try {

      if (writable) {
        // for reading and writing...
        forceWrite();
        raf.skipBytes(length);
        flush();

        filePointer += length;

        return length;
      }
      else {
        // else for read only...
        int bufferPos = length + bufferLevel;
        if (bufferPos >= bufferSize) {

          // In very very rare cases, this would cause the seek() to fail. This is because in seek() it
          // doesn't actually do a seek if the filePointer is already at the correct spot. This is in
          // seek() at the line...
          //
          // if (offset == getPointer()){
          //
          // Because this line would evaluate to true, the seek wouldn't actually move, even though it
          // is at the wrong spot. The new line corrects the problem, and filePointer still gets updated
          // in the seek method regardless.
          //
          // This is a very rare bug - it only occurred once in several years!

          //filePointer += bufferPos;
          //seek(filePointer);
          seek(filePointer + bufferPos);
        }
        else {
          bufferLevel += length;
        }

        return length;
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return 0;
    }
  }

  /***********************************************************************************************
   * Writes an array of data into the buffer
   * @param source the data to write to the buffer
   ***********************************************************************************************/
  @Override
  public void write(byte[] source) {
    write(source, 0, source.length);
  }

  /***********************************************************************************************
   * Writes <code>length</code> bytes of data from the <code>offset</code> in the
   * <code>source</code> array into the buffer
   * @param source the data to write to the buffer
   * @param offset the offset in the <code>source</code> to start reading from
   * @param length the length of data to write
   ***********************************************************************************************/
  @Override
  public void write(byte[] source, int offset, int length) {
    try {

      checkWrite(length);

      if (length >= bufferSize) {
        // just write straight to the disk
        raf.write(source, offset, length);
        filePointer += length;
      }
      else {
        // buffer it
        System.arraycopy(source, offset, buffer, bufferLevel, length);
        bufferLevel += length;
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Writes a single byte of data to the buffer
   * @param source the byte to write
   ***********************************************************************************************/
  @Override
  public void write(int source) {
    checkWrite(1);
    buffer[bufferLevel] = (byte) source;
    bufferLevel++;
  }
}