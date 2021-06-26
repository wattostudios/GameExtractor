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
import org.watto.ge.plugin.archive.Plugin_BDL_MOIK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BDL_MOIK_TGA_CTNR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BDL_MOIK_TGA_CTNR() {
    super("BDL_MOIK_TGA_CTNR", "Worms Revolution TGA (CTNR) Image");
    setExtensions("tga");

    setGames("Worms Revolution");
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
      if (plugin instanceof Plugin_BDL_MOIK) {
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

      // 4 - Header (CTNR)
      if (fm.readString(4).equals("CTNR")) {
        rating += 50;
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

      // 4 - Header (CTNR)
      // 3 - null
      fm.skip(7);

      // 1 or 2 - Image Name
      if (ByteConverter.unsign(fm.readByte()) >= 128) {
        fm.skip(1);
      }

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Number of Mipmaps
      int mipmapCount = fm.readShort();
      FieldValidator.checkRange(mipmapCount, 1, 20);

      // 2 - Image Format (0=DXT1, 2=DXT5)
      //int imageFormat = fm.readShort();
      fm.skip(2);

      // 1 - Number of Mipmaps
      // for each mipmap
      //   4 - Length of Mipmap Image Data
      fm.skip(1 + mipmapCount * 4);

      // 1 - Number of Mipmaps
      // for each mipmap
      //   4 - Offset to Mipmap Image Data (relative to the start of the Image Data)
      fm.skip(1 + mipmapCount * 4);

      // 4 - Unknown (9=DXT1 / 11=DXT5)
      int imageFormat = fm.readShort();
      // 4 - Unknown (1)
      fm.skip(6);

      // 1 - Unknown
      fm.skip(1);

      // 1-2-3 - Unknown
      if (ByteConverter.unsign(fm.readByte()) >= 128) {
        if (ByteConverter.unsign(fm.readByte()) >= 128) {
          if (ByteConverter.unsign(fm.readByte()) >= 128) {
            fm.skip(1);
          }
        }
      }

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 9) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 11) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_BDL_MOIK_TGA_CTNR] Unknown Image Format: " + imageFormat);
      }

      if (imageResource != null) {
        imageResource = ImageFormatReader.flipVertically(imageResource);
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