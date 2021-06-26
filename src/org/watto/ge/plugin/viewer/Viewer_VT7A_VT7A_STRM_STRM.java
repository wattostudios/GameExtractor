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
import org.watto.ge.plugin.archive.Plugin_VT7A_VT7A;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VT7A_VT7A_STRM_STRM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VT7A_VT7A_STRM_STRM() {
    super("VT7A_VT7A_STRM_STRM", "Broken Sword 5 STRM Image");
    setExtensions("strm");

    setGames("Broken Sword 5: The Serpent's Curse");
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
      if (plugin instanceof Plugin_VT7A_VT7A) {
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
      if (fm.readString(4).equals("STRM")) {
        rating += 25;
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

      // 4 - Header (STRM)
      // 4 - Version? (5)
      // 4 - Unknown
      // 4 - null
      fm.skip(16);

      long relativeOffset = fm.getOffset();

      // 4 - null
      fm.skip(4);

      // 2 - Maximum Image Width?
      int originalMaxWidth = fm.readShort();
      FieldValidator.checkWidth(originalMaxWidth);

      // 2 - Maximum Image Height?
      int originalMaxHeight = fm.readShort();
      FieldValidator.checkHeight(originalMaxHeight);

      // 2 - Unknown (1)
      fm.skip(2);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Offset (relative to the start of the Image Details)
      int dirOffset = (int) (fm.readInt() + relativeOffset);
      FieldValidator.checkOffset(dirOffset);

      // 4 - Color Palette Offset (relative to the start of the Image Details)
      int paletteOffset = (int) (fm.readInt() + relativeOffset);
      FieldValidator.checkOffset(paletteOffset);

      // 4 - Unknown
      // 4 - Unknown (28)
      // 4 - null
      // 4 - Unknown (8)
      // 4 - Name Offset (relative to the start of This field)
      // 20 - null
      // 2 - Unknown
      // 2 - Number of Files

      fm.seek(dirOffset);

      int[] x = new int[numFiles];
      int[] y = new int[numFiles];
      int[] widths = new int[numFiles];
      int[] heights = new int[numFiles];
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];

      int minX = 50000;
      int minY = 50000;

      int maxWidth = 0;
      int maxHeight = 0;

      for (int i = 0; i < numFiles; i++) {
        // 2 - X Pos
        short xPos = fm.readShort();
        x[i] = xPos;

        // 2 - Y Pos
        short yPos = fm.readShort();
        y[i] = yPos;

        // 2 - Image Width (not quite)
        short width = fm.readShort();
        widths[i] = width;

        // 2 - Image Height
        short height = fm.readShort();
        heights[i] = height;

        int thisWidth = originalMaxWidth + xPos;
        if (thisWidth > maxWidth) {
          maxWidth = thisWidth;
        }
        int thisHeight = originalMaxHeight + yPos;
        if (thisHeight > maxHeight) {
          maxHeight = thisHeight;
        }

        if (xPos < minX) {
          minX = xPos;
        }
        if (yPos < minY) {
          minY = yPos;
        }

        // 4 - File Offset (relative to the start of the Details Directory)
        int offset = dirOffset + fm.readInt();
        FieldValidator.checkOffset(offset);
        offsets[i] = offset;

        dirOffset += 16; // each subsequent file needs to add another 16 bytes to the offset to get the right spot

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;
      }

      // Now that we have all the xPos and yPos values, and we know the minimum of each, we can adjust the 
      // frame xPos and yPos values so that they aren't way off to the side somewhere.
      // ie so that we can center the image on the screen
      maxWidth -= minX;
      maxHeight -= minY;

      for (int i = 0; i < numFiles; i++) {
        x[i] -= minX;
        y[i] -= minY;
      }

      fm.seek(paletteOffset);

      int[] palette = new int[256];
      for (int i = 0; i < 256; i++) {
        // 4 - Color (BGRA)
        // INPUT = RGBA
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = ByteConverter.unsign(fm.readByte());

        // OUTPUT = ARGB
        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
      }

      int maxDecompLength = maxWidth * maxHeight;

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // X - Image Data
      ImageResource[] frames = new ImageResource[numFiles];

      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        //int width = widths[i];
        int height = heights[i];
        int length = lengths[i];

        // decompress the frame
        byte[] frameData = new byte[maxDecompLength];
        int decompLength = 0;

        exporter.openUnclosable(fm, length, maxDecompLength);

        for (int b = 0; b < maxDecompLength; b++) {
          if (exporter.available()) {
            frameData[b] = (byte) exporter.read();
          }
          else {
            decompLength = b;
            break;
          }
        }

        exporter.close();

        //int realWidth = width;
        //int realHeight = decompLength / realWidth;
        int realHeight = height;
        int realWidth = decompLength / realHeight;

        /*
        int[] pixels = new int[decompLength];
        for (int p = 0; p < decompLength; p++) {
          pixels[p] = palette[ByteConverter.unsign(frameData[p])];
        }
        */
        int[] pixels = new int[maxDecompLength];

        int startX = x[i];
        int endX = startX + realWidth;

        int startY = y[i];
        int endY = startY + realHeight;

        int readPos = 0;
        for (int y1 = startY; y1 < endY; y1++) {
          for (int x1 = startX; x1 < endX; x1++) {
            int pixelPos = y1 * maxWidth + x1;
            pixels[pixelPos] = palette[ByteConverter.unsign(frameData[readPos])];
            readPos++;
          }
        }

        //ImageResource imageResource = new ImageResource(pixels, realWidth, realHeight);
        ImageResource imageResource = new ImageResource(pixels, maxWidth, maxHeight);
        frames[i] = imageResource;

      }

      // set the transitions
      for (int i = 0; i < numFiles - 1; i++) {
        frames[i].setNextFrame(frames[i + 1]);
      }
      frames[numFiles - 1].setNextFrame(frames[0]);

      //frames[0].setManualFrameTransition(true); // TESTING ONLY

      exporter.close();
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