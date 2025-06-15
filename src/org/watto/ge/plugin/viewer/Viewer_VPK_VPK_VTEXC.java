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
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_VPK;
import org.watto.ge.plugin.archive.Plugin_VPK_2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VPK_VPK_VTEXC extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VPK_VPK_VTEXC() {
    super("VPK_VPK_VTEXC", "Valve VTEX_C Image");
    setExtensions("vtex_c");

    setGames("Valve Engine");
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

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_VPK || readPlugin instanceof Plugin_VPK_2) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      fm.skip(4);

      // 2 - Version Major (12)
      if (fm.readShort() == 12) {
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

      // 4 - Image Data Offset
      int imageDataOffset = fm.readInt();
      FieldValidator.checkOffset(imageDataOffset, arcSize);

      // 2 - Version Major? (12)
      // 2 - Version Minor? (1)
      // 4 - Unknown (8)
      // 4 - Unknown (2)
      // 4 - Header (REDI)
      // 4 - Unknown (20)
      // 4 - REDI Block Length
      // 4 - Header (DATA)
      fm.skip(28);

      // 4 - DATA Block Length (not including these 3 header fields)
      int dataBlockLength = fm.readInt();
      FieldValidator.checkLength(dataBlockLength, arcSize);

      // 4 - Image Descriptor Length
      fm.skip(4);

      // X - DATA Block
      fm.skip(dataBlockLength);

      // 12 - Unknown
      fm.skip(12);

      // 2 - Image Width
      int width = fm.readShort();

      // 2 - Image Height
      int height = fm.readShort();

      // 2 - Unknown (1)
      fm.skip(2);

      // 1 - Image Format (1=DXT1, 4=RGBA)
      int imageFormat = fm.readByte();

      // 1 - Number of Mipmaps
      int numMipmaps = fm.readByte();

      // X - Unknown
      int remainingLength = (int) (imageDataOffset - fm.getOffset());
      FieldValidator.checkPositive(remainingLength);
      fm.skip(remainingLength);

      // calculate the mipmap sizes based on the width/height
      int[] mipmapWidths = new int[numMipmaps];
      int[] mipmapHeights = new int[numMipmaps];
      int currentWidth = width;
      int currentHeight = height;
      for (int i = 0; i < numMipmaps; i++) {
        mipmapWidths[i] = currentWidth;
        mipmapHeights[i] = currentHeight;

        if (currentWidth != 0) {
          currentWidth /= 2;
        }
        if (currentHeight != 0) {
          currentHeight /= 2;
        }
      }

      // Image formats ref = https://github.com/ValveResourceFormat/ValveResourceFormat/blob/789de71c7c6b13e0a019dff8bec93fa824c3f1ef/ValveResourceFormat/Resource/Enums/VTexFormat.cs

      // work out how many bytes per pixel, based on the Image Format
      float bytesPerPixel = 1;
      if (imageFormat == 1) {
        bytesPerPixel = 0.5f;
      }
      else if (imageFormat == 2 || imageFormat == 3 || imageFormat == 20) {
        bytesPerPixel = 1;
      }
      else if (imageFormat == 8) {
        bytesPerPixel = 2;
      }
      else if (imageFormat == 4 || imageFormat == 28 || imageFormat == 9) {
        bytesPerPixel = 4;
      }
      else if (imageFormat == 10) {
        bytesPerPixel = 8;
      }
      else {
        ErrorLogger.log("[Viewer_VPK_VPK_VTEXC] Unknown Image Format: " + imageFormat);
        return null;
      }

      // Work out the size of the largest mipmap
      int numPixels = width * height;
      int numBytes = (int) (bytesPerPixel * numPixels);
      int totalNumBytes = 0;

      totalNumBytes = numBytes;

      // if the image is too large (for our Settings), choose a smaller one
      int maxSize = Settings.getInt("MaxImageDimension");
      if (maxSize <= 0) {
        // ignore it
      }
      else if (width > maxSize || height > maxSize) {
        // find the mipmap that's the right size

        while (width > maxSize || height > maxSize) {
          width /= 2;
          height /= 2;

          numPixels = width * height;
          numBytes = (int) (bytesPerPixel * numPixels);
          totalNumBytes += numBytes;
        }
      }

      // now go to the offset to the mipmap we want
      long offset = fm.getLength() - totalNumBytes;
      fm.seek(offset);

      // Now we're at the largest image, so grab it
      ImageResource imageResource = null;
      if (imageFormat == 1) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (imageFormat == 2) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (imageFormat == 3) { // I8
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.addProperty("ImageFormat", "I8");
      }
      else if (imageFormat == 4 || imageFormat == 28) { // RGBA
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
      }
      else if (imageFormat == 8) { // R16
        imageResource = ImageFormatReader.readR16(fm, width, height);
        imageResource.addProperty("ImageFormat", "R16");
      }
      else if (imageFormat == 9) { // R16G16
        imageResource = ImageFormatReader.readR16G16(fm, width, height);
        imageResource.addProperty("ImageFormat", "R16G16");
      }
      else if (imageFormat == 10) { // RGBA16161616
        imageResource = ImageFormatReader.read16F16F16F16F_RGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "16F16F16F16F_RGBA");
      }
      else if (imageFormat == 20) { // BC7
        imageResource = ImageFormatReader.readBC7(fm, width, height);
        imageResource.addProperty("ImageFormat", "BC7");
      }

      else {
        return null; // unknown (or other) image format
      }

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
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}