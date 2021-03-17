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
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_GSC_NU20_2;
import org.watto.ge.plugin.archive.Plugin_HGO_FOGH;
import org.watto.ge.plugin.archive.Plugin_HGO_HGOF;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_HGO_HGOF_TXM0_TXM0 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_HGO_HGOF_TXM0_TXM0() {
    super("HGO_HGOF_TXM0_TXM0", "HGO_HGOF_TXM0_TXM0 Image");
    setExtensions("txm0");

    setGames("Crash Bandicoot: The Wrath of Cortex");
    setPlatforms("xbox", "ps2");
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
      if (plugin instanceof Plugin_HGO_HGOF || plugin instanceof Plugin_HGO_FOGH || plugin instanceof Plugin_GSC_NU20_2) {
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

      // 4 - Header (TXM0)
      String header = fm.readString(4);
      if (header.equals("TXM0") || header.equals("0MXT")) {
        rating += 50;
      }
      else {
        return rating; // exit early for PS2
      }

      // 4 - Block Length (including these 2 header fields)
      // 4 - Image Format (5=Paletted)
      fm.skip(8);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
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

      long startOffset = fm.getOffset();

      // 4 - Header (TXM0)
      byte[] headerBytes = fm.readBytes(2);
      String header = StringConverter.convertLittle(headerBytes);
      if (header.equals("TX") || header.equals("0M")) {
        // 
        // XBox or Gamecube
        //

        // 4 - Block Length (including these 2 header fields)
        fm.skip(6);

        // 4 - Image Format (5=Paletted)
        int imageFormat = fm.readInt();

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Image Data Length
        fm.skip(4);

        ImageResource imageResource = null;

        if (imageFormat == 5) {
          // Paletted

          int pixelCount = width * height;

          // X - Pixels
          byte[] rawPixels = fm.readBytes(pixelCount);
          int[] pixels = new int[pixelCount];

          // X - Palette (256 colors)
          int[] palette = ImageFormatReader.readPaletteABGR(fm, 256);

          for (int i = 0; i < pixelCount; i++) {
            pixels[i] = palette[ByteConverter.unsign(rawPixels[i])];
          }

          imageResource = new ImageResource(pixels, width, height);
        }

        fm.close();

        return imageResource;
      }
      else {
        //
        // PS2
        //

        long arcSize = fm.getLength();

        int width = ShortConverter.convertLittle(headerBytes);

        if (width == 0) {
          fm.skip(6);
          width = fm.readShort();
          startOffset += 8;
        }

        FieldValidator.checkWidth(width);

        // 2 - Unknown (1)
        fm.skip(2);

        // 2 - Image Height
        int height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Unknown
        // 4 - Unknown
        fm.skip(6);

        // 4 - Unknown
        int formatFlag = fm.readByte();
        fm.skip(1);
        int swizzleFlag = fm.readByte();
        fm.skip(1);

        if (formatFlag == 3) {
          // RGBA

          // 128 - Unknown
          fm.skip(128);

          // X - Image Data (RGBA)
          ImageResource imageResource = ImageFormatReader.readRGBA(fm, width, height);
          fm.close();

          return imageResource;
        }
        else if (formatFlag == 2) {
          // RGB

          // 128 - Unknown
          fm.skip(128);

          // X - Image Data (RGB)
          ImageResource imageResource = ImageFormatReader.readRGB(fm, width, height);
          fm.close();

          return imageResource;
        }

        // 4 - Pixel Header Offset (relative to the start of this Image Data) [+16]
        long pixelOffset = fm.readInt() + startOffset + 16;
        FieldValidator.checkOffset(pixelOffset, arcSize);

        // 108 - Unknown
        fm.skip(108);

        // X - Palette
        int colorCount = (int) ((pixelOffset - fm.getOffset()) / 4);
        if (colorCount > 256) {
          colorCount = 256;
        }
        int[] palette = new int[colorCount];

        for (int i = 0; i < colorCount; i++) {
          // Alpha is -127 to 127
          int color = ((ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte()));

          int alpha = fm.readByte();
          if (alpha < 0) {
            alpha = 1 - alpha;
          }

          alpha = (alpha * 2) + 1; // to make 127 == full alpha
          if (alpha > 255) {
            alpha = 255;
          }
          else if (alpha == 1) {
            alpha = 0;
          }

          color |= (alpha << 24);

          palette[i] = color;
        }

        if (colorCount < 256) {
          // use a Greyscale palette
          palette = PaletteGenerator.getGrayscale();
        }

        // X - Padding (optional)
        int paddingLength = (int) (pixelOffset - fm.getOffset());
        FieldValidator.checkLength(paddingLength, arcSize);
        fm.skip(paddingLength);

        // 4 - Image Data Length (including padding or something)
        // 4 - Image Data Length
        // 184 - Unknown
        fm.skip(192);

        // X - Image Data (Paletted Indexes) (PS2 Swizzling)
        ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        if (imageResource != null) {
          //if (width != 64 && width != 256) {
          if (swizzleFlag == 0) {
            imageResource.setPixels(ImageFormatReader.unswizzlePS2(imageResource.getPixels(), width, height));
          }
        }

        fm.close();

        return imageResource;

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