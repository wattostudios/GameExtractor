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

package org.watto.ge.plugin.archive.datatype;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class LZWXDictionaryEntry {

  byte[] data = null;

  int length = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public LZWXDictionaryEntry() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public LZWXDictionaryEntry(byte[] data, int length) {
    this.data = data;
    this.length = length;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

}