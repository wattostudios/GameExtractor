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

import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import javax.swing.JLabel;
import org.watto.datatype.Palette;

public class PaletteManager {

  static Palette[] palettes = new Palette[0];
  static Image[] thumbnails = new Image[0];

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void addPalette(Palette palette) {
    addPalette(palette, true);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void addPalette(Palette palette, boolean checkDuplicates) {

    int numPalettes = palettes.length;

    if (checkDuplicates) {
      for (int i = 0; i < numPalettes; i++) {
        if (palettes[i] == palette) {
          // already exists, so don't add it
          return;
        }
      }
    }

    // expand array by 1
    Palette[] temp = palettes;
    palettes = new Palette[numPalettes + 1];
    System.arraycopy(temp, 0, palettes, 0, numPalettes);

    Image[] tempThumbs = thumbnails;
    thumbnails = new Image[numPalettes + 1];
    System.arraycopy(tempThumbs, 0, thumbnails, 0, numPalettes);

    // add it
    palettes[numPalettes] = palette;
    //generateThumbnail(numPalettes);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void generateThumbnail(int paletteNumber) {
    Palette palette = palettes[paletteNumber];

    int[] paletteData = palette.getPalette();
    int numColors = paletteData.length;

    int width = (int) Math.sqrt(numColors);
    int height = width;

    if (width * height != numColors) {
      int numDifferent = numColors - (width * width);
      height += (numDifferent / width);
      if (numDifferent % width != 0) {
        height++;
      }

      int[] temp = paletteData;
      paletteData = new int[width * height];
      System.arraycopy(temp, 0, paletteData, 0, numColors);
    }

    ColorModel model = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
    Image thumbnail = new JLabel().createImage(new MemoryImageSource(width, height, model, paletteData, 0, width));
    thumbnail = thumbnail.getScaledInstance(50, -1, Image.SCALE_FAST);

    thumbnails[paletteNumber] = thumbnail;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int getIndex(Palette palette) {

    int numPalettes = palettes.length;

    for (int i = 0; i < numPalettes; i++) {
      if (palettes[i] == palette) {
        return i;
      }
    }

    return -1;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int getNumPalettes() {
    return palettes.length;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Palette getPalette(int number) {
    return palettes[number];
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Palette[] getPalettes() {
    return palettes;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Image[] getThumbnails() {
    for (int i = thumbnails.length - 1; i >= 0; i--) {
      if (thumbnails[i] == null) {
        generateThumbnail(i);
      }
      else {
        break;
      }
    }
    return thumbnails;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PaletteManager() {
  }

}