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
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.stream.ManipulatorUnclosableOutputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LHD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LHD() {

    super("LHD", "LHD");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Lead and Gold: Gangs of the Wild West");
    setExtensions("lhd"); // MUST BE LOWER CASE
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public File decompressArchive(File origFile) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return decompFile;
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      FileManipulator fm = new FileManipulator(origFile, false);

      int compLength = (int) fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive") + " " + filenameOnly + "." + extensionOnly); // progress bar
      TaskProgressManager.setMaximum(compLength); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // 4 - Number Of Compressed Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      // 4 - Decompressed Block Size (65536)
      int blockSize = fm.readInt();

      TaskProgressManager.setMaximum(numBlocks);

      long[] offsets = new long[numBlocks];
      long[] lengths = new long[numBlocks];

      // Loop through directory
      for (int i = 0; i < numBlocks; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, compLength);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, compLength);
        lengths[i] = length;
      }

      // decompress each block
      for (int i = 0; i < numBlocks; i++) {
        fm.relativeSeek(offsets[i]);

        exporter.open(new Resource(origFile, "", offsets[i], lengths[i], blockSize));

        while (exporter.available()) {
          decompFM.writeByte(exporter.read());
        }

        exporter.close();

      }

      fm.close();

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the decompressed file
      return decompFile;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      TaskProgressManager.setMaximum(numNames);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // X - Name
        // 1 - null Name Terminator
        names[i] = fm.readNullString();
        TaskProgressManager.setValue(i);
      }

      // 4 - Number of Entries
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);
      //fm.skip(numEntries * 16);

      String[] filenameLookup = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // 4 - Directory Name ID
        int parentID = fm.readInt();
        FieldValidator.checkRange(parentID, 0, numNames);

        // 4 - File Name ID
        int nameID = fm.readInt();
        FieldValidator.checkRange(parentID, 0, numNames);

        filenameLookup[i] = names[parentID] + names[nameID];

        // 4 - File Length (0 = empty files or directories)
        // 4 - Archive ID (-1 = not in any archives (it's a directory name only))
        fm.skip(8);
      }

      // 4 - Number of FCL archives
      int numFCLs = fm.readInt();
      FieldValidator.checkNumFiles(numFCLs);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFCLs);

      String basePath = FilenameSplitter.getDirectory(path) + File.separatorChar;

      for (int a = 0; a < numFCLs; a++) {
        // 4 - Archive Filename ID
        int archiveNameID = fm.readInt();
        FieldValidator.checkRange(archiveNameID, 0, numNames);
        String archiveName = names[archiveNameID];

        // Find the archive in the filesystem
        File arcPath = new File(basePath + archiveName + ".fcl");
        if (!arcPath.exists()) {
          ErrorLogger.log("[LHD] Missing archive file " + archiveName);
          return null;
        }

        // Now try to find the decompressed archive (and decompress it if needed)
        arcPath = decompressArchive(arcPath);
        if (arcPath == null || !arcPath.exists()) {
          ErrorLogger.log("[LHD] Could not decompress archive file " + archiveName);
          return null;
        }

        arcSize = arcPath.length();

        archiveName += File.separatorChar; // so we can prepend the archive name to the filename

        // 4 - Number of Files in this Archive
        int numFilesInArchive = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInArchive);

        String[] filenames = new String[numFilesInArchive];
        for (int i = 0; i < numFilesInArchive; i++) {
          // 4 - Filename ID (ID pointer into the Filenames Directory)
          int filenameID = fm.readInt();
          FieldValidator.checkRange(filenameID, 0, numFilenames);
          filenames[i] = filenameLookup[filenameID];
        }

        long[] offsets = new long[numFilesInArchive];
        for (int i = 0; i < numFilesInArchive; i++) {
          // 4 - File Offset (in the decompressed archive)
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        // work out the file lengths
        long[] lengths = new long[numFilesInArchive];
        for (int i = 0; i < numFilesInArchive - 1; i++) {
          lengths[i] = offsets[i + 1] - offsets[i];
        }
        lengths[numFilesInArchive - 1] = arcSize - offsets[numFilesInArchive - 1];

        // add the Resources
        for (int i = 0; i < numFilesInArchive; i++) {
          Resource resource = new Resource(arcPath, archiveName + filenames[i], offsets[i], lengths[i]);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;
          realNumFiles++;
        }

        TaskProgressManager.setValue(a);
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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      //
      // STEP 1
      // Make a HashMap of all the files in the archive, so we can quickly find the matching ones when we need them.
      // Strip off the archive name from the files in the archive, so we can match them early in Step 2 (filename directory).
      //
      HashMap<String, Resource> resourceMap = new HashMap<String, Resource>(numFiles);
      char slashChar = File.separatorChar;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String resourceName = resource.getName();
        int slashPos = resourceName.indexOf(slashChar);
        if (slashPos > 0) {
          resourceName = resourceName.substring(slashPos + 1);
        }
        resourceMap.put(resourceName, resource);
      }

      //
      // STEP 2
      // Read the LHD, just like we do in read(). 
      // Update the file lengths in the Filename Directory, if the resource has changed.
      // Work out what archive files have been changed, so we know what FCLs to rebuild.
      // Store the order of Resources in each FCL, so we can build it the next Step. 
      //

      // 4 - Number of Names
      int numNames = src.readInt();
      FieldValidator.checkNumFiles(numNames);
      fm.writeInt(numNames);

      TaskProgressManager.setMaximum(numNames);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // X - Name
        // 1 - null Name Terminator
        String name = src.readNullString();
        names[i] = name;
        fm.writeString(name);
        fm.writeByte(0);
      }

      // 4 - Number of Entries
      int numFilenames = src.readInt();
      FieldValidator.checkNumFiles(numFilenames);
      fm.writeInt(numFilenames);

      String[] filenameLookup = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // 4 - Directory Name ID
        int parentID = src.readInt();
        FieldValidator.checkRange(parentID, 0, numNames);
        fm.writeInt(parentID);

        // 4 - File Name ID
        int nameID = src.readInt();
        FieldValidator.checkRange(parentID, 0, numNames);
        fm.writeInt(nameID);

        String filename = names[parentID] + names[nameID];
        filenameLookup[i] = filename;

        Resource resource = resourceMap.get(filename);
        if (resource != null && resource.isReplaced()) {
          // 4 - File Length (0 = empty files or directories)
          src.skip(4);
          fm.writeInt(resource.getDecompressedLength());

          // 4 - Archive ID (-1 = not in any archives (it's a directory name only))
          fm.writeBytes(src.readBytes(4));
        }
        else {
          // 4 - File Length (0 = empty files or directories)
          // 4 - Archive ID (-1 = not in any archives (it's a directory name only))
          fm.writeBytes(src.readBytes(8));
        }
      }

      // 4 - Number of FCL archives
      int numFCLs = src.readInt();
      FieldValidator.checkNumFiles(numFCLs);
      fm.writeInt(numFCLs);

      String[] archiveNames = new String[numFCLs]; // the names of each archive
      Resource[][] resourceLists = new Resource[numFCLs][0]; // the resources in each archive
      boolean[] archiveEdited = new boolean[numFCLs]; // whether any files in the archive were replaced or not

      for (int a = 0; a < numFCLs; a++) {
        // 4 - Archive Filename ID
        int archiveNameID = src.readInt();
        FieldValidator.checkRange(archiveNameID, 0, numNames);
        fm.writeInt(archiveNameID);

        String archiveName = names[archiveNameID];
        archiveNames[a] = archiveName;

        archiveEdited[a] = false;

        // 4 - Number of Files in this Archive
        int numFilesInArchive = src.readInt();
        FieldValidator.checkNumFiles(numFilesInArchive);
        fm.writeInt(numFilesInArchive);

        Resource[] resourceList = new Resource[numFilesInArchive];
        resourceLists[a] = resourceList;

        String[] filenames = new String[numFilesInArchive];
        for (int i = 0; i < numFilesInArchive; i++) {
          // 4 - Filename ID (ID pointer into the Filenames Directory)
          int filenameID = src.readInt();
          FieldValidator.checkRange(filenameID, 0, numFilenames);
          fm.writeInt(filenameID);

          String filename = filenameLookup[filenameID];
          filenames[i] = filename;
          resourceList[i] = resourceMap.get(filename);
        }

        long offset = 0;
        for (int i = 0; i < numFilesInArchive; i++) {
          // 4 - File Offset (in the decompressed archive)
          src.skip(4);
          fm.writeInt(offset);

          Resource resource = resourceList[i];
          if (resource.isReplaced()) {
            // at least 1 file was edited in this archive, so it'll have to be rebuilt
            archiveEdited[a] = true;
          }
          offset += resource.getDecompressedLength();
        }

        TaskProgressManager.setValue(a);
      }

      // write the rest of the LHD file (not sure what data it contains)
      int remainingSize = (int) src.getRemainingLength();
      fm.writeBytes(src.readBytes(remainingSize));

      src.close();
      fm.close();

      //
      // STEP 3
      // If an archive was edited, we want to rebuild it (in a decompressed state)
      //
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      String basePath = FilenameSplitter.getDirectory(path) + File.separatorChar;

      File[] decompArchiveNames = new File[numFCLs];
      File[] compArchiveNames = new File[numFCLs];
      for (int a = 0; a < numFCLs; a++) {
        if (!archiveEdited[a]) {
          continue; // not an edited archive
        }

        File decompOutputPath = new File(basePath + archiveNames[a] + "_edited_ge_decompressed.fcl");
        File compOutputPath = new File(basePath + archiveNames[a] + "_edited.fcl");
        for (int f = 0; f < 100; f++) {
          if (decompOutputPath.exists() || compOutputPath.exists()) {
            decompOutputPath = new File(basePath + archiveNames[a] + (f + 1) + "_edited_ge_decompressed.fcl");
            compOutputPath = new File(basePath + archiveNames[a] + (f + 1) + "_edited.fcl");
          }
          else {
            break; // found a valid filename
          }
        }

        decompArchiveNames[a] = decompOutputPath;
        compArchiveNames[a] = compOutputPath;

        // write the archive file (decompressed)
        Resource[] resourceList = resourceLists[a];
        FileManipulator archiveFM = new FileManipulator(decompOutputPath, true);
        write(resourceList, archiveFM);
        archiveFM.close();
      }

      //
      // STEP 4
      // If we have rebuilt an edited archive, we now want to re-compress it
      // 
      for (int a = 0; a < numFCLs; a++) {
        if (!archiveEdited[a]) {
          continue; // not an edited archive
        }

        File decompOutputPath = decompArchiveNames[a];
        File compOutputPath = compArchiveNames[a];

        // write the archive file (compressed)
        FileManipulator decompFM = new FileManipulator(decompOutputPath, false);
        FileManipulator archiveFM = new FileManipulator(compOutputPath, true);

        int decompLength = (int) decompOutputPath.length();
        int numBlocks = decompLength / 65536;
        int lastBlock = decompLength % 65536;

        if (lastBlock != 0) {
          numBlocks++;
        }

        int dataOffset = 8 + (numBlocks * 8);

        // 4 - Number of Blocks
        archiveFM.writeInt(numBlocks);

        // 4 - Decompressed Block Size (65536)
        archiveFM.writeInt(65536);

        // DETAILS DIRECTORY
        archiveFM.setLength(dataOffset);
        archiveFM.relativeSeek(dataOffset);

        // BLOCK DATA
        // for each block
        int[] compLengths = new int[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
          // X - Block Data (ZLib Compression)
          // 4 - Decompressed Block Size (65536)
          int blockSize = 65536;
          if (i == numBlocks - 1 && lastBlock != 0) {
            blockSize = lastBlock;
          }
          byte[] decompBlock = decompFM.readBytes(blockSize);

          if (blockSize != 65536) {
            // the last block needs to be padded to 65536 byte anyway (game crashes otherwise)
            byte[] oldDecompBlock = decompBlock;
            decompBlock = new byte[65536];
            Arrays.fill(decompBlock, (byte) 0);
            System.arraycopy(oldDecompBlock, 0, decompBlock, 0, blockSize);
            blockSize = 65536;
          }

          long startPos = archiveFM.getOffset();

          DeflaterOutputStream outputStream = new DeflaterOutputStream(new ManipulatorUnclosableOutputStream(archiveFM));
          outputStream.write(decompBlock);
          outputStream.finish();
          outputStream.close();

          long endPos = archiveFM.getOffset();
          compLengths[i] = (int) (endPos - startPos);

          // 4 - Decomp Block Length
          archiveFM.writeInt(blockSize);
        }

        // go back and write the directory, now that we have the compressed sizes
        archiveFM.seek(8);

        // for each file
        int offset = dataOffset;
        for (int i = 0; i < numBlocks; i++) {
          // 4 - Block Offset
          archiveFM.writeInt(offset);

          // 4 - Block Compressed Length (including the field at the end of each file data)
          int length = compLengths[i] + 4;
          archiveFM.writeInt(length);

          offset += length;
        }

        decompFM.close();
        archiveFM.close();
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
