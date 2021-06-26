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
import org.watto.ge.plugin.archive.Plugin_DR_DR12;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DR_DR12_8IMAGE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DR_DR12_8IMAGE() {
    super("DR_DR12_8IMAGE", "Showdown: Legends of Wrestling 8IMAGE Image");
    setExtensions("8image", "16image");

    setGames("Showdown: Legends of Wrestling");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_DR_DR12) {
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

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Number of Colors (256)
      if (fm.readInt() == 256) {
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

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Number of Colors (256)
      int numColors = fm.readInt();
      //FieldValidator.checkNumColors(numColors, 256);

      // 2 - Bit Depth (8/16)
      short bitDepth = fm.readShort();

      // 2 - Unknown (1)
      // 36 - Description
      fm.skip(38);

      // 4 - Pixel Data Length
      int dataLength = fm.readInt();
      FieldValidator.checkLength(dataLength, arcSize);

      // 8 - null
      fm.skip(8);

      ImageResource imageResource = null;
      if (bitDepth == 8) {

        // X - Pixel Data
        byte[] rawPixels = fm.readBytes(dataLength);

        // X - Palette
        int[] palette = new int[numColors];

        for (int i = 0; i < numColors; i++) {
          // INPUT = RGBA
          // OUTPUT = ARGB
          //palette[i] = ((ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 24));
          palette[i] = ((ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte()) | (255 << 24));
          fm.skip(1);//skip the alpha byte
        }

        // un-stripe the palette
        palette = ImageFormatReader.unstripePalettePS2(palette);
        /*
        int parts = palette.length / 32;
        int stripes = 2;
        int colors = 8;
        int blocks = 2;
        
        int j = 0;
        int[] newPalette = new int[numColors];
        for (int part = 0; part < parts; part++) {
          for (int block = 0; block < blocks; block++) {
            for (int stripe = 0; stripe < stripes; stripe++) {
              for (int color = 0; color < colors; color++) {
                newPalette[j++] = palette[part * colors * stripes * blocks + block * colors + stripe * stripes * colors + color];
              }
            }
          }
        }
        palette = newPalette;
        */

        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[ByteConverter.unsign(rawPixels[i])];
        }

        imageResource = new ImageResource(pixels, width, height);
      }
      else if (bitDepth == 16) {
        imageResource = ImageFormatReader.readABGR1555(fm, width, height);
        imageResource = ImageFormatReader.reverseAlpha(imageResource);
      }
      else {
        ErrorLogger.log("[Viewer_DR_DR12_8IMAGE] Unknown image bit depth: " + bitDepth);
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