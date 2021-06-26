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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BAG_4;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BAG_4_BM2_BM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BAG_4_BM2_BM() {
    super("BAG_4_BM2_BM", "Austerlitz BM2 Image");
    setExtensions("bm2");

    setGames("Austerlitz");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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
      if (plugin instanceof Plugin_BAG_4) {
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

      // 2 - Header
      if (fm.readString(2).equals("BM")) {
        rating += 25; // So that it doesn't get priority over normal BMP plugins
      }
      else {
        rating = 0;
      }

      fm.skip(8);

      // 4 - Image Header Length (54)
      if (FieldValidator.checkEquals(fm.readInt(), 54)) {
        rating += 5;
      }

      fm.skip(4);

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

      // 2 - Header (BM)
      // 4 - Raw Size (imageWidth * imageHeight * 3 + 56)
      // 4 - null
      // 4 - Image Header Length (54)
      // 4 - Unknown (40)
      fm.skip(18);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 2 - Unknown (1)
      // 2 - Unknown (24)
      // 8 - null
      // 4 - Unknown (2834)
      // 4 - Unknown (2834)
      // 8 - null
      fm.skip(32);

      // X - Pixels (RGB565)
      ImageResource imageResource = ImageFormatReader.readRGB565(fm, width, height);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      // 2 - Header (BM)
      fm.writeString("BM");

      // 4 - Raw Size (imageWidth * imageHeight * 3 + 56)
      int rawSize = imageWidth * imageHeight * 3 + 56;
      fm.writeInt(rawSize);

      // 4 - null
      fm.writeInt(0);

      // 4 - Image Header Length (54)
      fm.writeInt(54);

      // 4 - Unknown (40)
      fm.writeInt(40);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // 4 - Image Height
      fm.writeInt(imageHeight);

      // 2 - Unknown (1)
      fm.writeShort(1);

      // 2 - Unknown (24)
      fm.writeShort(24);

      // 8 - null
      fm.writeInt(0);
      fm.writeInt(0);

      // 4 - Unknown (2834)
      fm.writeInt(2834);

      // 4 - Unknown (2834)
      fm.writeInt(2834);

      // 8 - null
      fm.writeInt(0);
      fm.writeInt(0);

      // X - Pixels (RGB565)
      ImageFormatWriter.writeRGB565(fm, imageResource);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}