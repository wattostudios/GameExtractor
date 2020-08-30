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
import org.watto.io.FilenameSplitter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZIP_PK_PCT_TCIP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZIP_PK_PCT_TCIP() {
    super("ZIP_PK_PCT_TCIP", "World War Z PCT image");
    setExtensions("pct");

    setGames("World War Z");
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
        if (FilenameSplitter.getExtension(Archive.getBasePath()).equalsIgnoreCase("pak")) {
          rating += 50;
        }
        else {
          rating += 5;
        }
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

      // 2 - Unknown (240)
      if (fm.readShort() == 240) {
        rating += 5;
      }

      // 4 - Unknown (10)
      if (fm.readInt() == 10) {
        rating += 5;
      }

      // 4 - Header
      if (fm.readString(4).equals("TCIP")) {
        rating += 50;
      }
      else {
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

      long startOffset = fm.getOffset();

      // 2 - Unknown (240)
      // 4 - Unknown (10)
      // 4 - Header (TCIP)
      // 2 - Unknown (256/258)
      // 4 - Unknown (32)
      fm.skip(16);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (1)
      // 4 - Unknown (6/1)
      // 2 - Unknown (242)
      // 4 - Unknown (42)
      fm.skip(14);

      // 4 - Image Format (51=BC7/36=BC5/22=RGBA(A-reversed)
      int imageFormat = fm.readInt();

      // 2 - Unknown (249)
      // 4 - Unknown (52)
      // 4 - Number of Mipmaps
      // 2 - Unknown (263)
      fm.skip(12);

      // 4 - Header Length [+6]
      int headerLength = fm.readInt() + 6;

      // for each mipmap
      // 4 - Mipmap Data Offset (relative to the start of the Image Data)
      // 4 - Mipmap Data Length

      // 2 - Unknown (255)
      // 2 - Unknown (104/64)
      // 2 - Unknown (128/64)
      long dataOffset = startOffset + headerLength;
      FieldValidator.checkOffset(dataOffset, arcSize);

      dataOffset -= fm.getOffset();
      if (dataOffset < 0) {
        return null;
      }
      fm.skip(dataOffset);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 51 || imageFormat == 52) { // BC7
        imageResource = ImageFormatReader.readBC7(fm, width, height);
        imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
      }
      else if (imageFormat == 36) { // BC5
        imageResource = ImageFormatReader.readBC5(fm, width, height);
      }
      else if (imageFormat == 37) { // BC4
        imageResource = ImageFormatReader.readBC4(fm, width, height);
      }
      else if (imageFormat == 22) { // RGBA (reverse the alpha)
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource = ImageFormatReader.reverseAlpha(imageResource);
      }
      else if (imageFormat == 12) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_ZIP_PK_PCT_TCIP]: Unknown Image Format: " + imageFormat);
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