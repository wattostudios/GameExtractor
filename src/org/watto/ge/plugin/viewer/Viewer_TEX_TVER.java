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
import org.watto.ge.plugin.archive.Plugin_ZIP_PK;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_TVER extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_TVER() {
    super("TEX_TVER", "Serious Sam TEX Image");
    setExtensions("tex");

    setGames("Serious Sam",
        "Serious Sam: The Second Encounter");
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
      if (plugin instanceof Plugin_ZIP_PK) {
        rating += 10; // ZIP_PK is pretty generic - don't rank this too high
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
      if (fm.readString(4).equals("TVER")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Version
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // 4 - Header
      if (fm.readString(4).equals("TDAT")) {
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

      // 4 - Header (TVER)
      // 4 - Version (4)
      // 4 - Header (TDAT)
      fm.skip(12);

      // 4 - Image Format? (0=RGB, 1=RGBA, 2=RGB)
      int imageFormat = fm.readInt();

      // 4 - Image Width (>>= Bitshift Value)
      int width = fm.readInt();

      // 4 - Image Height (>>= Bitshift Value)
      int height = fm.readInt();

      // 4 - Unknown (1/4/8)
      int unknown = fm.readInt();
      if (unknown == 1 || unknown == 4 || (unknown == 5 && imageFormat == 0)) {
        // 4 - Mipmap Count
        fm.skip(4);
      }

      // 4 - Bitshift Value
      int bitShift = fm.readInt();

      try {
        width >>= bitShift;
        FieldValidator.checkWidth(width);

        height >>= bitShift;
        FieldValidator.checkHeight(height);
      }
      catch (Throwable t) {
        // go back a few bytes and try again
        fm.relativeSeek(16);

        // 4 - Image Width (>>= Bitshift Value)
        width = fm.readInt();

        // 4 - Image Height (>>= Bitshift Value)
        height = fm.readInt();

        // 4 - Unknown (1/4/8)
        fm.skip(4);
        unknown = 5;
        imageFormat = 0;

        // 4 - Mipmap Count
        fm.skip(4);

        // 4 - Bitshift Value
        bitShift = fm.readInt();

        width >>= bitShift;
        FieldValidator.checkWidth(width);

        height >>= bitShift;
        FieldValidator.checkHeight(height);
      }

      if (unknown == 1 || unknown == 4 || (unknown == 5 && imageFormat == 0)) {
        // 4 - Image Data Length
        fm.skip(4);
      }

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Pixel Compression Format (FRMS=no compression)
      String pixelFormat = fm.readString(4);
      if (!pixelFormat.equals("FRMS")) {

        if (unknown == 5) {
          // go back a few bytes and try again
          fm.relativeSeek(16);

          // 4 - Image Width (>>= Bitshift Value)
          width = fm.readInt();

          // 4 - Image Height (>>= Bitshift Value)
          height = fm.readInt();

          // 4 - Unknown (1/4/8)
          fm.skip(4);

          // 4 - Bitshift Value
          bitShift = fm.readInt();

          width >>= bitShift;
          FieldValidator.checkWidth(width);

          height >>= bitShift;
          FieldValidator.checkHeight(height);

          // 4 - Unknown (1)
          fm.skip(4);

          // 4 - Pixel Compression Format (FRMS=no compression)
          pixelFormat = fm.readString(4);
        }
        else if (unknown == 3 || unknown == 8) {
          // go back a few bytes and try again
          fm.relativeSeek(16);

          // 4 - Image Width (>>= Bitshift Value)
          width = fm.readInt();

          // 4 - Image Height (>>= Bitshift Value)
          height = fm.readInt();

          // 4 - Unknown (1/4/8)
          fm.skip(4);

          // 4 - Mipmap Count
          fm.skip(4);

          // 4 - Bitshift Value
          bitShift = fm.readInt();

          width >>= bitShift;
          FieldValidator.checkWidth(width);

          height >>= bitShift;
          FieldValidator.checkHeight(height);

          // 4 - Image Data Length
          fm.skip(4);

          // 4 - Unknown (1)
          fm.skip(4);

          // 4 - Pixel Compression Format (FRMS=no compression)
          pixelFormat = fm.readString(4);

          // Force it to RGB565
          unknown = 5;
        }

        if (!pixelFormat.equals("FRMS")) {
          ErrorLogger.log("[Viewer_TEX_TVER] Unsupported Pixel Format: " + pixelFormat);
          return null;
        }
      }

      // X - Pixels
      ImageResource imageResource = null;
      if (unknown == 1 || unknown == 4 || unknown == 5) {
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }
      else if (imageFormat == 0 || imageFormat == 2) {
        imageResource = ImageFormatReader.readRGB(fm, width, height);
      }
      else if (imageFormat == 1 || imageFormat == 3) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_TEX_TVER] Unknown Image Format: " + imageFormat);
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