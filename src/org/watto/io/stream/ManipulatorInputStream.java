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

package org.watto.io.stream;

import org.watto.io.*;
import org.watto.io.converter.ByteConverter;
import java.io.File;
import java.io.InputStream;


/***********************************************************************************************
An <code>InputStream</code> wrapper for <code>Manipulator</code>s
***********************************************************************************************/
public class ManipulatorInputStream extends InputStream {

  /** the reading source **/
  Manipulator manipulator;

  /** the marked location in the source **/
  int mark = 0;


  /***********************************************************************************************
  Creates an <code>InputStream</code> for the <code>file</code>
  @param file the <code>File</code> to read from
  ***********************************************************************************************/
  public ManipulatorInputStream(File file){
    this.manipulator = new FileManipulator(file,false);
  }


  /***********************************************************************************************
  Creates an <code>InputStream</code> for the <code>Manipulator</code>
  @param manipulator the <code>Manipulator</code> to read from
  ***********************************************************************************************/
  public ManipulatorInputStream(Manipulator manipulator){
    this.manipulator = manipulator;
  }


  /***********************************************************************************************
  Gets the number of bytes left to read from the <code>Manipulator</code> source
  @return the remaining length
  ***********************************************************************************************/
  public int available(){
    return (int)manipulator.getRemainingLength();
  }


  /***********************************************************************************************
  Closes the <code>Manipulator</code> source
  ***********************************************************************************************/
  public void close(){
    manipulator.close();
  }


  /***********************************************************************************************
  Gets the length of the <code>Manipulator</code> source
  @return the length of the source
  ***********************************************************************************************/
  public long getLength(){
    return manipulator.getLength();
  }


  /***********************************************************************************************
  Gets the underlying <code>Manipulator</code> source
  @return the <code>Manipulator</code> source
  ***********************************************************************************************/
  public Manipulator getManipulator(){
    return manipulator;
  }


  /***********************************************************************************************
  Gets the read offset in the <code>Manipulator</code> source
  @return the reading offset
  ***********************************************************************************************/
  public long getOffset(){
    return manipulator.getOffset();
  }


  /***********************************************************************************************
  Marks the current offset in the <code>Manipulator</code> source
  @param readLimit the number of bytes to read before the mark becomes invalid
  ***********************************************************************************************/
  public void mark(int readLimit){
    try {
      mark = (int)manipulator.getOffset();
    }
    catch (Throwable t) {
      mark = 0;
    }
  }


  /***********************************************************************************************
  Whether marking is supported by this <code>InputStream</code>
  @return true
  ***********************************************************************************************/
  public boolean markSupported(){
    return true;
  }


  /***********************************************************************************************
  Reads a <code>byte</code> from the <code>Manipulator</code> source
  @return the <code>byte</code> value
  ***********************************************************************************************/
  public int read(){
    return ByteConverter.unsign(manipulator.readByte());
  }


  /***********************************************************************************************
  Reads a number of <code>byte</code>s from the <code>Manipulator</code> source
  @param byteArray the array to read the <code>byte</code>s in to
  @return the number of bytes that were read
  ***********************************************************************************************/
  public int read(byte[] byteArray){
    return manipulator.getBuffer().read(byteArray);
  }


  /***********************************************************************************************
  Reads a number of <code>byte</code>s from the <code>Manipulator</code> source
  @param byteArray the array to read the <code>byte</code>s in to
  @param offset the offset in the <code>byteArray</code> to store the read values
  @param length the number of <code>byte</code>s to read
  @return the number of bytes that were actually read
  ***********************************************************************************************/
  public int read(byte[] byteArray,int offset,int length){
    return manipulator.getBuffer().read(byteArray,offset,length);
  }


  /***********************************************************************************************
  Moves to the <code>mark</code>ed position in the <code>Manipulator</code> source, and resets the
  <code>mark</code>ed position
  ***********************************************************************************************/
  public void reset(){
    try {
      manipulator.seek(mark);
    }
    catch (Throwable t) {}

    mark = 0;
  }


  /***********************************************************************************************
  Moves to the <code>offset</code> in the <code>Manipulator</code> source
  @param offset the offset to seek to
  ***********************************************************************************************/
  public void seek(long offset){
    manipulator.seek(offset);
  }


  /***********************************************************************************************
  Skips over a number of bytes in the <code>Manipulator</code> source
  @param byteCount the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  public long skip(long byteCount){
    return manipulator.skip(byteCount);
  }
}