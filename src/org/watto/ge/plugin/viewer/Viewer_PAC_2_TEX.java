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
import org.watto.ge.plugin.archive.Plugin_PAC_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PAC_2_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PAC_2_TEX() {
    super("PAC_2_TEX", "PAC_2_TEX Image");
    setExtensions("pac_tex", "datarc_tex", "dattexarc_tex");

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
      if (plugin instanceof Plugin_PAC_2 || plugin instanceof Plugin_DAT_103 || plugin instanceof Plugin_DAT_104) {
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

      // 2 - Block Type (0)
      int blockType = fm.readShort();
      if (blockType == 0 || blockType == 12336) {
        rating += 5;
      }

      if (blockType == 12336) {
        if (fm.readShort() == 12336) {
          rating += 5;
        }

        fm.skip(20);

        if (fm.readInt() == 64) {
          rating += 5;
        }
      }
      else {

        // 2 - Number of Images
        int numImages = fm.readShort();
        if (numImages == 0) {
          if (fm.readInt() == 1026) {
            rating += 5;
          }
        }
        else {
          if (FieldValidator.checkRange(numImages, 1, 1000)) { // guess max 1000 images
            rating += 5;
          }

          long arcSize = fm.getLength();

          // 4 - Pixel Data Offset (relative to the start of the Image Data block) (128)
          if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
            rating += 5;
          }

          // 4 - Color Palette Offset (relative to the start of the Image Data block)
          if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
            rating += 5;
          }
        }
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
  
  NOTE THAT THIS METHOD IS A LONG ONE INSTEAD OF SHORTCUTTING TO READTHUMBNAIL(). THIS IS SO WE
  CAN LOAD ALL FRAMES FOR PREVIEWS, BUT ONLY 1 FRAME AND THE PALETTE FOR THUMBNAILS (and in order)
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 2 - Block Type (0)
      int blockType = fm.readShort();

      int numImages = 0;

      boolean sacredBlaze = false;
      if (blockType == 12336) {
        // image type from Sacred Blaze
        sacredBlaze = true;

        // 4 - Unknown (all byte 48)
        // 4 - Unknown (1026)
        // 4 - Unknown
        // 8 - null
        fm.skip(18); // note 18, not 20, because we already read 2 bytes for the blockTypye check

        // 4 - Unknown (1)
        numImages = fm.readInt();
        FieldValidator.checkNumFiles(numImages);
      }
      else {
        // 2 - Number of Images
        numImages = fm.readShort();

        if (numImages == 0) {
          // image type from Sacred Blaze
          sacredBlaze = true;

          // 4 - Unknown (1026)
          // 4 - Unknown
          // 8 - null
          fm.skip(16);

          // 4 - Unknown (1)
          numImages = fm.readInt();
          FieldValidator.checkNumFiles(numImages);
        }
        else {
          FieldValidator.checkNumFiles(numImages);
        }
      }

      // 4 - Pixel Data Offset (relative to the start of the Image Data block) (128)
      int pixelOffset = fm.readInt();
      FieldValidator.checkOffset(pixelOffset, arcSize);

      // 4 - Color Palette Offset (relative to the start of the Image Data block)
      int paletteOffset = fm.readInt();
      FieldValidator.checkOffset(paletteOffset, arcSize);

      // read the palette first
      fm.seek(paletteOffset);

      // 4 - Unknown (1)
      // 12 - null
      fm.skip(16);

      // 4 - Unknown (16)
      int paletteType = fm.readShort();

      // 4 - Unknown (16)
      // 4 - Unknown (128)
      // 4 - Unknown (64)
      // 4 - Unknown (64)
      // 12 - null
      // 4 - Unknown
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
      fm.skip(110);

      // for each color
      //   4 - RGBA
      int[] palette = null;
      if (paletteType == 8) {
        // 4-bit
        palette = ImageFormatReader.readPaletteRGBA(fm, 16);
      }
      else {
        // 8-bit
        palette = ImageFormatReader.readPaletteRGBA(fm, 256);
        palette = ImageFormatReader.stripePalettePS2(palette); // Palette is PS2-striped
      }

      // now read the pixels for each image
      fm.relativeSeek(pixelOffset);

      // 4 - Number of Images
      numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      // 12 - null
      fm.skip(12);

      int[] widths = new int[numImages];
      int[] heights = new int[numImages];
      long[] offsets = new long[numImages];
      for (int i = 0; i < numImages; i++) {
        if (sacredBlaze) {
          // 2 - Image Width
          short width = fm.readShort();
          FieldValidator.checkWidth(width);
          widths[i] = width;

          // 2 - Image Height
          short height = fm.readShort();
          FieldValidator.checkHeight(height);
          heights[i] = height;

          // 2 - Unknown (256)
          // 2 - Unknown (256)
          fm.skip(4);

          // 4 - Image Data Offset (relative to the start of the Image Data section) (192)
          long offset = fm.readInt() + pixelOffset;
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - Unknown
          // 4 - Unknown
          // 12 - null
          // 4 - Unknown
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
        else {
          // 4 - Image Width
          int width = fm.readInt();
          FieldValidator.checkWidth(width);
          widths[i] = width;

          // 4 - Image Height
          int height = fm.readInt();
          FieldValidator.checkHeight(height);
          heights[i] = height;

          // 4 - Pixel Data Offset (relative to the start of the Pixel Data block)
          long offset = fm.readInt() + pixelOffset;
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - Unknown
          // 4 - Unknown
          // 12 - null
          // 4 - Unknown
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
      }

      ImageResource[] images = new ImageResource[numImages];
      for (int i = 0; i < numImages; i++) {
        int width = widths[i];
        int height = heights[i];
        long offset = offsets[i];

        fm.relativeSeek(offset);

        ImageResource imageResource = null;

        if (paletteType == 8) {
          // 4-bit
          imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        }
        else {
          // 8-bit
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        }

        images[i] = imageResource;
      }

      // set the frames
      if (numImages > 1) {
        for (int i = 0; i < numImages; i++) {
          if (i == 0) {
            images[i].setNextFrame(images[i + 1]);
            images[i].setPreviousFrame(images[numImages - 1]);
          }
          else if (i == numImages - 1) {
            images[i].setNextFrame(images[0]);
            images[i].setPreviousFrame(images[i - 1]);
          }
          else {
            images[i].setNextFrame(images[i + 1]);
            images[i].setPreviousFrame(images[i - 1]);
          }
        }

        // not an animation
        images[0].setManualFrameTransition(true);
      }

      fm.close();

      ImageResource imageResource = images[0];

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

      // 2 - Block Type (0)
      int blockType = fm.readShort();

      int numImages = 0;

      boolean sacredBlaze = false;
      if (blockType == 12336) {
        // image type from Sacred Blaze
        sacredBlaze = true;

        // 4 - Unknown (all byte 48)
        // 4 - Unknown (1026)
        // 4 - Unknown
        // 8 - null
        fm.skip(18); // note 18, not 20, because we already read 2 bytes for the blockTypye check

        // 4 - Unknown (1)
        numImages = fm.readInt();
        FieldValidator.checkNumFiles(numImages);
      }
      else {
        // 2 - Number of Images
        numImages = fm.readShort();

        if (numImages == 0) {
          // image type from Sacred Blaze
          sacredBlaze = true;

          // 4 - Unknown (1026)
          // 4 - Unknown
          // 8 - null
          fm.skip(16);

          // 4 - Unknown (1)
          numImages = fm.readInt();
          FieldValidator.checkNumFiles(numImages);
        }
        else {
          FieldValidator.checkNumFiles(numImages);
        }
      }

      // 4 - Pixel Data Offset (relative to the start of the Image Data block) (128)
      int pixelOffset = fm.readInt();
      FieldValidator.checkOffset(pixelOffset, arcSize);

      // 4 - Color Palette Offset (relative to the start of the Image Data block)
      int paletteOffset = fm.readInt();
      FieldValidator.checkOffset(paletteOffset, arcSize);

      // read the pixels for each image
      fm.skip(pixelOffset - fm.getOffset());

      // 4 - Number of Images
      numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      // 12 - null
      fm.skip(12);

      int[] widths = new int[numImages];
      int[] heights = new int[numImages];
      long[] offsets = new long[numImages];
      for (int i = 0; i < numImages; i++) {
        if (sacredBlaze) {
          // 2 - Image Width
          short width = fm.readShort();
          FieldValidator.checkWidth(width);
          widths[i] = width;

          // 2 - Image Height
          short height = fm.readShort();
          FieldValidator.checkHeight(height);
          heights[i] = height;

          // 2 - Unknown (256)
          // 2 - Unknown (256)
          fm.skip(4);

          // 4 - Image Data Offset (relative to the start of the Image Data section) (192)
          long offset = fm.readInt() + pixelOffset;
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - Unknown
          // 4 - Unknown
          // 12 - null
          // 4 - Unknown
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
        else {
          // 4 - Image Width
          int width = fm.readInt();
          FieldValidator.checkWidth(width);
          widths[i] = width;

          // 4 - Image Height
          int height = fm.readInt();
          FieldValidator.checkHeight(height);
          heights[i] = height;

          // 4 - Pixel Data Offset (relative to the start of the Pixel Data block)
          long offset = fm.readInt() + pixelOffset;
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - Unknown
          // 4 - Unknown
          // 12 - null
          // 4 - Unknown
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
      }

      // read image 1 only
      fm.skip(offsets[0] - fm.getOffset());

      int numPixels = widths[0] * heights[0];
      if (fm.getOffset() + numPixels > paletteOffset) {
        // probably a 4-bit image (rather than 8-bit), lets just set it to the max it can be, and go with that.
        numPixels = (int) (paletteOffset - fm.getOffset());
      }

      byte[] pixelBytes = fm.readBytes(numPixels);

      // now read the palette (which is after the pixels)
      fm.skip(paletteOffset - fm.getOffset());

      // 4 - Unknown (1)
      // 12 - null
      fm.skip(16);

      // 4 - Unknown (16)
      int paletteType = fm.readShort();

      // 4 - Unknown (16)
      // 4 - Unknown (128)
      // 4 - Unknown (64)
      // 4 - Unknown (64)
      // 12 - null
      // 4 - Unknown
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
      fm.skip(110);

      // for each color
      //   4 - RGBA
      int[] palette = null;
      if (paletteType == 8) {
        // 4-bit
        palette = ImageFormatReader.readPaletteRGBA(fm, 16);
      }
      else {
        // 8-bit
        palette = ImageFormatReader.readPaletteRGBA(fm, 256);
        palette = ImageFormatReader.stripePalettePS2(palette); // Palette is PS2-striped
      }

      // now build the image
      fm.close();
      fm = new FileManipulator(new ByteBuffer(pixelBytes));

      ImageResource imageResource = null;

      if (paletteType == 8) {
        // 4-bit
        imageResource = ImageFormatReader.read4BitPaletted(fm, widths[0], heights[0], palette);
      }
      else {
        // 8-bit
        imageResource = ImageFormatReader.read8BitPaletted(fm, widths[0], heights[0], palette);
      }

      //ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, widths[0], heights[0], palette);

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