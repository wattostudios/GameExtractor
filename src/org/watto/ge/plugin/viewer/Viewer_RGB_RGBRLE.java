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
import org.watto.ge.plugin.archive.Plugin_RGB;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RGB_RGBRLE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RGB_RGBRLE() {
    super("RGB_RGBRLE", "Arsenal Extended Power RGBRLE Image");
    setExtensions("rgbrle");

    setGames("Arsenal Extended Power");
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
      if (plugin instanceof Plugin_RGB) {
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

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown ((bytes)156,255)
      // 2 - Unknown ((bytes)136,255)
      fm.skip(4);

      // X - Image Data (RLE-compressed)
      int numBytes = width * height * 4;
      byte[] rawBytes = new byte[numBytes];
      int bytePos = 0;

      while (fm.getOffset() < arcSize) {
        // Read 1 byte
        int checkByte = ByteConverter.unsign(fm.readByte());

        if ((checkByte & 192) == 192) {
          //System.out.println("192 at " + (fm.getOffset() - 1));

          // for (byte-191) times, read the next 3 bytes (as an RGB color)
          int repeatCount = checkByte - 191;

          for (int r = 0; r < repeatCount; r++) {
            byte[] repeatBytes = fm.readBytes(3);
            rawBytes[bytePos++] = repeatBytes[0];
            rawBytes[bytePos++] = repeatBytes[1];
            rawBytes[bytePos++] = repeatBytes[2];
            rawBytes[bytePos++] = (byte) 255;
          }

        }
        else if ((checkByte & 128) == 128) {
          //System.out.println("128 at " + (fm.getOffset() - 1));

          // read the next (byte-127) bytes, each byte representing a greyscale value
          int copyCount = checkByte - 127;

          for (int r = 0; r < copyCount; r++) {
            byte readByte = fm.readByte();
            rawBytes[bytePos++] = readByte;
            rawBytes[bytePos++] = readByte;
            rawBytes[bytePos++] = readByte;
            rawBytes[bytePos++] = (byte) 255;
          }

        }
        else if ((checkByte & 64) == 64) {
          //System.out.println("64 at " + (fm.getOffset() - 1));

          // read the next 3 bytes (as an RGB color) and copy it (byte-63) times
          int repeatCount = checkByte - 63;

          byte[] repeatBytes = fm.readBytes(3);

          for (int r = 0; r < repeatCount; r++) {
            rawBytes[bytePos++] = repeatBytes[0];
            rawBytes[bytePos++] = repeatBytes[1];
            rawBytes[bytePos++] = repeatBytes[2];
            rawBytes[bytePos++] = (byte) 255;
          }

        }
        else {
          //System.out.println("0 at " + (fm.getOffset() - 1));

          // the next byte+1 values are blank/transparent

          for (int r = 0; r < checkByte + 1; r++) {
            rawBytes[bytePos++] = (byte) 0;
            rawBytes[bytePos++] = (byte) 0;
            rawBytes[bytePos++] = (byte) 0;
            rawBytes[bytePos++] = (byte) 0;
          }
        }
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(rawBytes));

      // X - Pixels
      ImageResource imageResource = ImageFormatReader.readRGBA(fm, width, height);
      //ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);

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