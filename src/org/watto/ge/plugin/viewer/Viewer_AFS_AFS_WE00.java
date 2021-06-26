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
import org.watto.ge.plugin.archive.Plugin_AFS_AFS;
import org.watto.ge.plugin.archive.Plugin_IMG_AFS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_AFS_AFS_WE00 extends ViewerPlugin {

  /** Image format from PES 2010 pre-header **/
  int imageFormat = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_AFS_AFS_WE00() {
    super("AFS_AFS_WE00", "AFS_AFS_WE00");
    setExtensions(""); // no extension

    setGames("Pro Evolution Soccer 2008",
        "Pro Evolution Soccer 2009");
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
      if (plugin instanceof Plugin_AFS_AFS || plugin instanceof Plugin_IMG_AFS) {
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

      // 4 - Image Header (WE00)
      if (fm.readString(4).equals("WE00")) {
        rating += 25;
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

      long startingOffset = fm.getOffset();

      // 4 - Image Header (WE00)
      // 2 - Unknown (0/1)
      // 2 - Unknown
      fm.skip(8);

      ImageResource imageResource = null;

      byte[] determinatorBytes = fm.readBytes(4);
      if (IntConverter.convertLittle(determinatorBytes) + 16 == fm.getLength()) {
        // DDS (including header fields)

        // 2 - null
        // 2 - Header Size (16)
        fm.skip(4);

        // X - DDS Image (including all DDS headers)
        imageResource = new Viewer_DDS_DDS().readThumbnail(fm);
      }
      else {
        // check for pre-header introduced in PES 2010...
        long currentOffset = fm.getOffset();

        int headerSkip = IntConverter.convertLittle(determinatorBytes) - 12;
        if (headerSkip >= 0 && headerSkip < 1000) {
          fm.skip(headerSkip);

          if (fm.readString(4).equals("WE00")) {
            // Found a pre-header, so skip it and re-read from this point
            currentOffset = fm.getOffset() - 4;

            if (startingOffset < 1000) { // workaround for ExporterByteBuffer which has no backwards seeking
              fm.seek(0);
              fm.seek(startingOffset);

              // Go through a bit, to read the PES 2010 Image Format header
              fm.skip(14);
              imageFormat = fm.readShort();
            }
            fm.seek(currentOffset);

            return readThumbnail(fm);
          }
          else {
            // no pre-header, so go back and treat as a normal image
            if (startingOffset < 1000) { // workaround for ExporterByteBuffer which has no backwards seeking
              fm.seek(0);
              fm.seek(startingOffset);
            }

            fm.seek(currentOffset);
          }
        }

        // Check for DDS header in PES 2010...
        currentOffset = fm.getOffset();

        fm.skip(4);

        if (fm.readString(4).equals("DDS ")) {
          currentOffset = currentOffset + 4;

          if (startingOffset < 1000) { // workaround for ExporterByteBuffer which has no backwards seeking
            fm.seek(0);
            fm.seek(startingOffset);
          }

          fm.seek(currentOffset);
          return new Viewer_DDS_DDS().readThumbnail(fm);
        }
        else {
          if (startingOffset < 1000) { // workaround for ExporterByteBuffer which has no backwards seeking
            fm.seek(0);
            fm.seek(startingOffset);
          }

          fm.seek(currentOffset);
        }

        // PALETTED or RGBA image...

        // 2 - Image Width
        short width = ShortConverter.convertLittle(new byte[] { determinatorBytes[0], determinatorBytes[1] });
        if (width == 0 || width > 10000) {
          return null;
        }
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = ShortConverter.convertLittle(new byte[] { determinatorBytes[2], determinatorBytes[3] });
        if (height == 0 || height > 10000) {
          return null;
        }
        FieldValidator.checkHeight(height);

        // 2 - Header Size (16)
        // 2 - Unknown
        fm.skip(4);

        if (startingOffset != 0) {

          // PES 2010 - pre-header Image Format Code
          if (imageFormat == 16) {
            // RGBA
            imageResource = ImageFormatReader.readRGBA(fm, width, height);
          }
          else if (imageFormat == 12) {
            // RGB
            imageResource = ImageFormatReader.readRGB(fm, width, height);
          }
          else {
            ErrorLogger.log("[Viewer_AFS_AFS_WE00]: PES2010 Image Format Code " + imageFormat + " is an unknown format");
          }

          if (imageResource != null) {
            fm.close();
            return imageResource;
          }
        }

        // PALETTED (PES 2008/9)

        // for each color (256)
        int numColors = 256;
        int[] palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
          // 1 - Red
          // 1 - Green
          // 1 - Blue
          // 1 - Alpha
          int r = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int b = ByteConverter.unsign(fm.readByte());
          int a = ByteConverter.unsign(fm.readByte());

          palette[i] = ((a << 24) | (r << 16) | (g << 8) | b);
        }

        // for each pixel
        int numPixels = width * height;
        int[] pixels = new int[numPixels];
        for (int i = 0; i < numPixels; i++) {
          // 1 - Palette Index
          pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
        }

        imageResource = new ImageResource(pixels, width, height);
        imageResource.addProperty("ImageFormat", "8BitPaletted");
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