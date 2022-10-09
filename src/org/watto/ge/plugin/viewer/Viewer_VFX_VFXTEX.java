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
import org.watto.ge.plugin.archive.Plugin_VFX;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VFX_VFXTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VFX_VFXTEX() {
    super("VFX_VFXTEX", "Gex 2: Enter The Gecko VFX_TEX Image");
    setExtensions("vfx_tex");

    setGames("Gex 2: Enter The Gecko");
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
      if (plugin instanceof Plugin_VFX) {
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

      fm.skip(136);

      // 4 - File Length
      if (fm.readInt() + 140 == fm.getLength()) {
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

      // 4 - Image Width/Height (1=128, 2=64, 3=32, 4=16)
      int height = fm.readInt();
      if (height == 1) {
        height = 128;
      }
      else if (height == 2) {
        height = 64;
      }
      else if (height == 3) {
        height = 32;
      }
      else if (height == 4) {
        height = 16;
      }
      else if (height == 5) {
        height = 8;
      }
      else if (height == 6) {
        height = 4;
      }
      else if (height == 7) {
        height = 2;
      }
      else if (height == 8) {
        height = 1;
      }
      else if (height == 0) {
        height = 64; // special case for w=64, h=128
      }
      else {
        ErrorLogger.log("[Viewer_VFX_VFXTEX] Unknown width code: " + height);
        return null;
      }

      // 4 - Unknown (1/2/3)
      fm.skip(4);

      // 4 - Dimension (2=Wide, 3=Square, 4=Tall)
      int dimension = fm.readInt();

      // 4 - Image Format (1=Grayscale, 11=RGBA5551, 12=ARGB4444)
      int imageFormat = fm.readInt();

      // 4 - Unknown
      // 112 - null
      // 4 - File Length
      fm.skip(120);

      // 4 - File Length
      int length = fm.readInt();

      int width = 0;
      if (imageFormat == 1) {
        // Grayscale
        width = length / height;
      }
      else if (imageFormat == 11) {
        // RGBA5551
        width = length / 2 / height;
      }
      else if (imageFormat == 12) {
        // ARGB4444
        width = length / 2 / height;
      }

      // now that we have the width and height, need to work out if they should be swapped (as per Dimension)
      if (dimension == 2) {
        // Wide
        if (height > width) {
          int oldHeight = height;
          height = width;
          width = oldHeight;
        }
      }
      else if (dimension == 4) {
        // Tall
        if (width > height) {
          int oldHeight = height;
          height = width;
          width = oldHeight;
        }
      }
      else if (dimension == 3) {
        // Square
      }
      else {
        ErrorLogger.log("[Viewer_VFX_VFXTEX] Unknown dimension code: " + dimension);
      }

      // X - File Data (Image)
      ImageResource imageResource = null;

      if (imageFormat == 1) {
        // Grayscale
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else if (imageFormat == 11) {
        // RGBA5551
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
      }
      else if (imageFormat == 12) {
        // ARGB4444
        imageResource = ImageFormatReader.readARGB4444(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_VFX_VFXTEX] Unknown image format: " + imageFormat);
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