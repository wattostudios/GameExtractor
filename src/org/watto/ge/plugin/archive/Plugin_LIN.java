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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LIN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LIN() {

    super("LIN", "LIN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Open Season",
        "Tom Clancy's Splinter Cell: Chaos Theory",
        "Tom Clancy's Splinter Cell: Double Agent");
    setExtensions("lin");
    setPlatforms("PC",
        "XBox");

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

      long arcSize = fm.getLength();

      // First File Decomp Length
      int firstLength = fm.readInt();
      if (FieldValidator.checkLength(firstLength)) {
        rating += 5;
      }

      if (firstLength == 230428365) {
        // already decompressed
        rating += 5;
        return rating;
      }

      // First File Comp Length
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

      fm.seek(0); // return to the start, ready for decompression

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      while (fm.getOffset() < arcSize) {

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        // X - File Data (block)
        long offset = fm.getOffset();
        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(fm, compLength, decompLength);

        while (exporter.available() && decompLength > 0) {
          decompFM.writeByte(exporter.read());
          decompLength--;
        }

        fm.relativeSeek(offset + compLength);

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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      TaskProgressManager.setMaximum(numFiles);

      // see if this file is decompressed already
      if (fm.readInt() == 230428365) {
        // decompressed already
      }
      else {
        // do the decompression
        FileManipulator decompFM = decompressArchive(fm);

        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file

          // skip the first 4 bytes (read above as the Check)
          fm.skip(4);

          path = fm.getFile(); // So the resources are stored against the decompressed file

          arcSize = path.length();
        }

      }

      // 1 - Filename Length
      int filenameLength = fm.readByte();
      FieldValidator.checkPositive(filenameLength);

      // X - Filename
      // 1 - null Filename Terminator
      String filename = fm.readNullString(filenameLength);
      FieldValidator.checkFilename(filename);

      Resource[] resources = null;

      /*
      if (filename.equals("entry")) {
        // many files
      
        // 22 - Unknown
        fm.skip(22);
      
        resources = new Resource[numFiles];
        int realNumFiles = 0;
      
        while (fm.getOffset() < arcSize) {
          // 1 - Filename Length
          filenameLength = ByteConverter.unsign(fm.readByte());
      
          if (filenameLength == 193) {
            // end of directory
            break;
          }
      
          // X - Filename
          // 1 - null Filename Terminator
          filename = fm.readNullString(filenameLength);
      
          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset);
      
          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
      
          // 4 - null
          fm.skip(4);
      
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
      
          TaskProgressManager.setValue(offset);
        }
      
        long dataOffset = fm.getOffset() - 1;
      
        numFiles = realNumFiles;
        resources = resizeResources(resources, realNumFiles);
      
        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];
          resource.setOffset(resource.getOffset() + dataOffset);
          resource.forceNotAdded(true);
        }
      
      }
      else {
      */
      // a single compressed file

      numFiles = 1;
      resources = new Resource[numFiles];

      filename += ".decompressedLin";

      long offset = fm.getOffset();
      long length = arcSize - offset;

      //path,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, filename, offset, length);
      resources[0].forceNotAdded(true);
      /*
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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      long[] offsets = new long[numFiles];
      long[] compLengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - Decompressed Length
        fm.writeInt((int) decompLength);

        // 4 - Compressed Length
        offsets[i] = fm.getOffset();
        fm.writeInt(0);

        // X - File Data (ZLib Compression)
        compLengths[i] = write(exporter, resources[i], fm);
      }

      // go back and write the compressed lengths
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - Compressed Length
        fm.writeInt((int) compLengths[i]);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
