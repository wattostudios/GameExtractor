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

package org.watto.ge.plugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class PluginAssistant_TRL_DirEntry {

  String filename;
  int entryType;
  int leftNodeDID;
  int rightNodeDID;
  int rootNodeDID;
  int fileSID;
  int length;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PluginAssistant_TRL_DirEntry(String filename, int entryType, int leftNodeDID, int rightNodeDID, int rootNodeDID, int fileSID, long length) {
    this.filename = filename;
    this.entryType = entryType;
    this.leftNodeDID = leftNodeDID;
    this.rightNodeDID = rightNodeDID;
    this.rootNodeDID = rootNodeDID;
    this.fileSID = fileSID;
    this.length = (int) length;
  }

  public int getEntryType() {
    return entryType;
  }

  // GET
  public String getFilename() {
    return filename;
  }

  public int getFileSID() {
    return fileSID;
  }

  public int getLeftNode() {
    return leftNodeDID;
  }

  public int getLength() {
    return length;
  }

  public int getRightNode() {
    return rightNodeDID;
  }

  public int getRootNode() {
    return rootNodeDID;
  }

  public void setEntryType(int entryType) {
    this.entryType = entryType;
  }

  // SET
  public void setFilename(String filename) {
    this.filename = filename;
  }

  public void setFileSID(int fileSID) {
    this.fileSID = fileSID;
  }

  public void setLeftNode(int leftNodeDID) {
    this.leftNodeDID = leftNodeDID;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public void setRightNode(int rightNodeDID) {
    this.rightNodeDID = rightNodeDID;
  }

  public void setRootNode(int rootNodeDID) {
    this.rootNodeDID = rootNodeDID;
  }

}
