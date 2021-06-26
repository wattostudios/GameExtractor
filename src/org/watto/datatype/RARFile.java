/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.datatype;

import java.io.File;
import java.util.Vector;

/**
**********************************************************************************************
RAR JNI interface (jRar)
Written by JBanes
**********************************************************************************************
**/
public class RARFile {

  // load the dll file
  static {
    System.loadLibrary("jrar");
  }
  public File file;

  public Vector<RARArchivedFile> filelist = new Vector<RARArchivedFile>();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public RARFile(File file) {
    this.file = file;
  }

  // NATIVE METHODS

  /**
  **********************************************************************************************
  Used by the JNI
  **********************************************************************************************
  **/
  private void addFile(String name, long packedSize, long unpackedSize, long timestamp, long attributes) {
    filelist.add(new RARArchivedFile(name, packedSize, unpackedSize, timestamp, attributes));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public RARArchivedFile[] getArchivedFiles() {
    if (filelist.size() < 1) {
      loadList(file.getPath());
    }

    return filelist.toArray(new RARArchivedFile[0]);
  }

  // NORMAL METHODS

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] getBytes(String filename) {
    return getData(file.getPath(), filename, "");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  private native byte[] getData(String filename, String arcfile, String password);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  private native void loadList(String filename);

}
