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
import org.watto.ge.plugin.archive.Plugin_SGA_ARCHIVE_2;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RGT_RELIC extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RGT_RELIC() {
    super("RGT_RELIC", "RGT_RELIC");
    setExtensions("rgt");

    setGames("Company of Heroes 2");
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
      if (plugin instanceof Plugin_SGA_ARCHIVE_2) {
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

      // 12 - Header ("Relic Chunky")
      if (fm.readString(12).equals("Relic Chunky")) {
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

      // 12 - Header ("Relic Chunky")
      // 4 - Unknown (1706509)
      // 4 - Unknown (3)
      // 4 - Unknown (1)
      // 4 - First Chunk Offset? (36)
      // 4 - Unknown (28)
      // 4 - Unknown (1)
      fm.skip(36);

      long arcSize = fm.getLength();

      int imageWidth = 0;
      int imageHeight = 0;
      int imageFormat = -1;
      int mipmapCount = 0;
      int largestMipmapOffset = 0;
      int largestLength = 0;
      int largestDecompLength = 0;

      while (fm.getOffset() < arcSize) {

        // 4 - Chunk Type ("DATA" or "FOLD")
        String chunkType = fm.readString(4);

        // 4 - Chunk ID Name (eg "FBIF", "TFMT", ...)
        String chunkID = fm.readString(4);

        // 4 - Chunk Version
        fm.skip(4);

        // 4 - Chunk Data Length (not including these header fields)
        int dataLength = fm.readInt();
        FieldValidator.checkLength(dataLength, arcSize);

        // 4 - Chunk Name Length (including the null Terminator)
        int nameLength = fm.readInt();
        FieldValidator.checkLength(nameLength, arcSize);

        // 4 - Unknown (0/-1)
        // 4 - null
        fm.skip(8);

        // X - Chunk Name
        // 1 - null Chunk Name Terminator (only exists if Name Length != 0)
        fm.skip(nameLength);

        // X - Chunk Data
        if (chunkType.equals("DATA")) {
          if (chunkID.equals("TFMT")) {
            // 4 - Image Width
            imageWidth = fm.readInt();
            FieldValidator.checkWidth(imageWidth);

            // 4 - Image Height
            imageHeight = fm.readInt();
            FieldValidator.checkHeight(imageHeight);

            // 4 - Unknown (1)
            // 4 - Unknown (2)
            fm.skip(8);

            // 4 - Image Format
            imageFormat = fm.readInt();

            // 4 - null
            // 1 - Unknown (1)
            fm.skip(5);
          }
          else if (chunkID.equals("TMAN")) {
            // 4 - Number of Mipmaps
            mipmapCount = fm.readInt();
            FieldValidator.checkRange(mipmapCount, 0, 30);

            // skip over all the small mipmaps, but calculate the offset
            for (int i = 0; i < mipmapCount - 1; i++) {
              // 4 - Decompressed Data Length
              fm.skip(4);

              // 4 - Compressed Data Length
              largestMipmapOffset += fm.readInt();
            }

            // Now read the details for the largest mipmap

            // 4 - Decompressed Data Length
            largestDecompLength = fm.readInt();
            FieldValidator.checkLength(largestDecompLength);

            // 4 - Compressed Data Length
            largestLength = fm.readInt();
            FieldValidator.checkLength(largestLength, arcSize);
          }
          else if (chunkID.equals("TDAT")) {
            fm.skip(largestMipmapOffset);

            if (largestLength != largestDecompLength) {
              // compressed

              // X - Mipmap Image Data (ZLib Compression)
              byte[] fileData = new byte[largestDecompLength];
              int decompWritePos = 0;

              Exporter_ZLib exporter = Exporter_ZLib.getInstance();
              exporter.open(fm, largestLength, largestDecompLength);

              for (int b = 0; b < largestDecompLength; b++) {
                if (exporter.available()) { // make sure we read the next bit of data, if required
                  fileData[decompWritePos++] = (byte) exporter.read();
                }
              }

              // open the decompressed file data for processing
              fm.close();
              fm = new FileManipulator(new ByteBuffer(fileData));

              // 4 - Unknown
              // 4 - Image Width
              // 4 - Image Height
              // 4 - Data Length
              fm.skip(16);

            }
            else {
              // not compressed

              // X - Raw Mipmap Image Data
            }

            // Now read the image
            ImageResource imageResource = null;

            if (imageFormat == 0) {
              // RG
              imageResource = ImageFormatReader.readRG(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "RG");
            }
            else if (imageFormat == 1) {
              // RGB
              imageResource = ImageFormatReader.readRGB(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "RGB");
            }
            else if (imageFormat == 2) {
              // RGBA
              imageResource = ImageFormatReader.readRGBA(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "RGBA");
            }
            else if (imageFormat == 3) {
              // RGBAmask
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: RGBAmask");
            }
            else if (imageFormat == 4) {
              // L
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: L");
            }
            else if (imageFormat == 5) {
              // LA
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: LA");
            }
            else if (imageFormat == 6) {
              // A
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: A");
            }
            else if (imageFormat == 7) {
              // UV
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: UV");
            }
            else if (imageFormat == 8) {
              // UVWQ
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: UVWQ");
            }
            else if (imageFormat == 9) {
              // Rf
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: Rf");
            }
            else if (imageFormat == 10) {
              // RGf
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: RGf");
            }
            else if (imageFormat == 11) {
              // RGf
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: RGBAf");
            }
            else if (imageFormat == 12) {
              // depth
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: depth");
            }
            else if (imageFormat == 13) {
              // DXT1
              imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT1");
            }
            else if (imageFormat == 14) {
              // DXT3
              imageResource = ImageFormatReader.readDXT3(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT3");
            }
            else if (imageFormat == 15) {
              // DXT5
              imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT5");
            }
            else if (imageFormat == 16) {
              // DXT7
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: DXT7");
            }
            else if (imageFormat == 17) {
              // SHADOWMAP
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: SHADOWMAP");
            }
            else if (imageFormat == 18) {
              // NULL
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: NULL");
            }
            else if (imageFormat == 19) {
              // DepthStencil
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: DepthStencil");
            }
            else if (imageFormat == 20) {
              // RGB_sRGB
              imageResource = ImageFormatReader.readRGB(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "RGB");
            }
            else if (imageFormat == 21) {
              // RGBA_sRGB
              imageResource = ImageFormatReader.readRGBA(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "RGBA");
            }
            else if (imageFormat == 22) {
              // DXT1_sRGB
              imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT1");
            }
            else if (imageFormat == 23) {
              // DXT3_sRGB
              imageResource = ImageFormatReader.readDXT3(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT3");
            }
            else if (imageFormat == 24) {
              // DXT5_sRGB
              imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT5");
            }
            else if (imageFormat == 25) {
              // DXT7_sRGB
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: DXT7_sRGB");
            }
            else if (imageFormat == 26) {
              // Invalid
              ErrorLogger.log("[Viewer_RGT_RELIC]: Unsupported Image Format: Invalid");
            }

            if (imageResource != null) {
              // flip vertically
              imageResource = ImageFormatReader.flipVertically(imageResource);
            }

            // Return the image (and exist the while loop)
            fm.close();
            return imageResource;
          }
          else {
            // skip over this data - we don't want it
            fm.skip(dataLength);
          }
        }
        else if (chunkType.equals("FOLD")) {
          // just move on to the next chunk, which is nested in this one
        }

      }

      fm.close();

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

  }

}