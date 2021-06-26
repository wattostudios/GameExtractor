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

package org.watto.ge.plugin.viewer;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RAW_MHWANH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RAW_MHWANH() {
    super("RAW_MHWANH", "Ecstatica 2 RAW Image");
    setExtensions("raw");

    setGames("Ecstatica 2");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(6).equals("mhwanh")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 6 - Header (mhwanh)
      // 1 - null
      // 1 - Unknown (4)
      fm.skip(8);

      // 1 - Image Format (0/1/2)
      int imageFormat = fm.readByte();

      // 1 - Image Width
      int width = ByteConverter.unsign(fm.readByte());
      if (imageFormat == 1 || imageFormat == 2) {
        width *= 5;
      }

      // 1 - Unknown
      fm.skip(1);

      int height = 0;
      if (imageFormat == 2) {
        // 2 - Image Height
        height = fm.readShort();
      }
      else {
        // 1 - Image Height
        height = ByteConverter.unsign(fm.readByte());
      }

      // 1 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown (44)
      // 13 - null
      fm.seek(32);

      int numColors = 256;

      // X - Palette
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Blue
        // 1 - Green
        // 1 - Red
        int b = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int r = ByteConverter.unsign(fm.readByte());

        palette[i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
      }

      // X - Pixels (paletted)
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        pixels[p] = palette[ByteConverter.unsign(fm.readByte())];
      }

      // for image type 1...
      // X - Depth Pixels (RGB565)
      // X - Unknown

      ImageResource imageResource = new ImageResource(pixels, width, height);

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}