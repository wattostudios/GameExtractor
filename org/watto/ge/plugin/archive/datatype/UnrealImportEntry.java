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
An entry from the Import Directory of an Unreal Engine archive
**********************************************************************************************
**/
public class UnrealImportEntry {

  long parentNameID = 0;
  long typeID = 0;
  int parentID = -1;
  int nameID = 0;
  int unknownID = 0;
  String name = "";
  String type = "";

  /**
  **********************************************************************************************
  Not to be used - only for dummy proposes
  **********************************************************************************************
  **/
  public UnrealImportEntry() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public UnrealImportEntry(long parentNameID, String type, long typeID, int parentID, String name, int nameID, int unknownID) {
    this.parentNameID = parentNameID;
    this.type = type;
    this.typeID = typeID;
    this.parentID = parentID;
    this.name = name;
    this.nameID = nameID;
    this.unknownID = unknownID;
  }

  public String getName() {
    return name;
  }

  public int getNameID() {
    return nameID;
  }

  public int getParentID() {
    return parentID;
  }

  public long getParentNameID() {
    return parentNameID;
  }

  public String getType() {
    return type;
  }

  public long getTypeID() {
    return typeID;
  }

  public int getUnknownID() {
    return unknownID;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNameID(int nameID) {
    this.nameID = nameID;
  }

  public void setParentID(int parentID) {
    this.parentID = parentID;
  }

  public void setParentNameID(long parentNameID) {
    this.parentNameID = parentNameID;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setTypeID(long typeID) {
    this.typeID = typeID;
  }

  public void setUnknownID(int unknownID) {
    this.unknownID = unknownID;
  }

}