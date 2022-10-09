/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_PAK_KPKA;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PAK_KPKA_TEX_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PAK_KPKA_TEX_TEX() {
    super("PAK_KPKA_TEX_TEX", "Capcom Arcade Stadium TEX Image");
    setExtensions("tex");

    setGames("Capcom Arcade Stadium");
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
      if (plugin instanceof Plugin_PAK_KPKA) {
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
      String header = fm.readString(3);
      int headerByte = fm.readByte();
      if (header.equals("TEX") && headerByte == 0) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      if (fm.readInt() == 30) {
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

      long arcSize = fm.getLength();

      // 4 - Header ("TEX" + null)
      // 4 - Unknown (30)
      fm.skip(8);

      // 2 - Image Width
      int width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown (1)
      // 2 - Unknown
      fm.skip(4);

      // 4 - Image Format? (28/29=RGBA, 72=DXT1, 80=BC4, 99=BC7)
      int imageFormat = fm.readInt();

      // 4 - Padding (-1)
      // 4 - null
      // 4 - Unknown
      // 8 - null
      fm.skip(20);

      // 4 - Image Data Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - null
      // 4 - Unknown
      // 4 - Image Data Length
      fm.seek(offset);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 28 || imageFormat == 29) { // RGBA
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat == 71 || imageFormat == 72) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 80) { // BC4
        imageResource = ImageFormatReader.readBC4(fm, width, height);
      }
      else if (imageFormat == 98 || imageFormat == 99) { // BC7
        imageResource = ImageFormatReader.readBC7(fm, width, height);
        imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
      }
      else {
        ErrorLogger.log("[Viewer_PAK_KPKA_TEX_TEX] Unknown Image Format: " + imageFormat);
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