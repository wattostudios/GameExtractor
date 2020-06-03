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

import org.watto.io.buffer.ManipulatorBuffer;


/***********************************************************************************************
Provides reading, writing, and data conversion methods for a <code>ManipulatorBuffer</code>
data source.
***********************************************************************************************/
public interface Manipulator {

  /***********************************************************************************************
  Closes the buffer, preventing further reading and writing
  ***********************************************************************************************/
  public void close();


  /***********************************************************************************************
  Forces any buffered data to be written to disk
  ***********************************************************************************************/
  public void forceWrite();


  /***********************************************************************************************
  Clears any buffered data
  ***********************************************************************************************/
  public void flush();


  /***********************************************************************************************
  Gets the buffer used for reading and writing
  @return the buffer
  ***********************************************************************************************/
  public ManipulatorBuffer getBuffer();


  /***********************************************************************************************
  Gets the length of the data source
  @return the length of the data source
  ***********************************************************************************************/
  public long getLength();


  /***********************************************************************************************
  Gets the current offset in the data source
  @return the offset in the data source
  ***********************************************************************************************/
  public long getOffset();


  /***********************************************************************************************
  Gets the number of bytes remaining to read in the data source
  @return the remaining length of the data source
  ***********************************************************************************************/
  public long getRemainingLength();


  /***********************************************************************************************
  Sets the <code>buffer</code> used to read and write data
  @param buffer the data buffer
  ***********************************************************************************************/
  public void open(ManipulatorBuffer buffer);


  /***********************************************************************************************
  Reads 8 bits from the data source
  @return the bits
  ***********************************************************************************************/
  public boolean[] readBits();


  /***********************************************************************************************
  Reads a <code>byte</code> from the data source
  @return the byte
  ***********************************************************************************************/
  public byte readByte();


  /***********************************************************************************************
  Reads <code>byteCount</code> bytes from the data source
  @param byteCount the number of bytes to read
  @return an array of <code>byteCount</code> bytes
  ***********************************************************************************************/
  public byte[] readBytes(int byteCount);


  /***********************************************************************************************
  Reads a <code>char</code> from the data source
  @return the char
  ***********************************************************************************************/
  public char readChar();


  /***********************************************************************************************
  Reads a <code>double</code> from the data source
  @return the double
  ***********************************************************************************************/
  public double readDouble();


  /***********************************************************************************************
  Reads a <code>float</code> from the data source
  @return the float
  ***********************************************************************************************/
  public float readFloat();


  /***********************************************************************************************
  Reads <code>byteCount</code> <code>Hex</code> values from the data source
  @param byteCount the number of bytes to read
  @return the <code>Hex</code> values
  ***********************************************************************************************/
  public Hex readHex(int byteCount);


  /***********************************************************************************************
  Reads an <code>int</code> from the data source
  @return the int
  ***********************************************************************************************/
  public int readInt();


  /***********************************************************************************************
  Reads a line of text from the data source
  @return the line of text
  ***********************************************************************************************/
  public String readLine();


  /***********************************************************************************************
  Reads a <code>long</code> from the data source
  @return the long
  ***********************************************************************************************/
  public long readLong();


  /***********************************************************************************************
  Reads a <code>short</code> from the data source
  @return the short
  ***********************************************************************************************/
  public short readShort();


  /***********************************************************************************************
  Reads a <code>String</code> of length <code>letterCount</code> from the data source
  @param letterCount the number of letters to read
  @return the String
  ***********************************************************************************************/
  public String readString(int letterCount);


  /***********************************************************************************************
  Reads a unicode <code>String</code> of length <code>letterCount</code> from the data source
  @param letterCount the number of unicode letters to read
  @return the String
  ***********************************************************************************************/
  public String readUnicodeString(int letterCount);


  /***********************************************************************************************
  Seeks to the <code>offset</code> in the data source, using a relative seek on the buffer
  @param offset the offset to seek to
  ***********************************************************************************************/
  public void relativeSeek(long offset);


  /***********************************************************************************************
  Skips backwards <code>byteCount</code> bytes in the data source
  @param byteCount the number of bytes to skip backwards
  ***********************************************************************************************/
  public void rewind(long byteCount);


  /***********************************************************************************************
  Seeks to the <code>offset</code> in the data source, using a direct seek on the buffer
  @param offset the offset to seek to
  ***********************************************************************************************/
  public void seek(long offset);


  /***********************************************************************************************
  Sets the <code>buffer</code> used to read and write data
  @param buffer the data buffer
  ***********************************************************************************************/
  public void setBuffer(ManipulatorBuffer buffer);


  /***********************************************************************************************
  Sets the minimum <code>length</code> of the data source
  @param length the minimum length of the data source
  ***********************************************************************************************/
  public void setLength(long length);


  /***********************************************************************************************
  Skips forward <code>byteCount</code> bytes in the data source
  @param byteCount the number of bytes to skip
  ***********************************************************************************************/
  public long skip(long skipCount);


  /***********************************************************************************************
  Writes 8 bits to the data source
  @param value the 8 bits to write
  ***********************************************************************************************/
  public void writeBits(boolean[] value);


  /***********************************************************************************************
  Writes a <code>byte</code> to the data source
  @param value the byte to write
  ***********************************************************************************************/
  public void writeByte(byte value);


  /***********************************************************************************************
  Writes a number of <code>byte</code>s to the data source
  @param value the bytes to write
  ***********************************************************************************************/
  public void writeBytes(byte[] values);


  /***********************************************************************************************
  Writes a <code>char</code> to the data source
  @param value the char to write
  ***********************************************************************************************/
  public void writeChar(char value);


  /***********************************************************************************************
  Writes a <code>double</code> to the data source
  @param value the double to write
  ***********************************************************************************************/
  public void writeDouble(double value);


  /***********************************************************************************************
  Writes a <code>float</code> to the data source
  @param value the float to write
  ***********************************************************************************************/
  public void writeFloat(float value);


  /***********************************************************************************************
  Writes a <code>Hex</code> string to the data source
  @param value the Hex string to write
  ***********************************************************************************************/
  public void writeHex(Hex value);


  /***********************************************************************************************
  Writes a <code>int</code> to the data source
  @param value the int to write
  ***********************************************************************************************/
  public void writeInt(int value);


  /***********************************************************************************************
  Writes a line of text to the data source
  @param text the line to write
  ***********************************************************************************************/
  public void writeLine(String text);


  /***********************************************************************************************
  Writes a <code>long</code> to the data source
  @param value the long to write
  ***********************************************************************************************/
  public void writeLong(long value);


  /***********************************************************************************************
  Writes a <code>short</code> to the data source
  @param value the short to write
  ***********************************************************************************************/
  public void writeShort(short value);


  /***********************************************************************************************
  Writes a <code>String</code> to the data source
  @param text the String to write
  ***********************************************************************************************/
  public void writeString(String text);


  /***********************************************************************************************
  Writes a unicode <code>String</code> to the data source
  @param text the unicode String to write
  ***********************************************************************************************/
  public void writeUnicodeString(String text);
}