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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PKG_PPKG;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PKG_PPKG_PTX_RIFF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PKG_PPKG_PTX_RIFF() {
    super("PKG_PPKG_PTX_RIFF", "WRC 7 PTX Image");
    setExtensions("ptx");

    setGames("WRC 7");
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_PKG_PPKG) {
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
      if (fm.readString(4).equals("RIFF")) {
        rating += 24; // so it doesn't match WAV files
      }
      else {
        rating = 0;
      }

      // 4 - File Length
      if (fm.readInt() + 8 == fm.getLength()) {
        rating += 5;
      }

      if (fm.readString(8).equals("PA__Head")) {
        rating += 5;
      }

      if (fm.readInt() == 78) {
        rating += 5;
      }

      if (fm.readString(12).equals("PATX-SP2ENDI")) {
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

      // 4 - Header (RIFF)
      // 4 - File Length [+8]
      // 4 - Details (PA__Head)
      // 4 - Unknown (78)
      // 12 - Header (PATX-SP2ENDI)
      // 4 - Unknown (3)
      // 8 - null
      // 4 - Platform Name Length (2)
      // 2 - Platform Name (PC)
      // 2 - Unknown (7)
      // 8 - null
      fm.skip(60);

      // 4 - Image Format (RGBA/DXT1/DXT5/BC5S)
      String imageFormat = fm.readString(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (1)
      // 4 - Mipmap Count
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (14/13)
      // 2 - Unknown
      // 4 - Header (LIST)
      // 4 - Block Length?
      // 4 - Header (Surf)
      // 4 - Header (LIST)
      fm.skip(42);

      long arcSize = fm.getLength();

      // 4 - Block Length (not including these 2 fields)
      int blockLength = fm.readInt();
      FieldValidator.checkLength(blockLength, arcSize);

      // 4 - Details (LevI)
      fm.skip(4);

      long endOffset = fm.getOffset() + blockLength - 50; // just to make it a bit smaller

      byte[] blockBytes = new byte[blockLength];
      int writePos = 0;

      while (fm.getOffset() < endOffset) {
        // 4 - Details (PixI)
        fm.skip(4);

        // 4 - Image Data Length
        int bitLength = fm.readInt();
        FieldValidator.checkLength(bitLength, blockLength);

        // X - Pixels
        byte[] bitBytes = fm.readBytes(bitLength);
        System.arraycopy(bitBytes, 0, blockBytes, writePos, bitLength);
        writePos += bitLength;
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(blockBytes));

      ImageResource imageResource = null;
      if (imageFormat.equals("RGBA")) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("BC5S")) {
        imageResource = ImageFormatReader.readBC5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PKG_PPKG_PTX_RIFF] Unknown Image Format: " + imageFormat);
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

}