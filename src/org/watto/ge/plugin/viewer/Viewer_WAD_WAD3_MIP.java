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
import org.watto.ge.plugin.archive.Plugin_WAD_WAD3;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WAD_WAD3_MIP extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_WAD_WAD3_MIP() {
    super("WAD_WAD3_MIP", "WAD_WAD3_MIP");
    setExtensions("mip");

    setGames("Half-Life");
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
      if (plugin instanceof Plugin_WAD_WAD3) {
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

      int arcSize = (int) fm.getLength();

      // 16 - Filename (without extension) (null terminated, padded with nulls to fill)
      fm.skip(16);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - First Mipmap Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      int arcSize = (int) fm.getLength();

      // 16 - Filename (without extension) (null terminated, padded with nulls to fill)
      fm.skip(16);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - First Mipmap Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      int numMipmaps = (offset - 24) / 4;

      // we need the last mipmap offset so we can find the color palette
      fm.skip((numMipmaps - 2) * 4);

      // 4 - Last Mipmap Offset
      int lastMipmapOffset = fm.readInt();
      FieldValidator.checkOffset(lastMipmapOffset, arcSize);

      // Now we should be at the start of the First Mipmap, but check it anyway
      fm.seek(offset);

      // X - Palette Indexes
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      for (int i = 0; i < numPixels; i++) {
        pixels[i] = ByteConverter.unsign(fm.readByte());
      }

      // work out the size of the last mipmap data, so we can skip it, to land on the color palette
      fm.seek(lastMipmapOffset);
      int lastNumPixels = (width / 8) * (height / 8);
      fm.skip(lastNumPixels);

      // 2 - Unknown
      fm.skip(2);

      // X - Color Palette
      int numColors = 256;
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());

        palette[i] = ((255 << 24) | (r << 16) | (g << 8) | b);
      }

      for (int i = 0; i < numPixels; i++) {
        pixels[i] = palette[pixels[i]];
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

      imageResource.addProperty("MipmapCount", "" + numMipmaps);
      imageResource.addProperty("ImageFormat", "8BitPaletted");

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
  }

}