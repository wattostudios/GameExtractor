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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_HSFS;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************
Ref: https://github.com/vpelletier/sword1-dc/blob/master/dump.py
**********************************************************************************************
**/
public class Viewer_DAT_HSFS_SPR8_SPR8 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_HSFS_SPR8_SPR8() {
    super("DAT_HSFS_SPR8_SPR8", "Broken Sword SPR Animations");
    setExtensions("spr8", "spr4", "spra", "back", "face", "fore", "fg16");

    setGames("Broken Sword: Shadow of the Templars");
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
      if (plugin instanceof Plugin_DAT_HSFS) {
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
      String header = fm.readString(4);
      if (header.equals("SPR8") || header.equals("SPR4") || header.equals("SPRA") || header.equals("BACK") || header.equals("FACE") || header.equals("FORE") || header.equals("FG16")) {
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
  
  **********************************************************************************************
  **/
  public ImageResource readFrame4(FileManipulator fm, int width, int height, int xPos, int yPos, int maxWidth, int maxHeight, int[] palette) {
    try {
      int numPixels = maxWidth * maxHeight;
      int[] pixels = new int[numPixels];

      int startX = xPos;
      int endX = startX + width;

      int startY = yPos;
      int endY = startY + height;

      boolean readByte = true;
      int currentByte = 0;

      for (int y = startY; y < endY; y++) {
        for (int x = startX; x < endX; x++) {
          int pixelPos = y * maxWidth + x;

          if (readByte) {
            readByte = false;
            currentByte = ByteConverter.unsign(fm.readByte());
          }
          else {
            readByte = true;
            currentByte >>= 4;
          }

          pixels[pixelPos] = palette[(currentByte & 15)];
        }
      }

      return new ImageResource(pixels, maxWidth, maxHeight);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readFrame8(FileManipulator fm, int width, int height, int xPos, int yPos, int maxWidth, int maxHeight, int[] palette) {
    try {
      int numPixels = maxWidth * maxHeight;
      int[] pixels = new int[numPixels];

      int startX = xPos;
      int endX = startX + width;

      int startY = yPos;
      int endY = startY + height;

      for (int y = startY; y < endY; y++) {
        for (int x = startX; x < endX; x++) {
          int pixelPos = y * maxWidth + x;
          pixels[pixelPos] = palette[ByteConverter.unsign(fm.readByte())];
        }
      }

      return new ImageResource(pixels, maxWidth, maxHeight);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readRowFore(FileManipulator fm, int width, int height, int[] columns, int[] offsets, int[] palette, int defaultColor) {
    try {
      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      for (int y = 0; y < height; y++) {

        //int column = columns[y];
        int offset = offsets[y];

        fm.relativeSeek(offset); // should already be here

        int x = 0;
        while (x < width) {
          // 1 - Offset Type
          int offsetType = ByteConverter.unsign(fm.readByte());

          // 1 - Length
          int length = ByteConverter.unsign(fm.readByte());

          if (offsetType == 1) {
            // default color
            for (int p = x; p < x + length; p++) {
              // just leave it alpha'd out

              //int pixelPos = y * width + p;
              //pixels[pixelPos] = palette[defaultColor];
            }
            x += length;
          }
          else if (offsetType == 2) {
            // raw data
            for (int p = x; p < x + length; p++) {
              int pixelPos = y * width + p;
              pixels[pixelPos] = palette[ByteConverter.unsign(fm.readByte())];
            }
            x += length;
          }

        }
      }

      return new ImageResource(pixels, width, height);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readRowFG16(FileManipulator fm, int width, int height, int[] columns, int[] offsets) {
    try {
      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      for (int y = 0; y < height; y++) {

        //int column = columns[y];
        int offset = offsets[y];

        fm.relativeSeek(offset); // should already be here

        int x = 0;
        while (x < width) {
          // 1 - Offset Type
          int offsetType = ByteConverter.unsign(fm.readByte());

          // 1 - Length
          int length = ByteConverter.unsign(fm.readByte());

          if (offsetType == 1) {
            // default color
            for (int p = x; p < x + length; p++) {
              // just leave it alpha'd out

              //int pixelPos = y * width + p;
              //pixels[pixelPos] = palette[defaultColor];
            }
            x += length;
          }
          else if (offsetType == 2) {
            // raw data
            for (int p = x; p < x + length; p++) {
              int pixelPos = y * width + p;

              int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

              int rPixel = ((pixel >> 11) & 31) * 8;
              int gPixel = ((pixel >> 5) & 63) * 4;
              int bPixel = (pixel & 31) * 8;
              int aPixel = 255;

              // OUTPUT = ARGB
              pixels[pixelPos] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

            }
            x += length;
          }

        }
      }

      return new ImageResource(pixels, width, height);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readFrameA(FileManipulator fm, int width, int height, int xPos, int yPos, int maxWidth, int maxHeight, int[] palette) {
    try {
      int numPixels = maxWidth * maxHeight;
      int[] pixels = new int[numPixels];

      int startX = xPos;
      int endX = startX + width;

      int startY = yPos;
      int endY = startY + height;

      for (int y = startY; y < endY; y++) {
        for (int x = startX; x < endX; x++) {
          int pixelPos = y * maxWidth + x;
          pixels[pixelPos] = palette[ByteConverter.unsign(fm.readByte())];
        }
      }

      // apply the alpha to the pixels
      for (int y = startY; y < endY; y++) {
        for (int x = startX; x < endX; x++) {
          int pixelPos = y * maxWidth + x;
          int alpha = ByteConverter.unsign(fm.readByte());
          pixels[pixelPos] = ((pixels[pixelPos] << 8 >> 8) | (alpha << 24));
        }
      }

      return new ImageResource(pixels, maxWidth, maxHeight);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] readPalette(FileManipulator fm, int numColors) {
    try {
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 2 - Palette Entry RGB565
        int pixel = ShortConverter.unsign(fm.readShort());

        int rPixel = ((pixel >> 11) & 31) * 8;
        int gPixel = ((pixel >> 5) & 63) * 4;
        int bPixel = (pixel & 31) * 8;
        int aPixel = 255;

        // OUTPUT = ARGB
        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
      }
      return palette;
    }
    catch (Throwable t) {
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
    String extension = FilenameSplitter.getExtension(fm.getFile());
    if (extension.equalsIgnoreCase("spr8")) {
      return readSPR8(fm);
    }
    else if (extension.equalsIgnoreCase("spr4")) {
      return readSPR4(fm);
    }
    else if (extension.equalsIgnoreCase("spra")) {
      return readSPRA(fm);
    }
    else if (extension.equalsIgnoreCase("back")) {
      return readBACK(fm);
    }
    else if (extension.equalsIgnoreCase("face")) {
      return readFACE(fm);
    }
    else if (extension.equalsIgnoreCase("fore")) {
      return readFORE(fm);
    }
    else if (extension.equalsIgnoreCase("fg16")) {
      return readFG16(fm);
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/

  public ImageResource readSPRA(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (SPRA)
      // 2 - null
      fm.skip(6);

      // 2 - Number of Frames
      short numFrames = fm.readShort();
      FieldValidator.checkPositive(numFrames);

      // X - Color Palette (for each frame)
      int[][] palettes = new int[numFrames][256];
      for (int i = 0; i < numFrames; i++) {
        int[] palette = readPalette(fm, 256);
        if (palette == null) {
          return null;
        }
        palettes[i] = palette;
      }

      int[] widths = new int[numFrames];
      int[] heights = new int[numFrames];
      int[] x = new int[numFrames];
      int[] y = new int[numFrames];
      int[] offsets = new int[numFrames];

      int relativeOffset = (int) (fm.getOffset() + (numFrames * 12));

      int maxWidth = 0;
      int maxHeight = 0;

      int minX = 50000;
      int minY = 50000;

      for (int i = 0; i < numFrames; i++) {
        // 2 - Frame Width
        short width = fm.readShort();

        // add an extra pixel if the width is odd
        if (width % 2 == 1) {
          width++;
        }

        FieldValidator.checkWidth(width + 1);
        widths[i] = width;

        // 2 - Frame Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height + 1);
        heights[i] = height;

        // 2 - X Position
        short xPos = fm.readShort();
        FieldValidator.checkWidth(xPos + 1); // +1 to allow 0
        x[i] = xPos;

        // 2 - Y Position
        short yPos = fm.readShort();
        FieldValidator.checkWidth(yPos + 1); // +1 to allow 0
        y[i] = yPos;

        int thisWidth = width + xPos;
        if (thisWidth > maxWidth) {
          maxWidth = thisWidth;
        }
        int thisHeight = height + yPos;
        if (thisHeight > maxHeight) {
          maxHeight = thisHeight;
        }

        if (xPos < minX) {
          minX = xPos;
        }
        if (yPos < minY) {
          minY = yPos;
        }

        // 4 - Offset to Frame Data (relative to the start of the Image Data)
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // Now that we have all the xPos and yPos values, and we know the minimum of each, we can adjust the 
      // frame xPos and yPos values so that they aren't way off to the side somewhere.
      // ie so that we can center the image on the screen
      maxWidth -= minX;
      maxHeight -= minY;

      for (int i = 0; i < numFrames; i++) {
        x[i] -= minX;
        y[i] -= minY;
      }

      ImageResource[] frames = new ImageResource[numFrames];

      for (int i = 0; i < numFrames; i++) {
        fm.relativeSeek(offsets[i]); // should already be at the right spot each time
        ImageResource frame = readFrameA(fm, widths[i], heights[i], x[i], y[i], maxWidth, maxHeight, palettes[i]);
        if (frame == null) {
          return null;
        }
        frames[i] = frame;
      }

      // Now join all the frames together
      if (numFrames != 1) {
        for (int i = 0; i < numFrames - 1; i++) {
          frames[i].setNextFrame(frames[i + 1]);
        }
        frames[numFrames - 1].setNextFrame(frames[0]); // continue again from the start
      }
      //frames[0].setManualFrameTransition(true);// For Testing Only

      fm.close();

      return frames[0];

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

  public ImageResource readBACK(FileManipulator fm) {
    try {

      // 5 - Header (BACKG)
      fm.skip(5);

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 1 - Number of Colors
      int numColors = ByteConverter.unsign(fm.readByte());

      // X - Color Palette
      int[] palette = readPalette(fm, numColors);
      if (palette == null) {
        return null;
      }

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixelPos = y * width + x;
          pixels[pixelPos] = palette[ByteConverter.unsign(fm.readByte())];
        }
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

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

  public ImageResource readFG16(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (FG16)
      fm.skip(4);

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      int relativeOffset = (int) (fm.getOffset() + (height * 8));

      int[] columns = new int[height];
      int[] offsets = new int[height];
      for (int i = 0; i < height; i++) {
        // 4 - Column
        columns[i] = fm.readInt();

        // 4 - Offset;
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // X - Pixels
      ImageResource imageResource = readRowFG16(fm, width, height, columns, offsets);
      if (imageResource == null) {
        return null;
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

  public ImageResource readFORE(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (FORE)
      fm.skip(4);

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 1 - Default Color
      int defaultColor = ByteConverter.unsign(fm.readByte());

      // X - Color Palette
      int[] palette = readPalette(fm, 256);
      if (palette == null) {
        return null;
      }

      int relativeOffset = (int) (fm.getOffset() + (height * 8));

      int[] columns = new int[height];
      int[] offsets = new int[height];
      for (int i = 0; i < height; i++) {
        // 4 - Column
        columns[i] = fm.readInt();

        // 4 - Offset;
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // X - Pixels
      ImageResource imageResource = readRowFore(fm, width, height, columns, offsets, palette, defaultColor);
      if (imageResource == null) {
        return null;
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

  public ImageResource readFACE(FileManipulator fm) {
    try {

      // 5 - Header (FACE8)
      // 1 - null
      fm.skip(6);

      // 2 - Number of Frames
      short numFrames = fm.readShort();
      FieldValidator.checkPositive(numFrames);

      // X - Color Palette
      int[] palette = readPalette(fm, 256);
      if (palette == null) {
        return null;
      }

      int width = 128;
      int height = 192;
      int xPos = 0;
      int yPos = 0;

      ImageResource[] frames = new ImageResource[numFrames];

      for (int i = 0; i < numFrames; i++) {
        ImageResource frame = readFrame8(fm, width, height, xPos, yPos, width, height, palette);
        if (frame == null) {
          return null;
        }
        frames[i] = frame;
      }

      // Now join all the frames together
      if (numFrames != 1) {
        for (int i = 0; i < numFrames - 1; i++) {
          frames[i].setNextFrame(frames[i + 1]);
        }
        frames[numFrames - 1].setNextFrame(frames[0]); // continue again from the start
      }
      //frames[0].setManualFrameTransition(true);// For Testing Only

      fm.close();

      return frames[0];

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

  public ImageResource readSPR8(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (SPR8)
      // 2 - null
      fm.skip(6);

      // 2 - Number of Frames
      short numFrames = fm.readShort();
      FieldValidator.checkPositive(numFrames);

      // X - Color Palette
      int[] palette = readPalette(fm, 256);
      if (palette == null) {
        return null;
      }

      int[] widths = new int[numFrames];
      int[] heights = new int[numFrames];
      int[] x = new int[numFrames];
      int[] y = new int[numFrames];
      int[] offsets = new int[numFrames];

      int relativeOffset = (int) (fm.getOffset() + (numFrames * 12));

      int maxWidth = 0;
      int maxHeight = 0;

      int minX = 50000;
      int minY = 50000;

      for (int i = 0; i < numFrames; i++) {
        // 2 - Frame Width
        short width = fm.readShort();

        // add an extra pixel if the width is odd
        if (width % 2 == 1) {
          width++;
        }

        FieldValidator.checkWidth(width + 1);
        widths[i] = width;

        // 2 - Frame Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height + 1);
        heights[i] = height;

        // 2 - X Position
        short xPos = fm.readShort();
        FieldValidator.checkWidth(xPos + 1); // +1 to allow 0
        x[i] = xPos;

        // 2 - Y Position
        short yPos = fm.readShort();
        FieldValidator.checkWidth(yPos + 1); // +1 to allow 0
        y[i] = yPos;

        int thisWidth = width + xPos;
        if (thisWidth > maxWidth) {
          maxWidth = thisWidth;
        }
        int thisHeight = height + yPos;
        if (thisHeight > maxHeight) {
          maxHeight = thisHeight;
        }

        if (xPos < minX) {
          minX = xPos;
        }
        if (yPos < minY) {
          minY = yPos;
        }

        // 4 - Offset to Frame Data (relative to the start of the Image Data)
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // Now that we have all the xPos and yPos values, and we know the minimum of each, we can adjust the 
      // frame xPos and yPos values so that they aren't way off to the side somewhere.
      // ie so that we can center the image on the screen
      maxWidth -= minX;
      maxHeight -= minY;

      for (int i = 0; i < numFrames; i++) {
        x[i] -= minX;
        y[i] -= minY;
      }

      ImageResource[] frames = new ImageResource[numFrames];

      for (int i = 0; i < numFrames; i++) {
        fm.relativeSeek(offsets[i]); // should already be at the right spot each time
        ImageResource frame = readFrame8(fm, widths[i], heights[i], x[i], y[i], maxWidth, maxHeight, palette);
        if (frame == null) {
          return null;
        }
        frames[i] = frame;
      }

      // Now join all the frames together
      if (numFrames != 1) {
        for (int i = 0; i < numFrames - 1; i++) {
          frames[i].setNextFrame(frames[i + 1]);
        }
        frames[numFrames - 1].setNextFrame(frames[0]); // continue again from the start
      }
      //frames[0].setManualFrameTransition(true);// For Testing Only

      fm.close();

      return frames[0];

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

  public ImageResource readSPR4(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (SPR4)
      // 2 - null
      fm.skip(6);

      // 2 - Number of Frames
      short numFrames = fm.readShort();
      FieldValidator.checkPositive(numFrames);

      // X - Color Palette
      int[] palette = readPalette(fm, 16);
      if (palette == null) {
        return null;
      }

      int[] widths = new int[numFrames];
      int[] heights = new int[numFrames];
      int[] x = new int[numFrames];
      int[] y = new int[numFrames];
      int[] offsets = new int[numFrames];

      int relativeOffset = (int) (fm.getOffset() + (numFrames * 12));

      int maxWidth = 0;
      int maxHeight = 0;

      int minX = 50000;
      int minY = 50000;

      for (int i = 0; i < numFrames; i++) {
        // 2 - Frame Width
        short width = fm.readShort();

        // add an extra pixel if the width is odd
        if (width % 2 == 1) {
          width++;
        }

        FieldValidator.checkWidth(width + 1);
        widths[i] = width;

        // 2 - Frame Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height + 1);
        heights[i] = height;

        // 2 - X Position
        short xPos = fm.readShort();
        FieldValidator.checkWidth(xPos + 1); // +1 to allow 0
        x[i] = xPos;

        // 2 - Y Position
        short yPos = fm.readShort();
        FieldValidator.checkWidth(yPos + 1); // +1 to allow 0
        y[i] = yPos;

        int thisWidth = width + xPos;
        if (thisWidth > maxWidth) {
          maxWidth = thisWidth;
        }
        int thisHeight = height + yPos;
        if (thisHeight > maxHeight) {
          maxHeight = thisHeight;
        }

        if (xPos < minX) {
          minX = xPos;
        }
        if (yPos < minY) {
          minY = yPos;
        }

        // 4 - Offset to Frame Data (relative to the start of the Image Data)
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize + 1);
        offsets[i] = offset;
      }

      // Now that we have all the xPos and yPos values, and we know the minimum of each, we can adjust the 
      // frame xPos and yPos values so that they aren't way off to the side somewhere.
      // ie so that we can center the image on the screen
      maxWidth -= minX;
      maxHeight -= minY;

      for (int i = 0; i < numFrames; i++) {
        x[i] -= minX;
        y[i] -= minY;
      }

      ImageResource[] frames = new ImageResource[numFrames];

      for (int i = 0; i < numFrames; i++) {
        fm.relativeSeek(offsets[i]); // should already be at the right spot each time
        ImageResource frame = readFrame4(fm, widths[i], heights[i], x[i], y[i], maxWidth, maxHeight, palette);
        if (frame == null) {
          return null;
        }
        frames[i] = frame;
      }

      // Now join all the frames together
      if (numFrames != 1) {
        for (int i = 0; i < numFrames - 1; i++) {
          frames[i].setNextFrame(frames[i + 1]);
        }
        frames[numFrames - 1].setNextFrame(frames[0]); // continue again from the start
      }
      //frames[0].setManualFrameTransition(true);// For Testing Only

      fm.close();

      return frames[0];

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