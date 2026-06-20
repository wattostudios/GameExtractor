/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_ARK_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARK_2_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARK_2_TEX() {
    super("ARK_2_TEX", "ARK_2_TEX Image");
    setExtensions("tex");

    setGames("MotoGP: Ultimate Racing Technology",
        "MotoGP: Ultimate Racing Technology 2",
        "MotoGP: Ultimate Racing Technology 3");
    setPlatforms("PC", "XBox");
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
  public boolean canReplace(PreviewPanel panel) {
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
      if (plugin instanceof Plugin_ARK_2) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      try {
        // PC
        fm.skip(4);

        // 2 - Image Width
        if (FieldValidator.checkWidth(fm.readShort())) {
          rating += 5;
        }

        // 2 - Image Height
        if (FieldValidator.checkHeight(fm.readShort())) {
          rating += 5;
        }
      }
      catch (Throwable t) {
        // XBox
        fm.relativeSeek(0);

        fm.skip(4);

        // 4 - File Length [+20]
        //if (FieldValidator.checkEquals(fm.readInt() + 20, fm.getLength())) {
        if (fm.readInt() + 20 == fm.getLength()) {
          rating += 5;
        }
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

      int width = 0;
      int height = 0;
      int type = 0;

      // lets try for XBox straight up, because that has a good verification field

      // 2 - Unknown (1)
      // 2 - Unknown (4)
      fm.skip(4);

      boolean foundFormat = false;
      // 4 - File Length [+20]
      if (fm.readInt() + 20 == fm.getLength()) {
        // XBox

        // 4 - null
        // 1 - Unknown
        fm.skip(5);

        // 1 - Image Type [&15] (15=DXT5, 12=DXT1)
        type = (fm.readByte() & 15);
        if (type == 15) { // convert the type from XBox to PC
          type = 2;
        }
        else if (type == 12) {
          type = 1;
        }

        // 1 - Image Width [>>4] (width = 1<<value)
        width = 1 << (ByteConverter.unsign(fm.readByte()) >> 4);
        FieldValidator.checkWidth(width);

        // 1 - Image Height [&15] (height = 1<<value)
        height = 1 << (ByteConverter.unsign(fm.readByte()) & 15);
        FieldValidator.checkHeight(height);

        // 4 - Unknown
        fm.skip(4);

        foundFormat = true;
      }

      if (!foundFormat) {
        try {
          // Try for PC
          fm.relativeSeek(0);

          // 1 - null
          // 1 - Unknown (0/3)
          fm.skip(2);

          // 1 - Image Type? (1=DXT1,2=DXT5)
          type = fm.readByte();

          // 1 - Number of Mipmaps
          fm.skip(1);

          // 2 - Image Width
          width = fm.readShort();
          //FieldValidator.checkWidth(width);
          FieldValidator.checkRange(width, 4, 4096);

          // 2 - Image Height
          height = fm.readShort();
          //FieldValidator.checkHeight(height);
          FieldValidator.checkRange(height, 4, 4096);
        }
        catch (Throwable t) {
          // try for XBox
          fm.relativeSeek(0);

          // 2 - Unknown (1)
          if (fm.readShort() != 1) {
            return null;
          }

          // 2 - Unknown (4)
          if (fm.readShort() != 4) {
            return null;
          }

          // 4 - File Length [+20]
          // 4 - null
          // 1 - Unknown
          fm.skip(9);

          // 1 - Image Type [&15] (15=DXT5, 12=DXT1)
          type = (fm.readByte() & 15);
          if (type == 15) { // convert the type from XBox to PC
            type = 2;
          }
          else if (type == 12) {
            type = 1;
          }

          // 1 - Image Width [>>4] (width = 1<<value)
          width = 1 << (ByteConverter.unsign(fm.readByte()) >> 4);
          FieldValidator.checkWidth(width);

          // 1 - Image Height [&15] (height = 1<<value)
          height = 1 << (ByteConverter.unsign(fm.readByte()) & 15);
          FieldValidator.checkHeight(height);

          // 4 - Unknown
          fm.skip(4);
        }
      }

      // X - Pixels
      ImageResource imageResource = null;
      if (type == 1) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (type == 2) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (type == 4) {
        // 1 - null
        fm.skip(1);

        // 1024 - Color Palette (RGBA)
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);

        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (type == 10) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_ARK_2_TEX] Unknown Image Format: " + type);
        return null;
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

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