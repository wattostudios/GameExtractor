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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_TEX1999C extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_TEX1999C() {
    super("TEX_TEX1999C", "TEX_TEX1999C Image");
    setExtensions("tex");

    setGames("Nexus: The Jupiter Incident");
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
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 8 - Header
      if (fm.readString(8).equals("TEX1999C")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(3);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
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

      // 11 - Header ("TEX1999C" + 13,10,26)
      fm.skip(11);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 15 - null
      fm.skip(15);

      String imageFormat = null;

      // 4 - Image Data Offset (if BGRA with no alpha Image) (103)
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      imageFormat = "BGRANoAlpha";

      // 1 - null
      fm.skip(1);

      if (offset == 0) {
        // 4 - Image Data Offset (if BGRA Image) (103)
        offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        imageFormat = "BGRA";

        if (offset == 0) {
          // 4 - Number of Images? (if BGRA Image) (1)
          // 2 - null
          fm.skip(6);

          // 4 - Image Data Offset (if DXT1 Image) (103)
          offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          imageFormat = "DXT1";

          if (offset == 0) {
            // 4 - Number of Images? (if DXT1 Image) (1)
            // 2 - null
            fm.skip(6);

            // 4 - Image Data Offset (if DXT3 Image) (103)
            offset = fm.readInt();
            FieldValidator.checkOffset(offset, arcSize);

            imageFormat = "DXT3";
          }
        }
      }

      // 4 - Number of Images? (if DXT3 Image) (1)
      // 40 - null
      if (offset == 0) {
        return null;
      }

      fm.relativeSeek(offset);

      // 2 - Image Width
      // 2 - Image Height (if this equals the header, it's DXT3, otherwise if it's 4x the size, it's BGRA)
      fm.skip(4);

      width += ArchivePlugin.calculatePadding(width, 32); // width values are padded to a multiple of 32 pixel

      // X - Image Data

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat.equals("BGRA")) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("BGRANoAlpha")) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
        imageResource = ImageFormatReader.removeAlpha(imageResource);
      }
      else {
        ErrorLogger.log("[Viewer_TEX_TEX1999C] Unknown Image Format: " + imageFormat);
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