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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_GZip;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RCRU_RCRU extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RCRU_RCRU() {

    super("RCRU_RCRU", "RCRU_RCRU");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("River City Ransom: Underground");
    setExtensions("rcru"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
  public FileManipulator decompressArchive(FileManipulator fm) {
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

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_GZip exporter = Exporter_GZip.getInstance();
      exporter.open(fm, 0, 0); // this exporter doesn't actually need the compLength or decompLength values, so use 0,0

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
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

      // Header
      if (fm.readString(4).equals("RCRU")) {
        rating += 50;
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

      // 4 - Header (RCRU)
      fm.skip(4);

      // X - Archive Name (" " + name without extension + " ")
      // 1 - null Archive Name terminator
      fm.readNullString();

      // 1 - Archive Type (1/5)
      int archiveType = ByteConverter.unsign(fm.readByte());

      if (archiveType == 1) {
        //
        // FULL ARCHIVE IS COMPRESSED
        //
        // X - Data (GZip Compression)
        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file

          path = fm.getFile(); // So the resources are stored against the decompressed file
        }

        long arcSize = fm.getLength();

        // 4 - Number Of Files
        int numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        Resource[] resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        int[] offsets = new int[numFiles];
        for (int i = 0; i < numFiles; i++) {
          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 1 - Filename Length
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          String filename = fm.readString(filenameLength);

          long offset = offsets[i];

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset);
          resource.forceNotAdded(true);
          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }

        calculateFileSizes(resources, arcSize);

        fm.close();

        return resources;
      }
      else if (archiveType == 5) {
        //
        // ONLY DIRECTORY IS COMPRESSED
        //

        long arcSize = fm.getLength() + 1;

        // 4 - Compressed Directory Length
        int compDirLength = fm.readInt();
        FieldValidator.checkLength(compDirLength, arcSize);

        int relativeOffset = (int) (fm.getOffset() + compDirLength);

        // X - Data (GZip Compression)
        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file
        }

        // 4 - Number Of Files
        int numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        Resource[] resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        int offset = relativeOffset;
        for (int i = 0; i < numFiles; i++) {
          // 1 - Filename Length
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          String filename = fm.readString(filenameLength);

          // 4 - File Offset
          int endOffset = fm.readInt() + relativeOffset;
          FieldValidator.checkOffset(endOffset, arcSize);

          int length = endOffset - offset;

          // 12-byte header on files
          offset += 12;
          length -= 12;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);

          offset = endOffset; // ready for the next file
        }

        fm.close();

        return resources;
      }
      else {
        fm.close();

        return null;
      }

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
