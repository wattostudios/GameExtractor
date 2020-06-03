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

package org.watto.io;

import java.io.File;
import org.watto.io.buffer.FileBuffer;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.converter.BooleanArrayConverter;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.CharConverter;
import org.watto.io.converter.DoubleConverter;
import org.watto.io.converter.FloatConverter;
import org.watto.io.converter.HexConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.io.converter.ShortConverter;

/***********************************************************************************************
Provides reading, writing, and data conversion methods for a <code>FileManipulator</code>
data source.
***********************************************************************************************/
public class FileManipulator implements Manipulator {

  /** the buffer that reads and writes to the underlying file **/
  ManipulatorBuffer buffer;

  /** if this FileManipulator sits atop anything other than a FileBuffer, you can set a fake filename here */
  File fakeFile = null;

  /***********************************************************************************************
  Opens a buffer to the <code>file</code>
  @param file the file to open
  @param writable whether the file should be writable or not
  ***********************************************************************************************/
  public FileManipulator(File file, boolean writable) {
    buffer = new FileBuffer(file, writable);
  }

  /***********************************************************************************************
  Opens a buffer to the <code>file</code>, with a given <code>bufferSize</code>
  @param file the file to open
  @param writable whether the file should be writable or not
  @param bufferSize the size of the buffer
  ***********************************************************************************************/
  public FileManipulator(File file, boolean writable, int bufferSize) {
    buffer = new FileBuffer(file, writable, bufferSize);
  }

  /***********************************************************************************************
  Allows manipulation of a <code>ManipulatorBuffer</code>
  @param buffer the buffer to manipulate
  ***********************************************************************************************/
  public FileManipulator(ManipulatorBuffer buffer) {
    this.buffer = buffer;
  }

  /***********************************************************************************************
  Closes the buffer, preventing further reading and writing
  ***********************************************************************************************/
  @Override
  public void close() {
    buffer.close();
  }

  /***********************************************************************************************
  Clears any buffered data
  ***********************************************************************************************/
  @Override
  public void flush() {
    buffer.flush();
  }

  /***********************************************************************************************
  Forces any buffered data to be written to disk
  ***********************************************************************************************/
  @Override
  public void forceWrite() {
    buffer.forceWrite();
  }

  /***********************************************************************************************
  Gets the buffer used for reading and writing
  @return the buffer
  ***********************************************************************************************/
  @Override
  public ManipulatorBuffer getBuffer() {
    return buffer;
  }

  /***********************************************************************************************
  Gets the <code>File</code> that is being manipulated
  @return the file
  ***********************************************************************************************/
  public File getFile() {
    if (buffer instanceof FileBuffer) {
      return ((FileBuffer) buffer).getFile();
    }
    return fakeFile;
  }

  /***********************************************************************************************
  Gets the path of the <code>File</code> that is being manipulated
  @return the file path
  ***********************************************************************************************/
  public String getFilePath() {
    if (buffer instanceof FileBuffer) {
      return ((FileBuffer) buffer).getFile().getAbsolutePath();
    }
    if (fakeFile != null) {
      return fakeFile.getAbsolutePath();
    }
    return "";
  }

  /***********************************************************************************************
  Gets the length of the data source
  @return the length of the data source
  ***********************************************************************************************/
  @Override
  public long getLength() {
    return buffer.length();
  }

  /***********************************************************************************************
  Gets the current offset in the data source
  @return the offset in the data source
  ***********************************************************************************************/
  @Override
  public long getOffset() {
    return buffer.getPointer();
  }

  /***********************************************************************************************
  Gets the number of bytes remaining to read in the data source
  @return the remaining length of the data source
  ***********************************************************************************************/
  @Override
  public long getRemainingLength() {
    return buffer.remainingLength();
  }

  /***********************************************************************************************
  Is this buffer open for reading or writing?
  @return true if the buffer is open, false otherwise
  ***********************************************************************************************/
  public boolean isOpen() {
    return buffer.isOpen();
  }

  /**
   * *********************************************************************************************
   * Sets the buffer used to read/write to the file
   * @param buf the buffer
   * *********************************************************************************************
   */
  public void open(File file) {
    buffer = new FileBuffer(file, false);
  }

