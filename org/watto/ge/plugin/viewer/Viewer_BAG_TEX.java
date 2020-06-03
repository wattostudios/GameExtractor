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

import java.io.File;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BAG;
import org.watto.ge.plugin.archive.Plugin_BAG_6;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BAG_TEX extends ViewerPlugin {

  String imageType = null;
  boolean extraPadding = false;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_BAG_TEX() {
    super("BAG_TEX", "Brian Lara International Cricket 2005 TEX Image");
    setExtensions("tex");

    setGames("Brian Lara International Cricket 2005");
    setPlatforms("PS2");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  If this plugin supports the conversion of files into a different format when "replacing", eg
  converting a PNG image into a propriety image format, this is where we do it.
  @param resourceBeingReplaced the Resource in the archive that is being replaced
  @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
         one that will be converted into a different format, if applicable.
  @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
  **********************************************************************************************
  **/
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    return fileToReplaceWith;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public String getImageType() {
    return imageType;
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
      if (plugin instanceof Plugin_BAG || plugin instanceof Plugin_BAG_6) {
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
  public boolean hasExtraPadding() {
    return extraPadding;
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

      // 2 - Image Width
      int imageWidth = fm.readShort();

      // 2 - Image Height
      int imageHeight = fm.readShort();

      // 2 - Unknown (5)
      fm.skip(2);

      // 2 - Number Of Mipmaps (4)
      int numMipmaps = fm.readShort();

      // 8 - Unknown
      fm.skip(8);

      int imageDataOffset = 16;

      boolean stripAlpha = false;

      if (imageWidth == 21328 && imageHeight == 50) {
        // PS2 Header
        imageDataOffset = 112;
        stripAlpha = true;

        // 16 - Stuff
        //fm.skip(16); // already skipped from all the reads above

        // 4 - position (1=bowler, 2=fielder, 3=batsman...)
        fm.skip(4);

        // 64 - filename
        fm.skip(64);

        // 4 - Image Width
        imageWidth = fm.readInt();

        boolean extraPadding = false;
        if (imageWidth == 0) {
          extraPadding = true;
          // 88 - stuff
          fm.skip(88);

          // 4 - read the new/correct Image Width
          imageWidth = fm.readInt();

          imageDataOffset += 92; // 88 + the 4 bytes read for the original imageWidth

          if (imageWidth == 0) {
            // 92 - stuff
            fm.skip(92);

            // 4 - read the new/correct Image Width
            imageWidth = fm.readInt();

            imageDataOffset += 96; // 92 + the 4 bytes read for the original imageWidth
          }
        }

        // 4 - Image Height
        imageHeight = fm.readInt();

        // 4 - Number Of Mipmaps (4)
        numMipmaps = fm.readInt();

        // 4 - Unknown (5)
        fm.skip(4);

        // 12 - null Padding
        fm.skip(12);

        if (extraPadding) {
          // 4 - null
          fm.skip(4);
          imageDataOffset += 4;
        }

      }

      int pixelLength = 0;
      int interWidth = imageWidth;
      int interHeight = imageHeight;
      for (int i = 0; i < numMipmaps; i++) {
        pixelLength += (interWidth * interHeight);
        interWidth /= 2;
        interHeight /= 2;
      }

      // Skip to the color palette
      fm.skip(pixelLength);

      int numColors = 256;
      int[] palette = new int[numColors];

      // read the color palette
      for (int i = 0; i < numColors; i++) {
        // INPUT = RGBA
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = ByteConverter.unsign(fm.readByte()) * 2;

        if (stripAlpha) {
          aPixel = 255;
        }

        // OUTPUT = ARGB
        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
      }

      // Apply the PS2 palette striping
      palette = ImageFormatReader.unstripePalettePS2(palette);

      // skip back to the first mipmap
      fm.seek(imageDataOffset);

      int pixelCount = imageWidth * imageHeight;
      byte[] data = fm.readBytes(pixelCount);
      data = ImageFormatReader.unswizzlePS2(data, imageWidth, imageHeight);

      int[] pixels = new int[pixelCount];

      for (int p = 0; p < pixelCount; p++) {
        pixels[p] = palette[ByteConverter.unsign(data[p])];
      }

      ImageResource imageResource = new ImageResource(pixels, imageWidth, imageHeight);

      imageResource.addProperty("MipmapCount", "" + numMipmaps);

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
  public void setExtraPadding(boolean extraPadding) {
    this.extraPadding = extraPadding;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setImageType(String imageType) {
    this.imageType = imageType;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      // Paletted image, 256 colors
      im.convertToPaletted();
      im.changeColorCount(256);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Generate all the PALETTED mipmaps of the image
      ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
      int mipmapCount = mipmaps.length;

      // for these TEX images, the smallest mipmap has smallest dimension = 16
      for (int i = 0; i < mipmapCount; i++) {
        ImageManipulator mipmap = mipmaps[i];
        if (mipmap.getWidth() < 16 || mipmap.getHeight() < 16) {
          // this mipmap is too small, so the smallest mipmap we want is the previous one
          mipmapCount = i;
          break;
        }
      }

      // Try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
      }

      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      if (imageType != null && imageType.equals("PS2")) {
        // 4 - Image Width
        fm.writeInt(imageWidth);

        // 4 - Image Height
        fm.writeInt(imageHeight);

        // 4 - Number Of Mipmaps (4)
        fm.writeInt(mipmapCount);

        // 4 - Unknown (5)
        fm.writeInt(5);

        // 12 - null Padding
        for (int i = 0; i < 12; i++) {
          fm.writeByte(0);
        }

        if (extraPadding) {
          // 4 - null
          fm.writeInt(0);
        }

      }
      else {
        // 2 - Image Width
        fm.writeShort((short) imageWidth);

        // 2 - Image Height
        fm.writeShort((short) imageHeight);

        // 2 - Unknown (5)
        fm.writeShort((short) 5);

        // 2 - Number Of Mipmaps (4)
        fm.writeShort((short) mipmapCount);

        // 8 - Unknown
        for (int i = 0; i < 8; i++) {
          fm.writeByte(251);
        }
      }

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageManipulator mipmap = mipmaps[i];
        int[] pixels = mipmap.getPixels();

        int mipmapWidth = mipmap.getWidth();
        int mipmapHeight = mipmap.getHeight();

        pixels = ImageFormatReader.swizzlePS2(pixels, mipmapWidth, mipmapHeight);

        // X - Pixels
        int pixelCount = mipmap.getNumPixels();
        for (int p = 0; p < pixelCount; p++) {
          // 1 - Color Palette Index
          fm.writeByte((byte) pixels[p]);
        }
      }

      // X - Color Palette
      int[] palette = im.getPalette();

      palette = ImageFormatReader.stripePalettePS2(palette);

      int numColors = palette.length;
      for (int i = 0; i < numColors; i++) {
        int color = palette[i];

        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        fm.writeByte((byte) ((color & 0x00FF0000) >> 16));
        fm.writeByte((byte) ((color & 0x0000FF00) >> 8));
        fm.writeByte((byte) ((color & 0x000000FF)));

        int alpha = ByteConverter.unsign((byte) ((color & 0xFF000000) >> 24));
        if (alpha != 255) {
          alpha /= 2;
        }
        fm.writeByte((byte) alpha);
      }

      //fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}