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
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TXD;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TXD_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TXD_TEX() {
    super("TXD_TEX", "Sonic Heroes TEX Image");
    setExtensions("tex");

    setGames("Sonic Heroes");
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
      if (plugin instanceof Plugin_TXD) {
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

      // 4 - Entry Type (8)
      // 4 - Unknown
      // 64 - Filename (null terminated)
      // 4 - Unknown
      fm.skip(76);

      // 4 - Image Type
      int imageType = fm.readInt();

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 4 - Unknown
      imageType = ByteConverter.unsign(fm.readByte());
      fm.skip(3);

      ImageResource imageResource = null;
      if (imageType == 8) {
        // Paletted

        // X - Palette
        int numColors = 256;
        int[] palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
          // 1 - Blue
          // 1 - Green
          // 1 - Red
          // 1 - Alpha
          int b = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());
          int a = ByteConverter.unsign(fm.readByte());

          palette[i] = ((a << 24) | (r << 16) | (g << 8) | (b));
        }

        // 4 - Pixel Data Length
        fm.skip(4);

        // X - Pixels
        int numPixels = width * height;
        int[] pixels = new int[numPixels];
        for (int p = 0; p < numPixels; p++) {
          // 1 - Color Palette Index
          pixels[p] = palette[ByteConverter.unsign(fm.readByte())];
        }

        imageResource = new ImageResource(pixels, width, height);
        imageResource.addProperty("ImageFormat", "8BitPaletted");

      }
      //else if (imageType == 1){
      else if (imageType == 32) {
        // BGRA

        // 4 - Image Data Length
        fm.skip(4);

        // X - Pixels
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
        imageResource.addProperty("ImageFormat", "BGRA");
      }
      else if (imageType == 16) {
        // DXT1

        // 4 - Image Data Length
        fm.skip(4);

        // X - Pixels
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else {
        return null;
      }

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