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
import java.io.File;
import java.io.OutputStream;


/***********************************************************************************************
An <code>OutputStream</code> wrapper for <code>Manipulator</code>s
***********************************************************************************************/
public class ManipulatorOutputStream extends OutputStream {

  /** the writing source **/
  Manipulator manipulator;


  /***********************************************************************************************
  Creates an <code>OuputStream</code> for the <code>file</code>
  @param file the <code>File</code> to write to
  ***********************************************************************************************/
  public ManipulatorOutputStream(File file){
    this.manipulator = new FileManipulator(file,true);
  }


  /***********************************************************************************************
  Creates an <code>OutputStream</code> for the <code>Manipulator</code>
  @param manipulator the <code>Manipulator</code> to write to
  ***********************************************************************************************/
  public ManipulatorOutputStream(Manipulator manipulator){
    this.manipulator = manipulator;
  }


  /***********************************************************************************************
  Closes the <code>Manipulator</code> source
  ***********************************************************************************************/
  public void close(){
    manipulator.close();
  }


  /***********************************************************************************************
  Does nothing - the flushing is controlled by the underlying <code>Manipulator</code> source
  ***********************************************************************************************/
  public void flush(){}


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
  Gets the write offset in the <code>Manipulator</code> source
  @return the writing offset
  ***********************************************************************************************/
  public long getOffset(){
    return manipulator.getOffset();
  }


  /***********************************************************************************************
  Moves to the <code>offset</code> in the <code>Manipulator</code> source
  @param offset the offset to seek to
  ***********************************************************************************************/
  public void seek(long offset){
    manipulator.seek(offset);
  }


  /***********************************************************************************************
  Writes a number of <code>byte</code>s to the <code>Manipulator</code> source
  @param byteArray the <code>byte</code>s to write
  ***********************************************************************************************/
  public void write(byte[] byteArray){
    manipulator.writeBytes(byteArray);
  }


  /***********************************************************************************************
  Writes a number of <code>byte</code>s to the <code>Manipulator</code> source
  @param byteArray the <code>byte</code>s to write
  @param offset the first <code>byte</code> in the <code>byteArray</code> to write
  @param length the number of <code>byte</code>s to write
  ***********************************************************************************************/
  public void write(byte[] byteArray,int offset,int length){
    manipulator.getBuffer().write(byteArray,offset,length);
  }


  /***********************************************************************************************
  Writes a single <code>byte</code> to the <code>Manipulator</code> source
  @param b the <code>byte</code> to write
  ***********************************************************************************************/
  public void write(int b){
    manipulator.writeByte((byte)b);
  }
}