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

import java.io.File;
import org.watto.io.Manipulator;


/***********************************************************************************************
An <code>OutputStream</code> wrapper for <code>Manipulator</code>s. When the <i>close()</i> method
is called, the underlying <code>Manipulator</code> is not closed.
***********************************************************************************************/
public class ManipulatorUnclosableOutputStream extends ManipulatorOutputStream {

  /***********************************************************************************************
  Creates an <code>OutputStream</code> for the <code>file</code>
  @param file the <code>File</code> to write to
  ***********************************************************************************************/
  public ManipulatorUnclosableOutputStream(File file){
    super(file);
  }


  /***********************************************************************************************
  Creates an <code>OutputStream</code> for the <code>Manipulator</code>
  @param manipulator the <code>Manipulator</code> to write to
  ***********************************************************************************************/
  public ManipulatorUnclosableOutputStream(Manipulator manipulator){
    super(manipulator);
  }


  /***********************************************************************************************
  Does nothing.
  ***********************************************************************************************/
  public void close(){}
}