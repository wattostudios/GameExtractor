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

import java.awt.Point;

/**
**********************************************************************************************
A property, as contained in an Unreal Engine archive
**********************************************************************************************
**/
public class UnrealProperty {

  String name = "";
  long nameID = 0;
  int arrayIndex = 0;
  long length = 0;
  long typeID = 0;
  String type = "";
  Object value = "";

  /**
  **********************************************************************************************
  Not to be used - only for dummy proposes
  **********************************************************************************************
  **/
  public UnrealProperty() {
  }

  /**
  **********************************************************************************************
  UE1-UE3
  **********************************************************************************************
  **/
  public UnrealProperty(String name, long nameID, int arrayIndex, int length, int typeID) {
    this.name = name;
    this.nameID = nameID;
    this.arrayIndex = arrayIndex;
    this.length = length;
    this.typeID = typeID;
  }

  /**
  **********************************************************************************************
  UE4
  **********************************************************************************************
  **/
  public UnrealProperty(String name, long nameID, String type, long typeID, long length) {
    this.name = name;
    this.nameID = nameID;
    this.type = type;
    this.typeID = typeID;
    this.length = length;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public int getArrayIndex() {
    return arrayIndex;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public long getLength() {
    return length;
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
  public long getNameID() {
    return nameID;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public String getType() {
    return type;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public long getTypeID() {
    return typeID;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Object getValue() {
    return value;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setArrayIndex(int arrayIndex) {
    this.arrayIndex = arrayIndex;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setLength(long length) {
    this.length = length;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setName(String name) {
    this.name = name;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setNameID(long nameID) {
    this.nameID = nameID;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setType(String type) {
    this.type = type;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setTypeID(long typeID) {
    this.typeID = typeID;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setValue(Object value) {
    this.value = value;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String toString() {
    String text = "Property(" + nameID + "): " + name + "\t\tType(" + typeID + "): " + type + "\t\tLength: " + length;
    if (value instanceof Boolean) {
      text += "\t\tValue(Boolean): " + ((Boolean) value).booleanValue();
    }
    else if (value instanceof Point) {
      Point point = (Point) value;
      text += "\t\tValue(Point): " + point.getX() + "," + point.getY();
    }
    else if (value instanceof UnrealProperty[]) {
      UnrealProperty[] properties = (UnrealProperty[]) value;
      for (int i = 0; i < properties.length; i++) {
        text += "\n\t> " + properties[i].toString();
      }
    }
    else if (value instanceof UnrealProperty) {
      UnrealProperty property = (UnrealProperty) value;
      text += "\n\t> " + property.toString();
    }
    else {
      text += "\t\tValue: " + value.toString();
    }
    return text;
  }

}