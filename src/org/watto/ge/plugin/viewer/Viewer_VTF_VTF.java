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
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_VPK;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VTF_VTF extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_VTF_VTF() {
    super("VTF_VTF", "Valve Texture Format");
    setExtensions("vtf");

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

      if (Archive.getReadPlugin() instanceof Plugin_VPK) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header ("VTF" + (byte)0)
      if (fm.readString(3).equals("VTF") && fm.readByte() == 0) {
        rating += 5;
      }

      // 4 - Version Major (7)
      if (fm.readInt() == 7) {
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
  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header ("VTF" + (byte)0)
      // 4 - Version Major (7)
      fm.skip(8);

      // 4 - Version Minor (2/3)
      int versionMinor = fm.readInt();

      // 4 - Header Size (80 + resourceDirectorySize)
      int headerSize = fm.readInt();
      FieldValidator.checkLength(headerSize, arcSize);

      // 2 - Width
      int width = fm.readShort();

      // 2 - Height
      int height = fm.readShort();

      // 4 - Flags
      int flags = fm.readInt();

      int numFaces = 1;
      if ((flags & 16384) == 16384) {
        numFaces = 6;
      }
      int numSlices = 1;

      // 2 - Number of Frames (1 = no animation)
      int numFrames = fm.readShort();

      // 2 - First Frame Number
      // 4 - null Padding to 16 bytes
      // 4 - Reflectivity Vector 1
      // 4 - Reflectivity Vector 1
      // 4 - Reflectivity Vector 1
      // 4 - null Padding to 16 bytes
      // 4 - Bumpmap Scale
      fm.skip(26);

      // 4 - High Resolution Image Format
      int imageFormat = fm.readInt();

      // 1 - Mipmap Count
      int numMipmaps = fm.readByte();

      // 4 - Low Resolution Image Format (13)
      int lowResImageFormat = fm.readInt();

      // 1 - Low Resolution Width
      int lowResWidth = fm.readByte();

      // 1 - Low Resolution Height
      int lowResHeight = fm.readByte();

      // 2 - Image Depth

      // if (version == 7.3+){
      //   3 - null Padding to 4 bytes
      //   4 - Number of Resources
      //   }

      // 8/15 - null Padding to offset 80
      fm.seek(headerSize);

      // work out the size of the lowRes image
      if (lowResImageFormat == 13) {
        // 4 bits per pixel for DXT1
        int numPixels = lowResWidth * lowResHeight;
        int imageLength = numPixels / 2; // 8bits --> 4bits 
        fm.skip(imageLength);
      }
      else {
        // unknown image format for lowres image
      }

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

      // work out how many bytes per pixel, based on the Image Format
      float bytesPerPixel = 1;
      if (imageFormat == 0 || imageFormat == 1) { // RGBA8888, ABGR8888
        bytesPerPixel = 4;
      }
      else if (imageFormat == 2 || imageFormat == 3) { // RGB888, BGR888
        bytesPerPixel = 3;
      }
      else if (imageFormat == 4) { // RGB565
        bytesPerPixel = 2;
      }
      else if (imageFormat == 5) { // I8
        bytesPerPixel = 1;
      }
      else if (imageFormat == 6) { // IA88
        bytesPerPixel = 2;
      }
      else if (imageFormat == 7 || imageFormat == 8) { // P8, A8
        bytesPerPixel = 1;
      }
      else if (imageFormat == 9 || imageFormat == 10) { // RGB888_BLUESCREEN, BGR888_BLUESCREEN
        bytesPerPixel = 3;
      }
      else if (imageFormat == 11 || imageFormat == 12) { // ARGB8888, BGRA8888
        bytesPerPixel = 4;
      }
      else if (imageFormat == 13) { // DXT1
        bytesPerPixel = 0.5f;
      }
      else if (imageFormat == 14 || imageFormat == 15) { // DXT3, DXT5
        bytesPerPixel = 1;
      }
      else if (imageFormat == 16) { // BGRX8888
        bytesPerPixel = 4;
      }
      else if (imageFormat == 17 || imageFormat == 18 || imageFormat == 19) { // BGR565, BGRX5551, BGRA4444
        bytesPerPixel = 2;
      }
      else if (imageFormat == 20) { // DXT1_ALPHA
        bytesPerPixel = 0.5f;
      }
      else if (imageFormat == 21 || imageFormat == 22) { // BGRA5551, UV88
        bytesPerPixel = 2;
      }
      else if (imageFormat == 23) { // UVWQ8888
        bytesPerPixel = 4;
      }
      else if (imageFormat == 24 || imageFormat == 25) { // RGBA16161616F, RGBA16161616
        bytesPerPixel = 8;
      }
      else if (imageFormat == 26) { // UVLX8888
        bytesPerPixel = 4;
      }

      /*
      if (numFrames == 1 && numFaces == 1 && numSlices == 1) {
        // a simple image - work out the size of the largest mipmap and subtract it from the file length to get the offset
        int numPixels = width * height;
        int numBytes = (int) (bytesPerPixel * numPixels);
        long offset = fm.getLength() - numBytes;
        fm.seek(offset);
      }
      else if (numFaces == 1 && numSlices == 1) {
        // multiple frames - but otherwise simple. Go back a few multiples of the largest mipmap to get the offset to the first mipmap
        int numPixels = width * height;
        int numBytes = (int) (bytesPerPixel * numPixels);

        numBytes *= numFrames;

        long offset = fm.getLength() - numBytes;
        fm.seek(offset);
      }
      else {
        // Now we can read each mipmap image in the following order...
        // for each mipmap (smallest to largest)
        //   for each frame (first to last)
        //     for each face (first to last)
        //       for each Z slice (min to max)
        //         X - High Resolution Image Data

        long skipLength = 0;
        for (int m = 0; m < numMipmaps - 1; m++) { // -1 because we want the last mipmap, not skip it
          for (int f = 0; f < numFrames; f++) {
            for (int a = 0; a < numFaces; a++) {
              for (int s = 0; s < numSlices; s++) {
                // Calculate the size of the data, and add it to the total to skip

                // Work backwards in the mipmapWidth/Height arrays, as they're in largest-to-smallest order
                currentWidth = mipmapWidths[numMipmaps - m - 1];
                currentHeight = mipmapHeights[numMipmaps - m - 1];

                // DXT1/3/5 have minimum sizes for the mipmaps, because they encode blocks of 4x4.
                // ie mipmaps of 2x2 and 1x1 need to have storage size 4x4
                // Other formats don't have this requirement, as they're stored as individual pixels
                if (imageFormat == 13 || imageFormat == 14 || imageFormat == 15 || imageFormat == 20) {
                  if (currentWidth < 4) {
                    currentWidth = 4;
                  }
                  if (currentHeight < 4) {
                    currentHeight = 4;
                  }
                }

                int numPixels = currentWidth * currentHeight;
                int bytes = (int) (bytesPerPixel * numPixels);

                skipLength += bytes;
              }
            }
          }
        }
        fm.skip(skipLength);
      }
      */

      // Work out the size of the largest mipmap
      int numPixels = width * height;
      int numBytes = (int) (bytesPerPixel * numPixels);
      int totalNumBytes = 0;

      // skip calculations for simple images...
      if (numFrames != 1 || numFaces != 1 || numSlices != 1) {
        // for multiple frames, we want the first one...
        totalNumBytes += (numBytes * numFrames);

        // for multiple faces, we want the first one...
        totalNumBytes += (numBytes * numFaces);

        // for multiple slices, we want the first one...
        totalNumBytes += (numBytes * numSlices);
      }
      else {
        totalNumBytes = numBytes;
      }

      // now go to the offset to the mipmap we want
      long offset = fm.getLength() - totalNumBytes;
      fm.seek(offset);

      // Now we're at the largest image, so grab it
      ImageResource imageResource = null;
      if (imageFormat == 0) { // RGBA8888
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
      }
      else if (imageFormat == 2) { // RGB888
        imageResource = ImageFormatReader.readRGB(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB");
      }
      else if (imageFormat == 3) { // BGR888
        imageResource = ImageFormatReader.readBGR(fm, width, height);
        imageResource.addProperty("ImageFormat", "BGR");
      }
      else if (imageFormat == 4) { // RGB565
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB565");
      }
      else if (imageFormat == 5) { // I8
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.addProperty("ImageFormat", "8BitPaletted");
      }
      else if (imageFormat == 6) { // IA88
        imageResource = ImageFormatReader.readL8A8(fm, width, height);
        imageResource.addProperty("ImageFormat", "L8A8");
      }
      else if (imageFormat == 7) { // P8
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.addProperty("ImageFormat", "P8");
      }
      else if (imageFormat == 8) { // A8
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.addProperty("ImageFormat", "A8");
      }
      else if (imageFormat == 11) { // ARGB8888
        imageResource = ImageFormatReader.readARGB(fm, width, height);
        imageResource.addProperty("ImageFormat", "ARGB");
      }
      else if (imageFormat == 12) { // BGRA8888
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
        imageResource.addProperty("ImageFormat", "BGRA");
      }
      else if (imageFormat == 13) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (imageFormat == 14) { // DXT3
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT3");
      }
      else if (imageFormat == 15) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (imageFormat == 22) { // UV88
        imageResource = ImageFormatReader.readU8V8(fm, width, height);
        imageResource.addProperty("ImageFormat", "U8V8");
      }
      else if (imageFormat == 24) { // RGBA16161616F
        imageResource = ImageFormatReader.read16F16F16F16F_RGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "16F16F16F16F_RGBA");
      }
      else {
        return null; // unknown (or other) image format
      }

      imageResource.addProperty("MipmapCount", "" + numMipmaps);
      imageResource.addProperty("FrameCount", "" + numFrames);
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