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

package org.watto.ge.helper;

/**
**********************************************************************************************
  Holds 2 items of data (offset, filename) and sorts based on the offset
**********************************************************************************************
**/
public class ResourceSorter_OffsetName implements Comparable<ResourceSorter_OffsetName> {

  long offset;
  String name;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ResourceSorter_OffsetName(long offset, String name) {
    this.offset = offset;
    this.name = name;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int compareTo(ResourceSorter_OffsetName obj) {
    long otherOffset = obj.getOffset();
    if (offset == otherOffset) {
      return 0;
    }
    else if (offset < otherOffset) {
      return -1;
    }
    else {
      return 1;
    }
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
  public long getOffset() {
    return offset;
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
  public void setOffset(int offset) {
    this.offset = offset;
  }

}