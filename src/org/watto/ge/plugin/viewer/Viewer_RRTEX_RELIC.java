/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_SGA_ARCHIVE_4;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RRTEX_RELIC extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RRTEX_RELIC() {
    super("RRTEX_RELIC", "RRTEX_RELIC");
    setExtensions("rrtex");

    setGames("Age Of Empires 4");
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
      if (plugin instanceof Plugin_SGA_ARCHIVE_4) {
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
      // 4 - Version? (4)
      // 4 - Unknown (1)
      fm.skip(24);

      long arcSize = fm.getLength();

      int imageWidth = 0;
      int imageHeight = 0;
      int imageFormat = -1;
      int largestMipmapOffset = 0;

      int[] blockLengths = null;
      int[] blockDecompLengths = null;
      int totalDecompLength = 0;

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

        // X - Chunk Data
        if (chunkType.equals("DATA")) {
          if (chunkID.equals("TMAN")) {
            // 4 - Unknown (5)
            fm.skip(4);

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
            fm.skip(4);

            // 4 - Mipmap Count
            int numMipmaps = fm.readInt();

            // for each mipmap
            // 4 - Number of Compressed Blocks
            int blocksToSkip = 0;
            for (int b = 0; b < numMipmaps - 1; b++) {
              int blocksInMipmap = fm.readInt();
              FieldValidator.checkNumFiles(blocksInMipmap);
              blocksToSkip += blocksInMipmap;
            }

            // 4 - Number of Compressed Blocks (for the largest mipmap)
            int numBlocks = fm.readInt();
            FieldValidator.checkNumFiles(numBlocks);

            // skip the block in the small mipmaps
            largestMipmapOffset = 0;
            for (int b = 0; b < blocksToSkip; b++) {
              // 4 - Block Decompressed Length
              fm.skip(4);

              // 4 - Block Compressed Length
              int blockLength = fm.readInt();
              FieldValidator.checkLength(blockLength, arcSize);
              largestMipmapOffset += blockLength;
            }

            blockLengths = new int[numBlocks];
            blockDecompLengths = new int[numBlocks];

            // read the blocks for the largest mipmap
            for (int b = 0; b < numBlocks; b++) {
              // 4 - Block Decompressed Length
              int blockDecompLength = fm.readInt();
              FieldValidator.checkLength(blockDecompLength);
              blockDecompLengths[b] = blockDecompLength;

              totalDecompLength += blockDecompLength;

              // 4 - Block Compressed Length
              int blockLength = fm.readInt();
              FieldValidator.checkLength(blockLength, arcSize);
              blockLengths[b] = blockLength;
            }

          }
          else if (chunkID.equals("TDAT")) {

            // 4 - Unknown (5)
            fm.skip(4);

            // skip to the largest mipmap
            fm.skip(largestMipmapOffset);

            if (blockLengths == null) {
              continue;
            }
            int numBlocks = blockLengths.length;

            // Read in all the compressed blocks (do it here, so we can use Exporter_ZLib to read it from the file, before having to decompress
            // each block using the same plugin, which trips it up a bit for the thumbnails)
            byte[][] compBytes = new byte[numBlocks][0];
            for (int b = 0; b < numBlocks; b++) {
              int compLength = blockLengths[b];

              byte[] compBlockBytes = fm.readBytes(compLength);
              compBytes[b] = compBlockBytes;
            }

            // decompress all the blocks
            byte[] fileData = new byte[totalDecompLength];
            int decompWritePos = 0;

            Exporter_ZLib exporter = new Exporter_ZLib(); // so it doesn't conflict with the exporter that's extracting the file, for thumbnail generation

            for (int b = 0; b < numBlocks; b++) {
              int compLength = blockLengths[b];
              int decompLength = blockDecompLengths[b];

              byte[] compBlockBytes = compBytes[b];

              if (compLength == decompLength) {
                // raw data
                for (int g = 0; g < decompLength; g++) {
                  fileData[decompWritePos++] = compBlockBytes[g];
                }
              }
              else {
                // compressed
                FileManipulator blockFM = new FileManipulator(new ByteBuffer(compBlockBytes));
                exporter.open(blockFM, compLength, decompLength);

                for (int g = 0; g < decompLength; g++) {
                  if (exporter.available()) { // make sure we read the next bit of data, if required
                    fileData[decompWritePos++] = (byte) exporter.read();
                  }
                }

                exporter.close();
                blockFM.close();
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

            // Now read the image
            ImageResource imageResource = null;

            if (imageFormat == 18) {
              imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT1");
            }
            else if (imageFormat == 19) {
              imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT1");
            }
            else if (imageFormat == 22) {
              imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT5");
            }
            else if (imageFormat == 23) {
              imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
              imageResource.addProperty("ImageFormat", "DXT5");
            }
            else if (imageFormat == 28) { // BC7 in BGRA format
              imageResource = ImageFormatReader.readBC7(fm, imageWidth, imageHeight);
              imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
              imageResource.addProperty("ImageFormat", "BC7");
            }
            else if (imageFormat == 29) { // BC7 in BGRA format
              imageResource = ImageFormatReader.readBC7(fm, imageWidth, imageHeight);
              imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
              imageResource.addProperty("ImageFormat", "BC7");
            }
            else {
              ErrorLogger.log("[Viewer_RRTEX_RELIC]: Unsupported Image Format: " + imageFormat);

              //FileManipulator tempOut = new FileManipulator(new File("C:\\temp.out"), true);
              //tempOut.writeBytes(fileData);
              //tempOut.close();
            }

            //if (imageResource != null) {
            // flip vertically
            //  imageResource = ImageFormatReader.flipVertically(imageResource);
            //}

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

          // X - Chunk Name
          // 1 - null Chunk Name Terminator (only exists if Name Length != 0)
          fm.skip(nameLength);
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