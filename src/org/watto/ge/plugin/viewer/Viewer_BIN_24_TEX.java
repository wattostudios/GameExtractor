/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.PalettedImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BIN_24;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_24_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_24_TEX() {
    super("BIN_24_TEX", "BIN_24 TEX Image");
    setExtensions("tex");

    setGames("Beyond Good & Evil");
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
      if (plugin instanceof Plugin_BIN_24) {
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

      // 4 - Header (-1)
      if (fm.readInt() == -1) {
        rating += 5;
      }
      else {
        return 0;
      }

      fm.skip(4);

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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public int[] extractPalette(Resource paletteResource) {
    try {
      int paletteLength = (int) paletteResource.getLength();

      ByteBuffer buffer = new ByteBuffer(paletteLength);
      FileManipulator fm = new FileManipulator(buffer);
      paletteResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      int numColors = 256;
      int[] palette = new int[numColors];

      if (paletteLength == 768) {
        for (int i = 0; i < numColors; i++) {
          // 256*3 - RGB
          int bPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int rPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = 255;

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }
      else if (paletteLength == 1024) {
        for (int i = 0; i < numColors; i++) {
          // 256*4 - RGBA
          int bPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int rPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = ByteConverter.unsign(fm.readByte());

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }
      if (paletteLength == 48) {
        numColors = 16;
        for (int i = 0; i < numColors; i++) {
          // 16*3 - RGB
          int bPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int rPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = 255;

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }
      else if (paletteLength == 64) {
        numColors = 16;
        for (int i = 0; i < numColors; i++) {
          // 16*4 - RGBA
          int bPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int rPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = ByteConverter.unsign(fm.readByte());

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }

      fm.close();

      return palette;
    }
    catch (Throwable t) {
      logError(t);
      return new int[0];
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

      long length = fm.getLength();

      // 4 - Header (-1)
      // 2 - Unknown (2)
      // 2 - Unknown (16390)
      fm.skip(8);

      // 2 - Image Width
      int width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown
      // 2 - Unknown (18)
      // 4 - null
      // 4 - Unknown
      // 2 - Number of Colors? (255)
      // 2 - Number of Colors? (255)
      // 4 - Padding? (222,192,222,192)
      fm.skip(20);

      // get all the color Palettes in the archive
      if (PaletteManager.getNumPalettes() <= 0) {
        Resource[] resources = Archive.getResources();

        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          Resource currentResource = resources[i];
          if (currentResource.getExtension().equalsIgnoreCase("pal")) {
            // found a color palette file - need to extract it and read the colors
            int[] palette = extractPalette(resources[i]);
            PaletteManager.addPalette(new Palette(palette));
          }
        }
      }

      int paletteID = -1;

      // get the paletteID from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        paletteID = Integer.parseInt(resource.getProperty("PaletteID"));
      }
      catch (Throwable t) {
        //
      }

      if (paletteID != -1) {
        PaletteManager.setCurrentPalette(paletteID);
      }

      int[] palette = PaletteManager.getCurrentPalette().getPalette();

      int numColors = palette.length;
      if (numColors <= 0) {
        ErrorLogger.log("[Viewer_BIN_24_TEX] Invalid number of colors: " + numColors);
        return null;
      }

      // Read the color indexes
      int numPixels = width * height;
      int[] indexes = new int[numPixels];

      if (numPixels > length) {
        // 4bit
        for (int i = 0; i < numPixels; i += 2) {
          int byteValue = ByteConverter.unsign(fm.readByte());

          int byte1 = byteValue >> 4;
          int byte2 = byteValue & 15;

          indexes[i] = byte1;
          indexes[i + 1] = byte2;
        }
      }
      else {
        // 8bit
        for (int i = 0; i < numPixels; i++) {
          indexes[i] = ByteConverter.unsign(fm.readByte());
        }
      }

      // image needs to be flipped vertically
      ImageResource flipResource = new ImageResource(indexes, width, height);
      flipResource = ImageFormatReader.flipVertically(flipResource);
      indexes = flipResource.getPixels();

      PalettedImageResource imageResource = new PalettedImageResource(indexes, width, height, palette);

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