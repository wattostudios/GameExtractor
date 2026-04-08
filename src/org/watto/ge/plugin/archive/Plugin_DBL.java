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

package org.watto.ge.plugin.archive;

import java.io.File;

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DBL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DBL() {

    super("DBL", "DBL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("25 to Life");
    setExtensions("dbl"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("dbl_tex", "Texture Image", FileType.TYPE_IMAGE));

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

      // 8 - Padding Multiple (48)
      if (fm.readLong() == 48) {
        rating += 5;
      }

      // 8 - Unknown (22596)
      // 8 - Unknown (256)
      fm.skip(16);

      // 4 - Number of Blocks
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Length of all Blocks
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
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

      // 8 - Padding Multiple (48)
      // 8 - Unknown (22596)
      // 8 - Unknown (256)
      fm.skip(24);

      // 4 - Number of Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      // 4 - Length of all Blocks
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;

      for (int b = 0; b < numBlocks; b++) {
        long blockStart = fm.getOffset();
        //System.out.println(blockStart);

        // 2 - Unknown
        // 2 - Unknown (14)
        fm.skip(4);

        // 4 - Block Data Length (not including this 64-byte header)
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength, arcSize);

        // 2 - Unknown
        // 4 - Text String (1000)
        // 2 - null
        // 48 - null
        fm.skip(56);

        long nextBlockOffset = fm.getOffset() + blockLength;
        FieldValidator.checkOffset(nextBlockOffset, arcSize + 1); // +1 to allow the last block to be the end of the archive 

        // 2 - Number of Entries
        short numEntries = fm.readShort();

        // 2 - Directory Type (0=files, 1=folders)
        short dirType = fm.readShort();

        if (dirType == 1) {
          // folders

          FieldValidator.checkNumFiles(numEntries);

          // for each entry
          long[] folderOffsets = new long[numEntries];
          for (int e = 0; e < numEntries; e++) {
            // 4 - Folder Offset (relative to the start of this directory)
            long folderOffset = blockStart + 64 + fm.readInt();
            FieldValidator.checkOffset(folderOffset, arcSize);
            folderOffsets[e] = folderOffset;
          }

          for (int e = 0; e < numEntries; e++) {
            fm.relativeSeek(folderOffsets[e]);

            // 4 - Folder ID (incremental from 0)
            int folderID = fm.readInt();

            // X - Folder Name
            // 1 - null Folder Name Terminator
            String folderName = fm.readNullString();

            // 0-3 - Padding to a multiple of 4 bytes (byte 238)
            int padding = calculatePadding(folderName.length() + 1, 4);
            fm.skip(padding);

            //System.out.println(folderID + "\t" + folderName);
          }

        }
        else if (dirType == 0 && numEntries != 0) {
          try {
            // files
            int numFilesInBlock = numEntries;
            FieldValidator.checkNumFiles(numFilesInBlock);

            // 8 - null
            fm.skip(8);

            // 8 - Number of Source Directories? (1)
            int numSources = fm.readInt();
            FieldValidator.checkNumFiles(numSources + 1); // +1 to allow nulls
            // 4 - null
            fm.skip(8);

            // for each source directory
            for (int s = 0; s < numSources; s++) {
              // 32 - Source Directory Name (max 31 characters, starting at the Left of the filename, followed by null terminator, and nulls to fill)
              String sourceName = fm.readNullString(32);
              //System.out.println(sourceName);
            }

            for (int i = 0; i < numFilesInBlock; i++) {
              // 4 - Image Format (9=8bit, 28=RGBA, 2=RGBA5551)
              int imageFormat = fm.readInt();

              String imageFormatString = "" + imageFormat;
              if (imageFormat == 9) {
                imageFormatString = "8BitPaletted";
              }
              else if (imageFormat == 8) {
                imageFormatString = "4BitPaletted";
              }
              else if (imageFormat == 28) {
                imageFormatString = "RGBA";
              }
              else if (imageFormat == 2) {
                imageFormatString = "RGBA5551";
              }

              // 4 - Unknown (usually null)
              // 4 - Unknown (usually null)
              fm.skip(8);

              // 4 - File Offset (relative to the start of this directory)
              long offset = fm.readInt();
              if (offset == 0) {
                // not a file that's stored in the archive?
                fm.skip(56);
                continue;
              }
              offset += +blockStart + 64;
              FieldValidator.checkOffset(offset, arcSize);

              // 2 - Image Width
              short width = fm.readShort();
              FieldValidator.checkWidth(width);

              // 2 - Image Height
              short height = fm.readShort();
              FieldValidator.checkWidth(height);

              // 4 - null
              // 4 - null
              // 4 - Unknown
              // 4 - Unknown
              // 4 - null
              fm.skip(20);

              // 32 - Filename (max 31 characters, from the Right of the filename, followed by null terminator, and nulls to fill)
              String filename = fm.readNullString(32);
              FieldValidator.checkFilename(filename);

              filename = FilenameSplitter.getFilename(filename) + ".dbl_tex";

              if (i != 0) {
                Resource previousResource = resources[realNumFiles - 1];
                long previousLength = offset - previousResource.getOffset();
                FieldValidator.checkLength(previousLength, arcSize);
                previousResource.setLength(previousLength);
                previousResource.setDecompressedLength(previousLength);
              }

              //path,name,offset,length,decompLength,exporter
              Resource resource = new Resource(path, filename, offset);
              resource.addProperty("Width", width);
              resource.addProperty("Height", height);
              resource.addProperty("ImageFormat", imageFormatString);
              resources[realNumFiles] = resource;
              realNumFiles++;

              TaskProgressManager.setValue(offset);

            }

            if (realNumFiles != 0) {
              Resource previousResource = resources[realNumFiles - 1];
              long previousLength = nextBlockOffset - previousResource.getOffset();
              FieldValidator.checkLength(previousLength, arcSize);
              previousResource.setLength(previousLength);
              previousResource.setDecompressedLength(previousLength);
            }

          }
          catch (Throwable t) {
            // something else, just skip this directory
          }

        }
        else {
          //ErrorLogger.log("[DBL] Unknown directory type: " + dirType);

          String filename = Resource.generateFilename(realNumFiles);

          long offset = blockStart + 64;
          int length = blockLength;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }

        // ready for the next block
        fm.seek(nextBlockOffset);
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

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
