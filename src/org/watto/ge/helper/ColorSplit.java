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

public class ColorSplit implements Comparable<ColorSplit> {

  int color = 0;

  int red = 0;
  int green = 0;
  int blue = 0;

  ColorSplit mappedColor = null;

  int paletteIndex = -1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ColorSplit() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ColorSplit(int color) {
    this.color = color;

    this.red = ((color & 0x00ff0000) >> 16);
    this.green = ((color & 0x0000ff00) >> 8);
    this.blue = (color & 0x000000ff);
  }

  /////
  //
  // METHODS
  //
  /////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int compareTo(ColorSplit otherObject) {
    int otherColor = otherObject.getColor();
    if (otherColor == color) {
      return 0;
    }
    else if (otherColor > color) {
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
  public int getBlue() {
    return blue;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getCloseness(ColorSplit otherColor) {
    if (mappedColor != null || otherColor.getMappedColor() != null) {
      //this color was replaced
      return 999;
    }

    int r = red - otherColor.getRed();
    int g = green - otherColor.getGreen();
    int b = blue - otherColor.getBlue();

    if (r < 0) {
      r = 0 - r;
    }
    if (g < 0) {
      g = 0 - g;
    }
    if (b < 0) {
      b = 0 - b;
    }

    return r + g + b;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getColor() {
    return color;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getGreen() {
    return green;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ColorSplit getMappedColor() {
    return mappedColor;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getPaletteIndex() {
    if (paletteIndex == -1) {
      paletteIndex = mappedColor.getPaletteIndex();
    }
    return paletteIndex;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getRed() {
    return red;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void recalculateColor() {
    this.color = red << 16 | green << 8 | blue;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setBlue(int blue) {
    this.blue = blue;
    recalculateColor();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setColor(int color) {
    this.color = color;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setGreen(int green) {
    this.green = green;
    recalculateColor();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setMappedColor(ColorSplit mappedColor) {
    this.mappedColor = mappedColor;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPaletteIndex(int paletteIndex) {
    this.paletteIndex = paletteIndex;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setRed(int red) {
    this.red = red;
    recalculateColor();
  }

}