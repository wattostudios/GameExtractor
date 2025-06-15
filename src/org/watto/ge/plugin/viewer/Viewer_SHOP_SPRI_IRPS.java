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
import org.watto.ge.plugin.archive.Plugin_SHOP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_SHOP_SPRI_IRPS extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_SHOP_SPRI_IRPS() {
    super("SHOP_SPRI_IRPS", "SHOP_SPRI_IRPS Image");
    setExtensions("spri");

    setGames("My Disney Kitchen");
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_SHOP) {
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

      if (IntConverter.changeFormat(fm.readInt()) == 1280) {
        rating += 5;
      }

      // 4 - Header
      String header = fm.readString(4);
      if (header.equals("IRPS")) {
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

      // 4 - Unknown (BIG) (1280)
      // 4 - Header (IRPS)
      // 16 - null
      // 2 - null
      fm.skip(26);

      // 2 - Image Height (Number of RLE Lines)
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - null
      // 2 - Unknown
      // 2 - Unknown
      fm.skip(6);

      // COLOR PALETTE
      int[] palette = ImageFormatReader.readPaletteBGRA(fm, 256);

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      int outPos = 0;

      for (int h = 0; h < height; h++) {
        outPos = h * width;

        // 2 - Line Data Length
        short lineLength = fm.readShort();

        int writtenPixels = 0;

        while (lineLength > 0) {
          // read byte (VVVVVVCC)
          int control = ByteConverter.unsign(fm.readByte());
          lineLength--;

          int code = control & 3;
          int value = control >> 2;

          if (code == 0) { // 00
            // copy the next VVVVVV pixels from the previous line 
            int readPos = outPos - width;
            for (int c = 0; c < value; c++) {
              pixels[outPos] = pixels[readPos];
              outPos++;
              readPos++;
            }

            //outPos += value;
          }
          else if (code == 1) { // 01
            // transparent for the next VVVVVV pixels
            outPos += value;
          }
          else if (code == 2) { // 10
            // read a palette index
            int pixel = palette[ByteConverter.unsign(fm.readByte())];
            lineLength--;

            // repeat the palette index VVVVVV times
            for (int c = 0; c < value; c++) {
              pixels[outPos] = pixel;
              outPos++;
            }
          }
          else if (code == 3) { // 11
            // read the next VVVVVV palette indexes
            for (int c = 0; c < value; c++) {
              int pixel = palette[ByteConverter.unsign(fm.readByte())];
              pixels[outPos] = pixel;
              outPos++;
            }
            lineLength -= value;
          }

          writtenPixels += value;

          if (lineLength < 0) {
            ErrorLogger.log("[Viewer_SHOP_SPRI_IRPS] Line Overrun on line number " + (h + 1));
          }
          if (lineLength == 0 && writtenPixels != width) {
            ErrorLogger.log("[Viewer_SHOP_SPRI_IRPS] Line Underrun on line number " + (h + 1));
          }

        }

      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

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