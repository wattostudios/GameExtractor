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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_JRZ_DEFLATEARCHIVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_JRZ_DEFLATEARCHIVE() {

    super("JRZ_DEFLATEARCHIVE", "JRZ_DEFLATEARCHIVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Jikandia: The Timeless Land");
    setExtensions("jrz"); // MUST BE LOWER CASE
    setPlatforms("PSP");

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
      if (fm.readString(24).equals("__DEFLATE_ARCHIVE_S_01__")) {
        rating += 50;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int numEntriesRead = 0;

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

      // RESETTING GLOBAL VARIABLES

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      // 24 - Header (__DEFLATE_ARCHIVE_S_01__)
      // 4 - Unknown (9)
      // 2 - null
      // 2 - Unknown (1)
      fm.skip(32);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Directories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      // 4 - Unknown (1)
      fm.skip(4);

      realNumFiles = 0;
      numEntriesRead = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      while (realNumFiles < numFiles) {
        readDirectory(fm, path, resources, "");
      }

      // Now go through each file and work out the compressed blocks
      fm.getBuffer().setBufferSize(24);
      fm.seek(1);

      long arcSize = fm.getLength();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();
        fm.seek(offset);

        long firstOffset = fm.readInt();
        FieldValidator.checkOffset(firstOffset);

        if (firstOffset == 4) {
          // 4 - Compressed Block Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          resource.setOffset(offset + 8);
          resource.setLength(length);
        }
        else {
          fm.seek(offset);

          int numBlocks = (int) (firstOffset / 4);
          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Block Offset
            long blockOffset = fm.readInt() + offset;
            FieldValidator.checkOffset(blockOffset, arcSize);
            blockOffsets[b] = blockOffset;

            long blockDecompLength = 65536;
            if (b == numBlocks - 1) {
              blockDecompLength = resource.getDecompressedLength() - (65536 * (numBlocks - 1));
              FieldValidator.checkLength(blockDecompLength);
            }
            blockDecompLengths[b] = blockDecompLength;
          }

          for (int b = 0; b < numBlocks; b++) {
            fm.seek(blockOffsets[b]);

            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);
            blockLengths[b] = blockLength;

            blockOffsets[b] += 4;
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);

        }

      }

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
  
   **********************************************************************************************
   **/
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, String dirName) {
    try {
      long arcSize = fm.getLength();

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // 2 - Entry Type (0=File, 1=Directory)
      short entryType = fm.readShort();

      if (entryType == 0) {
        // File

        // 2 - Filename Length (including Null Terminator)
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // 8 - null
        fm.skip(8);

        // 4 - Compressed Length (including the 2 file header fields)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length?
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - null
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString(filenameLength);

        numEntriesRead++;

        //System.out.println("File\t" + dirName + filename);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, dirName + filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;

      }
      else if (entryType == 1) {
        // Directory

        // 2 - Directory Name Length (including Null Terminator)
        short dirNameLength = fm.readShort();
        FieldValidator.checkFilenameLength(dirNameLength);

        // 4 - Number of Files in this Directory ?
        int numSubFiles = fm.readInt();
        FieldValidator.checkNumFiles(numSubFiles);

        // 4 - Number of Sub-Directories in this Directory ? (no sub-directories if this is the same value as the previous field?)
        int numSubDirectories = fm.readInt();
        if (numSubDirectories == numSubFiles) {
          numSubDirectories = 0;
        }
        FieldValidator.checkNumFiles(numSubDirectories + 1);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Offset to First File in this Directory
        fm.skip(16);

        // X - Directory Name
        // 1 - null Directory Name Terminator
        String subDirName = fm.readNullString(dirNameLength);

        numEntriesRead++;

        //System.out.println("Dir\t" + dirName + subDirName + "\\" + "\t" + numSubFiles + "\t" + numSubDirectories);

        int lastFile = numEntriesRead + numSubFiles;
        while (numEntriesRead < lastFile) {
          readDirectory(fm, path, resources, dirName + subDirName + "\\");
        }

      }
      else {
        ErrorLogger.log("[JRZ_DEFLATEARCHIVE] Unknown entry type: " + entryType);
        numEntriesRead++;
        return;
      }

    }
    catch (Throwable t) {
      logError(t);
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
