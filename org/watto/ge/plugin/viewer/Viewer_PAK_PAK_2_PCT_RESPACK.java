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
import org.watto.ge.plugin.archive.Plugin_PAK_PAK_2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PAK_PAK_2_PCT_RESPACK extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PAK_PAK_2_PCT_RESPACK() {
    super("PAK_PAK_2_PCT_RESPACK", "The Club PCT Image");
    setExtensions("pct");

    setGames("The Club");
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
      if (plugin instanceof Plugin_PAK_PAK_2) {
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
      if (fm.readString(7).equals("ResPack")) {
        rating += 50;
      }
      else {
        rating = 0;
      }
      fm.skip(1);

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

      // 8 - Header ("ResPack" + null)
      // 4 - File Length
      // 2 - Unknown (1)
      // 2 - Block Type (67)

      // RESOURCE BLOCK (20 bytes)
      // 4 - Header ("Res" + null)
      // 4 - Unknown (68)
      // 2 - Unknown (247)
      // 4 - Unknown (8)
      // 2 - Block Type (65)

      // TEXTURE BLOCK 1 (64 bytes)
      // 8 - Header ("Texture" + null)
      // 2 - Unknown (240)
      // 2 - Unknown (290)
      // 4 - null
      // 4 - Unknown (40)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 32 - Filename (without extension) (null terminated)

      // TEXTURE BLOCK 2 (16 bytes)
      // 8 - Header ("Texture" + null)
      // 2 - Unknown (167)
      // 2 - Unknown (8)
      // 2 - Unknown (2)
      // 2 - Block Type (67)

      // COMMON BLOCK (71 bytes)
      // 8 - Header ("Common" + 2 nulls)
      fm.skip(120);

      // 2 - Common Block Length (69/71)
      int commonBlockLength = fm.readShort();

      // 4 - null
      // 2 - Block Type (65)
      // 2 - null
      // 2 - Unknown (11)
      // 32 - Filename (without extension) (null terminated)
      fm.skip(42);

      // 4 - Image Width/Height
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Width/Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (4)
      // 4 - null
      // 1 - Number Of MipMaps? (1/10)
      fm.skip(9);

      // if (CommonBlockLength == 71){
      // 2 - null
      //   }
      int extraLength = commonBlockLength - 69;
      if (extraLength > 0) {
        fm.skip(extraLength);
      }

      // IMAGE BLOCK (16 bytes)
      // 8 - Header ("Image" + 3 nulls)
      // 2 - Unknown (80)
      // 2 - Unknown (8)
      // 2 - Unknown (3)
      // 2 - Block Type (67)

      // HEADER BLOCK (20 bytes)
      // 8 - Header ("Header" + 2 nulls)
      // 2 - Header Block Length (20)
      // 4 - null
      // 2 - Block Type (65)
      fm.skip(32);

      // 4 - Image Format (1=DXT1, 4=DXT5)
      int imageFormat = fm.readInt();

      // DATA BLOCK (20 bytes + image data length)
      // 8 - Header ("Data" + 4 nulls)
      // 2 - Data Block Length (20)
      // 4 - Unknown (8)
      // 2 - Block Type (65)
      // 2 - null
      // 2 - Unknown (8)
      fm.skip(20);

      // X - Image Data
      ImageResource imageResource = null;
      if (imageFormat == 1) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 4) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PAK_PAK_2_PCT_RESPACK] Unknown Image Format: " + imageFormat);
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