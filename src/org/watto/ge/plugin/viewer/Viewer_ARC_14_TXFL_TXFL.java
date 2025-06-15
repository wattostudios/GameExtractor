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
import org.watto.ge.plugin.archive.Plugin_ARC_14;
import org.watto.ge.plugin.archive.Plugin_BIG_BIGF;
import org.watto.ge.plugin.archive.Plugin_SET;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_14_TXFL_TXFL extends ViewerPlugin {

  public static int FORMAT_URBZ = 1;
  public static int FORMAT_SIMS2 = 2;
  public static int FORMAT_URBZ_BIGF = 3;

  int format = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_14_TXFL_TXFL() {
    super("ARC_14_TXFL_TXFL", "ARC_14_TXFL_TXFL Image");
    setExtensions("txfl", "tgq");

    setGames("The Urbz: Sims in the City",
        "The Sims 2");
    setPlatforms("XBox");
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
      if (plugin instanceof Plugin_ARC_14 || plugin instanceof Plugin_SET || plugin instanceof Plugin_BIG_BIGF) {
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

      // 4 - Header (The Urbz)
      if (fm.readString(4).equals("TXFL")) {
        rating += 50;
      }
      else {
        // Check for The Sims 2
        if (fm.readString(4).equals("LFXT")) {
          rating += 50;
          return rating;
        }
        else {
          rating = 0;
        }
      }

      if (fm.readInt() == 8) {
        rating += 5;
      }

      // 4 - File Length
      if (fm.readInt() + 12 == fm.getLength()) {
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

      // 4 - Header (TXFL)
      String header = fm.readString(4);
      if (header.equals("TXFL")) {
        // The Urbz
        format = FORMAT_URBZ;

        // 4 - Unknown (8)
        fm.skip(4);

        // 4 - File Length [+12]
        int length = fm.readInt();
        if (length == 0) {
          // URBZ, within a BIG_BIGF file
          format = FORMAT_URBZ_BIGF;

          // 8 - null
          fm.skip(8);
        }
        else {
          // X - Filename
          // 1 - null Filename Terminator
          fm.readNullString();

          // 8 - null
          // 4 - Unknown
          // 4 - null
          fm.skip(16);
        }
      }
      else {
        // Check for The Sims 2
        header = fm.readString(4);
        if (header.equals("LFXT")) {

          format = FORMAT_SIMS2;

          // 4 - Unknown (-1)
          // 4 - Filename Length (including null terminator)
          fm.skip(8);

          // X - Filename
          // 1 - null Filename Terminator
          fm.readNullString();

          // 4 - Rest of File Length (Length of the rest of the data in this file, after this field)
          // 8 - null
          // 4 - Unknown (1251, 1056, ...)
          // 4 - null
          fm.skip(20);
        }
        else {
          // Unknown game
          return null;
        }
      }

      // 2 - Image Width
      int width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown (0/256)
      fm.skip(2);

      // 2 - Number of Mipmaps (0 = only 1 mipmap)
      int mipmapCount = fm.readShort();

      // 2 - Unknown (143=DXT3, 140=RGBA Swizzled)
      fm.skip(2);

      // 2 - Image Format? (8=DXT3, 32=RGBA Swizzled)
      int imageFormat = fm.readShort();

      // 4 - null
      fm.skip(4);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 8) {
        // DXT3
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 4) {
        // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        //imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
      }
      else if (imageFormat == 16) {
        // RGBA5551 + Swizzle
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
        if (format == FORMAT_URBZ_BIGF) {
          // no swizzle
        }
        else {
          imageResource.setPixels(ImageFormatReader.unswizzle(imageResource.getImagePixels(), width, height, 2));
        }
      }
      else if (imageFormat == 32) {
        // RGBA + Swizzle
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        if (format == FORMAT_URBZ_BIGF) {
          // no swizzle
        }
        else {
          imageResource.setPixels(ImageFormatReader.unswizzle(imageResource.getImagePixels(), width, height, 2));
        }
      }
      else if (imageFormat == 8200) {
        // 8-bit Paletted + Swizzled
        int largestMipmap = width * height;
        byte[] pixels = fm.readBytes(largestMipmap);

        int remainingMipmaps = 0;
        int mipmapWidth = width / 2;
        int mipmapHeight = height / 2;
        for (int m = 1; m < mipmapCount; m++) {
          remainingMipmaps += mipmapWidth * mipmapHeight;
          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }

        fm.skip(remainingMipmaps);

        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);

        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixels));

        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);

        if (format == FORMAT_URBZ_BIGF) {
          // no swizzle
        }
        else {
          imageResource.setPixels(ImageFormatReader.unswizzle(imageResource.getImagePixels(), width, height, 2));
        }
      }
      else {
        ErrorLogger.log("[Viewer_ARC_14_TXFL_TXFL] Unknown Image Format: " + imageFormat);
        return null;
      }

      if (format == FORMAT_URBZ_BIGF) {
        // no flip
      }
      else {
        imageResource = ImageFormatReader.flipVertically(imageResource);
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
      int srcLength = (int) resourceBeingReplaced.getDecompressedLength();
      if (srcLength > 100) {
        srcLength = 100; // allows enough reading for the header and color palette, but not much of the original image data
      }
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // Read through the src first, to find the image format, so we can then work out the new image length (eg if we change width/height); 

      // 4 - Header (TXFL)
      String header = src.readString(4);
      if (header.equals("TXFL")) {
        // The Urbz
        format = FORMAT_URBZ;

        // check whether it's in a BIG_BIGF or in a normal ARC
        src.skip(4);

        // 4 - File Length [+12]
        int length = src.readInt();
        if (length == 0) {
          // URBZ, within a BIG_BIGF file
          format = FORMAT_URBZ_BIGF;
        }

      }
      else {
        // Check for The Sims 2
        header = src.readString(4);
        if (header.equals("LFXT")) {
          format = FORMAT_SIMS2;
        }
      }
      src.seek(0);

      if (format == 0) {
        // assume The Urbz
        format = FORMAT_URBZ;
      }

      // FLIP VERTICALLY
      if (format == FORMAT_URBZ_BIGF) {
        // no flip
      }
      else {
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }

      int headerAdjustment = 0;
      if (format == FORMAT_URBZ) {
        src.skip(12);
        headerAdjustment = (int) src.getOffset();
        src.readNullString();
        src.skip(20);
        src.skip(2);//short paletted = src.readShort();        
      }
      else if (format == FORMAT_URBZ_BIGF) {
        src.skip(24);
        src.skip(2);//short paletted = src.readShort();        
      }
      else if (format == FORMAT_SIMS2) {
        src.skip(16);
        src.readNullString();
        headerAdjustment = (int) src.getOffset();
        src.skip(24);
        src.skip(2);//short paletted = src.readShort();        
      }

      short numMipmaps = src.readShort();
      src.skip(2);
      short imageFormat = src.readShort();
      src.skip(4);

      int headerSize = (int) src.getOffset() - headerAdjustment;

      src.seek(0); // back to the beginning of the src, now that we have the data we need

      // work out the new image size
      int newSize = headerSize;
      if (imageFormat == 8) {
        // DXT3
        newSize += width * height;

        int mipmapWidth = width / 2;
        int mipmapHeight = height / 2;
        for (int m = 1; m < numMipmaps; m++) {
          newSize += mipmapWidth * mipmapHeight;

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }
      else if (imageFormat == 4) {
        // DXT1
        newSize += width * height / 2;

        int mipmapWidth = width / 2;
        int mipmapHeight = height / 2;
        for (int m = 1; m < numMipmaps; m++) {
          newSize += mipmapWidth * mipmapHeight / 2;

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }
      else if (imageFormat == 16) {
        // RGBA5551 + Swizzle
        newSize += width * height * 3;

        int mipmapWidth = width / 2;
        int mipmapHeight = height / 2;
        for (int m = 1; m < numMipmaps; m++) {
          newSize += mipmapWidth * mipmapHeight * 3;

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }
      else if (imageFormat == 32) {
        // RGBA + Swizzle
        newSize += width * height * 4;

        int mipmapWidth = width / 2;
        int mipmapHeight = height / 2;
        for (int m = 1; m < numMipmaps; m++) {
          newSize += mipmapWidth * mipmapHeight * 4;

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }
      else if (imageFormat == 8200) {
        // 8-bit Paletted + Swizzled
        newSize += width * height + 256 * 4;

        int mipmapWidth = width / 2;
        int mipmapHeight = height / 2;
        for (int m = 1; m < numMipmaps; m++) {
          newSize += mipmapWidth * mipmapHeight;

          mipmapWidth /= 2;
          mipmapHeight /= 2;
        }
      }

      // NOW WRITE THE NEW IMAGE

      if (format == FORMAT_URBZ) {
        // 4 - Header (TXFL)
        // 4 - Unknown (8)
        fm.writeBytes(src.readBytes(8));

        // 4 - File Length [+12]
        src.skip(4);
        fm.writeInt(newSize);

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeString(src.readNullString());
        fm.writeByte(0);

        // 8 - null
        // 4 - Unknown
        // 4 - null
        fm.writeBytes(src.readBytes(16));
      }
      else if (format == FORMAT_URBZ_BIGF) {
        // 4 - Header (TXFL)
        // 16 - null
        fm.writeBytes(src.readBytes(20));
      }
      else if (format == FORMAT_SIMS2) {
        // 4 - Unknown (9)
        // 4 - Header (LFXT)
        // 4 - Unknown (-1)
        // 4 - Filename Length (including null terminator)
        fm.writeBytes(src.readBytes(16));

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeString(src.readNullString());
        fm.writeByte(0);

        // 4 - Rest of File Length (Length of the rest of the data in this file, after this field)
        src.skip(4);
        fm.writeInt(newSize);

        // 8 - null
        // 4 - Unknown (1251, 1056, ...)
        // 4 - null
        fm.writeBytes(src.readBytes(16));
      }

      // 2 - Image Width
      src.skip(2);
      fm.writeShort(width);

      // 2 - Image Height
      src.skip(2);
      fm.writeShort(height);

      // 2 - Paletted (0=not Paletted, 256=Paletted)
      // 2 - Number of Mipmaps (0 = only 1 mipmap)
      // 2 - Unknown (142=DXT1, 143=DXT3, 144=RGBA5551 Swizzled, 140=RGBA Swizzled, 653=Paletted and Swizzled)
      // 2 - Image Format? (4=DXT1, 8=DXT3, 16=RGBA5551 Swizzled, 32=RGBA Swizzled, 8200=Paletted and Swizzled)
      // 4 - null
      fm.writeBytes(src.readBytes(12));

      if (numMipmaps == 0) {
        numMipmaps = 1;
      }

      ImageManipulator im = new ImageManipulator(imageResource);
      ImageResource[] mipmaps = im.generateMipmaps(numMipmaps);

      if (imageFormat == 8) {
        // DXT3
        for (int m = 0; m < numMipmaps; m++) {
          ImageFormatWriter.writeDXT3(fm, mipmaps[m]);
        }
      }
      else if (imageFormat == 4) {
        // DXT1
        for (int m = 0; m < numMipmaps; m++) {
          ImageFormatWriter.writeDXT1(fm, mipmaps[m]);
        }
      }
      else if (imageFormat == 16) {
        // RGBA5551 + Swizzle
        for (int m = 0; m < numMipmaps; m++) {
          ImageResource mipmap = mipmaps[m];

          if (format == FORMAT_URBZ_BIGF) {
            // no swizzle
          }
          else {
            int mipmapWidth = mipmap.getWidth();
            int mipmapHeight = mipmap.getHeight();
            mipmap = new ImageResource(ImageFormatReader.swizzle(mipmap.getImagePixels(), mipmapWidth, mipmapHeight, 2), mipmapWidth, mipmapHeight);
          }

          ImageFormatWriter.writeRGBA5551(fm, mipmap);
        }
      }
      else if (imageFormat == 32) {
        // RGBA + Swizzle
        for (int m = 0; m < numMipmaps; m++) {
          ImageResource mipmap = mipmaps[m];

          if (format == FORMAT_URBZ_BIGF) {
            // no swizzle
          }
          else {
            int mipmapWidth = mipmap.getWidth();
            int mipmapHeight = mipmap.getHeight();
            mipmap = new ImageResource(ImageFormatReader.swizzle(mipmap.getImagePixels(), mipmapWidth, mipmapHeight, 2), mipmapWidth, mipmapHeight);
          }

          ImageFormatWriter.writeRGBA(fm, mipmap);
        }
      }
      else if (imageFormat == 8200) {
        // 8-bit Paletted + Swizzled
        im = new ImageManipulator(imageResource);
        im.convertToPaletted();

        if (im.getNumColors() > 256) {
          im.changeToSingleAlpha(50);
          //im.changeColorCountRGBSingleAlpha(256);
          im.changeColorCountRGBKeepingExistingAlpha(256);
        }

        im.resizePalette(256); // if less than 256 colors, force to 256 colors (fill with empties)

        ImageManipulator[] palMipmaps = im.generatePalettedMipmaps();
        for (int m = 0; m < numMipmaps; m++) {
          ImageManipulator mipmap = palMipmaps[m];
          int mipmapWidth = mipmap.getWidth();
          int mipmapHeight = mipmap.getHeight();

          int[] pixels = mipmap.getPixels();
          if (format == FORMAT_URBZ_BIGF) {
            // no swizzle
          }
          else {
            pixels = ImageFormatReader.swizzle(pixels, mipmapWidth, mipmapHeight, 2);
          }
          int numPixels = pixels.length;

          for (int i = 0; i < numPixels; i++) {
            fm.writeByte(pixels[i]);
          }
        }

        // X - Color Palette
        int[] palette = im.getPalette();
        ImageFormatWriter.writePaletteRGBA(fm, palette);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}