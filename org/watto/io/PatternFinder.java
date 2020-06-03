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

import org.watto.io.buffer.FileBuffer;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.converter.ByteArrayConverter;
import java.io.File;


/***********************************************************************************************
Utilities for finding values in a <code>ManipulatorBuffer</code>
***********************************************************************************************/
public class PatternFinder {

  /** The buffer to search for patterns **/
  ManipulatorBuffer buffer = null;


  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public PatternFinder(){}


  /***********************************************************************************************
  Creates a <code>PatternFinder</code> for the <code>file</code>
  @param file the <code>File</code> to search for patterns
  ***********************************************************************************************/
  public PatternFinder(File file){
    this.buffer = new FileBuffer(file,false);
  }


  /***********************************************************************************************
  Creates a <code>PatternFinder</code> for the <code>Manipulator</code>
  @param manipulator the <code>Manipulator</code> to search for patterns
  ***********************************************************************************************/
  public PatternFinder(Manipulator manipulator){
    this.buffer = manipulator.getBuffer();
  }


  /***********************************************************************************************
  Creates a <code>PatternFinder</code> for the <code>ManipulatorBuffer</code>
  @param file the <code>ManipulatorBuffer</code> to search for patterns
  ***********************************************************************************************/
  public PatternFinder(ManipulatorBuffer buffer){
    this.buffer = buffer;
  }


  /***********************************************************************************************
  Searches the <code>buffer</code> for the next occurrence of <code>byteValue</code>. Moves the
  file pointer to the matching offset
  @param byteValue the <code>byte</code> to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long find(byte byteValue){
    return find(new byte[]{byteValue});
  }


  /***********************************************************************************************
  Searches the <code>buffer</code> for the next occurrence of a series of <code>byte</code>s.
  Moves the file pointer to the matching offset
  @param bytes the <code>byte</code>s to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long find(byte[] bytes){
    return find(bytes,buffer.length());
  }
  
  
  /***********************************************************************************************
  Searches the <code>buffer</code> for the next occurrence of a series of <code>byte</code>s.
  Moves the file pointer to the matching offset
  @param bytes the <code>byte</code>s to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long find(byte[] bytes, long maxSearchOffset){
    long pointer = buffer.getPointer();

    int arraySize = bytes.length;
    int matchPos = 0; // the position in the bytes array that is being matched

    while (pointer < maxSearchOffset) {
      if (buffer.read() == bytes[matchPos]) {
        matchPos++;
        if (matchPos >= arraySize) {
          // found the full string
          pointer -= arraySize;
          pointer++;
          buffer.seek(pointer);
          return pointer;
        }
      }
      else {
        matchPos = 0;
      }
      pointer++;
    }

    return -1;
  }


  /***********************************************************************************************
  Searches the <code>buffer</code> for the next occurrence of a <code>String</code>. Moves the
  file pointer to the matching offset
  @param text the <code>String</code> to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long find(String text){
    return find(ByteArrayConverter.convertLittle(text));
  }


  /***********************************************************************************************
  Moves forward 1 <code>byte</code>, then searches the <code>buffer</code> for the next occurrence
  of <code>byteValue</code>. Moves the file pointer to the matching offset
  @param byteValue the <code>byte</code> to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long findNext(byte byteValue){
    buffer.skip(1);
    return find(byteValue);
  }


  /***********************************************************************************************
  Moves forward 1 <code>byte</code>, then searches the <code>buffer</code> for the next occurrence
  of a series of <code>byte</code>s. Moves the file pointer to the matching offset
  @param bytes the <code>byte</code>s to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long findNext(byte[] bytes){
    buffer.skip(1);
    return find(bytes);
  }


  /***********************************************************************************************
  Moves forward 1 <code>byte</code>, then searches the <code>buffer</code> for the next occurrence
  of a <code>String</code>. Moves the file pointer to the matching offset
  @param text the <code>String</code> to search for in the <code>buffer</code>
  @return the offset to the match
  ***********************************************************************************************/
  public long findNext(String text){
    buffer.skip(1);
    return find(text);
  }
}