/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_103;
import org.watto.ge.plugin.archive.Plugin_DAT_104;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_103_DATTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_103_DATTEX() {
    super("DAT_103_DATTEX", "DAT_103_DATTEX Image");
    setExtensions("dat_tex");

    setGames("Summon Night 3");
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
      if (plugin instanceof Plugin_DAT_103 || plugin instanceof Plugin_DAT_104) {
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

      short footerFlag = fm.readShort();
      if (footerFlag == 0 || footerFlag == 1 || footerFlag == 3) {
        rating += 5;
      }

      fm.skip(2);

      if (FieldValidator.checkOffset(fm.readInt(), fm.getLength())) {
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
  
  **********************************************************************************************
  **/

  public PreviewPanel readWrapper(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 12 - null
      fm.skip(24);

      //
      //
      // READ IMAGE 1
      //
      //

      int relativeOffset = 32;

      // 2 - Footer Flag? (0=No Footer, 1=Has Footer)
      // 2 - Number of Images (often just 1)
      fm.skip(4);

      // 4 - Image Data Offset (128)
      int dataOffset = fm.readInt() + relativeOffset;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Color Palette Offset
      int paletteOffset = fm.readInt() + relativeOffset;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Footer Offset
      // 112 - null Padding to a multiple of 128 bytes
      fm.seek(dataOffset);

      // 4 - Number of Images (often just 1)
      int numImages = fm.readInt();
      FieldValidator.checkRange(numImages, 1, 200); //guess

      // 12 - null
      fm.skip(12);

      int[] offsets = new int[numImages];
      int[] widths = new int[numImages];
      int[] heights = new int[numImages];

      for (int i = 0; i < numImages; i++) {
        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);
        widths[i] = width;

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);
        heights[i] = height;

        // 4 - Image Data Offset (relative to the start of the Image Data block) (160 if there is only 1 image)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (17920)
        // 4 - Unknown (17920)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Image Width
        // 4 - Image Height
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (14)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown (6)
        // 4 - Unknown (6)
        // 4 - null
        fm.skip(132);
      }

      fm.seek(paletteOffset);

      // 4 - Number of Palettes (1)
      int numPalettes = fm.readInt();

      // 12 - null
      fm.skip(12);

      long[] palOffsets = new long[numPalettes];

      for (int i = 0; i < numPalettes; i++) {
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        fm.skip(8);

        // 4 - Color Palette Data Offset (relative to the start of the Color Palette block) (128)
        int palOffset = fm.readInt() + paletteOffset;
        FieldValidator.checkOffset(palOffset, arcSize);
        palOffsets[i] = palOffset;

        // 4 - Unknown (64)
        // 4 - Unknown (64)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        fm.skip(100);
      }

      fm.seek(palOffsets[0]);

      // X - Palette (RGBA)
      int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
      palette = ImageFormatReader.stripePalettePS2(palette); // Palette is PS2-striped
      palette = ImageFormatReader.doubleAlpha(palette);

      ImageResource[] firstImageResources = new ImageResource[numImages];

      for (int i = 0; i < numImages; i++) {
        fm.seek(offsets[i]);

        int width = widths[i];
        int height = heights[i];

        firstImageResources[i] = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }

      //
      //
      // READ MIDDLE BLOCK
      //
      //

      // first, go to the end of the last palette, and then skip the footer for that image
      fm.seek(palOffsets[numPalettes - 1]);
      fm.skip(1024); // palette length
      fm.skip(2052); // footer length
      fm.skip(12); // 12 - null Padding to a multiple of 16? bytes

      // now read the middle block
      // 4 - Unknown (1)
      // 4 - Offset 1 (16)
      // 4 - Offset 2
      fm.skip(12);

      // 4 - Offset 3
      int middleLength = fm.readInt();

      // X - Unknown Data to Offset 3
      fm.skip(middleLength - 16); // -16 because we've already read 4 fields

      relativeOffset = (int) fm.getOffset();

      // 20/24 - null Padding to a multiple of 32? bytes
      fm.skip(ArchivePlugin.calculatePadding(relativeOffset, 32));

      //
      //
      // READ IMAGE 2
      //
      //
      relativeOffset = (int) fm.getOffset();

      // 2 - Footer Flag? (0=No Footer, 1=Has Footer)
      // 2 - Number of Images (often just 1)
      fm.skip(4);

      // 4 - Image Data Offset (128)
      dataOffset = fm.readInt() + relativeOffset;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Color Palette Offset
      paletteOffset = fm.readInt() + relativeOffset;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Footer Offset
      // 112 - null Padding to a multiple of 128 bytes
      fm.seek(dataOffset);

      // 4 - Number of Images (often just 1)
      numImages = fm.readInt();
      FieldValidator.checkRange(numImages, 1, 200); //guess

      // 12 - null
      fm.skip(12);

      offsets = new int[numImages];
      widths = new int[numImages];
      heights = new int[numImages];

      for (int i = 0; i < numImages; i++) {
        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);
        widths[i] = width;

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);
        heights[i] = height;

        // 4 - Image Data Offset (relative to the start of the Image Data block) (160 if there is only 1 image)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (17920)
        // 4 - Unknown (17920)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Image Width
        // 4 - Image Height
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (14)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown (6)
        // 4 - Unknown (6)
        // 4 - null
        fm.skip(132);
      }

      fm.seek(paletteOffset);

      // 4 - Number of Palettes (1)
      numPalettes = fm.readInt();

      // 12 - null
      fm.skip(12);

      palOffsets = new long[numPalettes];

      for (int i = 0; i < numPalettes; i++) {
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        fm.skip(8);

        // 4 - Color Palette Data Offset (relative to the start of the Color Palette block) (128)
        int palOffset = fm.readInt() + paletteOffset;
        FieldValidator.checkOffset(palOffset, arcSize);
        palOffsets[i] = palOffset;

        // 4 - Unknown (64)
        // 4 - Unknown (64)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        fm.skip(100);
      }

      fm.seek(palOffsets[0]);

      // X - Palette (RGBA)
      palette = ImageFormatReader.readPaletteRGBA(fm, 256);
      palette = ImageFormatReader.stripePalettePS2(palette); // Palette is PS2-striped
      palette = ImageFormatReader.doubleAlpha(palette);

      ImageResource[] secondImageResources = new ImageResource[numImages];

      for (int i = 0; i < numImages; i++) {
        fm.seek(offsets[i]);

        int width = widths[i];
        int height = heights[i];

        secondImageResources[i] = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }

      fm.close();

      // now add all the images to the same array and set the next/previous
      int numFirstImages = firstImageResources.length;
      int numSecondImages = secondImageResources.length;
      numImages = numFirstImages + numSecondImages;

      ImageResource[] imageResources = new ImageResource[numImages];
      System.arraycopy(firstImageResources, 0, imageResources, 0, numFirstImages);
      System.arraycopy(secondImageResources, 0, imageResources, numFirstImages, numSecondImages);

      ImageResource imageResource = imageResources[0];
      if (numImages == 1) {
        // already chose image 1
      }
      else {
        // set the next/previous images
        for (int i = 0; i < numImages; i++) {
          if (i == 0) {
            imageResources[i].setNextFrame(imageResources[i + 1]);
            imageResources[i].setPreviousFrame(imageResources[numImages - 1]);
          }
          else if (i == numImages - 1) {
            imageResources[i].setNextFrame(imageResources[0]);
            imageResources[i].setPreviousFrame(imageResources[i - 1]);
          }
          else {
            imageResources[i].setNextFrame(imageResources[i + 1]);
            imageResources[i].setPreviousFrame(imageResources[i - 1]);
          }
        }

        imageResource.setManualFrameTransition(true);
      }

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  
  NOTE THAT THIS METHOD IS A LONG ONE INSTEAD OF SHORTCUTTING TO READTHUMBNAIL(). THIS IS SO WE
  CAN LOAD ALL FRAMES FOR PREVIEWS, BUT ONLY 1 FRAME AND THE PALETTE FOR THUMBNAILS (and in order)
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 2 - Footer Flag? (0=No Footer, 1=Has Footer)
      // 2 - Number of Images (often just 1)
      int fileType = fm.readInt();

      // 4 - Image Data Offset (128)
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      if (fileType == 3 && dataOffset == 4) {
        return readWrapper(fm);
      }

      // 4 - Color Palette Offset
      int paletteOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Footer Offset
      // 112 - null Padding to a multiple of 128 bytes
      fm.seek(dataOffset);

      // 4 - Number of Images (often just 1)
      int numImages = fm.readInt();
      FieldValidator.checkRange(numImages, 1, 200); //guess

      // 12 - null
      fm.skip(12);

      int[] offsets = new int[numImages];
      int[] widths = new int[numImages];
      int[] heights = new int[numImages];

      for (int i = 0; i < numImages; i++) {
        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);
        widths[i] = width;

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);
        heights[i] = height;

        // 4 - Image Data Offset (relative to the start of the Image Data block) (160 if there is only 1 image)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (17920)
        // 4 - Unknown (17920)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Image Width
        // 4 - Image Height
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (14)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown (6)
        // 4 - Unknown (6)
        // 4 - null
        fm.skip(132);
      }

      fm.seek(paletteOffset);

      // 4 - Number of Palettes (1)
      int numPalettes = fm.readInt();

      // 12 - null
      fm.skip(12);

      long[] palOffsets = new long[numPalettes];

      for (int i = 0; i < numPalettes; i++) {
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        fm.skip(8);

        // 4 - Color Palette Data Offset (relative to the start of the Color Palette block) (128)
        int palOffset = fm.readInt() + paletteOffset;
        FieldValidator.checkOffset(palOffset, arcSize);
        palOffsets[i] = palOffset;

        // 4 - Unknown (64)
        // 4 - Unknown (64)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        fm.skip(100);
      }

      fm.seek(palOffsets[0]);

      // X - Palette (RGBA)
      int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
      palette = ImageFormatReader.stripePalettePS2(palette); // Palette is PS2-striped
      palette = ImageFormatReader.doubleAlpha(palette);

      ImageResource[] imageResources = new ImageResource[numImages];

      for (int i = 0; i < numImages; i++) {
        fm.seek(offsets[i]);

        int width = widths[i];
        int height = heights[i];

        imageResources[i] = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }

      fm.close();

      ImageResource imageResource = imageResources[0];
      if (numImages == 1) {
        // already chose image 1
      }
      else {
        // set the next/previous images
        for (int i = 0; i < numImages; i++) {
          if (i == 0) {
            imageResources[i].setNextFrame(imageResources[i + 1]);
            imageResources[i].setPreviousFrame(imageResources[numImages - 1]);
          }
          else if (i == numImages - 1) {
            imageResources[i].setNextFrame(imageResources[0]);
            imageResources[i].setPreviousFrame(imageResources[i - 1]);
          }
          else {
            imageResources[i].setNextFrame(imageResources[i + 1]);
            imageResources[i].setPreviousFrame(imageResources[i - 1]);
          }
        }

        imageResource.setManualFrameTransition(true);
      }

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
  
  NOTE THAT THIS METHOD IS ONLY FOR THUMBNAILS - IT'LL ONLY READ THE FIRST IMAGE AND THE PALETTE
  (in that order, which is important for thumbnail generation)
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 2 - Footer Flag? (0=No Footer, 1=Has Footer)
      // 2 - Number of Images (often just 1)
      int fileType = fm.readInt();

      // 4 - Image Data Offset (128)
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      int relativeOffset = 0;
      if (fileType == 3 && dataOffset == 4) {
        relativeOffset = 32;
        fm.seek(relativeOffset);

        // 2 - Footer Flag? (0=No Footer, 1=Has Footer)
        // 2 - Number of Images (often just 1)
        fm.skip(4);

        // 4 - Image Data Offset (128)
        dataOffset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(dataOffset, arcSize);
      }

      // 4 - Color Palette Offset
      int paletteOffset = fm.readInt() + relativeOffset;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Footer Offset
      // 112 - null Padding to a multiple of 128 bytes
      fm.seek(dataOffset);

      // 4 - Number of Images (often just 1)
      int numImages = fm.readInt();
      FieldValidator.checkRange(numImages, 1, 200); //guess

      // 12 - null
      fm.skip(12);

      int[] offsets = new int[numImages];
      int[] widths = new int[numImages];
      int[] heights = new int[numImages];

      for (int i = 0; i < numImages; i++) {
        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);
        widths[i] = width;

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);
        heights[i] = height;

        // 4 - Image Data Offset (relative to the start of the Image Data block) (160 if there is only 1 image)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (17920)
        // 4 - Unknown (17920)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Image Width
        // 4 - Image Height
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (14)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown (6)
        // 4 - Unknown (6)
        // 4 - null
        fm.skip(132);
      }

      // read the image data for image #1
      fm.seek(offsets[0]);
      int numPixels = widths[0] * heights[0];
      byte[] imageBytes = fm.readBytes(numPixels);

      fm.seek(paletteOffset);

      // 4 - Number of Palettes (1)
      int numPalettes = fm.readInt();

      // 12 - null
      fm.skip(12);

      long[] palOffsets = new long[numPalettes];

      for (int i = 0; i < numPalettes; i++) {
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        fm.skip(8);

        // 4 - Color Palette Data Offset (relative to the start of the Color Palette block) (128)
        int palOffset = fm.readInt() + paletteOffset;
        FieldValidator.checkOffset(palOffset, arcSize);
        palOffsets[i] = palOffset;

        // 4 - Unknown (64)
        // 4 - Unknown (64)
        // 12 - null
        // 4 - Unknown (32772)
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Unknown (16)
        // 4 - Unknown (16)
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        fm.skip(100);
      }

      fm.seek(palOffsets[0]);

      // X - Palette (RGBA)
      int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
      palette = ImageFormatReader.stripePalettePS2(palette); // Palette is PS2-striped
      palette = ImageFormatReader.doubleAlpha(palette);

      fm.close();
      fm = new FileManipulator(new ByteBuffer(imageBytes));

      // read image 1 only
      int width = widths[0];
      int height = heights[0];

      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);

      return imageResource;

    }
    catch (

    Throwable t) {
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