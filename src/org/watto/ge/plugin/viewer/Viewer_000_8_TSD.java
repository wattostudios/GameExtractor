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
import org.watto.ge.plugin.archive.Plugin_000_8;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_000_8_TSD extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_000_8_TSD() {
    super("000_8_TSD", "Batman Vengeance TSD Image");
    setExtensions("tsd");

    setGames("Batman Vengeance");
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
      if (plugin instanceof Plugin_000_8) {
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

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      if (fm.readInt() == 1) {
        rating += 5;
      }

      if (fm.readInt() == 1) {
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

  /** So we can keep the file open when reading IFLs with nested images in it **/
  boolean keepFileOpen = false;

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

      // 4 - File Length
      // 4 - Unknown (1)
      fm.skip(8);

      // 4 - Block Type (1=Image, 0=IFL)
      int blockType = fm.readInt();

      // 256 - Source Filename (null terminated, filled with nulls)
      fm.skip(256);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (9)
      // 4 - null
      // 4 - Unknown (0=RGBA, 1=8-bit paletted, 3=4-bit paletted, 258/274=8-bit paletted with a funny number of colors)
      fm.skip(12);

      // 4 - Image Format (0=RGBA, 16=8-bit paletted, 8=4-bit paletted)
      int imageFormat = fm.readInt();

      // 8 - null
      fm.skip(8);

      ImageResource imageResource = null;

      if (blockType == 0) {
        // IFL with multiple images in it

        // 4 - null
        // 4 - Number of Footer Entries
        fm.skip(8);

        // 4 - Number of Images
        int numImages = fm.readInt();
        FieldValidator.checkNumFiles(numImages);

        ImageResource[] imageResources = new ImageResource[numImages];
        for (int i = 0; i < numImages; i++) {
          // Repeat from the IMAGE HEADER

          keepFileOpen = true;
          imageResource = readThumbnail(fm);
          keepFileOpen = false;

          if (imageResource == null) {
            // early exit - something went wrong reading an image
            return null;
          }
          imageResources[i] = imageResource;
        }

        // set the next frames
        for (int i = 0; i < numImages - 1; i++) {
          imageResources[i].setNextFrame(imageResources[i + 1]);
        }
        imageResources[numImages - 1].setNextFrame(imageResources[0]);

        // set the first frame, which will be returned at the end of this method
        imageResource = imageResources[0];
        //imageResource.setManualFrameTransition(true);

      }
      else {
        // a single image
        if (imageFormat == 16) {
          // 8-bit Paletted

          // X - Pixels
          int numPixels = width * height;
          byte[] pixelBytes = fm.readBytes(numPixels);

          // 4 - Unknown
          fm.skip(4);

          // X - Palette
          //System.out.println("Palette at " + fm.getOffset());
          int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);

          int[] pixels = new int[numPixels];
          for (int i = 0; i < numPixels; i++) {
            pixels[i] = palette[ByteConverter.unsign(pixelBytes[i])];
          }

          imageResource = new ImageResource(pixels, width, height);
        }
        else if (imageFormat == 0) {
          // RGBA
          imageResource = ImageFormatReader.readRGBA(fm, width, height);
        }
        else if (imageFormat == 8) {
          // 4-bit Paletted

          // X - Pixels
          int numPixels = width * height;
          byte[] pixelBytes = fm.readBytes(numPixels / 2);

          // 4 - Unknown
          fm.skip(4);

          // X - Palette
          int[] palette = ImageFormatReader.readPaletteRGBA(fm, 16);

          int[] pixels = new int[numPixels];
          for (int i = 0, s = 0; i < numPixels; i += 2, s++) {
            int sourcePixel = ByteConverter.unsign(pixelBytes[s]);

            int pixel1 = sourcePixel & 15;
            int pixel2 = sourcePixel >> 4;

            pixels[i] = palette[pixel1];
            pixels[i + 1] = palette[pixel2];
          }

          imageResource = new ImageResource(pixels, width, height);

        }
        else {
          ErrorLogger.log("[Viewer_000_8_TSD] Unknown Image Format: " + imageFormat);
        }
      }

      if (!keepFileOpen) {
        fm.close();
      }

      if (imageResource != null && blockType != 0) {
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }

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