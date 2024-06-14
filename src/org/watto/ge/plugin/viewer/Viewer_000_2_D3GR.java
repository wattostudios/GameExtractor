/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_000_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_000_2_D3GR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_000_2_D3GR() {
    super("000_2_D3GR", "DreamForge Intertainment D3GR Image");
    setExtensions("d3gr");

    setGames("Sanitarium");
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
      if (plugin instanceof Plugin_000_2) {
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

      // 4 - Header
      if (fm.readString(4).equals("D3GR")) {
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

      // get all the color Palettes in the archive
      if (PaletteManager.getNumPalettes() <= 0) {
        Resource[] resources = Archive.getResources();

        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          Resource currentResource = resources[i];
          if (currentResource.getExtension().equalsIgnoreCase("palette")) {
            // found a color palette file - need to extract it and read the colors
            int[] palette = extractPalette(resources[i]);
            PaletteManager.addPalette(new Palette(palette));
          }
        }
      }

      long arcSize = fm.getLength();

      // 4 - Header (D3GR)
      // 4 - Flags (flags[1]&32 == palette)
      fm.skip(8);

      // 4 - File Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - null
      // 4 - ID Directory Offset (can be null)
      // 4 - null
      fm.skip(12);

      // 2 - Number of Frames
      short numFrames = fm.readShort();
      FieldValidator.checkPositive(numFrames);

      // 2 - Maximum Width
      fm.skip(2);

      int[] offsets = new int[numFrames];
      for (int f = 0; f < numFrames; f++) {
        // 4 - Frame Data Offset (relative to the start of the File Data)
        int offset = dataOffset + fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[f] = offset;
      }

      int maxHeight = 0;
      int maxWidth = 0;

      ImageResource[] imageResources = new ImageResource[numFrames];
      short[] xPositions = new short[numFrames];
      short[] yPositions = new short[numFrames];
      for (int f = 0; f < numFrames; f++) {
        fm.relativeSeek(offsets[f]);

        // 4 - File Data Length (including these header fields)
        // 4 - Flags
        fm.skip(8);

        // 2 - X Position
        short xPos = fm.readShort();
        xPositions[f] = xPos;

        // 2 - Y Position
        short yPos = fm.readShort();
        yPositions[f] = yPos;

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        if (xPos + width > maxWidth) {
          maxWidth = xPos + width;
        }
        if (yPos + height > maxHeight) {
          maxHeight = yPos + height;
        }

        // X - Image Data (8-bit paletted)
        ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);
        imageResources[f] = imageResource;
      }

      fm.close();

      if (numFrames == 1) {
        return imageResources[0];
      }

      // now we have max width and height, so set that as the base image size and apply the image frames on to it
      int dataSize = maxWidth * maxHeight;
      for (int f = 0; f < numFrames; f++) {
        ImageResource imageResource = imageResources[f];
        int[] pixels = imageResource.getPixels();
        int width = imageResource.getWidth();
        int height = imageResource.getHeight();
        int xPos = xPositions[f];
        int yPos = yPositions[f];

        int[] newPixels = new int[dataSize];
        int readPos = 0;
        int writePos = (yPos * maxWidth) + xPos;
        //int xDifference = maxWidth - width;

        for (int y = 0; y < height; y++) {
          System.arraycopy(pixels, readPos, newPixels, writePos, width);
          readPos += width;
          writePos += maxWidth;
        }

        imageResource.setPixels(newPixels);
        imageResource.setWidth(maxWidth);
        imageResource.setHeight(maxHeight);
      }

      for (int f = 0; f < numFrames - 1; f++) {
        imageResources[f].setNextFrame(imageResources[f + 1]);
      }
      for (int f = 1; f < numFrames; f++) {
        imageResources[f].setPreviousFrame(imageResources[f - 1]);
      }
      imageResources[0].setPreviousFrame(imageResources[numFrames - 1]);
      imageResources[numFrames - 1].setNextFrame(imageResources[0]);

      return imageResources[0];

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
  public int[] extractPalette(Resource paletteResource) {
    try {
      int paletteLength = (int) paletteResource.getLength();

      ByteBuffer buffer = new ByteBuffer(paletteLength);
      FileManipulator fm = new FileManipulator(buffer);
      paletteResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      // 4 - Header (D3GR)
      // 4 - Flags (flags[1]&32 == palette)
      // 4 - null
      fm.skip(12);

      // 4 - Palette Offset (28)
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, paletteLength);

      // 12 - null
      fm.relativeSeek(offset);

      // 2 - Number of Colors (256)
      short numColors = fm.readShort();
      FieldValidator.checkNumColors(numColors);

      // 2 - Number of Colors (256)
      fm.skip(2);

      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // apply a gamma adjustment to the palette to make it a bit brighter
        int gammaAdjustment = 50;

        // 256*3 - RGB
        int bPixel = ByteConverter.unsign(fm.readByte()) + gammaAdjustment;
        if (bPixel > 255) {
          bPixel = 255;
        }
        int gPixel = ByteConverter.unsign(fm.readByte()) + gammaAdjustment;
        if (gPixel > 255) {
          gPixel = 255;
        }
        int rPixel = ByteConverter.unsign(fm.readByte()) + gammaAdjustment;
        if (rPixel > 255) {
          rPixel = 255;
        }
        int aPixel = 255;

        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
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
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}