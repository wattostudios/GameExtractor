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

/**
**********************************************************************************************
RAR JNI interface (jRar)
Written by JBanes
**********************************************************************************************
**/
public class RARArchivedFile {

  public static final long DIRECTORY = 0x10;

  String name;
  long packedSize;
  long unpackedSize;
  long timestamp;
  long attributes;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public RARArchivedFile(String name, long packedSize, long unpackedSize, long timestamp, long attributes) {
    this.name = name;
    this.packedSize = packedSize;
    this.unpackedSize = unpackedSize;
    this.timestamp = timestamp;
    this.attributes = attributes;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getAttributes() {
    return attributes;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getName() {
    return name;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getPackedSize() {
    return packedSize;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getTimestamp() {
    return timestamp;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getUnpackedSize() {
    return unpackedSize;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isDirectory() {
    return ((attributes & DIRECTORY) > 0);
  }

}
