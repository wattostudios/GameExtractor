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

public class ColorSplitAlpha extends ColorSplit {

  int alpha = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public ColorSplitAlpha() {
    super();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public ColorSplitAlpha(int color) {
    super(color);

    int checkAlpha = ((color & 0xff000000) >> 24);
    if (checkAlpha < 0) {
      checkAlpha = 128 + (0 - checkAlpha);
      //checkAlpha = ByteConverter.unsign((byte) checkAlpha);
      //127 - checkAlpha;
    }
    this.alpha = checkAlpha;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public int getAlpha() {
    return alpha;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getCloseness(ColorSplit otherColor) {
    if (mappedColor != null || otherColor.getMappedColor() != null) {
      //this color was replaced
      return 999;
    }

    int r = red - otherColor.getRed();
    int g = green - otherColor.getGreen();
    int b = blue - otherColor.getBlue();
    int a = 0;

    if (otherColor instanceof ColorSplitAlpha) {
      a = alpha - ((ColorSplitAlpha) otherColor).getAlpha();
      a *= 3; // alphas a more noticable if the closeness is wrong, so we want to factor closeness on RGB much more than on A
    }

    if (r < 0) {
      r = 0 - r;
    }
    if (g < 0) {
      g = 0 - g;
    }
    if (b < 0) {
      b = 0 - b;
    }
    if (a < 0) {
      a = 0 - a;
    }

    return r + g + b + a;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setAlpha(int alpha) {
    this.alpha = alpha;
  }

}