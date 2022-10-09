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

import org.watto.ErrorLogger;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.CharArrayConverter;
import org.watto.io.converter.CharConverter;
import org.watto.io.converter.StringConverter;

/***********************************************************************************************
 * Utility methods for reading and writing different Strings from a
 * <code>ManipulatorBuffer</code>
 ***********************************************************************************************/
public class StringHelper {

  /***********************************************************************************************
   * Reads a line of 8-bit text from the buffer. A line ends with either the \n New Line, \r
   * Return, or the \r\n characters, where each character is 8-bits long
   * @param buffer the buffer to read from
   * @return the line of text
   ***********************************************************************************************/
  public static String readLine(ManipulatorBuffer buffer) {
    return readLine(buffer, true, true);
  }

  /***********************************************************************************************
   * Reads a line of 8-bit text from the buffer. A line ends with either the \n New Line, \r
   * Return, or the \r\n characters, where each character is 8-bits long
   * @param buffer the buffer to read from
   * @param carriageReturn whether a carriage return <i>(\r)</i> should indicate the end of a
   *        line
   * @param lineFeed whether a line feed <i>(\n)</i> should indicate the end of a line
   * @return the line of text
   ***********************************************************************************************/
  public static String readLine(ManipulatorBuffer buffer, boolean carriageReturn, boolean lineFeed) {
    try {

      String line = "";

      byte n = 10;
      byte r = 13;

      long remainingLength = buffer.remainingLength();

      for (int i = 0; i < remainingLength; i++) {
        byte currentByte = (byte) buffer.read();
        if (currentByte == n) {
          // new line character '\n'
          return line;
        }
        else if (currentByte == r) {
          // carriage return character '\r'

          // check for the \r\n combination
          //buffer.checkFill(1);
          int returnCheck = buffer.peek(); // looks at the next byte, but doesn't move the pointers
          if (returnCheck == -1) {
            // Error or End Of File
            return line;
          }
          else if (returnCheck == n) {
            // the next byte is the /n, so skip the next byte
            buffer.skip(1);
          }

          return line;
        }
        else {
          line += (char) currentByte;
        }
      }

      // Error or End Of File
      return line;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return "";
    }
  }

  /***********************************************************************************************
   * Reads a <code>String</code> until the first null byte <i>(byte 0)</i> is found
   * @param buffer the buffer to read from
   * @return the String
   ***********************************************************************************************/
  public static String readNullString(ManipulatorBuffer buffer) {
    return readTerminatedString(buffer, (byte) 0);
  }

  /***********************************************************************************************
   * Reads <code>byteCount</code> bytes from the <code>buffer</code>, then returns the
   * null-terminated <code>String</code> from within it. If no null byte <i>(byte 0)</i> is
   * found, the entire <code>String</code> is returned. If a null byte is found, the
   * <code>String</code> is returned and the remaining bytes are discarded.
   * @param buffer the buffer to read from
   * @param byteCount the number of bytes to read.
   * @return the String
   ***********************************************************************************************/
  public static String readNullString(ManipulatorBuffer buffer, int byteCount) {
    return readTerminatedString(buffer, (byte) 0, byteCount);
  }

  /***********************************************************************************************
   * Reads a unicode <code>String</code> until the first null byte <i>(byte 0)</i> is found
   * @param buffer the buffer to read from
   * @return the unicode String
   ***********************************************************************************************/
  public static String readNullUnicodeString(ManipulatorBuffer buffer) {
    return readTerminatedUnicodeString(buffer, (char) 0);
  }

  /***********************************************************************************************
   * Reads <code>charCount</code> bytes from the <code>ManipulatorBuffer</code>, then returns the
   * null-terminated unicode <code>String</code> from within it. If no null byte <i>(byte 0)</i>
   * is found, the entire unicode <code>String</code> is returned. If a null byte is found, the
   * unicode <code>String</code> is returned and the remaining bytes are discarded.
   * @param buffer the buffer to read from
   * @param charCount the number of characters to read. (where 1 character = 2 bytes)
   * @return the String
   ***********************************************************************************************/
  public static String readNullUnicodeString(ManipulatorBuffer buffer, int charCount) {
    return readTerminatedUnicodeString(buffer, (char) 0, charCount);
  }

  /***********************************************************************************************
   * Reads a <code>String</code> of length <code>letterCount</code> from the data source
   * @param buffer the buffer to read from
   * @param byteCount the number of bytes to read
   * @return the String
   ***********************************************************************************************/
  public static String readString(ManipulatorBuffer buffer, int byteCount) {
    byte[] stringBytes = new byte[byteCount];
    buffer.read(stringBytes);
    return StringConverter.convertLittle(stringBytes);
  }

