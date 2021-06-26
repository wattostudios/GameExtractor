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
import org.watto.ge.plugin.archive.Plugin_MFS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_MFS_WIM_WIMG extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_MFS_WIM_WIMG() {
    super("MFS_WIM_WIMG", "Made Man WIM (WIMG) Image [MFS_WIM_WIMG]");
    setExtensions("wim");

    setGames("Made Man");
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
      if (plugin instanceof Plugin_MFS) {
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
      if (fm.readString(4).equals("WIMG")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      // 4 - Version? (1)
      if (fm.readInt() == 1) {
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

      // 4 - Header (WIMG)
      // 4 - File Length
      // 4 - Version? (1)
      fm.skip(12);

      // 4 - Short Filename (null-filled, maximum of 4 bytes)
      fm.skip(4);

      // 4 - Unknown (24)
      // 4 - null
      // 1 - Flag 1 (138/10)
      fm.skip(9);

      // 1 - Flag 2 (1/0)
      int flag2 = fm.readByte();

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 2 - null
      fm.skip(2);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - null
      // 2 - null
      fm.skip(6);

      // 2 - Number of Mipmaps [+1] (ie "0" means 1 mipmap, "6" means 7 mipmaps)
      short mipmapCount = (short) (fm.readShort() + 1);
      FieldValidator.checkNumFiles(mipmapCount);

      // 4 - Image Format (5TXD, 1TXD)
      String imageFormat = StringConverter.reverse(fm.readString(4));

      if (flag2 == 1) {
        // adjust the width to be a multiple of 2 [-2]
        if (width < 32) {
          width = 30;
        }
        else if (width == 32) {
          // OK
        }
        else if (width < 64) {
          width = 62;
        }
        else if (width == 64) {
          // OK
        }
        else if (width < 128) {
          width = 126;
        }
        else if (width == 128) {
          // OK
        }
        else if (width < 256) {
          width = 254;
        }
        else if (width == 256) {
          // OK
        }
        else if (width < 512) {
          width = 510;
        }
        else if (width == 512) {
          // OK
        }
      }

      // X - Image Data (DXT1/5)
      ImageResource imageResource = null;
      if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else {
        // BGRA
        imageFormat = "BGRA"; // for use later on
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      imageResource.addProperty("MipmapCount", "" + mipmapCount);
      imageResource.addProperty("ImageFormat", imageFormat);

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
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

}