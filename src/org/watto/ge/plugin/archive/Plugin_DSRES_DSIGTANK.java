/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DSRES_DSIGTANK extends ArchivePlugin {

  long dirOffset;

  long filesDirOffset;

  int realNumFiles;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DSRES_DSIGTANK() {

    super("DSRES_DSIGTANK", "DSRES_DSIGTANK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Copperhead Retaliation",
        "Dungeon Siege: Legends of Aranna");
    setExtensions("dsres");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

    setTextPreviewExtensions("gas", "gpg", "nnk", "skrit"); // LOWER CASE

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
      if (fm.readString(8).equals("DSigTank")) {
        rating += 50;
      }

      // Version
      if (fm.readShort() == 2) {
        rating += 5;
      }

      // Version
      if (fm.readShort() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      dirOffset = 0;
      filesDirOffset = 0;
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (DSigTank)
      // 2 - Version Major (2)
      // 2 - Version Minor (1)
      fm.skip(12);

      // 4 - Folders Directory Offset
      dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Files Directory Offset?
      filesDirOffset = fm.readInt() - 4; // -4 so that we account for some padding at the end of the archive
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length of all Directories (ie ArchiveSize - FoldersDirectoryOffset)
      fm.skip(4);

      // 4 - Number Of Files?
      fm.skip(4);
      int numFiles = Archive.getMaxFiles();
      //int numFiles = fm.readInt();
      //FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // Loop through the folder offsets
      int[] offsets = new int[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Offset (relative to the folders directory offset)
        int offset = (int) (fm.readInt() + dirOffset);
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // read the folders
      /*
      for (int i = 0; i < numFolders; i++) {
        fm.seek(offsets[i]);
        if (fm.getOffset() < arcSize - 4) {
          readDirectory(resources, fm, path, "");
        }
      }
      */
      readDirectory(resources, fm, path, "");

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

      /*
       * // Loop through directory for(int i=0;i<numFiles;i++){
       * 
       * // 4 - File Offset
       * 
       * 
       * // 4 - File Length
       * 
       * 
       * // X - Filename (null) String filename = fm.readNullString(); FieldValidator.checkFilename(filename);
       * 
       * 
       * String filename = Resource.generateFilename(i);
       * 
       * //path,name,offset,length,decompLength,exporter resources[i] = new
       * Resource(path,filename,offset,length);
       * 
       * TaskProgressManager.setValue(i); }
       * 
       * 
       * resources = resizeResources(resources,realNumFiles);
       * calculateFileSizes(resources,arcSize);
       */

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
  public void readDirectory(Resource[] resources, FileManipulator fm, File path, String parentDirName) throws Exception {

    long arcSize = (int) fm.getLength();
    ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
    ExporterPlugin exporterDefault = Exporter_Default.getInstance();

    // 4 - Parent Directory Offset (relative to the folders directory offset)
    fm.skip(4);

    // 4 - Number Of Files in this folder
    int numFilesInFolder = fm.readInt();
    FieldValidator.checkNumFiles(numFilesInFolder);

    // 8 - Hash
    fm.skip(8);

    // 2 - Folder Name Length
    short folderNameLength = fm.readShort();
    try {
      FieldValidator.checkFilenameLength(folderNameLength + 1); // +1 to allow null (root)
    }
    catch (Throwable t) {
      return;
    }

    // X - Folder Name
    String folderName = fm.readString(folderNameLength);
    //System.out.println(fm.getOffset() + "\t" + folderName);

    // 0-3 - null Padding to a multiple of 4 bytes (including the folder name length)
    int paddingSize = 4 - ((folderNameLength + 2) % 4);
    if (paddingSize != 4) {
      fm.skip(paddingSize);
    }

    // for each file in this folder
    int[] innerOffsets = new int[numFilesInFolder];
    for (int j = 0; j < numFilesInFolder; j++) {
      // 4 - File Entry Offset (relative to the folders directory offset)
      int innerOffset = (int) (fm.readInt() + dirOffset);
      if (innerOffset == dirOffset) {
        innerOffset = (int) (fm.readInt() + dirOffset); // sometimes there's 4 nulls for the first entry after a folderName - this'll fix it
      }
      FieldValidator.checkOffset(innerOffset, arcSize);
      innerOffsets[j] = innerOffset;
    }

    // go to each offset
    for (int j = 0; j < numFilesInFolder; j++) {
      int innerOffset = innerOffsets[j];
      fm.seek(innerOffset);

      if (innerOffset < filesDirOffset) {
        // directory
        if (folderName.equals("")) {
          readDirectory(resources, fm, path, parentDirName + folderName);
        }
        else {
          readDirectory(resources, fm, path, parentDirName + folderName + "\\");
        }
      }
      else {
        // file

        // 4 - Parent Directory Offset (relative to the folders directory offset)
        fm.skip(4);

        // 4 - Decompressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset?
        long offset = fm.readInt() + 804;
        FieldValidator.checkOffset(offset, arcSize);

        // 12 - Hash
        fm.skip(12);

        // 4 - Compression Flag? (0=Uncompressed, 1=ZLib Compression)
        int compressionFlag = fm.readInt();

        // 2 - Filename Length
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = parentDirName + folderName + "\\" + fm.readString(filenameLength);
        //System.out.println(fm.getOffset() + "\t" + filename);

        // 0-3 - null Padding to a multiple of 4 bytes (including the filename length)
        int filenamePaddingSize = 4 - ((filenameLength + 2) % 4);
        if (filenamePaddingSize != 4) {
          fm.skip(filenamePaddingSize);
        }

        if (compressionFlag == 1) {
          long decompLength = length;

          // 4 - Compressed Length
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Block Size (16384)
          //fm.skip(4);
          int blockSize = fm.readInt();

          if (length == 0 && blockSize != 16384) {
            length = blockSize;
            blockSize = fm.readInt();
          }

          //System.out.println(filename + "\t" + blockSize + "\t" + decompLength);

          int numBlocks = (int) (decompLength / blockSize) * 2; // *2 to allow for "extra bytes" between each compressed chunk
          if (decompLength % blockSize != 0) {
            numBlocks++;
          }

          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];
          ExporterPlugin[] blockExporters = new ExporterPlugin[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Decompressed Block Length
            int blockDecompLength = fm.readInt();
            FieldValidator.checkLength(blockDecompLength);

            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);

            // 4 - More Blocks Flag? (16 = more blocks, 0 = last block)
            int extraBytes = fm.readInt();
            //blockDecompLength += extraBytes;
            //blockLength += extraBytes;

            blockDecompLengths[b] = blockDecompLength;
            blockLengths[b] = blockLength;

            // 4 - Decompressed Block Offset
            long blockOffset = fm.readInt() + offset;
            FieldValidator.checkLength(blockOffset, arcSize);
            blockOffsets[b] = blockOffset;

            blockExporters[b] = exporterZLib;

            if (extraBytes != 0) {
              // reduce the previous block by the extraBytes
              blockDecompLength -= extraBytes;
              blockDecompLengths[b] = blockDecompLength;

              // now add the 16 bytes as raw data
              b++;
              blockOffsets[b] = blockOffset + blockLength;
              blockLengths[b] = extraBytes;
              blockDecompLengths[b] = extraBytes;
              blockExporters[b] = exporterDefault;
            }

          }

          if (numBlocks == 1) {
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporterZLib);
          }
          else {
            //BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
            BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, blockExporter);
          }
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;

      }
    }

  }

}
