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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_POD_PODFILE_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_POD_PODFILE_2_CEL_CEL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_POD_PODFILE_2_CEL_CEL() {
    super("POD_PODFILE_2_CEL_CEL", "POD_PODFILE_2_CEL_CEL Image");
    setExtensions("cel");

    setGames("Ready For Math With Pooh",
        "Ready To Read With Pooh");
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
      if (plugin instanceof Plugin_POD_PODFILE_2) {
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
      if (header.equals("CEL ")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - File Length
      if (IntConverter.changeFormat(fm.readInt()) == fm.getLength()) {
        rating += 5;
      }

      // 4 - Header
      header = fm.readString(4);
      if (header.equals("INFO")) {
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

      long arcSize = fm.getLength();

      // read in the full image (which is small), so that the thumbnails don't cut short when reading the palette. Saves in double-handling
      byte[] imageBytes = fm.readBytes((int) arcSize);
      fm = new FileManipulator(new ByteBuffer(imageBytes));

      // 4 - Header ("CEL ")
      // 4 - File Length
      // 4 - Header (INFO)
      // 4 - Block Length (including these header fields) (36)
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown (1)
      // 8 - null
      fm.skip(36);

      // 4 - Image Width
      int width = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkHeight(height);

      String header = fm.readString(4);
      int blockSize = IntConverter.changeFormat(fm.readInt()) - 8;
      FieldValidator.checkLength(blockSize, arcSize);
      if (header.equals("BLTM")) {
        fm.skip(blockSize);

        header = fm.readString(4);
        blockSize = IntConverter.changeFormat(fm.readInt()) - 8;
      }

      if (!header.equals("LINZ")) {
        ErrorLogger.log("[POD_PODFILE_2_CEL_CEL] Unexpected block: " + header);
        return null;
      }

      // 4 - Number of Lines
      int numLines = IntConverter.changeFormat(fm.readInt());

      int[] offsets = new int[numLines];
      for (int i = 0; i < numLines; i++) {
        // 4 - Line Offset (relative to the start of the Image Data)
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // 4 - Header (DATA)
      header = fm.readString(4);

      if (!header.equals("DATA")) {
        ErrorLogger.log("[POD_PODFILE_2_CEL_CEL] Unexpected block: " + header);
        return null;
      }

      // 4 - Block Length (including these header fields)
      fm.skip(4);

      // COLOR PALETTE
      // get the Palette
      int[] palette = new int[0];
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getExtension().equalsIgnoreCase("pal")) {
          // found the color palette file - need to extract it and read the colors
          palette = extractPalette(resources[i]);
          break;
        }
      }

      int numColors = palette.length;
      if (numColors <= 0) {
        ErrorLogger.log("[VIEWER_IFF_SPR] Invalid number of colors: " + numColors);
        return null;
      }

      // X - Pixels
      int relOffset = (int) fm.getOffset();

      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      int outPos = 0;

      for (int h = 0; h < numLines; h++) {
        int jumpToOffset = offsets[h] - ((int) (fm.getOffset()) - relOffset);
        if (jumpToOffset < 0) {
          ErrorLogger.log("[POD_PODFILE_2_CEL_CEL] Line Overrun for line " + (h + 1));
          return null;
        }
        if (jumpToOffset > 0) {
          if (jumpToOffset != 2) { // seems like 2 is a common under-run
            ErrorLogger.log("[POD_PODFILE_2_CEL_CEL] Line Underrun for line " + (h + 1) + " by " + jumpToOffset);
          }
          fm.skip(jumpToOffset);
        }

        outPos = h * width;

        int writtenPixels = 0;
        while (writtenPixels < width && outPos < numPixels) {
          // 1 - Count
          int count = ByteConverter.unsign(fm.readByte());

          if (count != 0) {
            // 1 - Palette Index to repeat Count times
            int pixel = palette[ByteConverter.unsign(fm.readByte())];

            for (int c = 0; c < count; c++) {
              pixels[outPos] = pixel;
              outPos++;
            }

            writtenPixels += count;
          }
          else if (count == 0) {
            // 1 - Number of Palette Indexes to read
            count = ByteConverter.unsign(fm.readByte());

            if (count == 0) {
              // End Of Line
              if (writtenPixels < width) {
                ErrorLogger.log("[POD_PODFILE_2_CEL_CEL] End Of Line");
              }
              break;
            }
            else {
              // X - Palette Indexes
              for (int c = 0; c < count; c++) {
                int pixel = palette[ByteConverter.unsign(fm.readByte())];
                pixels[outPos] = pixel;
                outPos++;
              }

              writtenPixels += count;
            }
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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public int[] extractPalette(Resource paltResource) {
    try {
      ByteBuffer buffer = new ByteBuffer((int) paltResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      paltResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      // 4 - Header ("CEL ")
      // 4 - File Length
      // 4 - Header ("PAL ")
      // 4 - Block Length (including these header fields)
      fm.skip(16);

      // 4 - Number of Colors
      int numColors = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumColors(numColors);

      int[] palette = new int[256];

      for (int i = 0; i < numColors; i++) {
        // 4 - BGRA color

        /*
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = ByteConverter.unsign(fm.readByte());
        */

        //aPixel = 255;

        /*
        int byte1 = ByteConverter.unsign(fm.readByte());
        int byte2 = ByteConverter.unsign(fm.readByte());
        int byte3 = ByteConverter.unsign(fm.readByte());
        int byte4 = ByteConverter.unsign(fm.readByte());
        
        int rPixel = ((byte4 & 63) << 2) | (byte3 >> 6);
        int gPixel = ((byte3 & 63) << 2) | (byte2 >> 6);
        int bPixel = ((byte2 & 63) << 2) | (byte1 >> 6);
        int aPixel = ((byte1 & 63) << 2) | (byte4 >> 6);
        */

        int indexNumber = ByteConverter.unsign(fm.readByte());
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = 255;

        palette[indexNumber] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
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