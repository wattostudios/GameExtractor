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
import org.watto.ge.plugin.archive.Plugin_BGD;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BGD_BTF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BGD_BTF() {
    super("BGD_BTF", "Redline BTF Image");
    setExtensions("btf");

    setGames("Redline");
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
      if (plugin instanceof Plugin_BGD) {
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

      if (fm.readShort() == 2) {
        rating += 5;
      }
      else {
        rating = 0;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      fm.skip(4);

      // 2 - Number of Mipmaps
      if (FieldValidator.checkRange(fm.readShort(), 0, 20)) { // this format allows 0 to mean 1 mipmap
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

      // 2 - Unknown (2)
      fm.skip(2);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Bitcount (8=Paletted, 24=BGR, 32=ARGB)
      short imageFormat = fm.readShort();

      // 2 - Number of Colors (0 if not a paletted image)
      short numColors = fm.readShort();

      // 2 - Mipmap Count
      short mipmapCount = fm.readShort();
      if (mipmapCount == 0) {
        mipmapCount = 1;
      }
      FieldValidator.checkRange(mipmapCount, 1, 20);

      ImageResource imageResource = null;
      if (imageFormat == 8) {
        // Paletted

        // X - Palette
        int[] palette = ImageFormatReader.readBGR(fm, 1, numColors).getPixels();

        // X - Mipmap
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (imageFormat == 24) {
        // X - Mipmap
        imageResource = ImageFormatReader.readBGR(fm, width, height);
      }
      else if (imageFormat == 32) {
        // X - Mipmap
        imageResource = ImageFormatReader.readARGB(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_BGD_BTF] Unknown Image Format: " + imageFormat);
        return null;
      }

      fm.close();

      imageResource = ImageFormatReader.flipVertically(imageResource);

      imageResource.addProperty("MipmapCount", "" + mipmapCount);
      imageResource.addProperty("ImageFormat", "" + imageFormat);
      imageResource.addProperty("ColorCount", "" + numColors);

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Write from scratch
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

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      int mipmapCount = 1;
      int imageFormat = 32;
      int numColors = 0;

      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      // Now try to get the property values from the ImageResource, if they exist
      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        imageFormat = imageResource.getProperty("ImageFormat", imageFormat);
        numColors = imageResource.getProperty("ColorCount", numColors);
      }

      // Flip the image
      imageResource = ImageFormatReader.flipVertically(imageResource);
      im = new ImageManipulator(imageResource);

      // 2 - Unknown (2)
      fm.writeShort(2);

      // 2 - Image Height
      fm.writeShort(imageHeight);

      // 2 - Image Width
      fm.writeShort(imageWidth);

      // 2 - Bitcount (8=Paletted, 32=ARGB)
      fm.writeShort(imageFormat);

      // 2 - Number of Colors (0 if not a paletted image)
      fm.writeShort(numColors);

      if (imageFormat == 8) {
        // Paletted
        ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
        if (mipmaps.length < mipmapCount) {
          mipmapCount = mipmaps.length;
        }

        // 2 - Mipmap Count
        fm.writeShort(mipmapCount);

        // X - Palette
        int[] palette = mipmaps[0].getPalette();
        ImageFormatWriter.writePaletteBGR(fm, palette);

        // X - Pixels
        for (int m = 0; m < mipmapCount; m++) {
          byte[] mipmapBytes = mipmaps[m].getPixelBytes();
          fm.writeBytes(mipmapBytes);
        }
      }
      else if (imageFormat == 24) {
        // BGR

        ImageResource[] mipmaps = im.generateMipmaps();
        if (mipmaps.length < mipmapCount) {
          mipmapCount = mipmaps.length;
        }

        // 2 - Mipmap Count
        fm.writeShort(mipmapCount);

        // X - Pixels
        for (int m = 0; m < mipmapCount; m++) {
          ImageFormatWriter.writeBGR(fm, mipmaps[m]);
        }
      }
      else {
        // ARGB

        ImageResource[] mipmaps = im.generateMipmaps();
        if (mipmaps.length < mipmapCount) {
          mipmapCount = mipmaps.length;
        }

        // 2 - Mipmap Count
        fm.writeShort(mipmapCount);

        // X - Pixels
        for (int m = 0; m < mipmapCount; m++) {
          ImageFormatWriter.writeARGB(fm, mipmaps[m]);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  Replace an existing BTF (so keep the same format, mipmapCount, ...)  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      // Extract the original resource into a byte[] array, so we can reference it
      int srcLength = (int) resourceBeingReplaced.getDecompressedLength();
      if (srcLength > 12) {
        srcLength = 12; // allows enough reading for the header and color palette, but not much of the original image data
      }
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      //
      // Get the values from the source
      //

      // 2 - Unknown (2)
      // 2 - Image Height
      // 2 - Image Width
      src.skip(6);

      // 2 - Bitcount (8=Paletted, 32=ARGB)
      short imageFormat = src.readShort();

      // 2 - Number of Colors (0 if not a paletted image)
      short numColors = src.readShort();

      // 2 - Mipmap Count
      short mipmapCount = src.readShort();

      boolean mipmapNull = false;
      if (mipmapCount == 0) {
        mipmapCount = 1;
        mipmapNull = true;
      }

      src.close();

      //
      // Now that we know what the original was like, build the new one the same as it.
      //
      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      imageResource = ImageFormatReader.flipVertically(imageResource);
      im = new ImageManipulator(imageResource);

      // 2 - Unknown (2)
      fm.writeShort(2);

      // 2 - Image Height
      fm.writeShort(imageHeight);

      // 2 - Image Width
      fm.writeShort(imageWidth);

      // 2 - Bitcount (8=Paletted, 32=ARGB)
      fm.writeShort(imageFormat);

      // 2 - Number of Colors (0 if not a paletted image)
      fm.writeShort(numColors);

      if (imageFormat == 8) {
        // Paletted

        // Convert to the right number of colors
        im.changeColorCount(numColors);

        // now generate the mipmaps for this number of colors (and use the same palette across all mipmaps)
        ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
        if (mipmaps.length < mipmapCount) {
          mipmapCount = (short) mipmaps.length;
        }

        // 2 - Mipmap Count
        if (mipmapNull) {
          fm.writeShort(0);
        }
        else {
          fm.writeShort(mipmapCount);
        }

        // X - Palette
        int[] palette = mipmaps[0].getPalette();
        ImageFormatWriter.writePaletteBGR(fm, palette);

        // X - Pixels
        for (int m = 0; m < mipmapCount; m++) {
          byte[] mipmapBytes = mipmaps[m].getPixelBytes();
          fm.writeBytes(mipmapBytes);
        }
      }
      else if (imageFormat == 24) {
        // BGR

        ImageResource[] mipmaps = im.generateMipmaps();
        if (mipmaps.length < mipmapCount) {
          mipmapCount = (short) mipmaps.length;
        }

        // 2 - Mipmap Count
        fm.writeShort(mipmapCount);

        // X - Pixels
        for (int m = 0; m < mipmapCount; m++) {
          ImageFormatWriter.writeBGR(fm, mipmaps[m]);
        }
      }
      else {
        // ARGB

        ImageResource[] mipmaps = im.generateMipmaps();
        if (mipmaps.length < mipmapCount) {
          mipmapCount = (short) mipmaps.length;
        }

        // 2 - Mipmap Count
        if (mipmapNull) {
          fm.writeShort(0);
        }
        else {
          fm.writeShort(mipmapCount);
        }

        // X - Pixels
        for (int m = 0; m < mipmapCount; m++) {
          ImageFormatWriter.writeARGB(fm, mipmaps[m]);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}