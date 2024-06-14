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
import java.util.Hashtable;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RPACK_RP6L extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RPACK_RP6L() {

    super("RPACK_RP6L", "RPACK_RP6L");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dying Light: Bad Blood",
        "FIM Speedway Grand Prix 15",
        "Dead Island");
    setExtensions("rpack"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("tex_data", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("tex_thumbnail", "Texture Thumbnail Image", FileType.TYPE_OTHER),
        new FileType("tex_header", "Texture Header", FileType.TYPE_OTHER));

    setTextPreviewExtensions("dir"); // LOWER CASE

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

      // 4 - Header (RP6L)
      if (fm.readString(4).equals("RP6L")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Unknown (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // 4 - Number of Files in the Files Directory
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(8);

      // 4 - Filename Directory Length
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public File decompressArchive(FileManipulator fm, long offset, long compLength, long decompLength, int blockNumber) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed_" + blockNumber + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return decompFile;
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long arcSize = fm.getLength();

      fm.seek(offset); // return to the start, ready for decompression

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, (int) compLength, (int) decompLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
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

      // 4 - Header (RP5L)
      // 4 - Version (1)
      // 4 - Unknown (1/0)
      fm.skip(12);

      // 4 - Number of Files in the Files Directory
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      // 4 - Number of Filenames
      int numUnknown1 = fm.readInt();
      FieldValidator.checkNumFiles(numUnknown1);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number of Filenames
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);

      // 4 - Unknown (1)
      fm.skip(4);

      // Read the block offsets
      long[] blockOffsets = new long[numBlocks];
      long[] blockLengths = new long[numBlocks];
      long[] blockDecompLengths = new long[numBlocks];

      int[] fileTypes = new int[numBlocks];
      String[] extensions = new String[numBlocks];
      File[] blockDataFiles = new File[numBlocks];
      for (int i = 0; i < numBlocks; i++) {
        // 1 - File Type
        int fileType = ByteConverter.unsign(fm.readByte());
        fileTypes[i] = fileType;
        extensions[i] = "" + fileType;

        if (fileType == 16) {
          extensions[i] = "msh_data";
        }
        else if (fileType == 17) {
          extensions[i] = "msh_header";
        }
        else if (fileType == 18) {
          extensions[i] = "msh_footer";
        }
        else if (fileType == 32) {
          extensions[i] = "tex_header";
        }
        else if (fileType == 33) {
          extensions[i] = "tex_data";
        }
        else if (fileType == 34) {
          extensions[i] = "tex_thumbnail";
        }
        else if (fileType == 48) {
          extensions[i] = "shd";
        }
        else if (fileType == 64) {
          extensions[i] = "anm";
        }
        else if (fileType == 80) {
          extensions[i] = "fx";
        }
        else if (fileType == 240) {
          extensions[i] = "240"; // mesh-related
        }
        else if (fileType == 241) {
          extensions[i] = "241"; // mesh-related
        }
        else if (fileType == 255) {
          extensions[i] = "dir";
        }

        // 3 - Unknown
        fm.skip(3);

        // 4 - Offset to Block Data
        long blockOffset = IntConverter.unsign(fm.readInt());
        FieldValidator.checkOffset(blockOffset, arcSize);
        blockOffsets[i] = blockOffset;

        // 4 - Uncompressed Block Length
        long blockDecompLength = IntConverter.unsign(fm.readInt());

        // 4 - Compressed Block Length (null = uncompressed)
        long blockLength = IntConverter.unsign(fm.readInt());

        if (blockLength == 0) {
          // uncompressed
          FieldValidator.checkLength(blockDecompLength, arcSize);
          blockDataFiles[i] = null;

          blockLengths[i] = 0;
          blockDecompLengths[i] = 0;
        }
        else {
          // compressed
          FieldValidator.checkLength(blockLength, arcSize);
          FieldValidator.checkLength(blockDecompLength);

          blockLengths[i] = blockLength;
          blockDecompLengths[i] = blockDecompLength;

        }

        // 4 - Unknown
        fm.skip(4);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // decompress any blocks that are compressed

      for (int i = 0; i < numBlocks; i++) {
        long blockLength = blockLengths[i];
        long blockDecompLength = blockDecompLengths[i];
        long blockOffset = blockOffsets[i];

        if (blockLength != 0 && blockDecompLength != 0) {
          // extract the block to a temporary file
          blockDataFiles[i] = decompressArchive(fm, blockOffset, blockLength, blockDecompLength, i);
        }
      }

      // skip to the filenames
      //fm.seek(36 + numBlocks * 20 + numFiles * 16 + numUnknown1 * 12 + numFilenames * 4);
      fm.seek(36 + numBlocks * 20 + numFiles * 16 + numUnknown1 * 12);

      // read the Filename Offsets Directory
      int[] filenameOffsets = new int[numFilenames]; // 3.15 Need to use filename offsets because they're not guaranteed to be in order
      for (int i = 0; i < numFilenames; i++) {
        //System.out.println(i + "\t" + IntConverter.unsign(fm.readInt()));
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);
        filenameOffsets[i] = filenameOffset;
      }

      // read the Filename Directory
      String[] filenames = new String[numFiles];
      long endOfDirectory = fm.getOffset() + filenameDirLength;
      Hashtable<Integer, String> nameMap = new Hashtable<Integer, String>(numFilenames); // 3.15 Doing a name mapping because they aren't guaranteed to be in order
      //System.out.println(fm.getOffset());
      int nameOffset = 0;
      for (int i = 0; i < numFilenames; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        //fm.skip(calculatePadding(filename.length() + 1, 4));

        filenames[i] = filename;

        nameMap.put(nameOffset, filename);

        nameOffset += filename.length() + 1;

        if (fm.getOffset() >= endOfDirectory) {
          break; // finished reading all the filenames
        }
      }

      // Go back to the Files Directory
      fm.seek(36 + numBlocks * 20);

      // Loop through the Files Directory

      short[] filenameIDs = new short[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 2 - Block ID
        short blockID = fm.readByte();
        fm.skip(1);
        FieldValidator.checkRange(blockID, 0, numBlocks);

        // 2 - File Name ID
        short filenameID = fm.readShort();
        FieldValidator.checkRange(filenameID, 0, numFilenames);
        filenameIDs[i] = filenameID;

        File blockFile = blockDataFiles[blockID];

        // 4 - File Offset (relative to the start of the first file)
        long offset = IntConverter.unsign(fm.readInt());
        if (blockFile == null) {
          offset += blockOffsets[blockID]; // block is in the original archive
          FieldValidator.checkOffset(offset, arcSize);
        }
        else {
          // block has already been extracted and decompressed, so the offset starts at zero
        }

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Duplicate File ID (null = not a duplicate file, otherwise this is the ID number of the file that this duplicates. indexes start at 0)
        //int length = fm.readInt();
        //FieldValidator.checkLength(length, arcSize);
        fm.skip(4);

        //String filename = filenames[filenameID] + "." + blockID;
        String filename = nameMap.get(filenameOffsets[filenameID]) + "." + extensions[blockID];

        //System.out.println(i + "\t" + filename);

        if (blockFile == null) {
          blockFile = path;
        }

        /*
        //path,name,offset,length,decompLength,exporter
        if (length == 0) { // empty files should use the default exporter (because they're empty)
          resources[i] = new Resource(blockFile, filename, offset, decompLength);
        }
        else { // only want to use the ZLib exporter for real files
          resources[i] = new Resource(blockFile, filename, offset, length, decompLength, exporter);
        }
        */
        Resource resource = new Resource(blockFile, filename, offset, decompLength);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      // now go through the files and set the related file links (eg to link the tex_header and tex_data together)
      for (int i = 0; i < numFiles; i++) {
        short thisFilenameID = filenameIDs[i];
        Resource thisResource = resources[i];

        for (int j = i + 1; j < numFiles; j++) {
          short comparisonFilenameID = filenameIDs[j];

          if (thisFilenameID == comparisonFilenameID) {
            // related file
            Resource comparisonResource = resources[j];

            thisResource.addProperty(comparisonResource.getExtension(), j);
            comparisonResource.addProperty(thisResource.getExtension(), i);
          }
          else {
            // found all the related files - exit early
            j = numFiles;
          }
        }
      }

      /*
      // go and check for zlib compression
      fm.getBuffer().setBufferSize(1);
      ExporterPlugin exporterZLibCompressedOnly = Exporter_ZLib_CompressedSizeOnly.getInstance();
      
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());
        if (fm.readString(1).equals("x")) {
          resource.setExporter(exporterZLibCompressedOnly);
        }
      }
      */

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
