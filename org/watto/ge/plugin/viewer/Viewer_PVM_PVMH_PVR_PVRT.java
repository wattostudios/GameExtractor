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
import org.watto.ge.plugin.archive.Plugin_PVM_PVMH;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PVM_PVMH_PVR_PVRT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PVM_PVMH_PVR_PVRT() {
    super("PVM_PVMH_PVR_PVRT", "Sonic Adventure DX PVR Image");
    setExtensions("pvr");

    setGames("Sonic Adventure DX");
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
      if (plugin instanceof Plugin_PVM_PVMH) {
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
      String header = fm.readString(4);
      if (header.equals("PVRT") || header.equals("GBIX")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
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

      // 4 - Header (GBIX)
      String header = fm.readString(4);
      if (header.equals("GBIX")) {
        // 4 - Header Length (not including these 2 fields) (8)
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);

        // 4 - Header (PVRT)
        header = fm.readString(4);
      }

      // 4 - Data Length (not including these 2 fields)
      fm.skip(4);

      // 2 - Image Format
      short imageFormat = fm.readShort();

      // 2 - Unknown
      fm.skip(2);

      // 2 - Image Width/Height
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Width/Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 2304) {
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
      }
      else if (imageFormat == 2305) {
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }
      else if (imageFormat == 2306) {
        imageResource = ImageFormatReader.readGBAR4444(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PVM_PVMH_PVR_PVRT] Unknown Image Format: " + imageFormat);
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