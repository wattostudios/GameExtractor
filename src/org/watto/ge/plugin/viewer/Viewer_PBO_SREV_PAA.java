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
import org.watto.ge.plugin.archive.Plugin_PBO;
import org.watto.ge.plugin.archive.Plugin_PBO_SREV;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PBO_SREV_PAA extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PBO_SREV_PAA() {
    super("PBO_SREV_PAA", "ArmA PAA Image (PBO_SREV_PAA)");
    setExtensions("paa");

    setGames("ArmA",
        "ArmA 2",
        "Arma 3",
        "Argo");
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
      if (plugin instanceof Plugin_PBO_SREV || plugin instanceof Plugin_PBO) {
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

      // 2 - Image Format (65281=DXT1, 65282=DXT2, 65283=DXT3, 65284=DXT4, 65285=DXT5, 17476=RGBA4444, 5461=RGBA5551, 34952=RGBA8888, 32896=Gray with Alpha)
      int imageFormat = ShortConverter.unsign(fm.readShort());
      if (imageFormat == 65281 || imageFormat == 65282 || imageFormat == 65283 || imageFormat == 65284 || imageFormat == 65285 || imageFormat == 17476 || imageFormat == 5461 || imageFormat == 34952 || imageFormat == 32896) {
        rating += 25;
      }

      // 4 - Tag Header
      if (fm.readString(4).equals("GGAT")) {
        rating += 25;
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

      // 2 - Image Format (65281=DXT1, 65282=DXT2, 65283=DXT3, 65284=DXT4, 65285=DXT5, 17476=RGBA4444, 5461=RGBA5551, 34952=RGBA8888, 32896=Gray with Alpha)
      int imageFormat = ShortConverter.unsign(fm.readShort());
      if (imageFormat == 65281 || imageFormat == 65282 || imageFormat == 65283 || imageFormat == 65284 || imageFormat == 65285 || imageFormat == 17476 || imageFormat == 5461 || imageFormat == 34952 || imageFormat == 32896) {
        // ok
      }
      else {
        // unknown format
        ErrorLogger.log("[Viewer_PBO_SREV_PAA] Unknown Image Format: " + imageFormat);
        return null;
      }

      while (fm.readString(4).equals("GGAT")) {
        // 4 - Tag Type String
        String tagType = fm.readString(4);
        if (tagType.equals("ZIWS")) {
          // can't handle swizzled images
          //return null;
        }

        // 4 - Length of Tag Data
        int taglength = fm.readInt();
        // X - Tag Data
        fm.skip(taglength);
      }

      fm.relativeSeek(fm.getOffset() - 4); // go back - the 4 "tag" bytes are actually the palette and image width/height

      // 2 - Number of Colors
      // X - palette
      fm.skip(fm.readShort() * 3);

      boolean compressed = false;

      // 2 - Width
      short width = fm.readShort();
      if (width < 0) {
        // possibly compressed
        compressed = true;
        width &= 32767;
      }
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 3 - Image Data Length
      byte[] lengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
      int dataLength = IntConverter.convertLittle(lengthBytes);

      // X - Image Data
      if (compressed && (imageFormat == 65281 || imageFormat == 65282 || imageFormat == 65283 || imageFormat == 65284 || imageFormat == 65285)) {
        // LZO Compression - decompress the file and use that to read from later on

        int decompressedLength = width * height;
        if (imageFormat == 65281) {
          // DXT1 is only 4 bytes per pixel, so halve it
          decompressedLength /= 2;
        }

        byte[] decompBytes = new byte[decompressedLength];
        int decompPos = 0;

        Exporter_LZO_SingleBlock exporter = Exporter_LZO_SingleBlock.getInstance();
        //exporter.open(fm, dataLength);
        exporter.open(fm, dataLength, decompressedLength);
        while (exporter.available()) {
          decompBytes[decompPos] = (byte) exporter.read();
          decompPos++;
        }
        exporter.close();

        fm.close(); // close the actual Archive where we got the data from
        fm = new FileManipulator(new ByteBuffer(decompBytes)); // set up the ImageReader to read from the decompressed data instead

      }
      else if (imageFormat == 17476 || imageFormat == 5461 || imageFormat == 34952 || imageFormat == 32896) {
        // LZSS Compression - decompress the file and use that to read from later on

        int decompressedLength = width * height * 2;
        if (imageFormat == 34952) {
          // 4-bytes per pixel for RGBA8888, 2-bytes per pixel for all others
          decompressedLength *= 2;
        }

        byte[] decompBytes = new byte[decompressedLength];
        int decompPos = 0;

        Exporter_LZSS exporter = Exporter_LZSS.getInstance();
        //exporter.open(fm, dataLength);
        exporter.open(fm, decompressedLength);
        while (exporter.available()) {
          decompBytes[decompPos] = (byte) exporter.read();
          decompPos++;
        }
        exporter.close();

        fm.close(); // close the actual Archive where we got the data from

        //FileManipulator tempFM = new FileManipulator(new File("c:\\out_decomp.paa"), true);
        //tempFM.writeBytes(decompBytes);
        //tempFM.close();

        fm = new FileManipulator(new ByteBuffer(decompBytes)); // set up the ImageReader to read from the decompressed data instead
      }

      ImageResource imageResource = null;

      if (imageFormat == 65281) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "DXT1");
      }
      else if (imageFormat == 65282) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "DXT2");
      }
      else if (imageFormat == 65283) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "DXT3");
      }
      else if (imageFormat == 65284) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "DXT4");
      }
      else if (imageFormat == 65285) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "DXT5");
      }
      else if (imageFormat == 17476) {
        imageResource = ImageFormatReader.readRGBA4444(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "RGBA4444");
      }
      else if (imageFormat == 5461) {
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "RGBA5551");
      }
      else if (imageFormat == 34952) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "RGBA");
      }
      else if (imageFormat == 32896) {
        imageResource = ImageFormatReader.readL8A8(fm, width, height);
        imageResource.addProperty("ImageFormat", "" + "L8A8");
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

      return imageResource;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

}