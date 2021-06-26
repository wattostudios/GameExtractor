
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GCF_1 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GCF_1() {

    super("GCF_1", "GCF_1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("RACE 07");
    setExtensions("gcf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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

      // Unknown (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Unknown (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(20);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Padding Multiple (8192)
      if (fm.readInt() == 8192) {
        rating += 4;
      }

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

      // ARCHIVE HEADER

      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown (6)
      // 4 - Cache ID
      fm.skip(16);

      // 4 - Version (16)
      int version = fm.readInt();
      FieldValidator.checkNumFiles(version);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Archive Size
      fm.skip(12);

      // 4 - Padding Multiple (8192)
      int paddingMultiple = fm.readInt();
      FieldValidator.checkNumFiles(paddingMultiple);

      // 4 - Number Of Data Blocks
      // 4 - Unknown
      fm.skip(8);

      // BLOCKS DIRECTORY

      // 4 - Number Of Data Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkLength(numBlocks, arcSize);

      // 4 - Number Of Used Data Blocks
      // 4 - Number Of Data Blocks
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Checksum
      fm.skip(28);

      int[] firstDataBlockIndexes = new int[numBlocks];
      int[] directoryIndexes = new int[numBlocks];
      int[] blockLengths = new int[numBlocks];

      TaskProgressManager.setMaximum(numBlocks);

      // for each block (28 bytes per entry)
      for (int i = 0; i < numBlocks; i++) {

        // 2 - Entry Type (0 = Unused Entry)
        // 2 - Entry Indicator (5745)
        // 4 - Offset of this Block in the Exported File
        fm.skip(8);

        // 4 - Length of the Data in this block
        int blockLength = fm.readInt();
        //System.out.println(fm.getOffset() + " +: " + blockLength);
        //if (blockLength < 0 || blockLength > paddingMultiple){
        //  blockLength = paddingMultiple;
        //  }
        blockLengths[i] = blockLength;

        // 4 - First Data Block Index
        int firstDataBlockIndex = fm.readInt();
        firstDataBlockIndexes[i] = firstDataBlockIndex;

        // 4 - Next Data Block Index (if == blockCount, there is no next block)
        // 4 - Previous Data Block Index (if == blockCount, there is no previous block)
        fm.skip(8);

        // 4 - Main Directory Index
        int directoryIndex = fm.readInt();
        if (directoryIndex > 0) {
          FieldValidator.checkLength(directoryIndex, numBlocks);
          directoryIndexes[i] = directoryIndex;
          FieldValidator.checkLength(firstDataBlockIndex, numBlocks);
        }
        //fm.skip(4);

        TaskProgressManager.setValue(i);
      }

      // FRAGMENTATION MAP

      // 4 - Number Of Data Blocks
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Checksum
      fm.skip(16);

      int[] nextDataBlockIndexes = new int[numBlocks];

      // for each block
      for (int i = 0; i < numBlocks; i++) {

        // 4 - Next Data Block Index (-1 if there is no next index)
        int nextDataBlockIndex = fm.readInt();
        if (nextDataBlockIndex != -1) {
          FieldValidator.checkLength(nextDataBlockIndex, numBlocks + 1);
        }
        nextDataBlockIndexes[i] = nextDataBlockIndex;

        TaskProgressManager.setValue(i);
      }

      // MAIN DIRECTORY

      // 4 - Unknown (4)
      // 4 - Cache ID
      // 4 - Version
      fm.skip(12);

      // 4 - Number Of Entries (Files + Directories)
      int numEntries = fm.readInt();
      FieldValidator.checkNumFiles(numEntries);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Directory Length
      fm.skip(8);

      // 4 - Names Directory Length
      int namesDirLength = fm.readInt();
      FieldValidator.checkLength(namesDirLength, arcSize);

      // 4 - Number Of Info1 Entries
      int numInfo1 = fm.readInt();
      FieldValidator.checkNumFiles(numInfo1 + 1);

      // 4 - Number Of Copy Files
      int numCopy = fm.readInt();
      FieldValidator.checkNumFiles(numCopy + 1);

      // 4 - Number Of Local Files
      int numLocal = fm.readInt();
      FieldValidator.checkNumFiles(numLocal + 1);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Checksum
      fm.skip(12);

      // get the names, so skip over the directory first
      long dirOffset = fm.getOffset();

      long namesDirOffset = dirOffset + (numEntries * 28);
      fm.seek(namesDirOffset);

      // NAMES DIRECTORY

      String[] names = new String[numEntries];

      TaskProgressManager.setMaximum(numEntries);

      // for each Entry
      for (int i = 0; i < numEntries; i++) {
        // X - Filename (root entry is empty)
        // 1 - null Filename Terminator
        names[i] = fm.readNullString();

        TaskProgressManager.setValue(i);
      }

      // now skip to the directory map
      fm.seek(namesDirOffset + namesDirLength + (numInfo1 * 4) + (numEntries * 4) + (numCopy * 4) + (numLocal * 4));

      // DIRECTORY MAP

      // 4 - Unknown (1)
      // 4 - null
      fm.skip(8);

      int[] firstBlockIndexes = new int[numEntries];

      // for each Entry
      for (int i = 0; i < numEntries; i++) {
        // 4 - First Block Index (if == blockCount, there is no first block for this entry, such as for folders)
        int firstBlockIndex = fm.readInt();
        if (firstBlockIndex == numBlocks) {
          firstBlockIndex = -1;
        }
        else {
          FieldValidator.checkLength(firstBlockIndex, numBlocks);
        }
        firstBlockIndexes[i] = firstBlockIndex;

        TaskProgressManager.setValue(i);
      }

      // CHECKSUM DIRECTORY

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Checksum Directory Length (not including this and the previous field)
      int checksumDirLength = fm.readInt();
      FieldValidator.checkLength(checksumDirLength, arcSize);

      // skip the checksums
      fm.skip(checksumDirLength);

      // FILE DATA BLOCKS

      // 4 - Version
      // 4 - Number Of Data Blocks
      fm.skip(8);

      // 4 - Padding Multiple (8192)
      paddingMultiple = fm.readInt();
      FieldValidator.checkNumFiles(paddingMultiple);

      // 4 - First Data Block Offset
      int relOffset = fm.readInt();
      FieldValidator.checkOffset(relOffset, arcSize);

      // 4 - null
      // 4 - Checksum

      // skip back to the directory
      fm.seek(dirOffset);

      // MAIN DIRECTORY (ENTRIES ONLY)

      Resource[] resources = new Resource[numFiles];

      // for each Entry (28 bytes per entry)
      int realNumFiles = 0;
      for (int i = 0; i < numEntries; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        fm.skip(4);

        // 4 - File Length (Folder = numSubFolders)
        int length = fm.readInt();

        // 4 - Checksum Index (-1 if there is no checksum, such as for folders)
        fm.skip(4);

        // 4 - Entry Type (0=folder)
        int entryType = fm.readInt();

        // 4 - Parent Index (-1 if there is no parent)
        int parentNameIndex = fm.readInt();
        if (parentNameIndex >= 0) {
          FieldValidator.checkLength(parentNameIndex, numEntries);
          names[i] = names[parentNameIndex] + "\\" + names[i];
        }

        // 4 - The Index of the Next Entry in the same Parent directory (0 if there is no next entry)
        // 4 - First Index for the entries in this sub-directory (0 if there is no first entry, such as for files)
        fm.skip(8);

        if (entryType == 0) {
          // Folder
          FieldValidator.checkNumFiles(length);
        }
        else {
          // File
          FieldValidator.checkLength(length, arcSize);

          // this should be the approximate number of chunks
          int arraySize = (length / paddingMultiple) + 1;

          // get the index (in the block directory) of the first block for this file
          int blockIndex = firstBlockIndexes[i];

          int numFileBlocks = 0;
          long offset = 0;

          /*
           * if (blockIndex == -1){ System.out.println(names[i] + " -- " + length);
           *
           * for (int j=0;j<numBlocks;j++){ if (directoryIndexes[j] == i){ // found it the long
           * way blockIndex = j; System.out.println("actually found it at block " + blockIndex);
           * break; } } }
           */

          if (blockIndex == -1) {
            // there are actually some missing files
            length = 0;
          }
          else {
            offset = (firstDataBlockIndexes[blockIndex] * paddingMultiple) + relOffset;
            length = blockLengths[blockIndex];
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, names[i], offset, length);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
