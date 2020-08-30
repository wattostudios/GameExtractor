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
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TTARCH_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TTARCH_2() {

    super("TTARCH_2", "TTARCH_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Back to the Future: The Game: Episode 1");
    setExtensions("ttarch"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("aud", "Audio File", FileType.TYPE_AUDIO),
        new FileType("vox", "Vox Audio File", FileType.TYPE_OTHER));

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
  public FileManipulator decompressArchive(FileManipulator fm, int[] compLengths, int[] decompLengths) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      int numBlocks = compLengths.length;

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(numBlocks); // progress bar

      long currentOffset = fm.getOffset();

      Exporter_Deflate exporter = Exporter_Deflate.getInstance();
      //fm.getBuffer().setBufferSize(65536);
      for (int i = 0; i < numBlocks; i++) {
        int compLength = compLengths[i];

        fm.seek(currentOffset);
        exporter.open(fm, compLength, decompLengths[i]); // decomp lengths are max 65536

        while (exporter.available()) {
          decompFM.writeByte(exporter.read());
        }

        // ensure we're at the correct place for the next block to be read
        currentOffset += compLength;

        TaskProgressManager.setValue(i);
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
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
      }

      // 4 - Unknown (8)
      if (fm.readInt() == 8) {
        rating += 5;
      }

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // 4 - Unknown (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // 4 - Compression Flag (1=No Compression, 2=Compressed)
      if (FieldValidator.checkRange(fm.readInt(), 1, 2)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown (8)
      // 4 - null
      // 4 - Unknown (2)
      fm.skip(12);

      // 4 - Compression Flag (1=No Compression, 2=Compressed)
      int compression = fm.readInt();

      // 4 - Number of Compressed Blocks (null if there's no compression)
      int numCompressedBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numCompressedBlocks + 1); // +1 to allow "null" here as a valid value

      //fm.skip(numCompressedBlocks * 4);
      int[] compLengths = new int[numCompressedBlocks];
      int[] decompLengths = new int[numCompressedBlocks];
      for (int i = 0; i < numCompressedBlocks; i++) {
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);
        compLengths[i] = compLength;
        decompLengths[i] = 65536;
      }

      // 4 - File Data Length
      long relativeOffset = 0;
      if (numCompressedBlocks > 0) {
        fm.skip(4);
      }
      else {
        relativeOffset = arcSize - fm.readInt();
        FieldValidator.checkOffset(relativeOffset, arcSize);
      }

      // 4 - Unknown (51824)
      int lastDecompLength = fm.readInt();
      FieldValidator.checkLength(lastDecompLength, arcSize);
      if (numCompressedBlocks > 0) {
        decompLengths[numCompressedBlocks - 1] = lastDecompLength;
      }

      if (numCompressedBlocks > 0) {
        // 4 - Unknown (9999999)
        // 8 - null
        // 4 - Unknown (64)
        // 1 - null
        fm.skip(17);

        // 4 - File Data Offset [+49 + (numCompressedBlocks*4)]
        int decompressedDirectoryLength = fm.readInt();
        int fileDataOffset = decompressedDirectoryLength + 49 + (numCompressedBlocks * 4);
        FieldValidator.checkOffset(fileDataOffset, arcSize);

        if (compression == 2) {
          // 4 - Compressed Folders Directory and Details Directory Length
          int compressedLength = fm.readInt();
          FieldValidator.checkLength(compressedLength, arcSize);

          fileDataOffset += 4;

          long currentOffset = fm.getOffset();

          // Now do the decompress of the directory...
          byte[] dirBytes = new byte[decompressedDirectoryLength];
          int decompWritePos = 0;
          Exporter_Deflate exporter = Exporter_Deflate.getInstance();
          exporter.open(fm, compressedLength, decompressedDirectoryLength);

          for (int b = 0; b < decompressedDirectoryLength; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              dirBytes[decompWritePos++] = (byte) exporter.read();
            }
          }

          // Ensure we're at the correct place in the archive
          fm.seek(currentOffset + compressedLength);

          // now decompress the rest of the archive
          FileManipulator decompFM = decompressArchive(fm, compLengths, decompLengths);
          if (decompFM != null) {
            path = decompFM.getFile(); // So the resources are stored against the decompressed file
            arcSize = path.length();
            decompFM.close(); // we don't want to read from this file, we're going to read from the decompressed directory below instead
          }

          // open the decompressed directory for processing
          fm.close();
          fm = new FileManipulator(new ByteBuffer(dirBytes));
        }
      }

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // read the folder names
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Length
        int folderNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(folderNameLength);

        // X - Folder Name
        fm.skip(folderNameLength);
      }

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - null
        fm.skip(4);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + relativeOffset;// + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);
        resources[i].forceNotAdded(true);

        TaskProgressManager.setValue(i);
      }

      // now check all the AUD files, to remove the header from them
      fm.getBuffer().setBufferSize(60);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.getExtension().equalsIgnoreCase("aud")) {
          if (!resource.isCompressed()) {
            fm.seek(resource.getOffset());
            if (fm.readString(4).equals("ERTM")) {
              fm.skip(52);
              long offset = fm.getOffset();
              if (fm.readString(4).equals("OggS")) {
                long length = resource.getDecompressedLength() - 56;

                resource.setLength(length);
                resource.setDecompressedLength(length);
                resource.setOffset(offset);
              }
            }
          }
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

}