  /***********************************************************************************************
  Sets the <code>buffer</code> used to read and write data
  @param buffer the data buffer
  ***********************************************************************************************/
  @Override
  public void open(ManipulatorBuffer buffer) {
    this.buffer = buffer;
  }

  /***********************************************************************************************
  Reads 8 bits from the data source
  @return the bits
  ***********************************************************************************************/
  @Override
  public boolean[] readBits() {
    return BooleanArrayConverter.convertLittle(readByte());
  }

  /***********************************************************************************************
  Reads a <code>byte</code> from the data source
  @return the byte
  ***********************************************************************************************/
  @Override
  public byte readByte() {
    return (byte) buffer.read();
  }

  /***********************************************************************************************
  Reads <code>byteCount</code> bytes from the data source
  @param byteCount the number of bytes to read
  @return an array of <code>byteCount</code> bytes
  ***********************************************************************************************/
  @Override
  public byte[] readBytes(int byteCount) {
    byte[] bytes = new byte[byteCount];
    buffer.read(bytes);
    return bytes;
  }

  /***********************************************************************************************
  Reads a <code>char</code> from the data source
  @return the char
  ***********************************************************************************************/
  @Override
  public char readChar() {
    return CharConverter.convertLittle(readBytes(2));
  }

  /***********************************************************************************************
  Reads a <code>double</code> from the data source
  @return the double
  ***********************************************************************************************/
  @Override
  public double readDouble() {
    return DoubleConverter.convertLittle(readBytes(8));
  }

  /***********************************************************************************************
  Reads a <code>float</code> from the data source
  @return the float
  ***********************************************************************************************/
  @Override
  public float readFloat() {
    return FloatConverter.convertLittle(readBytes(4));
  }

  /***********************************************************************************************
  Reads <code>byteCount</code> <code>Hex</code> values from the data source
  @param byteCount the number of bytes to read
  @return the <code>Hex</code> values
  ***********************************************************************************************/
  @Override
  public Hex readHex(int byteCount) {
    return HexConverter.convertLittle(readBytes(byteCount));
  }

  /***********************************************************************************************
  Reads an <code>int</code> from the data source
  @return the int
  ***********************************************************************************************/
  @Override
  public int readInt() {
    return IntConverter.convertLittle(readBytes(4));
  }

  /***********************************************************************************************
  Reads a line of text from the data source
  @return the line of text
  ***********************************************************************************************/
  @Override
  public String readLine() {
    return StringHelper.readLine(buffer);
  }

  /***********************************************************************************************
  Reads a <code>long</code> from the data source
  @return the long
  ***********************************************************************************************/
  @Override
  public long readLong() {
    return LongConverter.convertLittle(readBytes(8));
  }

  /***********************************************************************************************
  Reads a <code>String</code> until the first null byte <i>(byte 0)</i> is found
  @return the String
  ***********************************************************************************************/
  public String readNullString() {
    return StringHelper.readNullString(buffer);
  }

  /***********************************************************************************************
  Reads <code>byteCount</code> bytes from the <code>ManipulatorBuffer</code>, then returns the
  null-terminated <code>String</code> from within it. If no null byte <i>(byte 0)</i> is found,
  the entire <code>String</code> is returned. If a null byte is found, the <code>String</code> is
  returned and the remaining bytes are discarded.
  @param byteCount the number of bytes to read.
  @return the String
  ***********************************************************************************************/
  public String readNullString(int byteCount) {
    return StringHelper.readNullString(buffer, byteCount);
  }

  /***********************************************************************************************
  Reads a unicode <code>String</code> until the first null byte <i>(byte 0)</i> is found
  @return the unicode String
  ***********************************************************************************************/
  public String readNullUnicodeString() {
    return StringHelper.readNullUnicodeString(buffer);
  }

  /***********************************************************************************************
  Reads <code>charCount</code> bytes from the <code>ManipulatorBuffer</code>, then returns the
  null-terminated unicode <code>String</code> from within it. If no null byte <i>(byte 0)</i> is
  found, the entire unicode <code>String</code> is returned. If a null byte is found, the unicode
  <code>String</code> is returned and the remaining bytes are discarded.
  @param charCount the number of characters to read. (where 1 character = 2 bytes)
  @return the String
  ***********************************************************************************************/
  public String readNullUnicodeString(int charCount) {
    return StringHelper.readNullUnicodeString(buffer, charCount);
  }

