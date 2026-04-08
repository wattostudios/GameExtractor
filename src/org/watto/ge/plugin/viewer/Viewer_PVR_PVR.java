/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PVR_PVR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PVR_PVR() {
    super("PVR_PVR", "PVR (PowerVR) Image");
    setExtensions("pvr");

    setGames("PVR (PowerVR) Image");
    setPlatforms("PC", "Android");
    setStandardFileFormat(true);
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 3 - Header (PVR)
      if (fm.readString(3).equals("PVR")) {
        rating += 50;
      }

      // 1 - Version (3)
      if (fm.readByte() == 3) {
        rating += 5;
      }

      fm.skip(4);

      // 8 - Pixel Format
      int pixelFormat = fm.readInt();
      if (FieldValidator.checkRange(pixelFormat, 0, 54) && fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(8);

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
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

      // 3 - Header (PVR)
      // 1 - Version (3)
      // 4 - Flags
      fm.skip(8);

      // 8 - Pixel Format
      int imageFormat = fm.readInt();
      FieldValidator.checkRange(imageFormat, 0, 54);
      fm.skip(4);

      // 4 - Color Space
      // 4 - Channel Type
      fm.skip(8);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Color Depth
      // 4 - Number of Surfaces
      // 4 - Number of Faces
      // 4 - Mipmap Count
      fm.skip(16);

      // 4 - Metadata Size (can be null)
      int metadataLength = fm.readInt();
      FieldValidator.checkLength(metadataLength, arcSize);

      // X - Metadata
      fm.skip(metadataLength);

      // X - Image Data
      ImageResource imageResource = null;

      if (imageFormat == 6) {
        // ETC1
        imageResource = ImageFormatReader.readETC1_RGB8(fm, width, height);
      }
      else if (imageFormat == 7) {
        // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 9) {
        // DXT3
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 11) {
        // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 12) {
        // BC4
        imageResource = ImageFormatReader.readBC4(fm, width, height);
      }
      else if (imageFormat == 13) {
        // BC5
        imageResource = ImageFormatReader.readBC5(fm, width, height);
      }
      else if (imageFormat == 14) {
        // BC6
        imageResource = ImageFormatReader.readBC6H(fm, width, height);
      }
      else if (imageFormat == 15) {
        // BC7
        imageResource = ImageFormatReader.readBC7(fm, width, height);
      }
      else if (imageFormat == 23) {
        // ETC2 RGBA
        imageResource = ImageFormatReader.readETC2_RGBA8(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PVR_PVR] Unsupported Image Format: " + imageFormat);
        return null;
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
      int fileID = 0;
      int hash = 0;
      String filename = "";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        fileID = imageResource.getProperty("FileID", 0);
        hash = imageResource.getProperty("Hash", 0);
        filename = imageResource.getProperty("Filename", "");
      }

      if (filename.equals("")) {
        filename = fm.getFile().getName();
      }
      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      // work out the file length
      long fileLength = 28 + filename.length() + 1 + (mipmapCount * 4);
      for (int i = 0; i < mipmapCount; i++) {
        int mipmapHeight = mipmaps[i].getHeight();
        int mipmapWidth = mipmaps[i].getWidth();

        // This is DXT3 format - width/height have to be a minimum of 4 pixels each (smaller images have padding around them to 4x4 size)
        if (mipmapHeight < 4) {
          mipmapHeight = 4;
        }
        if (mipmapWidth < 4) {
          mipmapWidth = 4;
        }

        // DXT3 is 1 byte per pixel
        int byteCount = (mipmapHeight * mipmapWidth);
        fileLength += byteCount;
      }

      // 4 - Header (3TXD)
      fm.writeString("3TXD");

      // 4 - File Length (including all these header fields)
      fm.writeInt(fileLength);

      // 4 - File ID
      fm.writeInt(fileID);

      // 2 - Image Height
      fm.writeShort((short) imageHeight);

      // 2 - Image Width
      fm.writeShort((short) imageWidth);

      // 4 - Number Of Mipmaps
      fm.writeInt(mipmapCount);

      // 4 - File Type? (28)
      fm.writeInt(28);

      // 4 - Hash?
      fm.writeInt(hash);

      // X - Filename
      // 1 - null Filename Terminator
      fm.writeString(filename);
      fm.writeByte(0);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        int mipmapHeight = mipmap.getHeight();
        int resizedHeight = mipmapHeight;
        if (resizedHeight < 4) {
          resizedHeight = 4;
        }

        int mipmapWidth = mipmap.getWidth();
        int resizedWidth = mipmapWidth;
        if (resizedWidth < 4) {
          resizedWidth = 4;
        }

        int pixelCount = resizedWidth * resizedHeight;

        // 4 - Data Length
        fm.writeInt(pixelCount); // DXT3 is 1bytes per pixel

        int pixelLength = mipmap.getNumPixels();
        if (pixelLength < pixelCount) {
          // one of the smallest mipmaps (eg 1x1 or 2x2) --> needs to be resized to 4x4
          int[] oldPixels = mipmap.getImagePixels();
          int[] newPixels = new int[pixelCount]; // minimum of 4x4, but if one dimension is already > 4, can be larger

          for (int h = 0; h < resizedHeight; h++) {
            for (int w = 0; w < resizedWidth; w++) {
              if (h < mipmapHeight && w < mipmapWidth) {
                // copy the pixel from the original
                newPixels[h * resizedWidth + w] = oldPixels[h * mipmapWidth + w];
              }
              else {
                newPixels[h * resizedWidth + w] = 0;
              }
            }
          }
          mipmap.setPixels(newPixels);
          mipmap.setWidth(resizedWidth);
          mipmap.setHeight(resizedHeight);
        }

        // X - Pixels
        ImageFormatWriter.writeDXT3(fm, mipmap);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}