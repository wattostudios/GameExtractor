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
import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.BlockQuickBMSExporterWrapper;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_42 extends ArchivePlugin {

  int numNames = 0;

  int readEntries = 0;

  int currentResource = 0;

  boolean dieNow = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_42() {

    super("DAT_42", "DAT_42");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("LEGO Batman",
        "Transformers: The Game");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ai2", "AI Level Path", FileType.TYPE_OTHER),
        new FileType("an3", "Animation", FileType.TYPE_OTHER),
        new FileType("ani", "Animation", FileType.TYPE_OTHER),
        new FileType("anm", "Animation", FileType.TYPE_OTHER),
        new FileType("cd", "Character Definition", FileType.TYPE_OTHER),
        new FileType("cft", "Font", FileType.TYPE_OTHER),
        new FileType("cu2", "Cutscene", FileType.TYPE_VIDEO),
        new FileType("fnt", "Font", FileType.TYPE_OTHER),
        new FileType("ft2", "Font", FileType.TYPE_OTHER),
        new FileType("git", "Git Options", FileType.TYPE_DOCUMENT),
        new FileType("giz", "Giz Obstacle", FileType.TYPE_OTHER),
        new FileType("gsc", "GSC Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("par", "Part", FileType.TYPE_OTHER),
        new FileType("scp", "Script File", FileType.TYPE_DOCUMENT),
        new FileType("sf", "Script File", FileType.TYPE_DOCUMENT),
        new FileType("spl", "Spline", FileType.TYPE_OTHER),
        new FileType("sub", "Subtitles", FileType.TYPE_DOCUMENT),
        new FileType("subopt", "Subtitles", FileType.TYPE_DOCUMENT),
        new FileType("txm", "Minikit", FileType.TYPE_DOCUMENT),
        new FileType("txc", "Collectable", FileType.TYPE_DOCUMENT),
        new FileType("tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("fmv", "FMV Video", FileType.TYPE_VIDEO));

    setTextPreviewExtensions("ats", "git", "h", "scp"); // LOWER CASE

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(2000);

      // null
      if (fm.readLong() == 0) {
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
      numNames = 0;
      readEntries = 0;
      dieNow = false;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Directory Offset
      long dirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      fm.seek(dirOffset);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      short[] compressionFlags = new short[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Offset [<<8]
        long offset = IntConverter.unsign(fm.readInt()) << 8;

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 3 - Compression Flag (0=Uncompressed/2=Compressed)
        short compression = fm.readShort();
        compressionFlags[i] = compression;
        fm.skip(1);

        // 1 - Offset Addition
        offset += ByteConverter.unsign(fm.readByte());
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);
      }

      // read the filenames

      // 4 - Number Of Filenames
      numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      dirOffset = fm.getOffset();

      fm.skip(numNames * 8); // skip over the offsets etc.

      // 4 - Filename Block Length
      int filenameBlockLength = fm.readInt();
      FieldValidator.checkLength(filenameBlockLength, arcSize);

      byte[] filenameBytes = fm.readBytes(filenameBlockLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      // Now read the name CRC directory
      int[] crcs = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename CRC
        crcs[i] = fm.readInt();
      }

      /*
      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        names[i] = fm.readNullString();
      }
      */

      fm.seek(dirOffset);

      /*
      readEntries = 0;
      currentResource = 0;
      
      while (readEntries < numNames) {
        readEntry(fm, "", resources, nameFM, 0);
      }
      */

      //
      //
      // THIS SECTION IS COPIED FROM Plugin_DAT_BEGINAPPIDSTRING WITH SLIGHT MODIFICATION
      //
      //
      // process the names directory, noting that the outer loop is for the number of FILES, and there's an inner loop as well. Complicated logic.
      String[] names = new String[numNames];
      int currentName = 0;
      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        short nextID = 999; // dummy to start the loop
        String fullPath = "";
        String name = "";

        while (nextID > 0) {
          // 2 - Next Name ID
          nextID = fm.readShort();

          // 2 - Previous Name ID
          short previousID = fm.readShort();

          // 4 - Name Offset (relative to the start of the Names Directory)
          int nameOffset = fm.readInt();
          if (nameOffset >= 0) { // want to allow negatives, just set their names to "". Want to still fail if the offset is too long
            FieldValidator.checkOffset(nameOffset, filenameBlockLength);
          }

          // get the actual name
          if (nameOffset >= 0) {
            nameFM.relativeSeek(nameOffset);
            name = nameFM.readNullString();
          }
          else {
            name = "";
          }

          // now do some processing for the parents
          //String fullPath = "";
          if (previousID != 0) {
            fullPath = names[previousID];
          }

          names[currentName] = fullPath;

          if (nextID > 0) { // a folder
            String tempParentName = names[previousID];
            if (!tempParentName.equals("")) {
              String oldName = "\\" + tempParentName + "\\";
              int oldNamePos = fullPath.lastIndexOf(oldName);
              if (oldNamePos >= 0) {
                fullPath = fullPath.substring(0, oldNamePos);
              }
            }
            if (!name.equals("")) {
              fullPath += name + "\\";
            }

          }
          currentName++;
        }

        String actualName = fullPath + name;

        // now work out the CRC of the name, so we know which file this name belongs to
        actualName = actualName.toUpperCase();
        int nameLength = actualName.length();

        int crc = 0x811c9dc5;
        for (int j = 0; j < nameLength; j++) {
          byte character = (byte) actualName.charAt(j);
          crc ^= character;
          crc *= 0x199933;
        }

        // now see if we can find the matching CRC
        int nameIndex = -1;
        for (int j = 0; j < numFiles; j++) {
          if (crc == crcs[j]) {
            // found it
            nameIndex = j;
            break;
          }
        }

        if (nameIndex == -1) {
          // not found
          ErrorLogger.log("[DAT_BEGINAPPIDSTRING]: Name CRC not found: " + crc);
        }
        else {
          Resource resource = resources[nameIndex];
          resource.setName(actualName);
          resource.setOriginalName(actualName);
        }

      }

      nameFM.close();

      /*
       * int numParents = 0; String[] parents = new String[10]; String parentName = "";
       * 
       * boolean justChangedDirectory = false; String prevExtension = "";
       * 
       * for (int i=0;i<numNames;i++){ // X - Filename // 1 - null Filename Terminator String
       * name = fm.readNullString(); if (name.indexOf('.') > 0){ // file if (justChangedDirectory
       * && ! prevExtension.equals("")){ if (!
       * name.substring(name.indexOf('.')+1).equals(prevExtension)){ for (int
       * n=0;n<numParents;n++){ parents[n] = parents[n+1]; } if (numParents > 0){ numParents--; }
       * } } prevExtension = name.substring(name.indexOf('.')+1);
       * 
       * System.out.println(parentName + name); justChangedDirectory = false; } else { //
       * directory if (justChangedDirectory){ numParents++; } parents[numParents] = name;
       * 
       * parentName = ""; for (int n=0;n<=numParents;n++){ parentName += parents[n] + "\\"; }
       * System.out.println(parentName); justChangedDirectory = true; } }
       */

      // now for each compressed file, we need to find the compressed blocks
      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        if (compressionFlags[i] != 0) {
          // a compressed file

          Resource resource = resources[i];
          int length = (int) resource.getLength();
          int decompLength = (int) resource.getDecompressedLength();
          if (length != decompLength) {

            fm.seek(resource.getOffset());

            int numBlocks = (decompLength / 16384) + 1; // guess
            int realNumBlocks = 0;

            String[] compressionTypes = new String[numBlocks];
            long[] blockDecompLengths = new long[numBlocks];
            long[] blockLengths = new long[numBlocks];
            long[] blockOffsets = new long[numBlocks];

            for (int b = 0; b < numBlocks; b++) {
              // 4 - Compression Type (LZ2K)
              String compressionType = fm.readString(4);
              compressionTypes[b] = compressionType;

              // 4 - Decompressed Block Length
              int blockDecompLength = fm.readInt();
              FieldValidator.checkLength(blockDecompLength);
              blockDecompLengths[b] = blockDecompLength;

              // 4 - Compressed Block Length
              int blockLength = fm.readInt();
              FieldValidator.checkLength(blockLength, arcSize);
              blockLengths[b] = blockLength;

              // X - Compressed File Data
              long blockOffset = fm.getOffset();
              blockOffsets[b] = blockOffset;

              fm.skip(blockLength);

              realNumBlocks++;

              length -= (12 + blockLength);
              if (length <= 0) {
                // finished reading the compressed blocks - return
                break;
              }

            }

            // if we originally allocated too many blocks, resize the arrays
            if (realNumBlocks != numBlocks) {
              String[] oldCompressionTypes = compressionTypes;
              long[] oldBlockDecompLengths = blockDecompLengths;
              long[] oldBlockLengths = blockLengths;
              long[] oldBlockOffsets = blockOffsets;

              compressionTypes = new String[realNumBlocks];
              blockDecompLengths = new long[realNumBlocks];
              blockLengths = new long[realNumBlocks];
              blockOffsets = new long[realNumBlocks];

              System.arraycopy(oldCompressionTypes, 0, compressionTypes, 0, realNumBlocks);
              System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, realNumBlocks);
              System.arraycopy(oldBlockLengths, 0, blockLengths, 0, realNumBlocks);
              System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, realNumBlocks);
            }

            // This is a special exporter that will generate a script to append each block to the same single output file
            BlockQuickBMSExporterWrapper exportWrapper = new BlockQuickBMSExporterWrapper(compressionTypes, blockOffsets, blockLengths, blockDecompLengths);
            resource.setExporter(exportWrapper);
          }
        }
      }

      fm.close();

      if (dieNow) {
        return null;
      }

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
  public void readEntry(FileManipulator fm, String parentName, Resource[] resources, FileManipulator nameFM, int depth) {
    if (depth >= 50) {
      dieNow = true;
      return; // because some very rare files were causing stack overflows here (when the file wasn't even valid for this plugin)
    }
    try {

      // 2 - File ID (if this entry is a directory, this value is the last entry in this sub-directory)
      int lastFile = fm.readShort();

      // 2 - Unknown
      fm.skip(2);

      // 4 - Filename Offset (relative to the start of the filenames directory)
      int filenameOffset = fm.readInt();
      FieldValidator.checkOffset(filenameOffset, nameFM.getLength());

      nameFM.seek(filenameOffset);
      String filename = nameFM.readNullString();

      if (lastFile <= 0) {
        // file
        //System.out.println("adding file " + names[readEntries] + " to " + parentName);
        String name = parentName + filename;
        Resource resource = resources[currentResource];
        resource.setName(name);
        resource.setOriginalName(name);
        currentResource++;
        readEntries++;
      }
      else {
        // directory

        String dirName = parentName + filename;
        if (!dirName.equals("")) {
          dirName += "\\";
        }
        readEntries++;

        //System.out.println(parentName + names[readEntries-1] + " has " + (lastFile-readEntries) + " files");
        while (readEntries <= lastFile && !dieNow) {
          //System.out.println("Reading into " + parentName + names[readEntries-1] + " entry " + (readEntries));
          readEntry(fm, dirName, resources, nameFM, depth + 1);
        }
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
