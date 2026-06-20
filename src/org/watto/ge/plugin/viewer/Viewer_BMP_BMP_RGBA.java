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
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BMP_BMP_RGBA extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BMP_BMP_RGBA() {
    super("BMP_BMP_RGBA", "Bitmap Image with Alpha");
    setExtensions("bmp");
    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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

      if (fm.readString(2).equals("BM")) {
        rating += 50;
      }

      fm.skip(26);
      if (fm.readShort() == 32 && fm.readInt() == 0) {
        // This is specifically for custom handling of 32-bit RGBA images
        rating += 5;
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

      long arcSize = fm.getLength();

      // 2 - Header (BM)
      if (!fm.readString(2).equals("BM")) {
        return null;
      }

      // 4 - File Length
      FieldValidator.checkEquals(fm.readInt(), arcSize);

      // 2 - Reserved (null)
      // 2 - Reserved (null)
      fm.skip(4);

      // 4 - Pixel Data Offset (54)
      int pixelOffset = fm.readInt();
      FieldValidator.checkOffset(pixelOffset);

      // 4 - Header Size (40)
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 2 - Number of Color Planes (1)
      FieldValidator.checkEquals(fm.readShort(), 1);

      // 2 - Bits per Pixel
      FieldValidator.checkEquals(fm.readShort(), 32);

      // 4 - Compression
      FieldValidator.checkEquals(fm.readInt(), 0);

      // 4 - Pixel Data Length
      FieldValidator.checkLength(fm.readInt(), arcSize);

      // 4 - Horizontal Resolution
      // 4 - Vertical Resolution
      // 4 - Number of Colors in the Palette
      // 4 - Number of Important Colors
      // X - Pixel Data (RGBA)
      fm.relativeSeek(pixelOffset);

      ImageResource imageResource = ImageFormatReader.readBGRA(fm, width, height);
      imageResource = ImageFormatReader.flipVertically(imageResource);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      int width = imageResource.getWidth();
      int height = imageResource.getHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // 2 - Header (BM)
      fm.writeString("BM");

      // 4 - File Length
      int pixelLength = width * height * 4;
      int fileLength = 54 + pixelLength;
      fm.writeInt(fileLength);

      // 2 - Reserved (null)
      // 2 - Reserved (null)
      fm.writeInt(0);

      // 4 - Pixel Data Offset (54)
      fm.writeInt(54);

      // 4 - Header Size (40);
      fm.writeInt(40);

      // 4 - Image Width
      fm.writeInt(width);

      // 4 - Image Height
      fm.writeInt(height);

      // 2 - Number of Color Planes (1)
      fm.writeShort(1);

      // 2 - Bits per Pixel
      fm.writeShort(32);

      // 4 - Compression
      fm.writeInt(0);

      // 4 - Pixel Data Length
      fm.writeInt(pixelLength);

      // 4 - Horizontal Resolution
      fm.writeInt(0);

      // 4 - Vertical Resolution
      fm.writeInt(0);

      // 4 - Number of Colors in the Palette
      fm.writeInt(0);

      // 4 - Number of Important Colors
      fm.writeInt(0);

      // X - Pixel Data (RGBA)
      imageResource = ImageFormatReader.flipVertically(imageResource);
      ImageFormatWriter.writeRGBA(fm, imageResource);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}