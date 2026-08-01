/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_COD_UNIQUE;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_COD_UNIQUE_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_COD_UNIQUE_TEX() {
    super("COD_UNIQUE_TEX", "COD_UNIQUE_TEX Image");
    setExtensions("tex");

    setGames("Screamer 4x4");
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
      if (plugin instanceof Plugin_COD_UNIQUE) {
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

      fm.skip(16);

      // 4 - File Length
      if ((fm.readInt() + 20) == fm.getLength()) {
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

      // 4 - Image Format 1 (8=RGB555/565, 0=RGBA)
      fm.skip(4);

      // 4 - Image Format 2 (0=RGB555, 1=RGB565, 2=L8A8)
      int imageFormat = fm.readInt();

      // 4 - Unknown (1/2/3)
      int unknown = fm.readInt();
      if (unknown != 3) {
        return null;
      }

      // 4 - Number of Mipmaps? [-3]
      /*
      int numMipmaps = fm.readInt() - 3;
      FieldValidator.checkRange(numMipmaps, 0, 20);
      
      int width = 1 << numMipmaps;
      int height = 1 << numMipmaps;
      */

      fm.skip(4);

      int width = 1;
      int height = 1;

      int numPixels = width * height * 2;

      while (numPixels < arcSize) {
        width *= 2;
        height *= 2;
        numPixels = width * height * 2;
      }

      width /= 2;
      height /= 2;

      // 4 - Image Data Length
      fm.skip(4);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 0) {
        imageResource = ImageFormatReader.readRGB555(fm, width, height);
      }
      else if (imageFormat == 1) {
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }
      else if (imageFormat == 2) {
        imageResource = ImageFormatReader.readL8A8(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_COD_UNIQUE_TEX] Unknown Image Format: " + imageFormat);
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