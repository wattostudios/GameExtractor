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

import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.stream.ManipulatorUnclosableInputStream;
import org.watto.io.stream.ManipulatorUnclosableOutputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_CMP_CMAP extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_CMP_CMAP() {
    super("CMP_CMAP", "Baram CMP Image");
    setExtensions("cmp");

    setGames("Baram");
    setPlatforms("PC");
    setStandardFileFormat(false);
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
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("CMAP")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      // 1 - ZLib Compression Header
      if (fm.readString(1).equals("x")) {
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

      // 4 - Header (CMAP)
      fm.skip(4);

      // 2 - Image Width/Height
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height/Width
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // ZLib Compression
      InflaterInputStream readSource = new InflaterInputStream(new ManipulatorUnclosableInputStream(fm));

      // X - Pixels
      int pixelCount = width * height;
      int[] pixels = new int[pixelCount];

      for (int p = 0; p < pixelCount; p++) {
        // 1 - Red
        // 1 - Alpha
        // 2 - Unknown
        // 1 - Blue
        // 1 - Green
        int r = ByteConverter.unsign((byte) readSource.read());
        int a = ByteConverter.unsign((byte) readSource.read());
        readSource.skip(2);
        int b = ByteConverter.unsign((byte) readSource.read());
        int g = ByteConverter.unsign((byte) readSource.read());

        // reverse the alpha
        a = 255 - a;

        pixels[p] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

      // close ZLib
      readSource.close();

      // close file
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

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // 4 - Header (CMAP)
      fm.writeString("CMAP");

      // 2 - Image Width/Height
      fm.writeShort((short) imageWidth);

      // 2 - Image Height/Width
      fm.writeShort((short) imageHeight);

      // ZLib Compression
      DeflaterOutputStream outputStream = new DeflaterOutputStream(new ManipulatorUnclosableOutputStream(fm));

      // X - Pixels
      int[] pixels = im.getImagePixels();
      int numPixels = pixels.length;

      for (int p = 0; p < numPixels; p++) {
        int pixel = pixels[p];

        // 1 - Red
        // 1 - Alpha
        // 2 - Unknown
        // 1 - Blue
        // 1 - Green

        // reverse the alpha
        int aPixel = (pixel >> 24);
        aPixel = 255 - aPixel;

        outputStream.write((byte) (pixel >> 16));
        outputStream.write((byte) aPixel);
        outputStream.write((byte) 0);
        outputStream.write((byte) 0);
        outputStream.write((byte) (pixel));
        outputStream.write((byte) (pixel >> 8));
      }

      // close ZLib
      outputStream.close();

      // close file
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}