  /***********************************************************************************************
   * Reads a <code>String</code> until the first <code>terminator</code> byte is found
   * @param buffer the buffer to read from
   * @param terminator the byte value that indicates the end of the <code>String</code>
   * @return the String
   ***********************************************************************************************/
  public static String readTerminatedString(ManipulatorBuffer buffer, byte terminator) {
    String filename = "";
    byte filenameByte = (byte) buffer.read();
    while (filenameByte != terminator) {
      filename += (char) filenameByte;
      filenameByte = (byte) buffer.read();
    }
    return filename;
  }

  /***********************************************************************************************
   * Reads <code>byteCount</code> bytes from the <code>buffer</code>, then returns the terminated
   * <code>String</code> from within it. If no <code>terminator</code> byte is found, the entire
   * <code>String</code> is returned. If a <code>terminator</code> byte is found, the
   * <code>String</code> is returned and the remaining bytes are discarded.
   * @param buffer the buffer to read from
   * @param terminator the byte value that indicates the end of the <code>String</code>
   * @param byteCount the number of bytes to read.
   * @return the String
   ***********************************************************************************************/
  public static String readTerminatedString(ManipulatorBuffer buffer, byte terminator, int byteCount) {
    byte[] bytes = new byte[byteCount];
    buffer.read(bytes);

    for (int i = 0; i < byteCount; i++) {
      if (bytes[i] == terminator) {
        byte[] nameBytes = new byte[i];
        System.arraycopy(bytes, 0, nameBytes, 0, i);
        return new String(nameBytes);
      }
    }

    return new String(bytes);
  }

  /***********************************************************************************************
   * Reads a unicode <code>String</code> until the first <code>terminator</code> char is found
   * @param buffer the buffer to read from
   * @param terminator the char value that indicates the end of the <code>String</code>
   * @return the unicode String
   ***********************************************************************************************/
  public static String readTerminatedUnicodeString(ManipulatorBuffer buffer, char terminator) {
    String filename = new String(new char[0]);

    byte[] charBytes = new byte[2];
    buffer.read(charBytes);
    char filenameChar = CharConverter.convertLittle(charBytes);

    while (filenameChar != terminator) {
      filename += filenameChar;

      buffer.read(charBytes);
      filenameChar = CharConverter.convertLittle(charBytes);
    }
    return filename;
  }

  /***********************************************************************************************
   * Reads <code>charCount</code> chars from the <code>buffer</code>, then returns the terminated
   * <code>String</code> from within it. If no <code>terminator</code> char is found, the entire
   * <code>String</code> is returned. If a <code>terminator</code> char is found, the
   * <code>String</code> is returned and the remaining chars are discarded.
   * @param buffer the buffer to read from
   * @param terminator the char value that indicates the end of the <code>String</code>
   * @param charCount the number of characters to read. (where 1 character = 2 bytes)
   * @return the String
   ***********************************************************************************************/
  public static String readTerminatedUnicodeString(ManipulatorBuffer buffer, char terminator, int charCount) {

    byte[] charBytes = new byte[charCount * 2];
    buffer.read(charBytes);
    char[] chars = CharArrayConverter.convertLittle(charBytes);

    for (int i = 0; i < charCount; i++) {
      if (chars[i] == terminator) {
        char[] filenameChars = new char[i];
        System.arraycopy(chars, 0, filenameChars, 0, i);
        return new String(filenameChars);
      }
    }

    return new String(chars);
  }

  /***********************************************************************************************
   * Reads a line of 16-bit unicode text from the buffer. A line ends with either the \n New
   * Line, \r Return, or the \r\n characters, where each character is 16-bits long
   * @param buffer the buffer to read from
   * @return the line of unicode text
   ***********************************************************************************************/
  public static String readUnicodeLine(ManipulatorBuffer buffer) {
    return readUnicodeLine(buffer, true, true);
  }

