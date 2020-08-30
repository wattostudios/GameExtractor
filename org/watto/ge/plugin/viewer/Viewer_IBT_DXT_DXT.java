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
import org.watto.ge.plugin.archive.Plugin_IBT;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_IBT_DXT_DXT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_IBT_DXT_DXT() {
    super("IBT_DXT_DXT", "Thief 3 DXT Image");
    setExtensions("dxt");

    setGames("Thief 3: Deadly Shadows");
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
      if (plugin instanceof Plugin_IBT) {
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

      // version (1)
      if (fm.readByte() == 1) {
        rating += 5;
      }

      // 4 - Header
      if (fm.readString(3).equals("DXT")) {
        rating += 20;
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

      // 1 - Version (1)
      // 4 - Header (DXT1/DXT3)
      fm.skip(4);
      String type = fm.readString(1);

      // 1 - Extra Data Length
      int extraLength = fm.readByte();
      fm.skip(extraLength);

      // 4 - Width
      byte[] widthBytes = fm.readBytes(4);
      while (widthBytes[0] == 0 && widthBytes[1] == 0 && widthBytes[2] == 0 && widthBytes[3] == 0) {
        widthBytes[3] = fm.readByte();
      }
      int width = IntConverter.changeFormat(IntConverter.convertLittle(widthBytes));

      // 4 - Height
      int height = IntConverter.changeFormat(fm.readInt());

      try {
        FieldValidator.checkWidth(width);
        FieldValidator.checkHeight(height);
      }
      catch (Throwable t) {
        // effectively go back a byte
        width >>= 8;
        height >>= 8;
        FieldValidator.checkWidth(width);
        FieldValidator.checkHeight(height);
      }

      fm.seek(64);

      // X - Pixels
      ImageResource imageResource = null;
      if (type.equals("1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (type.equals("3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (type.equals("5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_IBT_DXT_DXT] Unknown type: " + type);
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

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