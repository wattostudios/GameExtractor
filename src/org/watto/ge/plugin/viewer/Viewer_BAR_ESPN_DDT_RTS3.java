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
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BAR;
import org.watto.ge.plugin.archive.Plugin_BAR_ESPN;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BAR_ESPN_DDT_RTS3 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BAR_ESPN_DDT_RTS3() {
    super("BAR_ESPN_DDT_RTS3", "Age Of Empires 3 DDT Image");
    setExtensions("ddt");

    setGames("Age Of Empires 3");
    setPlatforms("PC");
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
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_BAR_ESPN || plugin instanceof Plugin_BAR) {
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
      if (fm.readString(4).equals("RTS3")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(4);

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
  
  Ref: https://github.com/Weesals/ModHQ/blob/master/RTS4.ModHQ/Ext/RTS4.Data/DDTImage.cs#L53
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (RTS3)
      // 1 - Usage
      fm.skip(5);

      // 1 - Palette Format (0 = RGB565, 1=RGB555, 2=ARGB4444)
      int paletteFormat = fm.readByte();

      // 1 - Image Format (Uncompressed32 = 1, Palette = 3, DXT1 = 4, DXT3 = 5, DXT5 = 6, Grayscale8 = 7)
      int imageFormat = fm.readByte();

      // 1 - Number Of MipMaps
      int mipmapCount = fm.readByte();

      // 4 - Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      int numColors = 0;
      int paletteOffset = 0;
      if (imageFormat == 3) {
        // 4 - Number of Colors
        numColors = fm.readInt();
        FieldValidator.checkNumColors(numColors);

        // 16 - null
        fm.skip(16);

        // 4 - Palette Offset
        paletteOffset = fm.readInt();
        FieldValidator.checkOffset(paletteOffset, arcSize);
      }

      // 4 - Image Data Offset
      int imageOffset = fm.readInt();
      FieldValidator.checkOffset(imageOffset, arcSize);

      // 4 - Image Data Length
      int imageLength = fm.readInt();
      FieldValidator.checkLength(imageLength, arcSize);

      // read the color palette
      int[] palette = null;
      if (imageFormat == 3) {
        fm.relativeSeek(paletteOffset);

        if (paletteFormat == 0) {
          palette = ImageFormatReader.readRGB565(fm, 1, numColors).getImagePixels();
        }
        else if (paletteFormat == 1) {
          palette = ImageFormatReader.readRGB555(fm, 1, numColors).getImagePixels();
        }
        else if (paletteFormat == 4) {
          palette = ImageFormatReader.readARGB4444(fm, 1, numColors).getImagePixels();
        }
        else {
          ErrorLogger.log("[Viewer_BAR_ESPN_DDT_RTS3] Unknown palette format: " + paletteFormat);
          return null;
        }
      }

      fm.relativeSeek(imageOffset);

      // X - Pixels (Uncompressed32 = 1, Palette = 3, DXT1 = 4, DXT3 = 5, DXT5 = 6, Grayscale8 = 7)
      ImageResource imageResource = null;
      if (imageFormat == 1) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.setProperty("ImageFormat", "RGBA");
      }
      else if (imageFormat == 3) {
        if (paletteFormat == 4) {
          // for each 4 pixels, we first read 2 bytes (4*4-bit alpha values), then read 4 pixels, and apply the alpha to them

          int colorMask = 16777215;

          int numPixels = width * height;
          int[] pixels = new int[numPixels];

          for (int i = 0; i < numPixels; i += 4) {
            int alpha1 = ByteConverter.unsign(fm.readByte());
            int alpha2 = ByteConverter.unsign(fm.readByte());

            int a1 = ((alpha1 >> 4) & 15) * 16;
            int a2 = ((alpha1) & 15) * 16;
            int a3 = ((alpha2 >> 4) & 15) * 16;
            int a4 = ((alpha2) & 15) * 16;

            pixels[i] = ((palette[ByteConverter.unsign(fm.readByte())] & colorMask) | (a1 << 24));
            pixels[i + 1] = ((palette[ByteConverter.unsign(fm.readByte())] & colorMask) | (a2 << 24));
            pixels[i + 2] = ((palette[ByteConverter.unsign(fm.readByte())] & colorMask) | (a3 << 24));
            pixels[i + 3] = ((palette[ByteConverter.unsign(fm.readByte())] & colorMask) | (a4 << 24));

          }

          imageResource = new ImageResource(pixels, width, height);

        }
        else {
          // standard 8-bit
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        }

        imageResource.setProperty("ImageFormat", "8BitPaletted");
      }
      else if (imageFormat == 4) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.setProperty("ImageFormat", "DXT1");
      }
      else if (imageFormat == 5) {

        if (paletteFormat != 0) {
          // alpha bits are stored as 2 bytes before each **DXT1** block
          // Ref: https://github.com/Weesals/ModHQ/blob/master/RTS4.ModHQ/Ext/RTS4.Data/DDTImage.cs#L258
          // Not quite right yet

          int numBytes = width * height / 2;
          byte[] dxtBytes = new byte[numBytes];
          for (int i = 0; i < numBytes; i += 8) {
            // 2 - alpha bytes
            fm.skip(2);

            // 8 - DXT1 data
            dxtBytes[i] = fm.readByte();
            dxtBytes[i + 1] = fm.readByte();
            dxtBytes[i + 2] = fm.readByte();
            dxtBytes[i + 3] = fm.readByte();
            dxtBytes[i + 4] = fm.readByte();
            dxtBytes[i + 5] = fm.readByte();
            dxtBytes[i + 6] = fm.readByte();
            dxtBytes[i + 7] = fm.readByte();
          }

          FileManipulator dxtFM = new FileManipulator(new ByteBuffer(dxtBytes));
          imageResource = ImageFormatReader.readDXT1(dxtFM, width, height);
          dxtFM.close();
        }
        else {
          // normal DXT3
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
        imageResource.setProperty("ImageFormat", "DXT3");
      }
      else if (imageFormat == 6) {

        if (paletteFormat != 0) {
          // alpha bits are stored as 4 bytes before each **DXT3** block
          // Ref: https://github.com/Weesals/ModHQ/blob/master/RTS4.ModHQ/Ext/RTS4.Data/DDTImage.cs#L258
          // Not quite right yet

          int numBytes = width * height;
          byte[] dxtBytes = new byte[numBytes];
          for (int i = 0; i < numBytes; i += 16) {
            // 4 - alpha bytes
            fm.skip(4);

            // 8 - DXT1 data
            dxtBytes[i] = fm.readByte();
            dxtBytes[i + 1] = fm.readByte();
            dxtBytes[i + 2] = fm.readByte();
            dxtBytes[i + 3] = fm.readByte();
            dxtBytes[i + 4] = fm.readByte();
            dxtBytes[i + 5] = fm.readByte();
            dxtBytes[i + 6] = fm.readByte();
            dxtBytes[i + 7] = fm.readByte();
            dxtBytes[i + 8] = fm.readByte();
            dxtBytes[i + 9] = fm.readByte();
            dxtBytes[i + 10] = fm.readByte();
            dxtBytes[i + 11] = fm.readByte();
            dxtBytes[i + 12] = fm.readByte();
            dxtBytes[i + 13] = fm.readByte();
            dxtBytes[i + 14] = fm.readByte();
            dxtBytes[i + 15] = fm.readByte();
          }

          FileManipulator dxtFM = new FileManipulator(new ByteBuffer(dxtBytes));
          imageResource = ImageFormatReader.readDXT3(dxtFM, width, height);
          dxtFM.close();
        }
        else {
          // normal DXT5
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
        }

        imageResource.setProperty("ImageFormat", "DXT5");
      }
      else if (imageFormat == 7) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.setProperty("ImageFormat", "8BitPaletted");
      }
      else {
        ErrorLogger.log("[Viewer_BAR_ESPN_DDT_RTS3] Unknown image format: " + imageFormat);
        return null;
      }

      imageResource = ImageFormatReader.flipVertically(imageResource);

      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Generate all the mipmaps of the image
      ImageResource[] mipmaps = im.generateMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String imageFormat = "DXT3";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        imageFormat = imageResource.getProperty("ImageFormat", "DXT3");
      }

      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }
      if (!(imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5"))) {
        // a different image format not allowed in this image - change to DXT3
        imageFormat = "DXT3";
      }

      // 4 - Header (RTS3)
      fm.writeString("RTS3");

      //3 - Unknown (DXT1=0,0,4  DXT3/5=1,4,8 or 0,4,8)
      if (imageFormat.equals("DXT1")) {
        fm.writeByte(0);
        fm.writeByte(0);
        fm.writeByte(4);
      }
      else if (imageFormat.equals("DXT3") || imageFormat.equals("DXT5")) {
        fm.writeByte(1);
        fm.writeByte(4);
        fm.writeByte(8);
      }

      // 1 - Number Of MipMaps
      fm.writeByte(mipmapCount);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // 4 - Image Height
      fm.writeInt(imageHeight);

      int offset = 16 + (mipmapCount * 8);

      // for each mipmap
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];
        int calcWidth = mipmap.getWidth();
        int calcHeight = mipmap.getHeight();

        // 4 - Image Data Offset
        fm.writeInt(offset);

        // 4 - Image Data Length
        int length = calcWidth * calcHeight;
        if (imageFormat.equals("DXT1")) {
          length /= 2; // 0.5bytes per pixel for DXT1, 1byte per pixel for DXT3/5
        }
        fm.writeInt(length);

        offset += length;

      }

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        // X - Pixels
        if (imageFormat.equals("DXT1")) {
          ImageFormatWriter.writeDXT1(fm, mipmap);
        }
        else if (imageFormat.equals("DXT3")) {
          ImageFormatWriter.writeDXT3(fm, mipmap);
        }
        else if (imageFormat.equals("DXT5")) {
          ImageFormatWriter.writeDXT5(fm, mipmap);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}