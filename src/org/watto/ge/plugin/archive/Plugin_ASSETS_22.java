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
import java.io.FileNotFoundException;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.Unity3DHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB5_ProcessWithinArchive;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.resource.Resource_Unity3D_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASSETS_22 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_22() {

    super("ASSETS_22", "Unity3D Engine Resource (Version 22)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Zoria: Age of Shattering: Prologue");

    setExtensions("assets"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(Unity3DHelper.getFileTypes());

    setTextPreviewExtensions("textasset"); // LOWER CASE

  }

  int[] fileTypeMapping = new int[1500]; // assuming a maximum of 1500 classes in a single file

  int numFileTypeMappings = 0;

  /**
   **********************************************************************************************
   When given a basePath, it looks for any ".split##" files and if it finds them, merges them in
   to the basePath file. (ie joins all the split files back into a real single archive file)
   **********************************************************************************************
   **/
  public File mergeSplitFiles(File basePath) {
    try {
      if (basePath.exists()) {
        // the file already exists - just return it
        return basePath;
      }

      String baseName = basePath.getAbsolutePath();
      // need to create this file, by merging all the split files in to it
      FileManipulator mergeFM = new FileManipulator(basePath, true);

      int splitNumber = 0;
      File splitFile = new File(baseName + ".split" + splitNumber);
      while (splitFile.exists()) {
        // copy all the contents from the split file into the merge file
        int splitLength = (int) splitFile.length();
        FileManipulator splitFM = new FileManipulator(splitFile, false);
        byte[] splitBytes = splitFM.readBytes(splitLength);
        mergeFM.writeBytes(splitBytes);
        splitFM.close();

        // Prepare the next split file
        splitNumber++;
        splitFile = new File(baseName + ".split" + splitNumber);
      }

      // done all the merging, so close it and return the merged file
      mergeFM.close();
      return basePath;
    }
    catch (Throwable t) {
      logError(t);
      return basePath;
    }
  }

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressLZ4Archive(FileManipulator fm, int firstOffset, int[] compLengths, int[] decompLengths) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      //File decompFile = new File(pathOnly + File.separatorChar + outputFilename);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZ4 exporter = Exporter_LZ4.getInstance();

      long currentOffset = firstOffset;

      int numBlocks = decompLengths.length;
      //boolean adjustedForPadding = false;
      for (int b = 0; b < numBlocks; b++) {
        // go to the right place in the file (just in case of an overshoot form a previous block)
        fm.seek(currentOffset);

        int decompBytesRemaining = decompLengths[b]; // so we can cut it short if there's an overshoot in the available()

        if (decompLengths[b] == compLengths[b]) {
          // this block isn't compressed - copy it raw
          while (decompBytesRemaining > 0) {
            decompFM.writeByte(fm.readByte());
            decompBytesRemaining--;
          }
        }
        else {

          exporter.open(fm, decompLengths[b], decompLengths[b]);

          while (exporter.available() && decompBytesRemaining != 0) {
            decompFM.writeByte(exporter.read());
            decompBytesRemaining--;
          }

          /*
          if (b == 0 && decompBytesRemaining == decompLengths[b]) {
          // we weren't able to decompress at all! Maybe we need to see if the offset is a multiple of 2 bytes, and adjust
          if (!adjustedForPadding) {
            adjustedForPadding = true;
            currentOffset = firstOffset + 1;
            b--; // because it'll get ++ added by the loop
            continue;
          }
          }
          */

          while (decompBytesRemaining > 0) { // if it's cut short for some reason, padd out to the right size
            decompFM.writeByte(0);
            decompBytesRemaining--;
          }
        }

        currentOffset += compLengths[b];
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();

      if (decompFile.length() <= 0) {
        // didn't decompress, so just return the original archive
        decompFile.delete();
        decompFile = origFile;
      }

      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setIndeterminate(false);
      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
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

        fm.skip(4); // for the next field
      }
      else if (FieldValidator.checkExtension(fm, "resource")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        File dirFile = getDirectoryFile(fm.getFile(), "assets");
        if (dirFile != null && dirFile.exists()) {
          rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
          return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
        }
      }
      else if (FieldValidator.checkExtension(fm, "ress")) { // "resS", but all lowercase for the comparison
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = fm.getFilePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 5) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 5));
          if (dirFile != null && dirFile.exists()) {
            rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
            return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
          }
        }
      }
      else if (FilenameSplitter.getExtension(fm.getFile()).indexOf("split") == 0) {
        // a split archive

        // check to see that the extension is actually filename.assets.split## or filename.resource.split##
        String pathName = fm.getFilePath();
        if (pathName.indexOf(".assets.split") > 0 || pathName.indexOf(".resource.split") > 0) {
          rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
          return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
        }
      }
      else if (fm.readString(4).equals("Unit")) {
        // No extension, so maybe a UnityFS file?

        // 8 - Header ("UnityFS" + null)
        String headerString = fm.readString(3);
        int headerByte = fm.readByte();
        if (headerString.equals("yFS") && headerByte == 0) {
          rating += 50;
        }

        // 4 - Version Number (6) (BIG ENDIAN)
        if (IntConverter.changeFormat(fm.readInt()) == 6) {
          rating += 5;
        }

        // X - General Version String (2019.2.10f1)
        if (fm.readString(4).equals("2019")) {
          rating += 5;
        }

        return rating;
      }
      else if (FilenameSplitter.getExtension(fm.getFile()).equals("")) {
        // no extension, like one of the "level" files
        rating += 20;

        fm.skip(4); // for the next field
      }

      // 4 - Unknown ((bytes)9,105,130,228)
      //fm.skip(4); // already skipped in the check above

      // 4 - Unknown ((bytes)247,127,0,0)
      fm.skip(4);

      // 4 - Version (22) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 22) {
        rating += 5;
      }

      // 4 - Unknown (82)
      if (fm.readInt() == 82) {
        rating += 5;
      }

      // 8 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      fm.skip(8);

      long arcSize = fm.getLength();

      // 8 - Size of Assets file (BIG ENDIAN)
      if (LongConverter.convertBig(fm.readBytes(8)) == arcSize) {
        rating += 5;
      }

      // 8 - Data Directory Offset (BIG ENDIAN)
      if (FieldValidator.checkOffset(LongConverter.convertBig(fm.readBytes(8)), arcSize)) {
        rating += 5;
      }

      // 4 - null
      fm.skip(4);

      // 4 - Unknown (82)
      if (fm.readInt() == 82) {
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

      // First up, if they clicked on the "resource" or the "resS" file, point to the ASSETS file instead
      String extension = FilenameSplitter.getExtension(path);
      if (extension.equals("resource")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        File dirFile = getDirectoryFile(path, "assets");
        if (dirFile != null && dirFile.exists()) {
          path = dirFile;
        }
      }
      else if (extension.equals("resS")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = path.getAbsolutePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 5) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 5));
          if (dirFile != null && dirFile.exists()) {
            path = dirFile;
          }
        }
      }
      else if (extension.length() > 1 && extension.indexOf("split") == 0) {
        // Found a split archive (used on Android, for example)
        // Lets merge all the splits together into a real assets file
        String pathName = path.getAbsolutePath();
        pathName = pathName.substring(0, pathName.length() - extension.length() - 1); // -1 for the "."
        File dirFile = new File(pathName);
        if (dirFile != null && dirFile.exists()) {
          // already built a merged archive for this file
          path = dirFile;
        }
        else {
          // need to create this file, by merging all the split files in to it
          path = mergeSplitFiles(dirFile);
        }

      }
      // Now we know we're pointing to the ASSETS file

      fileTypeMapping = new int[1500]; // assuming a maximum of 1500 classes in a single file
      numFileTypeMappings = 0;

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      boolean unityFS = false;
      int relativeOffset = 0;
      int relativeDataOffset = 0;
      String header = fm.readString(7);
      if (header.substring(0, 4).equals("FSB5")) {
        return new Plugin_FSB_FSB5().read(path);
      }
      else if (header.equals("UnityFS")) {
        // a UnityFS file - skip over the header stuff, to reach the real file data
        unityFS = true;

        // 8 - Header ("UnityFS" + null)
        // 4 - Version Number (6) (BIG ENDIAN)
        fm.skip(5);

        // X - General Version String (5.x.x)
        // 1 - null Terminator
        fm.readNullString();

        // X - Version String (5.5.2f1)
        // 1 - null Terminator
        fm.readNullString();

        // 4 - null
        fm.skip(4);

        // 4 - Archive Length (BIG ENDIAN)
        int arcLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(arcLength, arcSize);

        // 4 - Compressed Data Header Size (File Data Offset [+46]) (BIG ENDIAN)
        int compDataHeaderSize = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(compDataHeaderSize, arcSize);

        relativeOffset = compDataHeaderSize + 46;
        FieldValidator.checkOffset(relativeOffset, arcSize);

        // 4 - Decompressed Data Header Size (BIG ENDIAN)
        int decompDataHeaderSize = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompDataHeaderSize);

        // 4 - Compression Flags (BIG ENDIAN) (&3=LZ4, &1=LZMA, &80=DirectoryAtEnd, &40=EntryInfoPresent)
        int flags = IntConverter.changeFormat(fm.readInt());

        if ((flags & 128) == 128) {
          // the directory is at the end of the file
          fm.seek(arcLength - compDataHeaderSize);
        }

        if ((flags & 3) == 3) {

          // LZ4 Compression
          byte[] dirBytes = new byte[decompDataHeaderSize];
          int decompWritePos = 0;
          Exporter_LZ4 exporter = Exporter_LZ4.getInstance();
          exporter.open(fm, compDataHeaderSize, decompDataHeaderSize);

          for (int b = 0; b < decompDataHeaderSize; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              dirBytes[decompWritePos++] = (byte) exporter.read();
            }
          }

          // open the decompressed data for processing
          FileManipulator fmDir = new FileManipulator(new ByteBuffer(dirBytes));

          // 16 - Unknown
          fmDir.skip(16);

          // 4 - Number of Storage Blocks
          int numBlocks = IntConverter.changeFormat(fmDir.readInt());
          FieldValidator.checkNumFiles(numBlocks);

          int[] blockDecompLengths = new int[numBlocks];
          int[] blockCompLengths = new int[numBlocks];
          for (int b = 0; b < numBlocks; b++) {
            // 4 - Decomp Block Size
            int blockDecompLength = IntConverter.changeFormat(fmDir.readInt());
            blockDecompLengths[b] = blockDecompLength;

            // 4 - Comp Block Size
            int blockCompLength = IntConverter.changeFormat(fmDir.readInt());
            blockCompLengths[b] = blockCompLength;

            // 2 - Block Flags
            fmDir.skip(2);
            //System.out.println("Block " + b + " with comp length " + blockCompLength + " and decomp length " + blockDecompLength);
          }

          long currentOffset = fm.getOffset();

          // Decompress the file from the blocks
          FileManipulator decompFM = decompressLZ4Archive(fm, (int) currentOffset, blockCompLengths, blockDecompLengths);
          if (decompFM == null) {
            return null; // couldn't decompress the file for some reason
          }

          // 4 - Number of Bundle Entries
          int numEntries = IntConverter.changeFormat(fmDir.readInt());
          FieldValidator.checkNumFiles(numEntries);

          TaskProgressManager.setMessage(Language.get("Progress_SplittingArchive")); // progress bar
          TaskProgressManager.setIndeterminate(true);

          long initialOffset = fm.getOffset();
          // now we want to split the decompressed file into separate files for each entry
          File[] splitFiles = new File[numEntries];
          for (int e = 0; e < numEntries; e++) {
            // 8 - Offset
            //long entryOffset = LongConverter.changeFormat(fmDir.readLong());
            fmDir.skip(4);
            long entryOffset = IntConverter.changeFormat(fmDir.readInt());
            //FieldValidator.checkOffset(entryOffset, arcSize);

            // 8 - Decomp Length
            //long entryLength = LongConverter.changeFormat(fmDir.readLong());
            fmDir.skip(4);
            long entryLength = IntConverter.unsign(IntConverter.changeFormat(fmDir.readInt()));
            //FieldValidator.checkLength(entryLength, arcSize);

            // 4 - Flags
            int entryFlags = IntConverter.changeFormat(fmDir.readInt());

            // X - Name
            String entryName = fmDir.readNullString();

            //System.out.println("Entry " + entryName + " at offset " + entryOffset + " with length " + entryLength + " and flags " + entryFlags);

            if (numEntries == 1) {
              splitFiles[e] = decompFM.getFile();
              continue; // don't bother splitting if it's only 1 file anyway
            }

            // Build a new split file in the current directory, with the entry name
            File origFile = fm.getFile();
            String pathOnly = FilenameSplitter.getDirectory(origFile);

            File splitFile = new File(pathOnly + File.separatorChar + entryName);
            splitFiles[e] = splitFile;

            if (splitFile.exists()) {
              // we've already split this file before - don't split it again
            }
            else {
              // do the split
              FileManipulator fmSplit = new FileManipulator(splitFile, true);

              decompFM.seek(entryOffset);
              for (long b = 0; b < entryLength; b++) {
                fmSplit.writeByte(decompFM.readByte());
              }

              fmSplit.close();
            }

          }

          fm.close(); // close the original archive
          decompFM.close(); // close the decompressed file
          fmDir.close(); // close the decompressed directory

          // now we want to open each split file, one at a time, and use that to build the archive
          Resource[] resources = new Resource[0];
          int numResources = 0;

          TaskProgressManager.setIndeterminate(false);
          TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

          for (int e = 0; e < numEntries; e++) {
            // read the archive
            Resource[] splitResources = read(splitFiles[e]);
            if (splitResources == null) {
              continue;
            }

            // resize the existing array
            int numSplitResources = splitResources.length;
            int newArraySize = numResources + numSplitResources;
            Resource[] oldResources = resources;
            resources = new Resource[newArraySize];

            // add the split resources to the end of the array
            System.arraycopy(oldResources, 0, resources, 0, numResources);
            System.arraycopy(splitResources, 0, resources, numResources, numSplitResources);
            numResources = newArraySize;
          }

          return resources; // stop early, as we've already done all the reading

        }
        else if ((flags & 3) == 1) {
          // LZMA Compression
          ErrorLogger.log("[Plugin_ASSETS_22] LZMA Compression Not Implemented");
          return null;
        }
        else {
          // no compression

          // X - Other Stuff
          // X - Unity Archive
          fm.seek(relativeOffset);

          // Now we're reading the first 2 fields of the normal unity file

          // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
          fm.skip(4);

          // 4 - Size of Assets file (BIG ENDIAN)
          relativeDataOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;
          //FieldValidator.checkOffset(relativeDataOffset, arcSize + 1);// +1 to allow for UnityFS files where the data is all inline
        }

      }
      else {
        fm.skip(1);
      }

      // 4 - Unknown ((bytes)9,105,130,228)
      // 4 - Unknown ((bytes)247,127,0,0)
      //fm.skip(8); // already read 7 bytes in the check above, and 1 byte afterwards

      // 4 - Version Number (22) (BIG ENDIAN)
      // 4 - Unknown (82)
      // 8 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      // 8 - Size of Assets file (BIG ENDIAN)
      fm.skip(24);

      // 8 - Data Directory Offset (BIG ENDIAN)
      long dirOffset = LongConverter.convertBig(fm.readBytes(8)) + relativeOffset; // +relativeOffset to account for the UnityFS header, in those files
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - Unknown (82)
      fm.skip(8);

      // X - Version String (2020.1.0b15)
      // 1 - null Version String Terminator
      fm.readNullString();

      // 4 - Unknown
      // 1 - null
      fm.skip(5);

      // 4 - Number of Bases
      int numBases = fm.readInt();
      FieldValidator.checkNumFiles(numBases);

      // bring out the variables from the loop below
      int numFiles = 0;
      Resource[] resources = new Resource[0];

      // this boolean, while, try is so we can come back and try again, in case this file was extracted from a unityFS and it has nested property details
      boolean repeatCheck = true;
      boolean forcedUnityFS = false;
      long startOffset = fm.getOffset();

      int skipLarge = 35;
      int skipSmall = 19;
      int baseToCheck = 114;
      boolean skipAltered = false;
      boolean forcedLargeSkips = false;

      while (repeatCheck) {
        fm.seek(startOffset);

        repeatCheck = false;

        try {

          // BASES DIRECTORY
          // for each Base...
          for (int b = 0; b < numBases; b++) {
            // 4 - ID Number?
            int baseID = fm.readInt();
            if (baseID == baseToCheck) {
              // 35 - Base Name (encrypted)
              fm.skip(skipLarge);
            }
            else {
              // 19 - Base Name (encrypted)
              fm.skip(skipSmall);
            }

            if (unityFS) {
              // read all the nested property data

              // 4 - Number of Entries
              int numEntries = fm.readInt();
              try {
                FieldValidator.checkNumFiles(numEntries);
              }
              catch (Throwable t) {
                if (forcedUnityFS) {
                  if (!skipAltered) {

                    // If we've gone through loop without unityFS, then we've gone through with unityFS forced, then try option 3 which is some different sized skips for the encrypted names
                    skipAltered = true;
                    repeatCheck = true;

                    skipLarge -= 3;
                    skipSmall -= 3;
                    baseToCheck = -1;

                    unityFS = false; // so this + the throw below will trigger a re-loop
                    throw t;
                  }
                  else if (skipAltered) {
                    // option 4 - the small skip is 16 bytes and the large is 32 bytes. Assume we did a small skip (16) and we want to see
                    // about the large skip instead. We've already read 4 bytes for the check above, read another 12 then try numEntries again.
                    fm.skip(12);
                    numEntries = fm.readInt();
                    FieldValidator.checkNumFiles(numEntries);
                    /*
                    forcedLargeSkips = true;  
                    
                    skipSmall = skipLarge;
                    
                    unityFS = false; // so this + the throw below will trigger a re-loop
                    throw t;
                    */
                  }

                }
              }

              // 4 - Filename Directory Length
              //System.out.println(fm.getOffset());
              int filenameDirLength = fm.readInt();
              FieldValidator.checkLength(filenameDirLength, arcSize);

              // Some of the newer ones have size 32 (followed by a null) instead of size 24. Check that here...
              long preFilenameBlockOffset = fm.getOffset();

              //for (int e = 0; e < numEntries; e++) {
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 4 - Flags? (-1/1/4/16)
              // 4 - ID (incremental from 0)
              // 4 - Unknown
              //}
              // X - Filename Directory

              fm.skip(numEntries * 32);
              fm.skip(filenameDirLength);

              // 4 - null (check for a 32-byte directory)
              if (fm.readInt() != 0) {
                // wasn't 32-byte, try 24-byte (the old standard)
                fm.relativeSeek(preFilenameBlockOffset);

                fm.skip(numEntries * 24);
                fm.skip(filenameDirLength);
              }

            }

            // map the bases to the file types
            fileTypeMapping[b] = baseID;
          }
          numFileTypeMappings = numBases;

          // 4 - Number of Files
          numFiles = fm.readInt();
          if (numFiles < 8) {
            FieldValidator.checkNumFiles(numFiles);
          }
          else {
            FieldValidator.checkNumFiles(numFiles / 8);
          }

          // 0-3 - null to a multiple of 4 bytes
          fm.skip(calculatePadding(fm.getOffset() - relativeOffset, 4));

          resources = new Resource[numFiles];
          TaskProgressManager.setMaximum(numFiles);

          // FILES DIRECTORY
          // for each file (24 bytes per entry)
          for (int i = 0; i < numFiles; i++) {
            // 4 - ID Number (incremental from 1)
            fm.skip(4);

            // 4 - null
            int null1 = fm.readInt();

            // 8 - File Offset (relative to the start of the Filename Directory) - points to the FilenameLength field
            long offset = fm.readLong() + dirOffset;

            // 4 - File Size
            int length = fm.readInt();

            // 4 - File Type Code
            int fileTypeCode = fm.readInt();

            if (skipAltered) {
              // 8 - unknown
              fm.skip(8);
            }

            if (unityFS) {
              if (fileTypeCode == 0 && (length < 0 || offset < 0)) {
                // abrupt end of the directory
                numFiles = i;
                resources = resizeResources(resources, numFiles);
                break;
              }
            }
            // else, do checking as per normal
            FieldValidator.checkOffset(offset, arcSize + 1); // +1 to allow for empty files at the end of the archive
            FieldValidator.checkLength(length, arcSize);

            //String fileType = convertFileType(fileTypeCode);
            String fileType = null;
            if (fileTypeCode < 0) {
              for (int c = 0; c < numFileTypeMappings; c++) {
                if (fileTypeMapping[c] == fileTypeCode) {
                  fileType = Unity3DHelper.getFileExtension(c);
                  break;
                }
              }
              if (fileType == null) {
                fileType = "." + fileTypeCode;
              }
            }
            else {
              // 3.14
              //if (unityFS) {
              //  fileType = Unity3DHelper.getFileExtension(fileTypeCode);
              //}
              //else {
              try {
                int mapping = fileTypeMapping[fileTypeCode];
                if (mapping < 0) {
                  fileType = Unity3DHelper.getFileExtension(fileTypeCode);
                }
                else {
                  fileType = Unity3DHelper.getFileExtension(mapping);
                }
              }
              catch (Throwable t) {
                fileType = Unity3DHelper.getFileExtension(fileTypeCode);
              }
              //}
            }

            /*
            try {
            Integer.parseInt(fileType.substring(1));
            // output the details for further analysis
            System.out.println(null1 + "\t" + offset + "\t" + length + "\t" + fileTypeCode + "\t" + fileTypeCodeSmall + "\t" + unknownSmall + "\t" + null2);
            }
            catch (Throwable t) {
            }
            */

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, fileType, offset, length);

            TaskProgressManager.setValue(i);
          }

        }
        catch (Throwable t) {
          // try again, reading as if it IS a unityFS file
          if (!unityFS) {
            unityFS = true;
            forcedUnityFS = true;
            repeatCheck = true;
          }
          else {
            // if it was already being read as a unityFS file, we really do want to throw the exception
            throw t;
          }
        }

      } // end of repeatCheck while()

      if (forcedUnityFS) {
        unityFS = false; // reset so the remaining values are read properly
      }

      //
      // In this loop...
      // * Get the filenames for each file
      // * Detect all the Type1 Resources
      // * If a SND or TEX Resource has its data in an external archive, point to it instead
      // * Sets the exporter for the SND file, so that we can analyse and preview the audio
      //
      TaskProgressManager.setValue(0);

      ExporterPlugin exporterFSB = Exporter_Custom_FSB5_ProcessWithinArchive.getInstance();

      Resource[] type1Resources = new Resource[numFiles];
      int numType1Resources = 0;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String fileType = resource.getName();
        if (fileType.equals(".GameObject")) {
          // not a real file - just a folder structure or something
          // store it for analysis further down

          type1Resources[numType1Resources] = resource;
          numType1Resources++;

          continue;
        }

        // Go to the data offset
        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength <= 0) {
          resource.setName(Resource.generateFilename(i) + fileType);
          continue;
        }
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to 4 bytes
        int paddingSize = calculatePadding(filenameLength, 4);
        fm.skip(paddingSize);

        resource.setName(filename + fileType);

        long realOffset = fm.getOffset();
        long realSize = resource.getLength() - (realOffset - offset);

        if (fileType.equals(".TextAsset")) {
          // 4 - File Size
          realSize = fm.readInt();
          realOffset = fm.getOffset();
        }
        else if (fileType.equals(".AudioClip")) {
          try {
            // 4 - Unknown (0/1)
            // 4 - Number of Channels? (1/2)
            // 4 - Sample Rate? (44100)
            // 4 - Bitrate? (16)
            // 4 - Unknown
            // 4 - null
            // 4 - null
            // 2 - Unknown (1)
            // 2 - Unknown (1)
            fm.skip(32);

            // 4 - External Archive Filename Length
            int externalFilenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(externalFilenameLength);

            // X - External Archive Filename
            String externalFilename = fm.readString(externalFilenameLength);
            FieldValidator.checkFilename(externalFilename);

            // 0-3 - null Padding to 4 bytes
            int externalPaddingSize = calculatePadding(externalFilenameLength, 4);
            fm.skip(externalPaddingSize);

            // Check that the filename is a valid File in the FileSystem
            File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
            if (!unityFS && !externalArchive.exists()) {
              if (forcedUnityFS) {
                // see if we can find the archive name locally
                externalFilename = FilenameSplitter.getFilenameAndExtension(externalFilename);
                externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
                if (!externalArchive.exists()) {
                  throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
                }
              }
              else {
                throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
              }
            }
            else if (unityFS) {
              // UnityFS files contain the file data after the end of the unity data
              externalArchive = path;
            }

            long externalArcSize = externalArchive.length();

            // 4 - File Offset
            int extOffset = fm.readInt();
            if (unityFS) {
              extOffset += relativeDataOffset;
            }
            FieldValidator.checkOffset(extOffset, externalArcSize);

            // 4 - null
            fm.skip(4);

            // 4 - File Length
            int extSize = fm.readInt();
            FieldValidator.checkLength(extSize, externalArcSize);

            // 4 - null
            // 4 - Unknown (1)

            // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
            resource.setSource(externalArchive);
            realOffset = extOffset;
            realSize = extSize;

            resource.setExporter(exporterFSB);

            //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }
        else if (fileType.equals(".Texture2D")) {
          try {
            // 4 - Width/Height? (1024)
            int imageWidth = fm.readInt();

            // 4 - Width/Height? (1024/512)
            int imageHeight = fm.readInt();

            if (imageWidth == 4 && (imageHeight == 0 || imageHeight == 256)) {
              // these 2 fields were an 8-byte header, so need to read the real width/height next (GWENT game)
              // 4+256 = some newer v22 games like While True Learn

              // 4 - Width/Height? (1024)
              imageWidth = fm.readInt();

              // 4 - Width/Height? (1024/512)
              imageHeight = fm.readInt();
            }

            // 4 - File Size
            int imageFileSize = fm.readInt();

            // 4 - null
            fm.skip(4);

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 4 - Mipmap Count
            int mipmapCount = fm.readInt();

            if (resource.getLength() - imageFileSize > 0) {
              // This file is in the existing archive, not a separate archive file

              //System.out.println("INT" + fm.getOffset());

              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              for (int p = 0; p < 64; p += 4) { // a little loop to account for variable-sized padding
                // 4 - File Size
                realSize = fm.readInt();

                if (realSize == imageFileSize) {
                  // found the matching file size value
                  break;
                }
              }

              FieldValidator.checkLength(realSize, arcSize);

              realOffset = fm.getOffset();

              // Convert the Resource into a Resource_Unity3D_TEX
              Resource oldResource = resource;
              resource = new Resource_Unity3D_TEX();
              resource.copyFrom(oldResource); // copy the data from the old Resource to the new Resource
              resources[i] = resource; // stick the new Resource in the array, overwriting the old Resource

              // Set the image-specific properties on the new Resource
              Resource_Unity3D_TEX castResource = (Resource_Unity3D_TEX) resource;
              castResource.setImageWidth(imageWidth);
              castResource.setImageHeight(imageHeight);
              castResource.setFormatCode(imageFormat);
              castResource.setMipmapCount(mipmapCount);

              //System.out.println("This is an internal resource --> Resource " + resource.getName());
            }
            else {
              // This file is in an external archive, not the current one
              //System.out.println("EXT" + fm.getOffset());
              // 4 - Unknown (1)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - Unknown (2)
              // 4 - Unknown (2/1)
              // 4 - Unknown (1/0)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - Unknown (1)
              // 4 - null
              // 4 - Unknown (6)
              // 4 - Unknown (1)
              fm.skip(48);

              // 4 - null
              fm.skip(4);

              // 8 - File Offset
              long extOffset = fm.readLong();
              if (extOffset != 0 && ((int) extOffset) == 0) {
                // the first 4 bytes are null, the next 4 bytes are part of the offset
                extOffset = fm.readInt() | extOffset >> 32;
              }

              // 4 - File Length
              int extSize = fm.readInt();
              if (extSize != imageFileSize) {
                extOffset = extSize;
                extSize = fm.readInt();

                if (extSize != imageFileSize) {
                  extOffset = extSize;
                  extSize = fm.readInt();

                  if (extSize != imageFileSize) {
                    extOffset = extSize;
                    extSize = fm.readInt();
                  }
                }
              }

              // 4 - External Archive Filename Length
              int externalFilenameLength = fm.readInt();
              try {
                FieldValidator.checkFilenameLength(externalFilenameLength);
              }
              catch (Throwable t) {
                extOffset = extSize;
                extSize = externalFilenameLength;

                externalFilenameLength = fm.readInt();
                FieldValidator.checkFilenameLength(externalFilenameLength);
              }

              // X - External Archive Filename
              String externalFilename = fm.readString(externalFilenameLength);
              FieldValidator.checkFilename(externalFilename);

              // 0-3 - null Padding to 4 bytes
              int externalPaddingSize = calculatePadding(externalFilenameLength, 4);
              fm.skip(externalPaddingSize);

              // Check that the filename is a valid File in the FileSystem
              File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
              //if (!externalArchive.exists()) {
              //  throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
              //}
              if (!unityFS && !externalArchive.exists()) {
                if (forcedUnityFS) {
                  // see if we can find the archive name locally
                  externalFilename = FilenameSplitter.getFilenameAndExtension(externalFilename);
                  externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
                  if (!externalArchive.exists()) {
                    throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
                  }
                }
                else {
                  throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
                }
              }
              else if (unityFS) {
                // UnityFS files contain the file data after the end of the unity data
                externalArchive = path;
              }

              if (unityFS) {
                extOffset += relativeDataOffset;
              }

              // Now check the offsets and sizes
              long externalArcSize = externalArchive.length();

              FieldValidator.checkOffset(extOffset, externalArcSize);
              FieldValidator.checkLength(extSize, externalArcSize);

              // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
              resource.setSource(externalArchive);
              realOffset = extOffset;
              realSize = extSize;

              // Convert the Resource into a Resource_Unity3D_TEX
              Resource oldResource = resource;
              resource = new Resource_Unity3D_TEX();
              resource.copyFrom(oldResource); // copy the data from the old Resource to the new Resource
              resources[i] = resource; // stick the new Resource in the array, overwriting the old Resource

              // Set the image-specific properties on the new Resource
              Resource_Unity3D_TEX castResource = (Resource_Unity3D_TEX) resource;
              castResource.setImageWidth(imageWidth);
              castResource.setImageHeight(imageHeight);
              castResource.setFormatCode(imageFormat);
              castResource.setMipmapCount(mipmapCount);

              //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
            }
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }
        else if (fileType.equals(".Cubemap")) {
          try {
            // 4 - Unknown (32)
            // 4 - Unknown (32)
            // 4 - Unknown (1392)
            // 4 - Unknown (24)
            // 4 - Unknown (6)
            // 4 - null
            // 4 - Unknown (6)
            // 4 - Unknown (2)
            // 4 - Unknown (2)
            // 4 - null
            // 4 - null
            // 4 - Unknown (1)
            // 4 - null
            // 4 - null
            // 4 - null
            fm.skip(60);

            // 4 - File Offset
            int extOffset = fm.readInt();

            // 4 - File Length
            int extSize = fm.readInt();

            // 4 - External Archive Filename Length
            int externalFilenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(externalFilenameLength);

            // X - External Archive Filename
            String externalFilename = fm.readString(externalFilenameLength);
            FieldValidator.checkFilename(externalFilename);

            // Check that the filename is a valid File in the FileSystem
            File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
            if (!externalArchive.exists()) {
              throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
            }

            long externalArcSize = externalArchive.length();

            // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
            FieldValidator.checkOffset(extOffset, externalArcSize);
            FieldValidator.checkLength(extSize, externalArcSize);

            resource.setSource(externalArchive);
            realOffset = extOffset;
            realSize = extSize;

            //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }

        resource.setOffset(realOffset);
        resource.setLength(realSize);
        resource.setDecompressedLength(realSize);
        TaskProgressManager.setValue(i);
      }

      //
      // In this loop...
      // * Go through all the Type 1 Resources and use them to set the folder names on the referenced Resources
      //
      TaskProgressManager.setValue(0);

      if (!unityFS && !forcedUnityFS) {
        for (int r = 0; r < numType1Resources; r++) {
          Resource resource = type1Resources[r];

          // Go to the data offset
          long offset = resource.getOffset();
          fm.seek(offset);

          // 4 - Number of Referenced Files
          int numRefFiles = fm.readInt();
          FieldValidator.checkNumFiles(numRefFiles);

          // for each referenced file...
          Resource[] refResources = new Resource[numRefFiles];
          for (int i = 0; i < numRefFiles; i++) {
            // 4 - null
            fm.skip(4);

            // 4 - File ID of Referenced File
            int refFileID = fm.readInt() - 1; // -1 because fileID numbers start at 1, not 0
            FieldValidator.checkRange(refFileID, 0, numFiles);
            refResources[i] = resources[refFileID];

            // 4 - null
            fm.skip(4);
          }

          // 4 - null
          fm.skip(4);

          // 4 - Folder Name Length
          int folderNameLength = fm.readInt();
          if (folderNameLength == 0) {
            continue;
          }
          FieldValidator.checkFilenameLength(folderNameLength);

          // X - Folder Name
          String folderName = fm.readString(folderNameLength);
          FieldValidator.checkFilename(folderName);

          // Set the folder name for each referenced file
          for (int i = 0; i < numRefFiles; i++) {
            Resource refResource = refResources[i];

            // Otherwise for other refType files (and for the ref files as well), set the folder name on the file itself.
            //System.out.println("Setting folder name " + folderName + " on Resource " + refResource.getName());
            refResource.setName(folderName + "\\" + refResource.getName());

          }

          TaskProgressManager.setValue(r);
        }
      }

      //
      // In this loop...
      // * Remove all the Type1 Resources from the File List --> they're not real files
      // * Clear all the renames/replaced flags on the Resource, caused by setting the name, changing the file size, etc.
      // * Set the forceNotAdded flag on the Resources to override the "added" icons
      //
      int realNumFiles = numFiles - numType1Resources;
      Resource[] realResources = new Resource[realNumFiles];
      int realArrayPos = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.getExtension().equals("GameObject")) {
          continue;
        }

        resource.setReplaced(false);
        resource.setOriginalName(resource.getName());
        resource.forceNotAdded(true);

        realResources[realArrayPos] = resource;
        realArrayPos++;
      }

      fm.close();

      //return resources;
      return realResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
