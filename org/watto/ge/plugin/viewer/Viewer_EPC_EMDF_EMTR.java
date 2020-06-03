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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_EPC_EMDF;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_EPC_EMDF_EMTR extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_EPC_EMDF_EMTR() {
    super("EPC_EMDF_EMTR", "EPC_EMDF_EMTR");
    setExtensions("emtr");

    setGames("Doctor Who: Episode 1: City of the Daleks",
        "Doctor Who: Episode 2: Blood of the Cybermen",
        "Doctor Who: Episode 3: Tardis",
        "Doctor Who: Episode 4: Shadows of the Vashta Nerada");
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
      if (plugin instanceof Plugin_EPC_EMDF) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 8 - null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // 4 - Header
      if (fm.readString(4).equals("EMTR")) {
        rating += 5;
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

      long arcSize = fm.getLength();

      // 8 - null
      // 4 - File Type (EMTR)
      // 4 - File Length
      // 4 - null
      fm.skip(20);

      // 4 - Offset to Image Data (144)
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 24 - null
      // 4 - Unknown
      // 20 - null
      // 4 - Unknown (31)
      // 4 - Unknown (1)
      // 4 - Unknown (3)
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown (44/55/56)
      // 16 - null
      // 2 - null
      // 2 - Unknown
      fm.skip(92);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Image Data Length (for all mipmaps)
      int dataLength = fm.readInt();
      FieldValidator.checkLength(dataLength, arcSize);

      // 4 - Unknown
      // 4 - Padding
      // 8 - null
      fm.seek(offset);

      // Work out if it's DXT1 or DXT3/5
      int numPixels = width * height;

      String imageFormat = "";
      if (numPixels > dataLength) {
        imageFormat = "DXT1";
      }
      else {
        imageFormat = "DXT5";
      }

      // X - Image Data
      ImageResource imageResource = null;
      if (imageFormat.equals("DXT5")) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else {
        return null; // unknown (or other) image format
      }

      imageResource.addProperty("ImageFormat", imageFormat);

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