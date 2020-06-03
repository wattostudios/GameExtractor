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
import org.watto.ge.plugin.archive.Plugin_BIG_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIG_2_TXTR extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_BIG_2_TXTR() {
    super("BIG_2_TXTR", "BIG_2_TXTR");
    setExtensions("txtr");

    setGames("Armed and Dangerous");
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
      if (plugin instanceof Plugin_BIG_2) {
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

      // 4 - Number of Mipmaps
      if (FieldValidator.checkRange(fm.readInt(), 0, 30)) {
        rating += 5;
      }

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

      // 4 - Number of Mipmaps
      int numMipmaps = fm.readInt();
      FieldValidator.checkRange(numMipmaps, 0, 30);

      // 2 - Image Width
      short width = fm.readShort();

      // 2 - Image Height
      short height = fm.readShort();

      // 4 - Image Format ("DXT1", "DXT2", 21=RGBA)
      String imageFormat = "DXT1";

      byte[] formatBytes = fm.readBytes(4);
      String formatBytesString = new String(formatBytes);
      int formatBytesInt = IntConverter.convertLittle(formatBytes);
      if (formatBytesString.equals("DXT1")) {
        imageFormat = "DXT1";
      }
      else if (formatBytesString.equals("DXT2")) {
        imageFormat = "DXT2";
      }
      else if (formatBytesInt == 21) {
        imageFormat = "RGBA";
      }
      else if (formatBytesInt == 23) {
        imageFormat = "RGB565";
      }
      else {
        ErrorLogger.log("Viewer_BIG_2_TXTR: Unknown image format: " + formatBytesInt);
        return null;
      }

      // 2 - Unknown
      // 2 - Unknown
      // 4 - Image Data Length
      fm.skip(8);

      // X - Image Data
      ImageResource imageResource = null;

      if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT2")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.equals("RGBA")) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat.equals("RGB565")) {
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      imageResource = ImageFormatReader.flipVertically(imageResource);

      imageResource.addProperty("MipmapCount", "" + numMipmaps);
      imageResource.addProperty("ImageFormat", "" + imageFormat);

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