/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import java.util.Arrays;
import java.util.Hashtable;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_93 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_93() {

    super("DAT_93", "DAT_93");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Asheron's Call 2");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("texture", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readLong() == 0) {
        rating += 10;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  Hashtable<Long, Long> processedOffsets = null;

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

      ExporterPlugin exporter = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;
      processedOffsets = new Hashtable<Long, Long>(1000);

      FileManipulator fm = new FileManipulator(path, false, 1024); // 1024 is the block size

      long arcSize = fm.getLength();

      // 300 - null
      // 4 - Archive Type (21570)
      fm.skip(304);

      // 4 - Block Size (1024)
      int blockSize = fm.readInt();
      FieldValidator.checkRange(blockSize, 1, 102400);

      // 4 - Archive Length
      // 4 - Archive Type (1)
      // 4 - Data Subset
      // 4 - Free Head
      // 4 - Free Tail
      // 4 - Free Count (1054)
      fm.skip(24);

      // 4 - BTree
      long rootOffset = IntConverter.unsign(fm.readInt());

      // 4 - New LRU (null)
      // 4 - Old LRU (null)
      // 4 - Use LRU (null)
      // 4 - Master Map ID (null)
      // 4 - Engine Pack Version (104)
      // 4 - Game Pack Version (3)
      // 16 - Version Major
      // 4 - Version Minor
      // 4 - Unknown (11777)

      int numFiles = Archive.getMaxFiles() * 2;
      realNumFiles = 0;
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      readDirectory(path, fm, arcSize, blockSize, rootOffset, resources);

      // there are duplicates - filter them out
      numFiles = realNumFiles;
      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];
      for (int i = 0; i < numFiles; i++) {
        sorter[i] = new ResourceSorter_Offset(resources[i]);
      }

      // Sort the Resources by their offsets
      Arrays.sort(sorter);

      // Put the resources back into the array based on their offset
      realNumFiles = 0;
      long previousOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource currentResource = sorter[i].getResource();
        long currentOffset = currentResource.getOffset();
        if (currentOffset == previousOffset) {
          // skip (duplicate)
          continue;
        }
        resources[realNumFiles] = currentResource;

        // also reset the filename so that it's incremental
        String extension = currentResource.getExtension();
        String filename = Resource.generateFilename(realNumFiles);
        if (extension != null && !extension.equals("")) {
          filename += "." + extension;
        }
        currentResource.setName(filename);
        currentResource.setOriginalName(filename);

        realNumFiles++;
        previousOffset = currentOffset;
      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      // now go and process all the blocks in the files, add them as an exporter
      int blockSizeWithoutHeader = blockSize - 4;
      TaskProgressManager.setMaximum(numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long offset = resource.getOffset();
        long length = resource.getLength();

        int numBlocks = (int) (length / blockSizeWithoutHeader);
        int lastBlockSize = (int) (length % blockSizeWithoutHeader);
        if (lastBlockSize != 0) {
          numBlocks++;
        }

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];

        for (int b = 0; b < numBlocks; b++) {
          fm.relativeSeek(offset);

          // 4 - Next Block Offset
          long nextOffset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(nextOffset, arcSize);

          blockOffsets[b] = fm.getOffset();
          blockLengths[b] = blockSizeWithoutHeader;

          offset = nextOffset;
        }

        if (lastBlockSize != 0) {
          blockLengths[numBlocks - 1] = lastBlockSize;
        }

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockLengths);
        resource.setExporter(blockExporter);

        TaskProgressManager.setValue(i);
      }

      processedOffsets = null; // release memory

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
   * 
   **********************************************************************************************
   **/

  public void readDirectory(File path, FileManipulator fm, long arcSize, int blockSize, long dirOffset, Resource[] resources) {
    try {

      if (processedOffsets.containsKey(dirOffset)) {
        // already processed the directory at this offset
        return;
      }
      processedOffsets.put(dirOffset, dirOffset);

      fm.seek(dirOffset);

      // 4 - Next Block Offset (0 = no next block)
      long nextBlockOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nextBlockOffset, arcSize);

      long[] branchOffsets = new long[62];
      int numBranches = 0;
      for (int i = 0; i < 62; i++) {
        // 4 - Next Branch Offset
        long branchOffset = IntConverter.unsign(fm.readInt());
        if (branchOffset != 0) {
          FieldValidator.checkOffset(branchOffset, arcSize);
          branchOffsets[i] = branchOffset;
          numBranches++;
        }
      }

      // 4 - Number of Files in this Block
      int numFilesInBlock = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInBlock + 1); // +1 to allow 0 files in this block

      int bytesRead = 4 + (62 * 4) + 4;

      // Loop through directory
      for (int i = 0; i < numFilesInBlock; i++) {

        if (bytesRead >= blockSize) { // read the next block
          fm.relativeSeek(nextBlockOffset);
          nextBlockOffset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(nextBlockOffset, arcSize);
          bytesRead = 4;
        }

        // 4 - Object ID
        int objectID = fm.readInt();
        bytesRead += 4;

        if (bytesRead >= blockSize) { // read the next block
          fm.relativeSeek(nextBlockOffset);
          nextBlockOffset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(nextBlockOffset, arcSize);
          bytesRead = 4;
        }

        // 4 - File Offset
        long offset = IntConverter.unsign(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);
        bytesRead += 4;

        if (bytesRead >= blockSize) { // read the next block
          fm.relativeSeek(nextBlockOffset);
          nextBlockOffset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(nextBlockOffset, arcSize);
          bytesRead = 4;
        }

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        bytesRead += 4;

        if (bytesRead >= blockSize) { // read the next block
          fm.relativeSeek(nextBlockOffset);
          nextBlockOffset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(nextBlockOffset, arcSize);
          bytesRead = 4;
        }

        // 4 - Flags?
        fm.skip(4);
        //System.out.println(fm.readInt());
        bytesRead += 4;

        //int objectTypeID = objectID >> 24;
        int objectTypeID = objectID >> 16;
        String extension = "";
        //if (objectTypeID == 65) {
        if (objectTypeID == 16640) {
          extension = ".Texture";
        }
        //else {
        //  extension = "." + objectTypeID;
        //}

        String filename = Resource.generateFilename(realNumFiles) + extension;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(realNumFiles);
      }

      // now read all the branches
      for (int i = 0; i < numBranches; i++) {
        readDirectory(path, fm, arcSize, blockSize, branchOffsets[i], resources);
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