  /***********************************************************************************************
   * Reads a line of 16-bit unicode text from the buffer. A line ends with either the \n New
   * Line, \r Return, or the \r\n characters, where each character is 16-bits long
   * @param buffer the buffer to read from
   * @param carriageReturn whether a carriage return <i>(\r)</i> should indicate the end of a
   *        line
   * @param lineFeed whether a line feed <i>(\n)</i> should indicate the end of a line
   * @return the line of unicode text
   ***********************************************************************************************/
  public static String readUnicodeLine(ManipulatorBuffer buffer, boolean carriageReturn, boolean lineFeed) {
    try {

      String line = new String(new byte[0], "UTF-16LE");

      byte n = 10;
      byte r = 13;

      long remainingLength = buffer.remainingLength();

      for (int i = 0; i < remainingLength; i++) {
        byte[] currentBytes = new byte[2];
        buffer.read(currentBytes);

        if (currentBytes[0] == n && currentBytes[1] == 0) {
          // new line character '\n'
          return line;
        }
        else if (currentBytes[0] == r && currentBytes[1] == 0) {
          // carriage return character '\r'

          // check for the \r\n combination
          buffer.checkFill(2);
          byte[] returnCheck = buffer.getBuffer(2); // looks at the next byte, but doesn't move the pointers
          if (returnCheck == null || returnCheck.length != 2) {
            // Error or End Of File
            return line;
          }
          else if (returnCheck[0] == n && returnCheck[1] == 0) {
            // the next byte is the /n, so skip the next byte
            buffer.skip(2);
          }

          return line;
        }
        else {
          line += CharConverter.convertLittle(currentBytes);
        }
      }

      // Error or End Of File
      return line;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return "";
    }
  }

  /***********************************************************************************************
   * Reads a unicode <code>String</code> of length <code>letterCount</code> from the data source
   * @param buffer the buffer to read from
   * @param charCount the number of characters to read. (where 1 character = 2 bytes)
   * @return the String
   ***********************************************************************************************/
  public static String readUnicodeString(ManipulatorBuffer buffer, int charCount) {
    byte[] stringBytes = new byte[charCount * 2];
    buffer.read(stringBytes);
    // convert to char[] then convert to String, so that it knows that its unicode
    //return StringConverter.convertLittle(CharArrayConverter.convertLittle(stringBytes));
    return new String(CharArrayConverter.convertLittle(stringBytes));
  }

  /***********************************************************************************************
   * Writes a line of text to the buffer, terminated by the <i>/n</i> new line character
   * @param buffer the buffer to write to
   * @param text the line to write
   ***********************************************************************************************/
  public static void writeLine(ManipulatorBuffer buffer, String text) {
    writeLine(buffer, text, true, false);
  }

