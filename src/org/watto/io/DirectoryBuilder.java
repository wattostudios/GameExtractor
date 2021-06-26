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
import org.watto.ErrorLogger;

/***********************************************************************************************
Utilities for creating a directory tree for a given <code>File</code> path
***********************************************************************************************/
public class DirectoryBuilder {

  /***********************************************************************************************
  Creates the directory tree for the given <code>path</code>. The <code>path</code> must be a
  directory location, not a file.
  @param path the directory tree to create
  ***********************************************************************************************/
  public static void buildDirectory(File path) {
    buildDirectory(path, true);
  }

  /***********************************************************************************************
  Creates the directory tree for the given <code>path</code>. The <code>path</code> can be a file
  or a directory.
  @param path the directory tree to create
  @param isDirectory true if the <code>File</code> is a directory, false if the <code>File</code>
  is a file
  ***********************************************************************************************/
  public static void buildDirectory(File path, boolean isDirectory) {
    if (path == null) {
      return;
    }

    try {

      File parent = path.getParentFile();
      if (parent == null) {
        return;
      }
      if (!parent.exists()) {
        buildDirectory(parent, true);
      }

      if (isDirectory) {
        path.mkdir();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public DirectoryBuilder() {
  }

}