  /***********************************************************************************************
  Reads a <code>short</code> from the data source
  @return the short
  ***********************************************************************************************/
  @Override
  public short readShort() {
    return ShortConverter.convertLittle(readBytes(2));
  }

  /***********************************************************************************************
  Reads a <code>String</code> of length <code>letterCount</code> from the data source
  @param byteCount the number of bytes to read
  @return the String
  ***********************************************************************************************/
  @Override
  public String readString(int byteCount) {
    return StringHelper.readString(buffer, byteCount);
  }

  /***********************************************************************************************
  Reads a unicode <code>String</code> of length <code>letterCount</code> from the data source
  @param charCount the number of characters to read. (where 1 character = 2 bytes)
  @return the String
  ***********************************************************************************************/
  @Override
  public String readUnicodeString(int charCount) {
    return StringHelper.readUnicodeString(buffer, charCount);
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the data source, using a relative seek on the buffer
  @param offset the offset to seek to
  ***********************************************************************************************/
  @Override
  public void relativeSeek(long offset) {
    buffer.relativeSeek(offset);
  }

  /***********************************************************************************************
  Skips backwards <code>byteCount</code> bytes in the data source
  @param byteCount the number of bytes to skip backwards
  ***********************************************************************************************/
  @Override
  public void rewind(long byteCount) {
    buffer.relativeSeek(buffer.getPointer() - byteCount);
  }

  /***********************************************************************************************
  Seeks to the <code>offset</code> in the data source, using a direct seek on the buffer
  @param offset the offset to seek to
  ***********************************************************************************************/
  @Override
  public void seek(long offset) {
    buffer.seek(offset);
  }

  /***********************************************************************************************
  Sets the <code>buffer</code> used to read and write data
  @param buffer the data buffer
  ***********************************************************************************************/
  @Override
  public void setBuffer(ManipulatorBuffer buffer) {
    this.buffer = buffer;
  }

  /***********************************************************************************************
  Sets a fake file
  ***********************************************************************************************/
  public void setFakeFile(File fakeFile) {
    this.fakeFile = fakeFile;
  }

  /***********************************************************************************************
  Sets the minimum <code>length</code> of the data source
  @param length the minimum length of the data source
  ***********************************************************************************************/
  @Override
  public void setLength(long length) {
    buffer.setLength(length);
  }

  /***********************************************************************************************
  Skips forward <code>byteCount</code> bytes in the data source
  @param byteCount the number of bytes to skip
  ***********************************************************************************************/
  @Override
  public long skip(long skipCount) {
    int skipCountSmall = (int) skipCount;
    if (skipCountSmall != skipCount) {
      // the skip is larger than an int
      long newOffset = buffer.getPointer() + skipCount;
      buffer.seek(newOffset);

      // work out how many bytes were actually skipped
      long skipLength = newOffset - buffer.getPointer();
      if (skipLength == 0) {
        return skipCount;
      }
      else {
        return skipCount - skipLength;
      }
    }
    else {
      return buffer.skip(skipCountSmall);
    }
  }

  /***********************************************************************************************
  Writes 8 bits to the data source
  @param value the 8 bits to write
  ***********************************************************************************************/
  @Override
  public void writeBits(boolean[] value) {
    writeByte(ByteConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>byte</code> to the data source
  @param value the byte to write
  ***********************************************************************************************/
  @Override
  public void writeByte(byte value) {
    buffer.write(value);
  }

  /***********************************************************************************************
  Writes a <code>byte</code> (declared as an <code>int</code>) to the data source
  @param value the byte to write
  ***********************************************************************************************/
  public void writeByte(int value) {
    buffer.write(value);
  }

  /***********************************************************************************************
  Writes a number of <code>byte</code>s to the data source
  @param value the bytes to write
  ***********************************************************************************************/
  @Override
  public void writeBytes(byte[] values) {
    buffer.write(values);
  }

  /***********************************************************************************************
  Writes a <code>char</code> to the data source
  @param value the char to write
  ***********************************************************************************************/
  @Override
  public void writeChar(char value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>double</code> to the data source
  @param value the double to write
  ***********************************************************************************************/
  @Override
  public void writeDouble(double value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>float</code> to the data source
  @param value the float to write
  ***********************************************************************************************/
  @Override
  public void writeFloat(float value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>Hex</code> string to the data source
  @param value the Hex string to write
  ***********************************************************************************************/
  @Override
  public void writeHex(Hex value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>int</code> to the data source
  @param value the int to write
  ***********************************************************************************************/
  @Override
  public void writeInt(int value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>int</code> to the data source
  @param value the int to write
  ***********************************************************************************************/
  public void writeInt(long value) {
    writeBytes(ByteArrayConverter.convertLittle((int) value));
  }

  /***********************************************************************************************
  Writes a line of text to the data source
  @param text the line to write
  ***********************************************************************************************/
  @Override
  public void writeLine(String text) {
    writeBytes(ByteArrayConverter.convertLittle(text + "\n"));
  }

  /***********************************************************************************************
  Writes a <code>long</code> to the data source
  @param value the long to write
  ***********************************************************************************************/
  @Override
  public void writeLong(long value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>String</code> to the data source, followed by a null byte <i>(byte 0)</i>
  @param text the String to write
  ***********************************************************************************************/
  public void writeNullString(String text) {
    StringHelper.writeNullString(buffer, text);
  }

  /***********************************************************************************************
  Writes a <code>String</code> to the data source, followed by a null byte <i>(byte 0)</i>. If the
  <code>String</code> is longer than <code>byteCount</code>, it is shortened to
  <code>byteCount</code> length, and no null byte is written. If the <code>String</code> is
  shorter than <code>byteCount</code>, the remaining space is filled with null bytes.
  @param text the String to write
  @param byteCount the maximum number of bytes for the <code>String</code>
  ***********************************************************************************************/
  public void writeNullString(String text, int byteCount) {
    StringHelper.writeNullString(buffer, text, byteCount);
  }

  /***********************************************************************************************
  Writes a unicode <code>String</code> to the data source, followed by a 2 null bytes <i>(char 0)</i>
  @param text the unicode String to write
  ***********************************************************************************************/
  public void writeNullUnicodeString(String text) {
    StringHelper.writeNullUnicodeString(buffer, text);
  }

  /***********************************************************************************************
  Writes a unicode <code>String</code> to the data source, followed by 2 null bytes <i>(char 0)</i>.
  If the unicode <code>String</code> is longer than <code>charCount</code> <code>char</code>s, it
  is shortened to <code>charCount</code> <code>char</code>s, and no null bytes are written. If
  the unicode <code>String</code> is shorter than <code>charCount</code> <code>char</code>s, the
  remaining space is filled with null bytes.
  @param text the unicode String to write
  @param charCount the maximum number of <code>char</code>s for the unicode <code>String</code>,
  where a char = 2 bytes
  ***********************************************************************************************/
  public void writeNullUnicodeString(String text, int charCount) {
    StringHelper.writeNullUnicodeString(buffer, text, charCount);
  }

  /***********************************************************************************************
  Writes a <code>short</code> to the data source, where the <code>value</code> is defined as a
  <code>int</code>
  @param value the short to write
  ***********************************************************************************************/
  public void writeShort(int value) {
    writeBytes(ByteArrayConverter.convertLittle((short) value));
  }

  /***********************************************************************************************
  Writes a <code>short</code> to the data source
  @param value the short to write
  ***********************************************************************************************/
  @Override
  public void writeShort(short value) {
    writeBytes(ByteArrayConverter.convertLittle(value));
  }

  /***********************************************************************************************
  Writes a <code>String</code> to the data source
  @param text the String to write
  ***********************************************************************************************/
  @Override
  public void writeString(String text) {
    StringHelper.writeString(buffer, text);
  }

  /***********************************************************************************************
  Writes a unicode <code>String</code> to the data source
  @param text the unicode String to write
  ***********************************************************************************************/
  @Override
  public void writeUnicodeString(String text) {
    StringHelper.writeUnicodeString(buffer, text);
  }
}