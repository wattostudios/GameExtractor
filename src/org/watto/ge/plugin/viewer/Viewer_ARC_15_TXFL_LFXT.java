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
import org.watto.ge.plugin.archive.Plugin_ARC_15;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_15_TXFL_LFXT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_15_TXFL_LFXT() {
    super("ARC_15_TXFL_LFXT", "ARC_15_TXFL_LFXT Image");
    setExtensions("txfl");

    setGames("The Urbz: Sims in the City");
    setPlatforms("GameCube");
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
      if (plugin instanceof Plugin_ARC_15) {
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
      if (fm.readString(4).equals("LFXT")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      if (IntConverter.changeFormat(fm.readInt()) == 8) {
        rating += 5;
      }

      // 4 - File Length
      if (IntConverter.changeFormat(fm.readInt()) + 12 == fm.getLength()) {
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

  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 4 - Header (LFXT)
      // 4 - Unknown (8)
      // 4 - File Length [+12]
      fm.skip(12);

      // X - Filename
      // 1 - null Filename Terminator
      fm.readNullString();

      // 8 - null
      // 4 - Unknown
      // 4 - null
      fm.skip(16);

      // 2 - Image Width
      int width = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkHeight(height);

      // 2 - Unknown (0/256)
      fm.skip(2);

      // 2 - Number of Mipmaps (0 = only 1 mipmap)
      int mipmapCount = ShortConverter.changeFormat(fm.readShort());

      // 2 - Unknown (143=DXT3, 140=RGBA Swizzled)
      fm.skip(2);

      // 2 - Image Format? (8=DXT3, 32=RGBA Swizzled)
      int imageFormat = fm.readShort();

      // 4 - null
      fm.skip(4);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 8224) {
        // RGBA8Wii
        imageResource = ImageFormatReader.readRGBA8Wii(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_ARC_15_TXFL_LFXT] Unknown Image Format: " + imageFormat);
        return null;
      }

      imageResource = ImageFormatReader.flipVertically(imageResource);

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