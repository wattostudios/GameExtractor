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

import java.io.*;


/***********************************************************************************************
A file filter that only shows files
***********************************************************************************************/
public class FilesOnlyFileFilter implements FileFilter {
  

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public FilesOnlyFileFilter(){}


  /***********************************************************************************************
  Whether the <code>File</code> is a file
  @param path the <code>File</code> to check
  @return <b>true</b>  if the <code>File</code> is a file<br />
          <b>false</b> if the <code>File</code> is a directory or a drive.
  ***********************************************************************************************/
  public boolean accept(File path){
    if (path.isFile()) {
      return true;
    }
    return false;
  }


}