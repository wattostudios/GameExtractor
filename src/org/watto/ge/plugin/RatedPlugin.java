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

import org.watto.component.WSPlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class RatedPlugin implements Comparable<RatedPlugin> {

  WSPlugin plugin;
  int rating = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public RatedPlugin(WSPlugin plugin, int rating) {
    this.plugin = plugin;
    this.rating = rating;
  }

  /**
  **********************************************************************************************
  NOTE - the "0-" is needed to sort them in REVERSE order (ie 100 > 1 instead of 1 > 100)
  **********************************************************************************************
  **/
  @Override
  public int compareTo(RatedPlugin other) {
    return 0 - (getRatingInteger().compareTo(other.getRatingInteger()));
    //return toString().compareTo(other.toString());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSPlugin getPlugin() {
    return plugin;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getRating() {
    return rating;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Integer getRatingInteger() {
    return new Integer(rating);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String toString() {
    return plugin.toString();
  }

}