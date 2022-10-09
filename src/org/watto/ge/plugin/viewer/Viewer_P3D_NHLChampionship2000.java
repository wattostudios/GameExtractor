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
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_P3D_NHLChampionship2000 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_P3D_NHLChampionship2000() {
    super("P3D_NHLChampionship2000", "NHL Championship 2000 P3D Image");
    setExtensions("p3d");

    setGames("NHL Championship 2000");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 2 - Code
      if (fm.readShort() == -252) {
        rating += 5;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }
      else {
        rating = 0;
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

      // 2 - Unknown (-252)
      // 4 - File Length
      fm.skip(6);

      short width = 0;
      short height = 0;
      int[] palette = null;

      while (fm.getOffset() < arcSize) {
        // 2 - Code (12320)
        short code = fm.readShort();

        // 4 - Block Length (including these 6 bytes)
        int blockLength = fm.readInt() - 6;
        FieldValidator.checkLength(blockLength, arcSize);

        // X - Block Data
        if (code == 12353) {
          // IMAGE BLOCK

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          fm.skip(nameLength);

          // 4 - Unknown (640)
          // 4 - Unknown (480)
          fm.skip(8);

          // 1 - Name Length
          nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          fm.skip(nameLength);
        }
        else if (code == 12304) {
          // IMAGE DETAILS BLOCK

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          fm.skip(nameLength);

          // 4 - null
          fm.skip(4);

          // 2 - Image Width
          width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - Bit Depth? (8)
          fm.skip(2);
        }
        else if (code == 12305) {
          // COLOR PALETTE BLOCK

          // for each color (256)
          //   3 - Color (RGB)
          palette = ImageFormatReader.readPaletteRGB(fm, 256);

        }
        else if (code == 12306) {
          // IMAGE DATA

          // for each pixel
          //   1 - Palette Index
          if (width == 0 || height == 0 || palette == null) {
            return null;
          }

          ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          imageResource = ImageFormatReader.flipVertically(imageResource);

          fm.close();

          return imageResource;

        }
        else if (code == 12313) {
          // COMPACT DETAILS BLOCK

          // 1 - String Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - String
          fm.skip(nameLength);

          // 4 - null
          fm.skip(4);

          // 2 - Image Width
          width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - Bit Depth? (8)
          fm.skip(2);
        }
        else {
          fm.skip(blockLength);
        }
      }

      return null;

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

      int width = im.getWidth();
      int height = im.getHeight();

      if (width == -1 || height == -1) {
        return;
      }

      im.changeColorCount(256);

      String filename = FilenameSplitter.getFilename(fm.getFile().getName());
      FieldValidator.checkFilename(filename);

      int filenameLength = filename.length();

      // Work out the length of each block (and the file overall)
      int imageDataBlockLength = 6 + (width * height);
      int paletteBlockLength = 6 + (256 * 3);
      int imageDetailsBlockLength = 6 + 1 + filenameLength + 10 + paletteBlockLength + imageDataBlockLength;
      int imageBlockLength = 6 + 1 + filenameLength + 8 + 1 + filenameLength + imageDetailsBlockLength;
      int descriptionBlockLength = 6 + 2 + 3 + (14 * 3); // Game Extractor
      int unknownBlockLength = 6 + 1 + filenameLength + 4 + 9 + 15 + 14;
      int fileLength = 6 + unknownBlockLength + descriptionBlockLength + imageBlockLength;

      // HEADER
      // 2 - Unknown (-252)
      fm.writeShort(-252);

      // 4 - File Length
      fm.writeInt(fileLength);

      // UNKNOWN BLOCK
      // 2 - Code (12320)
      fm.writeShort(12320);

      // 4 - Block Length (including these 6 bytes)
      fm.writeInt(unknownBlockLength);

      // 1 - Name Length
      fm.writeByte(filenameLength);

      // X - Name
      fm.writeString(filename);

      // 4 - null
      fm.writeInt(0);

      // INNER BLOCK 1
      //   2 - Code (12321)
      fm.writeShort(12321);

      //   4 - Block Length (including these 6 bytes) (9)
      fm.writeInt(9);

      //   3 - Unknown (-1)
      fm.writeByte(255);
      fm.writeByte(255);
      fm.writeByte(255);

      // INNER BLOCK 2
      //   2 - Code (12322)
      fm.writeShort(12322);

      //   4 - Block Length (including these 6 bytes) (15)
      fm.writeInt(15);

      //   1 - Unknown Length (8)
      fm.writeByte(8);

      //   8 - Unknown (bytes 127,239,127,239,127,239,127,239)
      fm.writeByte(127);
      fm.writeByte(239);
      fm.writeByte(127);
      fm.writeByte(239);
      fm.writeByte(127);
      fm.writeByte(239);
      fm.writeByte(127);
      fm.writeByte(239);

      // INNER BLOCK 3
      //   2 - Code (12324)
      fm.writeShort(12324);

      //   4 - Block Length (including these 6 bytes) (14)
      fm.writeInt(14);

      //   4 - Unknown (7)
      fm.writeInt(7);

      //   4 - Unknown (8)
      fm.writeInt(8);

      // DESCRIPTION BLOCK
      // 2 - Code (28672)
      fm.writeShort(28672);

      // 4 - Block Length (including these 6 bytes)
      fm.writeInt(descriptionBlockLength);

      // 2 - Number of Strings (3)
      fm.writeShort(3);

      // for each String
      //   1 - String Length
      //   X - String
      fm.writeByte(14);
      fm.writeString("Game Extractor");
      fm.writeByte(14);
      fm.writeString("Game Extractor");
      fm.writeByte(14);
      fm.writeString("Game Extractor");

      // IMAGE BLOCK  
      // 2 - Code (12353)
      fm.writeShort(12353);

      // 4 - Block Length (including these 6 bytes)
      fm.writeInt(imageBlockLength);

      // 1 - Name Length
      fm.writeByte(filenameLength);

      // X - Name
      fm.writeString(filename);

      // 4 - Unknown (640)
      fm.writeInt(640);

      // 4 - Unknown (480)
      fm.writeInt(480);

      // 1 - Name Length
      fm.writeByte(filenameLength);

      // X - Name
      fm.writeString(filename);

      // IMAGE DETAILS BLOCK
      // 2 - Code (12304)
      fm.writeShort(12304);

      // 4 - Block Length (including these 6 bytes)
      fm.writeInt(imageDetailsBlockLength);

      // 1 - Name Length
      fm.writeByte(filenameLength);

      // X - Name
      fm.writeString(filename);

      // 4 - null
      fm.writeInt(0);

      // 2 - Image Width
      fm.writeShort(width);

      // 2 - Image Height
      fm.writeShort(height);

      // 2 - Bit Depth? (8)
      fm.writeShort(8);

      // COLOR PALETTE BLOCK
      // 2 - Code (12305)
      fm.writeShort(12305);

      // 4 - Block Length (including these 6 bytes) (774)
      fm.writeInt(paletteBlockLength);

      // for each color (256)
      //   3 - Color (RGB)
      int[] palette = im.getPalette();
      ImageFormatWriter.writePaletteRGB(fm, palette);

      // IMAGE DATA
      // 2 - Code (12306)
      fm.writeShort(12306);

      // 4 - Block Length (including these 6 bytes)
      fm.writeInt(imageDataBlockLength);

      // for each pixel
      //   1 - Palette Index

      // NEED TO FLIP THE IMAGE VERTICALLY
      int[] pixels = im.getPixels();
      for (int h = height - 1; h >= 0; h--) {
        int pixelStart = h * width;
        for (int w = 0; w < width; w++) {
          fm.writeByte(pixels[pixelStart + w]);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}