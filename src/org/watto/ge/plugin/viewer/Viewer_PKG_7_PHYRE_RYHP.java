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
import org.watto.ge.plugin.archive.Plugin_PKG_7;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PKG_7_PHYRE_RYHP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PKG_7_PHYRE_RYHP() {
    super("PKG_7_PHYRE_RYHP", "The Legend of Heroes: Trails of Cold Steel 3 DDS.PHYRE Image");
    setExtensions("phyre");

    setGames("The Legend of Heroes: Trails of Cold Steel 3");
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
      if (plugin instanceof Plugin_PKG_7) {
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

      if (fm.getFilePath().contains("dds.phyre")) {
        rating += 5;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("RYHP")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Header Size (84)
      if (fm.readInt() == 84) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Header (11XD)
      if (fm.readString(4).equals("11XD")) {
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

      // 4 - Header (RYHP)
      fm.skip(4);

      // 4 - Header Size (84)
      int headerSize = fm.readInt();
      FieldValidator.checkLength(headerSize, arcSize);

      // 4 - Properties Block Size
      int propertySize = fm.readInt();
      FieldValidator.checkLength(propertySize, arcSize);

      // 4 - Header (11XD)
      // X - Unknown
      int skipSize = headerSize + propertySize - 12; // -12 because we've read 12 bytes already
      fm.skip(skipSize);

      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown (56)
      // 4 - Unknown (4)
      fm.skip(16);

      // 4 - Filename Length (including padding)
      int filenameLength = fm.readInt();
      FieldValidator.checkFilenameLength(filenameLength);

      // 4 - null
      // 4 - Unknown (1)
      // 4 - Unknown (2)
      // 4 - null
      // 4 - Unknown (10)
      // 4 - Unknown (1)
      // 4 - Unknown (22)
      // 4 - Unknown (22)
      // 12 - null
      // 4 - Unknown (1)
      // 8 - null
      fm.skip(56);

      // X - Image Filename
      // 1 - null Filename Terminator
      fm.readNullString(filenameLength);

      // 2 - Unknown
      // 4 - Number of Mipmaps?
      // 4 - Number of Mipmaps?
      // 4 - Unknown (512)
      fm.skip(14);

      // 4 - Image Width?
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height?
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // X - Texture Field (PTexture2D)
      // 1 - null Texture Field Terminator
      fm.readNullString();

      // X - Image Format (BC7/DXT1/...)
      // 1 - null Image Format Terminator
      String imageFormat = fm.readNullString();

      // 4 - Unknown (8)
      // 4 - Unknown (11)
      // 4 - null
      // 4 - Unknown (2)
      // 4 - Unknown (4)
      // 4 - Unknown (11)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 1 - null
      fm.skip(37);

      // X - Pixels
      ImageResource imageResource = null;

      if (imageFormat.equals("BC7")) {
        imageResource = ImageFormatReader.readBC7(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PKG_7_PHYRE_RYHP] Unknown Image Format: " + imageFormat);
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