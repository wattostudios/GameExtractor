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
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BIN_37;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_37_BINTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_37_BINTEX() {
    super("BIN_37_BINTEX", "BIN_37_BINTEX Image");
    setExtensions("bin_tex");

    setGames("Michael Jackson: The Experience");
    setPlatforms("Wii");
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
      if (plugin instanceof Plugin_BIN_37) {
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

      // 2 - Image Width
      int width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = fm.readShort();
      FieldValidator.checkHeight(height);

      // X - Image Data (CMPR format)
      ImageResource imageResource = ImageFormatReader.readCMPR(fm, width, height);
      imageResource = ImageFormatReader.flipVertically(imageResource);

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