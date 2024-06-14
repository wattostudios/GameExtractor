/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import java.awt.Image;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TXD_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TXD_2_TXDTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TXD_2_TXDTEX() {
    super("TXD_2_TXDTEX", "TXD_2 Image");
    setExtensions("txd_tex");

    setGames("Kill Switch");
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
      if (plugin instanceof Plugin_TXD_2) {
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
      if (fm.readInt() == 21) {
        rating += 25;
      }

      fm.skip(8);

      if (fm.readInt() == 1) {
        rating += 5;
      }
      else {
        rating = 0;
      }

      fm.skip(88);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
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

      // 4 - Chunk Type (21 = Raster)
      // 4 - Chunk Data Length
      // 4 - Version
      // 4 - Chunk Type (1 = Struct)
      // 4 - Chunk Data Length
      // 4 - Version
      // 4 - Platform ID
      // 1 - Filter Mode
      // 1 - Addressing
      // 2 - Padding
      // 32 - Raster Name (null terminated, filled with nulls)
      // 32 - Mask Name (null terminated, filled with nulls)
      // 4 - Alpha Flags
      fm.skip(100);

      // 4 - DirectX Image Format (or null)
      String imageFormat = fm.readString(4);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 1 - Bits Per Pixel
      int bpp = fm.readByte();

      // 1 - Number of Mipmaps
      // 1 - Raster Type
      // 1 - Flags
      fm.skip(3);

      int[] palette = null;
      if (bpp <= 8) {
        // 4 - Unknown
        fm.skip(4);

        // 1020 - Color Palette (255*RGBA)
        palette = ImageFormatReader.readPaletteBGRA(fm, 256);

        /*
        int[] oldPalette = palette;
        palette = new int[256];
        System.arraycopy(oldPalette, 0, palette, 0, 255);
        palette[255] = (255 << 24);
        */
      }
      else {
        // 4 - Mipmap Pixel Data Length
        fm.skip(4);
      }

      // X - Mipmap Pixel Data
      ImageResource imageResource = null;
      if (bpp == 8) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_TXD_2_TXDTEX] Unknown Image Format: " + imageFormat);
        return null;
      }

      imageResource.setPixels(ImageFormatReader.unswizzle(imageResource.getImagePixels(), width, height, 1));

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

  /**
  **********************************************************************************************
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      Image image = ivp.getImage();
      int width = ivp.getImageWidth();
      int height = ivp.getImageHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // Try to get the existing ImageResource (if it was stored), otherwise build a new one
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      if (imageResource == null) {
        imageResource = new ImageResource(image, width, height);
      }

      // Extract the original resource into a byte[] array, so we can reference it
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Chunk Type (21 = Raster)
      // 4 - Chunk Data Length
      // 4 - Version
      // 4 - Chunk Type (1 = Struct)
      // 4 - Chunk Data Length
      // 4 - Version
      // 4 - Platform ID
      // 1 - Filter Mode
      // 1 - Addressing
      // 2 - Padding
      // 32 - Raster Name (null terminated, filled with nulls)
      // 32 - Mask Name (null terminated, filled with nulls)
      // 4 - Alpha Flags
      fm.writeBytes(src.readBytes(100));

      // 4 - DirectX Image Format (or null)
      String imageFormat = src.readString(4);
      fm.writeString(imageFormat);

      // 2 - Image Width
      fm.writeShort(width);
      src.skip(2);

      // 2 - Image Height
      fm.writeShort(height);
      src.skip(2);

      // 1 - Bits Per Pixel
      int bpp = src.readByte();
      fm.writeByte(bpp);

      // 1 - Number of Mipmaps
      int mipmapCount = src.readByte();
      fm.writeByte(mipmapCount);

      // 1 - Raster Type
      // 1 - Flags
      fm.writeBytes(src.readBytes(2));

      if (bpp <= 8) {
        /*
        ImageManipulator im = new ImageManipulator(imageResource);
        im.changeColorCount(256);
        ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
        */
        ImageManipulator im = new ImageManipulator(imageResource);
        im.changeColorCount(256);
        ImageResource[] mipmaps = im.generateMipmaps(mipmapCount);

        int mipmapWidth = width;
        int mipmapHeight = height;
        for (int m = 0; m < mipmapCount; m++) {
          //ImageManipulator mipmap = mipmaps[m];

          // do the swizzle
          mipmaps[m].setPixels(ImageFormatReader.swizzle(mipmaps[m].getImagePixels(), mipmapWidth, mipmapHeight, 1));

          // then convert to paletted
          ImageManipulator mipmap = new ImageManipulator(mipmaps[m]);
          mipmap.changeColorCount(256);

          // 4 - Mipmap Pixel Data Length
          int srcLength = src.readInt();
          int mipmapLength = mipmapWidth * mipmapHeight;
          fm.writeInt(mipmapLength);

          // 1024 - Color Palette (256*BGRA)
          src.skip(1024);
          int[] palette = mipmap.getPalette();
          ImageFormatWriter.writePaletteBGRA(fm, palette);

          // X - Mipmap Pixel Data (palette indexes)
          src.skip(srcLength);
          int[] pixels = mipmap.getPixels();
          for (int p = 0; p < mipmapLength; p++) {
            fm.writeByte(pixels[p]);
          }

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }
      else {
        ImageManipulator im = new ImageManipulator(imageResource);
        ImageResource[] mipmaps = im.generateMipmaps(mipmapCount);

        int mipmapWidth = width;
        int mipmapHeight = height;
        for (int m = 0; m < mipmapCount; m++) {
          ImageResource mipmap = mipmaps[m];

          mipmap.setPixels(ImageFormatReader.unswizzle(mipmap.getImagePixels(), mipmapWidth, mipmapHeight, 1));

          if (imageFormat.equals("DXT1")) {
            // 4 - Mipmap Pixel Data Length
            int srcLength = src.readInt();
            int mipmapLength = mipmapWidth * mipmapHeight / 2;
            fm.writeInt(mipmapLength);

            // X - Mipmap Pixel Data (palette indexes)
            src.skip(srcLength);
            ImageFormatWriter.writeDXT1(fm, mipmap);
          }
          else if (imageFormat.equals("DXT3")) {
            // 4 - Mipmap Pixel Data Length
            int srcLength = src.readInt();
            int mipmapLength = mipmapWidth * mipmapHeight;
            fm.writeInt(mipmapLength);

            // X - Mipmap Pixel Data (palette indexes)
            src.skip(srcLength);
            ImageFormatWriter.writeDXT3(fm, mipmap);
          }
          else if (imageFormat.equals("DXT5")) {
            // 4 - Mipmap Pixel Data Length
            int srcLength = src.readInt();
            int mipmapLength = mipmapWidth * mipmapHeight;
            fm.writeInt(mipmapLength);

            // X - Mipmap Pixel Data (palette indexes)
            src.skip(srcLength);
            ImageFormatWriter.writeDXT5(fm, mipmap);
          }
          else { // fallback
            // 4 - Mipmap Pixel Data Length
            int srcLength = src.readInt();
            int mipmapLength = mipmapWidth * mipmapHeight;
            fm.writeInt(mipmapLength);

            // X - Mipmap Pixel Data (palette indexes)
            src.skip(srcLength);
            ImageFormatWriter.writeDXT5(fm, mipmap);
          }

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }

      // see if there's anything left in the source - if there is, copy it
      int srcRemaining = (int) (src.getLength() - src.getOffset());
      fm.writeBytes(src.readBytes(srcRemaining));

      // finally, go back and write the archive size
      fm.seek(4);

      // 4 - Chunk Size
      fm.writeInt((fm.getLength() - 12));

      // and write the image size
      fm.seek(16);

      // 4 - Chunk Size
      fm.writeInt((fm.getLength() - 24 - srcRemaining));

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}