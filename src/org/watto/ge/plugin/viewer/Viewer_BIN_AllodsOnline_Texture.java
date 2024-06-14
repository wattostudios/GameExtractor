/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_AllodsOnline_Texture extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_AllodsOnline_Texture() {
    super("BIN_AllodsOnline_Texture", "Allods Online BIN Image");
    setExtensions("bin");

    setGames("Allods Online");
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      if (fm.getFile().getName().contains("(Texture)")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      String header = fm.readString(1);
      if (header.equals("x")) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
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

      int arcSize = (int) fm.getLength();

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, arcSize, arcSize);

      byte[] fileData = new byte[arcSize * 10]; // guess max 10* compression

      int decompWritePos = 0;
      while (exporter.available()) { // make sure we read the next bit of data, if required
        fileData[decompWritePos++] = (byte) exporter.read();
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(fileData));

      long largestOffset = 0;
      int largestSizeIndicator = 12; // larger than ever possible
      int largestLength = 0;
      int mipmapCount = 0;
      while (fm.getOffset() < decompWritePos) {
        // 4 - Width/Height Indicator (2=256, 3=128, 4=64, 5=32, 6=16, 7=8, 8=4)
        int sizeIndicator = fm.readInt();
        if (sizeIndicator == 0) {
          break; // EOF
        }
        FieldValidator.checkRange(sizeIndicator, 0, 12);
        mipmapCount++;

        // 4 - Mipmap Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, decompWritePos);

        if (sizeIndicator < largestSizeIndicator) {
          largestSizeIndicator = sizeIndicator;
          largestOffset = fm.getOffset();
          largestLength = length;
        }

        // X - Mipmap Data (DXT3)
        fm.skip(length);
      }

      int width = 0;
      int height = 0;

      /*
      if (largestSizeIndicator == 1) {
        width = 512;
        height = 512;
      }
      else if (largestSizeIndicator == 2) {
        width = 256;
        height = 256;
      }
      else if (largestSizeIndicator == 3) {
        width = 128;
        height = 128;
      }
      else if (largestSizeIndicator == 4) {
        width = 64;
        height = 64;
      }
      else if (largestSizeIndicator == 5) {
        width = 32;
        height = 32;
      }
      else if (largestSizeIndicator == 6) {
        width = 16;
        height = 16;
      }
      else if (largestSizeIndicator == 7) {
        width = 8;
        height = 8;
      }
      else if (largestSizeIndicator == 8) {
        width = 4;
        height = 4;
      }
      */
      if (mipmapCount == 8) {
        width = 512;
        height = 512;
      }
      else if (mipmapCount == 7) {
        width = 256;
        height = 256;
      }
      else if (mipmapCount == 6) {
        width = 128;
        height = 128;
      }
      else if (mipmapCount == 5) {
        width = 64;
        height = 64;
      }
      else if (mipmapCount == 4) {
        width = 32;
        height = 32;
      }
      else if (mipmapCount == 3) {
        width = 16;
        height = 16;
      }
      else if (mipmapCount == 2) {
        width = 8;
        height = 8;
      }
      else if (mipmapCount == 1) {
        width = 4;
        height = 4;
      }
      else {
        ErrorLogger.log("[Viewer_BIN_AllodsOnline_Texture] Mipmap Size Too Small: " + largestSizeIndicator);
        return null;
      }

      // X - Mipmap Data
      fm.seek(largestOffset);

      ImageResource imageResource = null;

      int pixelCount = width * height;
      if (pixelCount == largestLength) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (pixelCount == largestLength * 2) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (pixelCount * 4 == largestLength) {
        imageResource = ImageFormatReader.readDXT5(fm, width * 2, height * 2);
      }
      else if (pixelCount * 2 == largestLength) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height * 2);
      }
      else {

        // DXT3
        if (largestLength == 262144) {
          width = 512;
          height = 512;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 65536) {
          width = 256;
          height = 256;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 16384) {
          width = 128;
          height = 128;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 4096) {
          width = 64;
          height = 64;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 1024) {
          width = 32;
          height = 32;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 524288) {
          width = 512;
          height = 1024;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 131072) {
          width = 256;
          height = 512;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 32768) {
          width = 128;
          height = 256;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 8192) {
          width = 64;
          height = 128;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        else if (largestLength == 2048) {
          width = 32;
          height = 64;
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }

        // unknown
        else {
          ErrorLogger.log("[Viewer_BIN_AllodsOnline_Texture] Unknown Image Format for size: " + largestLength + " and width/height: " + width);
          return null;
        }
      }

      //imageResource = ImageFormatReader.flipVertically(imageResource);
      //imageResource = ImageFormatReader.rotateLeft(imageResource);

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