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
import org.watto.ge.plugin.archive.Plugin_PPF_PPAK;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PPF_PPAK_1TX_XT1 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PPF_PPAK_1TX_XT1() {
    super("PPF_PPAK_1TX_XT1", "Psychonauts 1TX Image");
    setExtensions("1tx");

    setGames("Psychonauts");
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
      if (plugin instanceof Plugin_PPF_PPAK) {
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

      // 40 - Unknown
      fm.skip(16);
      int extraBlock = fm.readInt();
      fm.skip(20);

      // 2 - Filename Length (including null
      short filenameLength = fm.readShort();
      FieldValidator.checkFilenameLength(filenameLength);

      fm.skip(filenameLength);

      if (extraBlock != 0) {
        fm.skip(28);
      }

      fm.skip(16);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
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

      // 40 - Unknown
      fm.skip(16);
      int extraBlock = fm.readInt();
      fm.skip(20);

      // 2 - Filename Length (including Null Terminator)
      short filenameLength = fm.readShort();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Filename
      // 1 - null Filename Terminator
      fm.skip(filenameLength);

      if (extraBlock != 0) {
        fm.skip(28);
      }

      // 4 - Unknown
      fm.skip(4);

      // 4 - Image Format? (11)
      int imageFormat = fm.readInt();

      // 4 - null
      // 4 - Unknown
      fm.skip(8);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Mipmap Count
      // 8 - Unknown
      // 8 - null
      fm.skip(20);

      // X - Image Data
      ImageResource imageResource = null;

      if (imageFormat == 11) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 10) { // DXT3
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 9) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 0) { // ABGR
        imageResource = ImageFormatReader.readABGR(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PPF_PPAK_1TX_XT1] Unknown Image Format: " + imageFormat);
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