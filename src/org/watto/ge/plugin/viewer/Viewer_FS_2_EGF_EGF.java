/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_FS_2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_FS_2_EGF_EGF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_FS_2_EGF_EGF() {
    super("FS_2_EGF_EGF", "FS_2_EGF_EGF Image");
    setExtensions("egf");

    setGames("Disney's Tarzan");
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
      if (plugin instanceof Plugin_FS_2) {
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

      // 4 - Header ("EGF" + (byte)2)
      if (fm.readInt() == 38160197) {
        rating += 50;
      }
      else {
        rating = 0;
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

      // 4 - Header ("EGF" + (byte)2)
      fm.skip(4);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      int blockSize = 256;

      // work out the dimensions of the image, in blocks of 256x256
      int widthPadded = width + ArchivePlugin.calculatePadding(width, blockSize);
      int heightPadded = height + ArchivePlugin.calculatePadding(height, blockSize);

      int widthBlocks = widthPadded / blockSize;
      int heightBlocks = heightPadded / blockSize;

      int numBlocks = widthBlocks * heightBlocks;

      int[] lengths = new int[numBlocks];
      int[] offsets = new int[numBlocks];
      int offset = 72;
      for (int i = 0; i < numBlocks; i++) {
        // 4 - Piece Data Length (131072 = uncompressed, anything else = compressed)
        int length = fm.readInt();

        if (length != 131072) {
          ErrorLogger.log("[FS_2_EGF_EGF] Unknown image compression");
          return null;
        }

        lengths[i] = length;
        offsets[i] = offset;

        offset += length;
      }

      int numPixels = widthPadded * heightPadded;
      int[] pixels = new int[numPixels];

      int blockPixelLength = 256 * 256;

      // read each block
      for (int y = 0; y < heightBlocks; y++) {
        for (int x = 0; x < widthBlocks; x++) {
          int blockNum = y * widthBlocks + x;

          fm.relativeSeek(offsets[blockNum]);

          // X - Piece Image Data (256x256x2) (RGB555)
          int[] blockPixels = ImageFormatReader.readRGB555(fm, blockSize, blockSize).getImagePixels();

          // put the block in the right place in the image
          int startPos = y * widthBlocks * blockPixelLength + x * blockSize;
          for (int y2 = 0; y2 < blockSize; y2++) {
            //System.out.println("Writing out to " + startPos + " out of " + numPixels);
            System.arraycopy(blockPixels, y2 * blockSize, pixels, startPos, blockSize);
            startPos += widthBlocks * blockSize;
          }
        }
      }

      ImageResource imageResource = new ImageResource(pixels, widthPadded, heightPadded);

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

}