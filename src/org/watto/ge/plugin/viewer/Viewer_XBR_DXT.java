/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_XBR;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_XBR_DXT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_XBR_DXT() {
    super("XBR_DXT", "XBR_DXT Image");
    setExtensions("dxt");

    setGames("Cars");
    setPlatforms("XBox");
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
  public boolean canReplace(PreviewPanel panel) {
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
      if (plugin instanceof Plugin_XBR) {
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
      if (fm.readInt() == 2) {
        rating += 5;
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

      long arcSize = fm.getLength();

      // 4 - Unknown (2)
      fm.skip(4);

      // 4 - Image Format? (38=DXT1, 54=DXT5, 55=Paletted)
      int imageFormat = fm.readInt();

      // 4 - Number of Colors in the Palette (null for DXT-encoded images)
      int numColors = fm.readInt();
      int[] palette = null;
      if (numColors > 0 && numColors <= 256) {
        palette = ImageFormatReader.readPaletteBGRA(fm, numColors);
      }

      // 4 - Number of Mipmaps
      int mipmapCount = fm.readInt();
      FieldValidator.checkRange(mipmapCount, 1, 20);//guess

      // 4 - Largest Mipmap Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Largest Mipmap Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // skip over the small mipmaps
      for (int m = 0; m < mipmapCount - 1; m++) {
        // 4 - Image Width
        // 4 - Image Height
        fm.skip(8);

        // 4 - Image Data Length
        int dataLength = fm.readInt();
        FieldValidator.checkLength(dataLength, arcSize);

        // X - Image Data
        fm.skip(dataLength);
      }

      // read the largest mipmap
      // 4 - Image Width
      // 4 - Image Height
      // 4 - Image Data Length
      fm.skip(12);

      // X - Image Data

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 38) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 54) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 55 | imageFormat == 56) { // paletted and swizzled
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource.setPixels(ImageFormatReader.unswizzle(imageResource.getPixels(), width, height, 1));
      }
      else {
        ErrorLogger.log("[Viewer_XBR_DXT] Unknown Image Format: " + imageFormat);
        return null;
      }

      fm.close();

      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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