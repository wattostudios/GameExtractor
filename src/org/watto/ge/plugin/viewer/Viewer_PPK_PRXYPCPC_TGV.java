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
import org.watto.ge.plugin.archive.Plugin_DAT_EDAT_2;
import org.watto.ge.plugin.archive.Plugin_PPK_PRXYPCPC;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PPK_PRXYPCPC_TGV extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PPK_PRXYPCPC_TGV() {
    super("PPK_PRXYPCPC_TGV", "Wargame: Red Dragon TGV Image");
    setExtensions("tgv");

    setGames("Wargame: Red Dragon");
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
      if (plugin instanceof Plugin_PPK_PRXYPCPC || plugin instanceof Plugin_DAT_EDAT_2) {
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

      fm.skip(8);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

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
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      long startOffset = fm.getOffset();

      // 8 - Unknown (2)
      fm.skip(8);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Image Width
      // 4 - Image Height
      fm.skip(8);

      // 2 - Number of Mipmaps
      int mipmapCount = fm.readShort();
      FieldValidator.checkRange(mipmapCount, 1, 20);

      // 2 - Format Name Length
      int nameLength = fm.readShort();
      FieldValidator.checkRange(nameLength, 1, 20);

      // X - Format Name (DXT1_SRGB / DXT5_SRGB)
      String imageFormat = fm.readString(nameLength);

      // 0-3 - null Padding to a multiple of 4 bytes
      fm.skip(ArchivePlugin.calculatePadding(nameLength, 4));

      // 16 - CRC?
      fm.skip(16);

      // skip over the small mipmaps
      fm.skip((mipmapCount - 1) * 4);

      // 4 - Image Data Offset (relative to the start of this file data)
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // skip over the small mipmaps
      fm.skip((mipmapCount - 1) * 4);

      // 4 - Image Data Length
      int dataLength = fm.readInt();
      FieldValidator.checkLength(dataLength, arcSize);

      // X - Image Data
      int skipSize = (int) (dataOffset - (fm.getOffset() - startOffset));
      if (skipSize > 0) {
        fm.skip(skipSize);
      }

      byte[] dataBytes = fm.readBytes(dataLength);

      fm.close();
      fm = new FileManipulator(new ByteBuffer(dataBytes));

      if (fm.readString(4).equals("ZIPO")) {
        // compressed data

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // Do the decompression
        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(fm, dataLength - 8, decompLength); // we've already read 8 bytes for the compression header

        byte[] decompBytes = new byte[decompLength];
        int decompPos = 0;
        while (exporter.available()) {
          int currentByte = exporter.read();
          //System.out.println(currentByte + "\t" + ((byte) currentByte));
          decompBytes[decompPos++] = (byte) currentByte;
        }

        exporter.close();

        fm.close();
        fm = new FileManipulator(new ByteBuffer(decompBytes));
      }
      else {
        fm.relativeSeek(0);
      }

      ImageResource imageResource = null;
      if (imageFormat.equals("DXT1_SRGB") || imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT5_SRGB") || imageFormat.equals("DXT5") || imageFormat.equals("DXT5_LIN")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("A8R8G8B8_LIN") || imageFormat.equals("A8R8G8B8_SRGB")) {
        imageResource = ImageFormatReader.readARGB(fm, width, height);
      }
      else if (imageFormat.equals("L8")) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_PPK_PRXYPCPC_TGV] Unknown image format: " + imageFormat);
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