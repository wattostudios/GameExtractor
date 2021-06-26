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
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TCGIL_GIL3 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TCGIL_GIL3() {
    super("TCGIL_GIL3", "Utopia City TCGIL Image");
    setExtensions("tcgil");

    setGames("Utopia City");
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
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("GIL3")) {
        rating += 50;
      }
      else {
        rating = 0;
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

      // 4 - Header (GIL3)
      // 4 - Unknown (21)
      // 4 - Version (3)
      fm.skip(12);

      // 4 - Format Type (DXT1, DXT5)
      byte[] imageFormatBytes = fm.readBytes(4);
      String imageFormatString = StringConverter.convertLittle(imageFormatBytes);
      int imageFormatInt = IntConverter.convertLittle(imageFormatBytes);

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Number Of Mipmaps
      int numMipMaps = fm.readInt();
      FieldValidator.checkNumColors(numMipMaps);

      // 4 - null
      // 4 - Unknown
      // 4 - Hash?
      fm.skip(12);

      // for each mipmap
      //   4 - Mipmap Length
      fm.skip(numMipMaps * 4);

      // X - DDS-Format Image Data (DXT1/5 depends on the header Format Type)
      ImageResource imageResource = null;
      if (imageFormatString.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormatString.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormatString.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormatInt == 62) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource = ImageFormatReader.reverseAlpha(imageResource);
      }
      else if (imageFormatInt == 21) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource = ImageFormatReader.reverseAlpha(imageResource);
      }
      else {
        ErrorLogger.log("[Viewer_TCGIL_GIL3] Unknown image format: " + imageFormatString + " " + imageFormatInt);
      }

      fm.close();

      imageResource.addProperty("MipmapCount", "" + numMipMaps);

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