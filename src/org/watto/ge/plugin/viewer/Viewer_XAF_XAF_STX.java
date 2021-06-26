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

import java.awt.Image;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_XAF_XAF;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_XAF_XAF_STX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_XAF_XAF_STX() {
    super("XAF_XAF_STX", "Beat Down: Fist Of Vengeance STX Image");
    setExtensions("stx", "bin");

    setGames("Beat Down: Fist Of Vengeance");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_XAF_XAF) {
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

      // 4 - Unknown (24)
      if (fm.readInt() == 24) {
        rating += 5;
      }

      // 4 - Image Length [+12]
      if (fm.readInt() + 12 == fm.getLength()) {
        rating += 5;
      }

      // 4 - Unknown ((bytes)255,255,3,24)
      if (fm.readInt() == 402915327) {
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

      // 4 - Unknown (24)
      // 4 - Image Length [+12]
      fm.skip(8);

      // 4 - Unknown ((bytes)255,255,3,24)
      int extraBytes = fm.readInt();

      // 4 - Unknown (1)
      // 4 - Unknown (16)
      // 4 - Unknown ((bytes)255,255,3,24)
      fm.skip(12);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Bits Per Pixel (8)
      int imageFormat = fm.readInt();

      // 4 - Unknown (64)
      fm.skip(4);

      ImageResource imageResource = null;

      if (imageFormat == 8) {
        // X - Pixels
        int numPixels = width * height;
        byte[] rawPixels = fm.readBytes(numPixels);

        // 256*4 - RGBA Palette
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);

        int[] pixels = new int[numPixels];
        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[ByteConverter.unsign(rawPixels[i])];
        }

        imageResource = new ImageResource(pixels, width, height);
        imageResource.addProperty("ColorCount", "256");
        imageResource.addProperty("ImageFormat", "8BitPaletted");
      }
      else if (imageFormat == 4) {
        // X - Pixels
        int numRawPixels = width * height;
        byte[] rawPixels = fm.readBytes(numRawPixels);

        // 16*4 - RGBA Palette
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 16);

        int numPixels = width * height;
        int[] pixels = new int[numPixels];
        for (int r = 0; r < numRawPixels; r++) {
          int rawPixel = ByteConverter.unsign(rawPixels[r]);

          pixels[r] = palette[rawPixel];
        }

        imageResource = new ImageResource(pixels, width, height);
        imageResource.addProperty("ColorCount", "16");
        imageResource.addProperty("ImageFormat", "8BitPaletted");
      }
      else if (imageFormat == 32) {
        // X - Pixels (RGBA)
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
      }

      imageResource.addProperty("ExtraBytes", "" + extraBytes);

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
  Only writes as 32bit (not paletted)
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      Image image = ivp.getImage();
      int imageWidth = ivp.getImageWidth();
      int imageHeight = ivp.getImageHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Try to get the existing ImageResource (if it was stored), otherwise build a new one
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      if (imageResource == null) {
        imageResource = new ImageResource(image, imageWidth, imageHeight);
      }

      // if we have extraBytes stored on the image, keep them, otherwise just use 402915327 instead
      int extraBytes = imageResource.getProperty("ExtraBytes", 402915327);

      // work out the file length
      long fileLength = 28 + (imageWidth * imageHeight * 4);

      // 4 - Unknown (24)
      fm.writeInt(24);

      // 4 - Image Length [+12]
      fm.writeInt(fileLength);

      // 4 - Unknown ((bytes)255,255,3,24)
      fm.writeInt(extraBytes);

      // 4 - Unknown (1)
      fm.writeInt(1);

      // 4 - Unknown (16)
      fm.writeInt(16);

      // 4 - Unknown ((bytes)255,255,3,24)
      fm.writeInt(extraBytes);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // 4 - Image Height
      fm.writeInt(imageHeight);

      // 4 - Bits Per Pixel (4/8/32)
      fm.writeInt(32);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // X - Pixels (RGBA)
      ImageFormatWriter.writeRGBA(fm, imageResource);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}