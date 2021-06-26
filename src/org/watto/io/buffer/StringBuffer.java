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


/***********************************************************************************************
A class that reads, writes, and buffers data from a <code>String</code> data source.
***********************************************************************************************/
public class StringBuffer extends ByteBuffer implements ManipulatorBuffer {

  /***********************************************************************************************
  Creates the buffer for the <code>text</code> <code>String</code> data
  @param text the <code>String</code> data to buffer
  ***********************************************************************************************/
  public StringBuffer(String text){
    super();
    try {
      buffer = text.getBytes("UTF-8");
      bufferSize = buffer.length;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }


  /***********************************************************************************************
  Gets the underlying <code>String</code> that is buffered
  @return the buffered <code>String</code>
  ***********************************************************************************************/
  public String getString(){
    try {
      return new String(buffer,"UTF-8");
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return "";
    }
  }

}