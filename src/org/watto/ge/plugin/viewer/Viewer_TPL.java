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
import org.watto.ge.plugin.archive.Plugin_APP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TPL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TPL() {
    super("TPL", "Wii TPL Image");
    setExtensions("tpl");

    setGames("Wii");
    setPlatforms("Wii");
    setStandardFileFormat(true);
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
      if (plugin instanceof Plugin_APP) {
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
      if (fm.readInt() == 816783360) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Number of Images
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      // 4 - Details Directory Offset (12)
      if (IntConverter.changeFormat(fm.readInt()) == 12) {
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

      // 4 - Header ((bytes)0,32,175,48)
      fm.skip(4);

      // 4 - Number of Images
      int numImages = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numImages);

      // 4 - Details Directory Offset (12)
      fm.skip(4);

      ImageResource[] imageResources = new ImageResource[numImages];

      int[] imageOffsets = new int[numImages];
      int[] paletteOffsets = new int[numImages];
      for (int i = 0; i < numImages; i++) {
        // 4 - Image Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);
        imageOffsets[i] = offset;

        // 4 - Palette Offset (0 = no palette)
        offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);
        paletteOffsets[i] = offset;
      }

      for (int i = 0; i < numImages; i++) {
        int paletteOffset = paletteOffsets[i];
        int imageOffset = imageOffsets[i];

        if (paletteOffset != 0) {
          // Read Palette - not done
        }

        if (imageOffset != 0) {
          fm.relativeSeek(imageOffset);

          // 2 - Image Height
          short height = ShortConverter.changeFormat(fm.readShort());
          FieldValidator.checkHeight(height);

          // 2 - Image Width
          short width = ShortConverter.changeFormat(fm.readShort());
          FieldValidator.checkWidth(width);

          // 4 - Image Format
          int imageFormat = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkRange(imageFormat, 0, 16);

          // 4 - Image Data Offset
          imageOffset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(imageOffset, arcSize);

          // 4 - Wrap S
          // 4 - Wrap T
          // 4 - Min Filter
          // 4 - Mag Filter
          // 4 - LOD Bias
          // 1 - Edge LOD Enable
          // 1 - Min LOD
          // 1 - Max LOD
          // 1 - Unpacked
          fm.relativeSeek(imageOffset);

          int originalHeight = height;

          // X - Image Data
          if (imageFormat == 1) {
            // I8

            // ensure width and height are multiples of 8/4...
            int heightMod = height % 4;
            if (heightMod != 0) {
              height += (4 - heightMod);
            }
            int widthMod = width % 8;
            if (widthMod != 0) {
              width += (8 - widthMod);
            }

            ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
            imageResource = ImageFormatReader.reorderPixelBlocks(imageResource, 8, 4);
            imageResource.setHeight(originalHeight); // to ignore anything outside the 4-pixel block
            imageResources[i] = imageResource;
          }
          else if (imageFormat == 3) {
            // IA8

            // ensure width and height are multiples of 8/4...
            int heightMod = height % 4;
            if (heightMod != 0) {
              height += (4 - heightMod);
            }
            int widthMod = width % 4;
            if (widthMod != 0) {
              width += (4 - widthMod);
            }

            ImageResource imageResource = ImageFormatReader.readL8A8(fm, width, height);
            imageResource = ImageFormatReader.reorderPixelBlocks(imageResource, 4, 4);
            imageResource.setHeight(originalHeight); // to ignore anything outside the 4-pixel block
            imageResources[i] = imageResource;
          }
          else if (imageFormat == 4) {
            // RGB565

            // ensure width and height are multiples of 8/4...
            int heightMod = height % 4;
            if (heightMod != 0) {
              height += (4 - heightMod);
            }
            int widthMod = width % 4;
            if (widthMod != 0) {
              width += (4 - widthMod);
            }

            ImageResource imageResource = ImageFormatReader.readRGB565BigEndian(fm, width, height);
            imageResource = ImageFormatReader.reorderPixelBlocks(imageResource, 4, 4);
            imageResource.setHeight(originalHeight); // to ignore anything outside the 4-pixel block
            imageResources[i] = imageResource;
          }
          else if (imageFormat == 5) {
            // RGB5A3
            ImageResource imageResource = ImageFormatReader.readRGB5A3Wii(fm, width, height);
            imageResource.setHeight(originalHeight); // to ignore anything outside the 4-pixel block
            imageResources[i] = imageResource;
          }
          else if (imageFormat == 6) {
            // RGBA8
            ImageResource imageResource = ImageFormatReader.readRGBA8Wii(fm, width, height);
            imageResource.setHeight(originalHeight); // to ignore anything outside the 4-pixel block
            imageResources[i] = imageResource;
          }
          else if (imageFormat == 14) {
            // CMPR
            ImageResource imageResource = ImageFormatReader.readCMPR(fm, width, height);
            imageResource.setHeight(originalHeight); // to ignore anything outside the 4-pixel block
            imageResources[i] = imageResource;
          }
          else {
            ErrorLogger.log("[Viewer_TPL] Unsupported Image Format: " + imageFormat);
          }
        }
      }

      fm.close();

      if (numImages == 1) {
        // single image
        return imageResources[0];
      }
      else {
        // multiple images
        for (int i = 0; i < numImages - 1; i++) {
          imageResources[i].setNextFrame(imageResources[i + 1]);
        }
        for (int i = 1; i < numImages; i++) {
          imageResources[i].setPreviousFrame(imageResources[i - 1]);
        }
        imageResources[numImages - 1].setNextFrame(imageResources[0]);
        imageResources[0].setPreviousFrame(imageResources[numImages - 1]);

        imageResources[0].setManualFrameTransition(true);
        return imageResources[0];
      }

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