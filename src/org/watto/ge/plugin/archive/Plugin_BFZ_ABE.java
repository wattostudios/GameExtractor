/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BFZ_ABE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BFZ_ABE() {

    super("BFZ_ABE", "BFZ_ABE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Zombi");
    setExtensions("bfz"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("son", "SON Audio", FileType.TYPE_AUDIO));

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
      if (fm.readInt() == 4538945) { //"ABE"+null
        rating += 50;
      }

      fm.skip(36);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
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
  public FileManipulator decompressArchive(FileManipulator fm, long chunkDirectoryOffset) {
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

      long arcSize = fm.getLength();

      fm.seek(chunkDirectoryOffset);

      // 4 - Number of Chunks (including padding chunks)
      int numChunks = fm.readInt();
      FieldValidator.checkNumFiles(numChunks);

      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      fm.skip(12);

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(numChunks); // progress bar

      long previousPos = 0;
      for (int c = 0; c < numChunks; c++) {
        TaskProgressManager.setValue(c);

        // 8 - Chunk Offset in the Decompressed Archive
        long decompOffset = fm.readLong();
        //System.out.println((fm.getOffset() - 8) + "\t" + decompOffset);

        if (decompOffset == -506381209866536712l) {
          // reached the padding
          break;
        }

        FieldValidator.checkOffset(decompOffset);

        // 8 - Chunk Offset in the Compressed Archive
        long compOffset = fm.readLong();
        FieldValidator.checkOffset(compOffset, arcSize);

        // 4 - Hash?
        // 4 - null
        fm.skip(8);

        // 4 - Decompressed Chunk Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Chunk Length
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        FileManipulator compFM = new FileManipulator(origFile, false);

        // If there's a gap between previousPos and decompOffset, we need to copy that data verbatim
        int gap = (int) (decompOffset - previousPos);
        FieldValidator.checkPositive(gap);
        if (gap > 0) {
          compFM.seek(decompOffset - gap);
          decompFM.writeBytes(compFM.readBytes(gap));
          previousPos += gap;
        }

        compFM.seek(compOffset);

        // Now open for reading/decompressing
        Exporter_LZO_MiniLZO exporter = Exporter_LZO_MiniLZO.getInstance();
        exporter.open(compFM, compLength, decompLength);

        int bytesRemaining = decompLength;
        while (exporter.available()) {
          decompFM.writeByte(exporter.read());
          bytesRemaining--;
        }

        if (bytesRemaining != 0) {
          ErrorLogger.log("[BFZ_ABE] Didn't decompress enough data: " + bytesRemaining);
        }

        exporter.close();

        previousPos += decompLength;
        decompFM.seek(previousPos); // just in case
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

      // Return the file pointer to the beginning, and return the decompressed file
      return decompFM;
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

      // 4 - Header ("ABE" + null)
      // 4 - Unknown (5)
      // 4 - Unknown (7)
      // 4 - Unknown (4)
      // 4 - Unknown
      // 4 - Unknown (3)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown
      // 8 - Details Directory Offset
      // 8 - Folders Offset
      fm.skip(56);

      // 8 - Chunks Offset
      long chunkOffset = fm.readLong();
      FieldValidator.checkOffset(chunkOffset, arcSize);

      FileManipulator decompFM = decompressArchive(fm, chunkOffset);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        path = fm.getFile(); // So the resources are stored against the decompressed file
        arcSize = fm.getLength();
      }

      // go to the right point in the decomp file
      fm.seek(40);

      // 8 - Details Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);
      fm.seek(dirOffset);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);
      }

      for (int i = 0; i < numFiles; i++) {
        // 63 - Filename (null terminated, filled with byte 127)
        // 1 - null
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);

        // 8 - File Length
        // 8 - null
        // 8 - Unknown (128)
        // 4 - Unknown ID (incremental from -1)
        // 4 - Unknown
        fm.skip(32);

        long offset = offsets[i];
        long length = lengths[i];

        String extension = FilenameSplitter.getExtension(filename).toLowerCase();
        if (extension.equals("son")) {
          offset += 32;
          length -= 32;
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

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
