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

package org.watto.ge.plugin.resource;

import org.watto.datatype.Resource;

public class Resource_Property extends Resource {

  String code = "";

  String value = "";

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property(String code) {
    this.code = code;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property(String code, int value) {
    this(code, "" + value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property(String code, long value) {
    this(code, "" + value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property(String code, String value) {
    this.code = code;
    this.value = value;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getCode() {
    return code;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getValue() {
    return value;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setCode(String code) {
    this.code = code;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setValue(String value) {
    this.value = value;
  }

}