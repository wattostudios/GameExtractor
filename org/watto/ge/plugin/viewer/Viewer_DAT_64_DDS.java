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
import org.watto.ge.plugin.archive.Plugin_DAT_64;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_64_DDS extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_DAT_64_DDS() {
    super("DAT_64_DDS", "Anomoly Headerless DDS Images");
    setExtensions("dds_headerless");

    setGames("Anomoly Defenders");
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
      if (plugin instanceof Plugin_DAT_64) {
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

      // 4 - Image Width
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkWidth(fm.readInt() + 2)) { // +2 to allow a value of -1
        rating += 5;
      }

      // 4 - Header
      String header = fm.readString(4);
      if (header.equals("DXT5")) {
        rating += 50;
      }
      else {
        ErrorLogger.log("[Viewer_DAT_64_DDS]: Unknown Image Type: " + header);
        //rating = 0;
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

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      if (height == -1) {
        height = width;
      }
      FieldValidator.checkHeight(height);

      // 4 - Header (DXT5)
      String imageType = fm.readString(4);

      // 4 - Number Of Mipmaps
      int numMipmaps = fm.readInt();
      FieldValidator.checkRange(numMipmaps, 0, 20);

      // for each mipmap
      int largestMipmap = 0;
      for (int i = 0; i < numMipmaps; i++) {
        // 2 - Mipmap Width
        // 2 - Mipmap Height
        fm.skip(4);

        // 4 - Mipmap Data Length
        int mipmapLength = fm.readInt();
        FieldValidator.checkLength(mipmapLength, arcSize);

        largestMipmap = mipmapLength;
      }

      // 56 - null

      // skip to the last mipmap
      fm.seek(arcSize - largestMipmap);

      // X - Pixels
      ImageResource imageResource = null;

      if (imageType.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageType.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      //ColorConverter.convertToPaletted(resource);

      imageResource.addProperty("MipmapCount", "" + numMipmaps);
      imageResource.addProperty("ImageFormat", "" + imageType);

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