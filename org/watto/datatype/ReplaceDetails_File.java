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

public class ReplaceDetails_File extends ReplaceDetails {

  ReplacableResource resource;

  /**
  **********************************************************************************************
  @param offset the offset to the resource
  @param length the length of the resource
  @param value the resource
  **********************************************************************************************
  **/
  public ReplaceDetails_File(String name, long offset, long length, ReplacableResource resource) {
    this.name = name;
    this.offset = offset;
    this.length = length;
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource getResource() {
    return resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setResource(ReplacableResource resource) {
    this.resource = resource;
  }

}