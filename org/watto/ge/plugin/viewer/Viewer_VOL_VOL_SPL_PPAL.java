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
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_VOL_VOL;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VOL_VOL_SPL_PPAL extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_VOL_VOL_SPL_PPAL() {
    super("VOL_VOL_SPL_PPAL", "Red Baron 3D SPL Image");
    setExtensions("spl");

    setGames("Red Baron 3D");
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_VOL_VOL) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("PPAL")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(4);

      // 4 - Header
      if (fm.readString(4).equals("head")) {
        rating += 5;
      }

      // 4 - Header Length (4)
      if (fm.readInt() == 4) {
        rating += 5;
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

      long arcSize = fm.getLength();

      // 4 - Header (PPAL)
      // 4 - Image Length [+44]
      // 4 - Header (head)
      // 4 - Header Length (4)
      // 4 - Unknown (4)
      // 4 - Header (data)
      fm.skip(24);

      // 4 - Color Palette Length (1024)
      int numColors = fm.readInt() / 4;
      FieldValidator.checkNumColors(numColors);

      // X - Palette
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Blue
        // 1 - Green
        // 1 - Red
        // 1 - Alpha
        int b = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int r = ByteConverter.unsign(fm.readByte());
        int a = 255 - ByteConverter.unsign(fm.readByte()); // reverse alpha values

        palette[i] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      // 4 - Header (pspl)
      fm.skip(4);

      // 4 - Image Data Length (including next field)
      int imageDataLength = fm.readInt() - 2;
      FieldValidator.checkLength(imageDataLength, arcSize);

      // 2 - Unknown (1028/1029)
      fm.skip(2);

      int width = 256;
      int height = imageDataLength / width;

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        pixels[p] = palette[ByteConverter.unsign(fm.readByte())];
      }

      fm.close();

      return new ImageResource(pixels, width, height);

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