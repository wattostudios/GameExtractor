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
import org.watto.ge.plugin.archive.Plugin_S3DPAK;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_S3DPAK_TCIP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_S3DPAK_TCIP() {
    super("S3DPAK_TCIP", "S3DPAK_TCIP");
    setExtensions(""); // no extension

    setGames("TimeShift");
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
      if (plugin instanceof Plugin_S3DPAK) {
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

      // 6 - Stuff
      fm.skip(6);

      // 4 - Header (TCIP)
      if (fm.readString(4).equals("TCIP")) {
        rating += 25;
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

      int imageType = 12;
      int numMipmaps = 1;
      short width = 0;
      short height = 0;

      int arcSize = (int) fm.getLength();
      while (fm.getOffset() < arcSize) {
        // 2 - Property ID
        short propertyID = fm.readShort();

        // 4 - Unknown
        fm.skip(4);

        // X - Property Data
        if (propertyID == 240) {
          // 4 - Header (TCIP)
          fm.skip(4);
        }
        else if (propertyID == 241) {
          // 2 - Image Width
          width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - null
          fm.skip(2);

          // 2 - Image Height
          height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - null
          fm.skip(2);
        }
        else if (propertyID == 242) {
          // 4 - Image Data Format? (12=DXT1)
          imageType = fm.readInt();
        }
        else if (propertyID == 249) {
          // 4 - Number of Mipmaps
          numMipmaps = fm.readInt();
          FieldValidator.checkRange(numMipmaps, 1, 20);
        }
        else if (propertyID == 250) {
          // 4 - Unknown (0)
          fm.skip(4);
        }
        else if (propertyID == 251) {
          // 4 - Unknown (0)
          fm.skip(4);
        }
        else if (propertyID == 252) {
          // 4 - Unknown (0)
          fm.skip(4);
        }
        else if (propertyID == 255) {
          // X - Image Data
          break;
        }
        else if (propertyID == 256) {
          // 1 - Unknown
          fm.skip(1);
        }
        else if (propertyID == 1) {
          // end of file
          return null; // should only get here if we didn't find any image data
        }
        else {
          ErrorLogger.log("Viewer_S3DPAK_TCIP: Unknown property ID: " + propertyID + " at offset " + (fm.getOffset() - 6));
          return null;
        }
      }

      if (width == 0 || height == 0) {
        return null;
      }

      // We should only be here if we stopped at the image data

      ImageResource imageResource = null;

      if (imageType == 12) {
        // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (imageType == 17) {
        // DXT3/5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (imageType == 0) {
        // RGBA
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
      }
      else if (imageType == 22) {
        // RGBA (alpha inversed)
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource = ImageFormatReader.removeAlpha(imageResource);
        imageResource.addProperty("ImageFormat", "RGBA");
      }
      else if (imageType == 2) {
        // RGB
        imageResource = ImageFormatReader.readRGB(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB");
      }
      else if (imageType == 13) {
        // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (imageType == 5) {
        // RGB565
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB565");
      }
      else {
        ErrorLogger.log("Viewer_S3DPAK_TCIP: Unknown image type: " + imageType);
        return null;
      }

      fm.close();

      imageResource.addProperty("MipmapCount", "" + numMipmaps);

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