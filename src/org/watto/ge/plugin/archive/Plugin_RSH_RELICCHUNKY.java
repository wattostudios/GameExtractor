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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RSH_RELICCHUNKY extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RSH_RELICCHUNKY() {

    super("RSH_RELICCHUNKY", "RSH_RELICCHUNKY");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Company of Heroes",
        "Company of Heroes 2",
        "Warhammer 40k: Dawn Of War");
    setExtensions("rgt", "rsh", "wtp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      // Header
      if (fm.readString(12).equals("Relic Chunky")) {
        rating += 50;
      }

      fm.skip(4);

      // Version Fields?
      if (fm.readInt() == 1 && fm.readInt() == 1) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int imageWidth = 0;

  int imageHeight = 0;

  int imageFormat = 0;

  int archiveVersion = 1;

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();
      TaskProgressManager.setMaximum(arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      realNumFiles = 0;

      // 14 - Header ("Relic Chunky" + (bytes)13,10)
      // 2 - Unknown (26)
      fm.skip(16);

      // 4 - Unknown (1)
      archiveVersion = fm.readInt();
      if (archiveVersion == 3) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);
      }
      // 4 - Unknown (1)
      fm.skip(4);

      while (fm.getOffset() < arcSize) {
        readChunk(path, fm, resources, "");
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  int dxtcOffset = 0;

  int dxtcLength = 0;

  int dxtcDecompLength = 0;

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public void readChunk(File path, FileManipulator fm, Resource[] resources, String parentID) {
    try {

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      // 4 - Chunk Type (FOLD, DATA)
      String chunkType = fm.readString(4);

      // 4 - Chunk ID Name
      String chunkID = fm.readString(4);

      // 4 - Version
      //int version = fm.readInt();
      fm.skip(4);

      // 4 - Chunk Length (after the end of the Name field)
      int length = fm.readInt();
      FieldValidator.checkLength(length, fm.getLength());

      // 4 - Chunk Name Length (can be null)
      int nameLength = fm.readInt();

      if (archiveVersion == 3) {
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);
      }

      // X - Chunk Name (optional) (padded with nulls to a multiple of 4 bytes)
      String filename = null;
      if (nameLength != 0) {
        filename = fm.readNullString(nameLength);
      }
      else {
        filename = Resource.generateFilename(realNumFiles);
      }

      // X - Chunk Data
      long offset = fm.getOffset();
      if (chunkType.equals("FOLD")) {
        // folder (FOLD) - read nested chunks
        long endOffset = offset + length;
        while (fm.getOffset() < endOffset) {
          readChunk(path, fm, resources, chunkID);
        }
      }
      else {
        // file (DATA) - store the file

        filename += "." + chunkID;

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        //resource.addProperty("Version", "" + version);
        resources[realNumFiles] = resource;
        realNumFiles++;

        // Now comes some specific handlers for the chunkID...
        if (chunkID.equals("ATTR") && parentID.equals("IMAG")) {
          // 4 - Image Format
          imageFormat = fm.readInt();

          // 4 - Image Width
          imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);

          if (archiveVersion != 3) {
            // 4 - Mipmap Count
            fm.skip(4);
          }
        }
        else if (chunkID.equals("DATA") && parentID.equals("IMAG")) {
          // this is an image - store the image height and width
          resource.addProperty("Width", imageWidth);
          resource.addProperty("Height", imageHeight);

          String imageFormatString = "";
          if (imageFormat == 0) {
            imageFormatString = "BGRA";
          }
          else if (imageFormat == 8) {
            imageFormatString = "DXT1";
          }
          else if (imageFormat == 11) {
            imageFormatString = "DXT5";
          }
          else {
            imageFormatString = "" + imageFormat;
          }

          resource.addProperty("ImageFormat", imageFormatString);

          fm.skip(length);
        }
        else if (chunkID.equals("INFO") && parentID.equals("TPAT")) {
          // 4 - Image Width
          imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);
        }
        else if (chunkID.equals("PTLD") && parentID.equals("TPAT")) {
          // this is an image - store the image height and width
          resource.addProperty("Width", imageWidth);
          resource.addProperty("Height", imageHeight);

          fm.skip(length);
        }
        else if (chunkID.equals("TMAN") && parentID.equals("DXTC")) {
          // 4 - Number of Chunks
          fm.skip(4);

          // skip to the biggest chunk
          int numChunks = (length - 4) / 8;
          int largestOffset = 0;
          for (int c = 0; c < numChunks - 1; c++) {
            // 4 - Chunk Decompressed Length
            fm.skip(4);

            // 4 - Chunk Compressed Length
            int chunkLength = fm.readInt();
            FieldValidator.checkLength(chunkLength);
            largestOffset += chunkLength;
          }

          // now we're at the details for the largest chunk

          // 4 - Chunk Decompressed Length
          int largestDecompLength = fm.readInt();
          FieldValidator.checkLength(largestDecompLength);

          // 4 - Chunk Compressed Length
          int largestLength = fm.readInt();
          FieldValidator.checkLength(largestLength);

          dxtcOffset = largestOffset;
          dxtcLength = largestLength;
          dxtcDecompLength = largestDecompLength;
        }
        else if (chunkID.equals("TDAT") && parentID.equals("DXTC")) {
          // this is an image - store the image height and width

          resource.addProperty("Offset", dxtcOffset);
          resource.addProperty("Length", dxtcLength);
          resource.addProperty("DecompressedLength", dxtcDecompLength);
          resource.addProperty("Width", 1); // dummy so that the Viewer doesn't quit
          resource.addProperty("Height", 1); // dummy so that the Viewer doesn't quit

          fm.skip(length);
        }
        else if (chunkID.equals("DXTC") && parentID.equals("TXTR")) {
          // this is an image - store the image height and width

          // 4 - Image Width
          imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);

          resource.addProperty("Width", imageWidth);
          resource.addProperty("Height", imageHeight);

          // 4 - Unknown (1)
          // 4 - Unknown (2)
          fm.skip(8);

          // 4 - Image Format (13=DXT1, 15=DXT5)
          int imageFormat = fm.readInt();

          // the largest mipmap is last in the file. So, set the offset at the appropriate place
          int mipmapSize = imageWidth * imageHeight;
          if (imageFormat == 13) {
            mipmapSize /= 2; // DXT1
          }
          offset += (length - mipmapSize);
          resource.setOffset(offset);
          resource.setLength(mipmapSize);
          resource.setDecompressedLength(mipmapSize);

          if (imageFormat == 13) {
            resource.addProperty("ImageFormat", "DXT1");
          }
          else if (imageFormat == 15) {
            resource.addProperty("ImageFormat", "DXT5");
          }
          else {
            resource.addProperty("ImageFormat", "" + imageFormat);
          }

          fm.skip(length - 20); // we already read 20 bytes at the start
        }
        else {
          fm.skip(length);
        }

        TaskProgressManager.setValue(offset);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
