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
import org.watto.ge.helper.ColorSplitAlpha;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TEX_3TEX;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_3TEX_BMP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_3TEX_BMP() {
    super("TEX_3TEX_BMP", "High Heat Baseball 2000 BMP Image");
    setExtensions("bmp");

    setGames("High Heat Baseball 2000");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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
      if (plugin instanceof Plugin_TEX_3TEX) {
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

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Unknown (1)
      if (fm.readInt() == 1) {
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

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (1)
      fm.skip(4);

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        pixels[p] = ByteConverter.unsign(fm.readByte());
      }

      // X - Palette
      int numColors = 256;
      int[] palette = new int[numColors];
      for (int c = 0; c < numColors; c++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());
        int a = ByteConverter.unsign(fm.readByte());

        palette[c] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      // convert pixels to their real values
      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        pixels[p] = palette[pixels[p]];
      }

      // X - Pixels
      ImageResource imageResource = new ImageResource(pixels, width, height);

      fm.close();

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);
      im.convertToPaletted();
      im.changeColorCount(256);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // 4 - Width
      fm.writeInt(imageWidth);

      // 4 - Height
      fm.writeInt(imageHeight);

      // 4 - Unknown (1)
      fm.writeInt(1);

      // X - Pixels
      int[] pixels = im.getPixels();
      int numPixels = pixels.length;

      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        fm.writeByte(pixels[p]);
      }

      // X - Palette
      int[] palette = im.getPalette();
      int numColors = palette.length;
      for (int c = 0; c < numColors; c++) {
        ColorSplitAlpha split = new ColorSplitAlpha(palette[c]);
        // 1 - Red
        fm.writeByte(split.getRed());
        // 1 - Green
        fm.writeByte(split.getGreen());
        // 1 - Blue
        fm.writeByte(split.getBlue());
        // 1 - Alpha
        fm.writeByte(split.getAlpha());
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}