  /***********************************************************************************************
   * Writes a line of text to the buffer
   * @param buffer the buffer to write to
   * @param text the line to write
   * @param carriageReturn whether a carriage return <i>(\r)</i> should be placed at the end of
   *        the line
   * @param lineFeed whether a line feed <i>(\n)</i> should be placed at the end of the line
   ***********************************************************************************************/
  public static void writeLine(ManipulatorBuffer buffer, String text, boolean lineFeed, boolean carriageReturn) {
    buffer.write(ByteArrayConverter.convertLittle(text));
    if (carriageReturn) {
      buffer.write((byte) 13);
    }
    if (lineFeed) {
      buffer.write((byte) 10);
    }
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer, followed by a null byte <i>(byte 0)</i>
   * @param buffer the buffer to write to
   * @param text the String to write
   ***********************************************************************************************/
  public static void writeNullString(ManipulatorBuffer buffer, String text) {
    writeTerminatedString(buffer, text, (byte) 0);
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer, followed by a null byte <i>(byte 0)</i>. If the
   * <code>String</code> is longer than <code>byteCount</code>, it is shortened to
   * <code>byteCount</code> length, and no null byte is written. If the <code>String</code> is
   * shorter than <code>byteCount</code>, the remaining space is filled with null bytes.
   * @param buffer the buffer to write to
   * @param text the String to write
   * @param byteCount the maximum number of bytes for the <code>String</code>
   ***********************************************************************************************/
  public static void writeNullString(ManipulatorBuffer buffer, String text, int byteCount) {
    writeNullString(buffer, text, byteCount, false);
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer, followed by a null byte <i>(byte 0)</i>. If the
   * <code>String</code> is longer than <code>byteCount</code>, it is shortened to
   * <code>byteCount</code> length, and no null byte is written. If the <code>String</code> is
   * shorter than <code>byteCount</code>, the remaining space is filled with null bytes.
   * @param buffer the buffer to write to
   * @param text the String to write
   * @param byteCount the maximum number of bytes for the <code>String</code>
   * @param requiresTerminator whether the null byte needs to be written if the text is
   *        <code>byteCount</code> bytes long (ie. is the terminator always required?)
   ***********************************************************************************************/
  public static void writeNullString(ManipulatorBuffer buffer, String text, int byteCount, boolean requiresTerminator) {
    writeTerminatedString(buffer, text, (byte) 0, byteCount, requiresTerminator);
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer, followed by a 2 null bytes <i>(char
   * 0)</i>
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   ***********************************************************************************************/
  public static void writeNullUnicodeString(ManipulatorBuffer buffer, String text) {
    writeTerminatedUnicodeString(buffer, text, (char) 0);
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer, followed by 2 null bytes <i>(char
   * 0)</i>. If the unicode <code>String</code> is longer than <code>charCount</code>
   * <code>char</code>s, it is shortened to <code>charCount</code> <code>char</code>s, and no
   * null bytes are written. If the unicode <code>String</code> is shorter than
   * <code>charCount</code> <code>char</code>s, the remaining space is filled with null bytes.
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   * @param charCount the maximum number of <code>char</code>s for the unicode
   *        <code>String</code>, where a char = 2 bytes
   ***********************************************************************************************/
  public static void writeNullUnicodeString(ManipulatorBuffer buffer, String text, int charCount) {
    writeNullUnicodeString(buffer, text, charCount, false);
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer, followed by 2 null bytes <i>(char
   * 0)</i>. If the unicode <code>String</code> is longer than <code>charCount</code>
   * <code>char</code>s, it is shortened to <code>charCount</code> <code>char</code>s, and no
   * null bytes are written. If the unicode <code>String</code> is shorter than
   * <code>charCount</code> <code>char</code>s, the remaining space is filled with null bytes.
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   * @param charCount the maximum number of <code>char</code>s for the unicode
   *        <code>String</code>, where a char = 2 bytes
   * @param requiresTerminator whether the null char needs to be written if the text is
   *        <code>charCount</code> chars long (ie. is the terminator always required?)
   ***********************************************************************************************/
  public static void writeNullUnicodeString(ManipulatorBuffer buffer, String text, int charCount, boolean requiresTerminator) {
    writeTerminatedUnicodeString(buffer, text, (char) 0, charCount, requiresTerminator);
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer
   * @param buffer the buffer to write to
   * @param text the String to write
   ***********************************************************************************************/
  public static void writeString(ManipulatorBuffer buffer, String text) {
    buffer.write(ByteArrayConverter.convertLittle(text));
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer, followed by a <code>terminator</code> byte
   * @param buffer the buffer to write to
   * @param text the String to write
   * @param terminator the byte value that indicates the end of the <code>String</code>
   ***********************************************************************************************/
  public static void writeTerminatedString(ManipulatorBuffer buffer, String text, byte terminator) {
    buffer.write(ByteArrayConverter.convertLittle(text));
    buffer.write(terminator);
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer, followed by a <code>terminator</code> byte. If
   * the <code>String</code> is longer than <code>byteCount</code>, it is shortened to
   * <code>byteCount</code> length, and no <code>terminator</code> byte is written. If the
   * <code>String</code> is shorter than <code>byteCount</code>, the remaining space is filled
   * with <code>terminator</code> bytes.
   * @param buffer the buffer to write to
   * @param text the String to write
   * @param terminator the byte value that indicates the end of the <code>String</code>
   * @param byteCount the maximum number of bytes for the <code>String</code>
   ***********************************************************************************************/
  public static void writeTerminatedString(ManipulatorBuffer buffer, String text, byte terminator, int byteCount) {
    writeTerminatedString(buffer, text, terminator, byteCount, false);
  }

  /***********************************************************************************************
   * Writes a <code>String</code> to the buffer, followed by a <code>terminator</code> byte. If
   * the <code>String</code> is longer than <code>byteCount</code>, it is shortened to
   * <code>byteCount</code> length, and no <code>terminator</code> byte is written. If the
   * <code>String</code> is shorter than <code>byteCount</code>, the remaining space is filled
   * with <code>terminator</code> bytes.
   * @param buffer the buffer to write to
   * @param text the String to write
   * @param terminator the byte value that indicates the end of the <code>String</code>
   * @param byteCount the maximum number of bytes for the <code>String</code>
   * @param requiresTerminator whether the null byte needs to be written if the text is
   *        <code>byteCount</code> bytes long (ie. is the terminator always required?)
   ***********************************************************************************************/
  public static void writeTerminatedString(ManipulatorBuffer buffer, String text, byte terminator, int byteCount, boolean requiresTerminator) {

    int textLength = text.length();
    if (requiresTerminator && textLength >= byteCount) {
      // -1 so that the last byte can be the terminator
      textLength = byteCount - 1;
      text = text.substring(0, textLength);
    }
    else if (!requiresTerminator && textLength > byteCount) {
      textLength = byteCount;
      text = text.substring(0, textLength);
    }

    buffer.write(ByteArrayConverter.convertLittle(text));

    int padding = byteCount - textLength;
    while (padding > 0) {
      buffer.write(terminator);
      padding--;
    }
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer, followed by a <code>terminator</code>
   * char
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   * @param terminator the char value that indicates the end of the <code>String</code>
   ***********************************************************************************************/
  public static void writeTerminatedUnicodeString(ManipulatorBuffer buffer, String text, char terminator) {
    buffer.write(ByteArrayConverter.convertLittle(CharArrayConverter.convertLittle(text)));
    buffer.write(ByteArrayConverter.convertLittle(terminator));
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer, followed by a <code>terminator</code>
   * char. If the <code>String</code> is longer than <code>charCount</code>, it is shortened to
   * <code>charCount</code> length, and no <code>terminator</code> char is written. If the
   * <code>String</code> is shorter than <code>charCount</code>, the remaining space is filled
   * with <code>terminator</code> chars.
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   * @param terminator the char value that indicates the end of the <code>String</code>
   * @param charCount the maximum number of chars for the <code>String</code>
   ***********************************************************************************************/
  public static void writeTerminatedUnicodeString(ManipulatorBuffer buffer, String text, char terminator, int charCount) {
    writeTerminatedUnicodeString(buffer, text, terminator, charCount, false);
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer, followed by a <code>terminator</code>
   * char. If the <code>String</code> is longer than <code>charCount</code>, it is shortened to
   * <code>charCount</code> length, and no <code>terminator</code> char is written. If the
   * <code>String</code> is shorter than <code>charCount</code>, the remaining space is filled
   * with <code>terminator</code> chars.
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   * @param terminator the char value that indicates the end of the <code>String</code>
   * @param charCount the maximum number of chars for the <code>String</code>
   * @param requiresTerminator whether the null char needs to be written if the text is
   *        <code>charCount</code> chars long (ie. is the terminator always required?)
   ***********************************************************************************************/
  public static void writeTerminatedUnicodeString(ManipulatorBuffer buffer, String text, char terminator, int charCount, boolean requiresTerminator) {
    int textLength = text.length();
    if (requiresTerminator && textLength >= charCount) {
      // -1 so that the last char can be the terminator
      textLength = charCount - 1;
      text = text.substring(0, textLength);
    }
    else if (!requiresTerminator && textLength > charCount) {
      textLength = charCount;
      text = text.substring(0, textLength);
    }

    buffer.write(ByteArrayConverter.convertLittle(CharArrayConverter.convertLittle(text)));

    int padding = charCount - textLength;
    if (padding > 0) {
      byte[] terminatorBytes = ByteArrayConverter.convertLittle(terminator);
      while (padding > 0) {
        buffer.write(terminatorBytes);
        padding--;
      }
    }
  }

  /***********************************************************************************************
   * Writes a line of unicode text to the buffer, terminated by the <i>/n</i> new line character
   * @param buffer the buffer to write to
   * @param text the line to write
   ***********************************************************************************************/
  public static void writeUnicodeLine(ManipulatorBuffer buffer, String text) {
    writeUnicodeLine(buffer, text, true, false);
  }

  /***********************************************************************************************
   * Writes a line of unicode text to the buffer
   * @param buffer the buffer to write to
   * @param text the line to write
   * @param carriageReturn whether a carriage return <i>(\r)</i> should be placed at the end of
   *        the line
   * @param lineFeed whether a line feed <i>(\n)</i> should be placed at the end of the line
   ***********************************************************************************************/
  public static void writeUnicodeLine(ManipulatorBuffer buffer, String text, boolean carriageReturn, boolean lineFeed) {
    // to char[] first, so it knows its unicode, then to byte[] for writing
    buffer.write(ByteArrayConverter.convertLittle(CharArrayConverter.convertLittle(text)));
    if (carriageReturn) {
      buffer.write((byte) 13);
      buffer.write((byte) 0);
    }
    if (lineFeed) {
      buffer.write((byte) 10);
      buffer.write((byte) 0);
    }
  }

  /***********************************************************************************************
   * Writes a unicode <code>String</code> to the buffer
   * @param buffer the buffer to write to
   * @param text the unicode String to write
   ***********************************************************************************************/
  public static void writeUnicodeString(ManipulatorBuffer buffer, String text) {
    // to char[] first, so it knows its unicode, then to byte[] for writing
    buffer.write(ByteArrayConverter.convertLittle(CharArrayConverter.convertLittle(text)));
  }

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public StringHelper() {
  }
}