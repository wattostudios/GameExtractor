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

import org.watto.datatype.Resource;

/**
**********************************************************************************************
  Allows you to sort an array of Resources by their OriginalOffset (Plugin REZ_REZMGR only)
**********************************************************************************************
**/
public class REZ_REZMGR_ResourceSorter implements Comparable<REZ_REZMGR_ResourceSorter> {

  Resource resource;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public REZ_REZMGR_ResourceSorter(Resource resource) {
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int compareTo(REZ_REZMGR_ResourceSorter obj) {
    long otherOffset = obj.getOffset();
    long offset = Long.parseLong(resource.getProperty("OriginalOffset"));
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
  public long getOffset() {
    return Long.parseLong(resource.getProperty("OriginalOffset"));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource() {
    return resource;
  }

}