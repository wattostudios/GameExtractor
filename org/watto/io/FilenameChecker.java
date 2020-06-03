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
Utilities for checking and correcting errors in filenames. For example, checks for and removes
invalid characters like <i>:</i> and <i>tab</i>
***********************************************************************************************/
public class FilenameChecker {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public FilenameChecker(){
  }


  /***********************************************************************************************
  Checks the <code>File</code> for any invalid characters
  @param file the <code>File</code> to check
  @return -1 if the <code>File</code> is valid. Otherwise, the <code>File</code> is not valid, and
  the value returned is the index of the first invalid character in the filename
  ***********************************************************************************************/
  public static int checkFilename(File file){
    if (file.exists()) {
      return -1;
    }

    String path = file.getAbsolutePath();
    int length = path.length();

    for (int i = 0;i < length;i++) {
      char currentChar = path.charAt(i);
      if ((currentChar >= 32 || currentChar < 0) && currentChar != 34 && currentChar != 42 && currentChar != 47 && currentChar != 58 && currentChar != 60 && currentChar != 62 && currentChar != 63 && currentChar != 92 && currentChar != 124) {
        //the character is valid
      }
      else {
        return i;
      }
    }

    return -1;
  }


  /***********************************************************************************************
  Checks and corrects any invalid characters in the <code>File</code>. Invalid characters are
  replaced with the <i>_</i> character instead.
  @param file the <code>File</code> to check and correct
  @return the new valid <code>File</code>
  ***********************************************************************************************/
  public static File correctFilename(File file){
    return correctFilename(file,'_');
  }


  /***********************************************************************************************
  Checks and corrects any invalid characters in the <code>File</code>. Invalid characters are
  replaced with the <code>replaceCharacter</code> instead.
  @param file the <code>File</code> to check and correct
  @param replaceCharacter the <code>char</code> to use as a replacement for invalid characters
  @return the new valid <code>File</code>
  ***********************************************************************************************/
  public static File correctFilename(File file,char replaceCharacter){
    if (file.exists()) {
      return file;
    }

    String path = "";
    File parent = new File(file.getAbsolutePath());

    boolean everChanged = false;
    while (parent != null && !parent.exists()) {
      String name = parent.getName();
      char[] chars = name.toCharArray();
      int charCount = chars.length;

      boolean changed = false;
      for (int i = 0;i < charCount;i++) {
        char currentChar = chars[i];

        if ((currentChar >= 32 || currentChar < 0) && currentChar != 34 && currentChar != 42 && currentChar != 47 && currentChar != 58 && currentChar != 60 && currentChar != 62 && currentChar != 63 && currentChar != 92 && currentChar != 124) {
          //the character is valid
        }
        else {
          chars[i] = replaceCharacter;
          changed = true;
          everChanged = true;
        }
      }

      if (changed) {
        name = new String(chars);
      }

      if (path.length() > 0) {
        path = name + File.separator + path;
      }
      else {
        path = name + path;
      }

      parent = parent.getParentFile();
    }

    if (!everChanged) {
      // path was valid
      return file;
    }
    else if (parent == null) {
      return new File(path);
    }
    else {
      path = parent.getAbsolutePath() + File.separator + path;
      return new File(path);
    }
  }


  /***********************************************************************************************
  Builds the next incremental filename after the given one. If the <code>File</code> does not exist,
  the <code>File</code> is returned. If the <code>File</code> does exist, an incremental number is
  appended to the end of the filename until a valid filename is found, which can then be returned.
  @param file the <code>File</code> to check
  @return the next valid <code>File</code>
  ***********************************************************************************************/
  public static File incrementFilename(File file){
    file = correctFilename(file);

    if (file.exists()) {
      String filename = FilenameSplitter.getDirectory(file) + File.separator + FilenameSplitter.getFilename(file) + "[";
      String extension = "]." + FilenameSplitter.getExtension(file);

      int i = 1;
      file = new File(filename + i + extension);
      while (file.exists()) {
        i++;
        file = new File(filename + i + extension);
      }
    }

    return file;
  }

}