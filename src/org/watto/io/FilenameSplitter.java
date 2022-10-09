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


/***********************************************************************************************
Utilities for splitting a <code>File</code> into directories, filename, and extension
***********************************************************************************************/
public class FilenameSplitter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public FilenameSplitter(){

  }


  /***********************************************************************************************
  Gets the directories for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the directories
  ***********************************************************************************************/
  public static String getDirectory(File file){
    return getDirectory(file.getAbsolutePath());
  }


  /***********************************************************************************************
  Gets the directories for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the directories
  ***********************************************************************************************/
  public static String getDirectory(String file){

    int slashPos = file.lastIndexOf("\\");
    int backwardSlashPos = file.lastIndexOf("/");

    if (backwardSlashPos > slashPos) {
      slashPos = backwardSlashPos;
    }

    if (slashPos == -1) {
      // no slashes means no directories
      return "";
    }

    return file.substring(0,slashPos);
  }
  
  
  /***********************************************************************************************
  Gets the parent directories for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the parent directories
  ***********************************************************************************************/
  public static String getParentDirectory(File file){
    return getParentDirectory(file.getAbsolutePath());
  }


  /***********************************************************************************************
  Gets the parent directories for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the parent directories
  ***********************************************************************************************/
  public static String getParentDirectory(String file){

    int slashPos = file.lastIndexOf("\\");
    int backwardSlashPos = file.lastIndexOf("/");

    if (backwardSlashPos > slashPos) {
      slashPos = backwardSlashPos;
    }
    
    int endOffset = file.length() - 1;
    
    if (slashPos == endOffset){
      // the file is a directory, so  need to get the parent
      slashPos = file.lastIndexOf("\\",endOffset);
      backwardSlashPos = file.lastIndexOf("/",endOffset);

      if (backwardSlashPos > slashPos) {
        slashPos = backwardSlashPos;
      }
    }
    
    if (slashPos == -1) {
      // no slashes means no directories
      return "";
    }

    return file.substring(0,slashPos);
  }


  /***********************************************************************************************
  Gets the extension for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the extension
  ***********************************************************************************************/
  public static String getExtension(File file){
    return getExtension(file.getAbsolutePath());
  }


  /***********************************************************************************************
  Gets the extension for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the extension
  ***********************************************************************************************/
  public static String getExtension(String file){
    int dotPos = file.lastIndexOf(".");
    if (dotPos < 0) {
      // no ".", so no extension for the file
      return "";
    }

    file = file.substring(dotPos + 1);
    if (file.lastIndexOf("/") > 0 || file.lastIndexOf("\\") > 0) {
      // the last "." was part of a directory name, so isn't a real extension 
      return "";
    }

    return file;
  }


  /***********************************************************************************************
  Gets the filename for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the filename
  ***********************************************************************************************/
  public static String getFilename(File file){
    return getFilename(file.getAbsolutePath());
  }


  /***********************************************************************************************
  Gets the filename for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the filename
  ***********************************************************************************************/
  public static String getFilename(String file){
    int dotPos = file.lastIndexOf(".");

    if (dotPos < 0) {
      // no ".", so no extension for the file
      dotPos = file.length();
    }

    int slashPos = file.lastIndexOf("\\");
    int backwardSlashPos = file.lastIndexOf("/");

    if (backwardSlashPos > slashPos) {
      slashPos = backwardSlashPos;
    }

    // add 1 to the slashPos so we don't include the slash in the filename 
    slashPos++;

    if (slashPos < 0) {
      slashPos = 0;
    }
    
    if (slashPos > dotPos) {
      // dot before the slash, so the dot is not part of the filename. Generally occurs when the filename doesn't have an extension
      return file.substring(slashPos); 
    }

    return file.substring(slashPos,dotPos);

  }
  
  
  /***********************************************************************************************
  Gets the filename and extension for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the filename and extension
  ***********************************************************************************************/
  public static String getFilenameAndExtension(File file){
    return getFilenameAndExtension(file.getAbsolutePath());
  }


  /***********************************************************************************************
  Gets the filename and extension for the <code>File</code>
  @param file the <code>File</code> to analyse
  @return the filename and extension
  ***********************************************************************************************/
  public static String getFilenameAndExtension(String file){
    int slashPos = file.lastIndexOf("\\");
    int backwardSlashPos = file.lastIndexOf("/");

    if (backwardSlashPos > slashPos) {
      slashPos = backwardSlashPos;
    }

    // add 1 to the slashPos so we don't include the slash in the filename 
    slashPos++;

    if (slashPos < 0) {
      slashPos = 0;
    }

    return file.substring(slashPos,file.length());

  }

}