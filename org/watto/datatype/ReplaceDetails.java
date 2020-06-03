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

import org.watto.io.converter.IntConverter;

public class ReplaceDetails implements Comparable<ReplaceDetails> {

  public static final boolean ENDIAN_BIG = false;

  public static final boolean ENDIAN_LITTLE = true;

  String name = "";

  long offset = -1;

  long length = 4;

  long value = 0;

  boolean endian = true; // littleEndian = true, bigEndian = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails() {
  }

  /**
  **********************************************************************************************
  @param offset the offset to the field that stores the value
  @param length the length of the field that stores the value
  @param value the decompressed length
  **********************************************************************************************
  **/
  public ReplaceDetails(String name, long offset, long length, long value) {
    this(name, offset, length, value, true);
  }

  /**
  **********************************************************************************************
  @param offset the offset to the field that stores the value
  @param length the length of the field that stores the value
  @param value the decompressed length
  @param endian the endian order (true=Little, false=Big)
  **********************************************************************************************
  **/
  public ReplaceDetails(String name, long offset, long length, long value, boolean endian) {
    this.name = name;
    this.offset = offset;
    this.length = length;
    this.value = value;
    this.endian = endian;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unlikely-arg-type")
  @Override
  public int compareTo(ReplaceDetails otherHolder) {
    int comparison = new Long(offset).compareTo(new Long(otherHolder.getOffset()));
    if (comparison == 0) {
      // PADDING must come before FILE
      if (name.equals("PaddingBefore")) {
        return 1;
      }
      else if (otherHolder.equals("PaddingBefore")) {
        return -1;
      }
      else if (name.equals("PaddingAfter")) {
        return -1;
      }
      else if (otherHolder.equals("PaddingAfter")) {
        return 1;
      }
    }
    return comparison;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean getEndian() {
    return endian;
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
  public long getOffset() {
    return offset;
  }

  /**
  **********************************************************************************************
  Gets the value of this field. Not for FILEs
  **********************************************************************************************
  **/
  public long getValue() {
    return value;
  }

  /**
  **********************************************************************************************
  Gets the value of this field, formatted with the endian order.
  **********************************************************************************************
  **/
  public long getValueWithEndian() {
    if (endian) {
      return value;
    }

    int oldValue = (int) value;
    long newValue = IntConverter.changeFormat(oldValue);
    if (newValue < 0) {
      newValue = (4294967296L + (int) newValue);
    }
    return newValue;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setEndian(boolean endian) {
    this.endian = endian;
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
  public void setOffset(long offset) {
    this.offset = offset;
  }

  /**
  **********************************************************************************************
  Sets the value of this field. Not for FILEs
  **********************************************************************************************
  **/
  public void setValue(long value) {
    this.value = value;
  }

}