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
import org.watto.ge.plugin.archive.Plugin_WAD_12;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WAD_12_PIX extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_WAD_12_PIX() {
    super("WAD_12_PIX", "Croc PIX Image");
    setExtensions("pix");

    setGames("Croc: Legend of the Gobbos");
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
      if (plugin instanceof Plugin_WAD_12) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 15;
      }
      else {
        return 0;
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

      int imageFormat = 0;
      ImageResource imageResource = null;
      short width = 0;
      short height = 0;

      // 4 - Unknown (18)
      // 4 - Unknown (8)
      // 4 - Unknown (2)
      // 4 - Unknown (2)
      fm.skip(16);

      long arcSize = fm.getLength();

      while (fm.getOffset() < arcSize) {
        // 4 - Chunk ID
        int chunkID = IntConverter.changeFormat(fm.readInt());

        if (chunkID == 61 || chunkID == 3) {
          // 4 - Chunk Length (not including this field)
          fm.skip(4);

          // 1 - Pixel Format (4 = RGB555, 5 = RGB565)
          imageFormat = fm.readByte();
          if (imageFormat != 4 && imageFormat != 5) {
            ErrorLogger.log("[Viewer_WAD_12_PIX]: Unknown Image Format: " + imageFormat);
            return null;
          }

          // 2 - Row Width
          fm.skip(2);

          // 2 - Image Width
          width = ShortConverter.changeFormat(fm.readShort());
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          height = ShortConverter.changeFormat(fm.readShort());
          FieldValidator.checkWidth(height);

          // 2 - Offset X
          // 2 - Offset Y
          // 2 - Unknown
          fm.skip(6);

          // X - Filename
          // 1 - null Filename Terminator
          fm.readNullString();
        }
        else if (chunkID == 33) {
          if (imageFormat == 0) {
            ErrorLogger.log("[Viewer_WAD_12_PIX]: Reading Pixels before Header");
            return null;
          }

          // 4 - Pixel Data Length
          // 4 - Pixel Count
          // 4 - Bytes Per Pixel
          fm.skip(12);

          // X - Pixel Data
          if (imageFormat == 4) { // RGB555
            imageResource = ImageFormatReader.readRGB555BigEndian(fm, width, height);
          }
          else if (imageFormat == 5) { // RGB565
            imageResource = ImageFormatReader.readRGB565BigEndian(fm, width, height);
          }

          // found an image, so break out of the While
          break;
        }
        else {
          ErrorLogger.log("[Viewer_WAD_12_PIX]: Unknown Chunk ID: " + chunkID);
          return null;
        }
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