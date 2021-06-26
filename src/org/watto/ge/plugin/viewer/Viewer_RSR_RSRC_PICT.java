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
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RSR_RSRC;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RSR_RSRC_PICT extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_RSR_RSRC_PICT() {
    super("RSR_RSRC_PICT", "Imperialism 2 PICT image [RSR_RSRC_PICT]");
    setExtensions("pict");

    setGames("Imperialism 2");
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_RSR_RSRC) {
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

      // 4 - Header Size (40)
      if (fm.readInt() == 40) {
        rating += 5;
      }

      // 4 - Image Width/Height
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Width/Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 2 - Unknown (1)
      // 2 - Bits Per Pixel? (8)
      // 4 - null
      fm.skip(8);

      // 4 - Image Data Length
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
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

      // 4 - Header Size (40)
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 2 - Unknown (1)
      // 2 - Bits Per Pixel? (8)
      // 4 - null
      fm.skip(8);

      // 4 - Image Data Length
      int imageDataLength = fm.readInt();
      FieldValidator.checkLength(imageDataLength, arcSize);

      // 4 - Unknown (2835)
      // 4 - Unknown (2835)
      // 8 - null
      fm.skip(16);

      // read the color palette

      int numColors = 256;
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        // INPUT = RGBA
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = ~ByteConverter.unsign(fm.readByte()); // NOT (ie "0" is full alpha, "255" is no alpha)

        // OUTPUT = ARGB
        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
      }

      int widthPadding = 0;
      //int heightPadding = 0;

      if (width % 4 != 0) {
        widthPadding = 4 - (width % 4);
      }

      //if (height % 4 != 0) {
      //  heightPadding = 4 - (height % 4);
      //}

      // read the image data
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      int writePos = 0;
      for (int h = 0; h < height; h++) {
        for (int w = 0; w < width; w++) {
          // 1 - Color Palette Index
          pixels[writePos++] = palette[ByteConverter.unsign(fm.readByte())];
        }
        fm.skip(widthPadding);
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

      // flip the image vertically - it's upside down
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

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      int widthPadding = 0;
      int heightPadding = 0;

      if (imageWidth % 4 != 0) {
        widthPadding = 4 - (imageWidth % 4);
      }

      if (imageHeight % 4 != 0) {
        heightPadding = 4 - (imageHeight % 4);
      }

      im.convertToPaletted(); // This format is a paletted image
      im.changeColorCount(256);// Reduce to 256 colors, if necessary

      int imageDataLength = (imageHeight + heightPadding) * (imageWidth + widthPadding);

      // 4 - Header Size (40)
      fm.writeInt(40);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // 4 - Image Height
      fm.writeInt(imageHeight);

      // 2 - Unknown (1)
      fm.writeShort(1);

      // 2 - Bits Per Pixel? (8)
      fm.writeShort(8);

      // 4 - null
      fm.writeInt(0);

      // 4 - Image Data Length
      fm.writeInt(imageDataLength);

      // 4 - Unknown (2835)
      fm.writeInt(2835);

      // 4 - Unknown (2835)
      fm.writeInt(2835);

      // 8 - null
      fm.writeInt(0);
      fm.writeInt(0);

      int numColors = 256;
      int[] palette = im.getPalette();

      for (int i = 0; i < numColors; i++) {
        int color = palette[i];

        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        fm.writeByte((byte) (color >> 16));
        fm.writeByte((byte) (color >> 8));
        fm.writeByte((byte) (color));
        fm.writeByte(0); // NOT (ie "0" is full alpha, "255" is no alpha)
      }

      // X - Pixels
      int[] pixels = im.getPixels();

      // image is flipped vertically, so write it backwards
      for (int h = imageHeight - 1; h >= 0; h--) {
        for (int w = 0; w < imageWidth; w++) {
          // 1 - Color Palette Index
          fm.writeByte(pixels[h * imageWidth + w]);
        }

        // write any width padding for this row
        for (int p = 0; p < widthPadding; p++) {
          fm.writeByte(0);
        }
      }

      // write any height padding
      for (int h = 0; h < heightPadding; h++) {
        // write the padded row
        for (int w = 0; w < imageWidth; w++) {
          fm.writeByte(0);
        }

        // write any width padding for this row
        for (int p = 0; p < widthPadding; p++) {
          fm.writeByte(0);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}