/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BFL_CMPR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BFL_CMPR() {

    super("BFL_CMPR", "BFL_CMPR");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("bfl");
    setGames("Colin McRae Rally 2");
    setPlatforms("PC");

    setFileTypes("dds", "DDS Image",
        "pcf", "PCF Image",
        "hor", "Time Settings",
        "bsp", "Unknown File",
        "ai0", "AI Settings",
        "ai1", "AI Settings",
        "ai2", "AI Settings",
        "tm0", "Unknown File",
        "tm1", "Unknown File",
        "tm2", "Unknown File",
        "grp", "Track Groups",
        "xhi", "Unknown File",
        "tre", "Landscape Map",
        "cfl", "Unknown File",
        "tsc", "Unknown File",
        "cod", "Unknown File",
        "cat", "Unknown File",
        "csp", "Unknown File",
        "gro", "Landscape File",
        "sky", "Sky Data",
        "sht", "Landscape File",
        "obj", "3D Landscape Model",
        "dat", "Track Data",
        "srf", "Unknown File",
        "hpc", "Unknown File",
        "c3d", "3D Landscape Model",
        "dmd", "Unknown File");

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
      if (fm.readString(4).equals("CMPR")) {
        rating += 50;
      }
      else {
        // maybe a compressed file
        fm.relativeSeek(0);

        // GZIP Header
        if (fm.readInt() == 559903) {
          rating += 50;
          return rating;
        }
      }

      // Archive Size
      if (fm.readInt() + 8 == fm.getLength()) {
        rating += 5;
      }

      long arcSize = fm.getLength();
      if (arcSize - 4 < 0) {
        return 0;
      }
      fm.seek(arcSize - 4);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      if (fm.readInt() == 559903) {
        // decompress the archive first

        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file

          path = fm.getFile(); // So the resources are stored against the decompressed file
        }

      }

      long arcSize = fm.getLength();

      // 4 - Header (CMPR)
      // 4 - Archive Size [+8]
      fm.seek((int) arcSize - 4);

      // 4 - Directory Offset [+8]
      long dirOffset = fm.readInt();
      fm.seek(dirOffset + 8);

      int numFiles = (int) ((path.length() - dirOffset) / 16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 4) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset [+8]
        long offset = fm.readInt() + 8;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 0-3 - Filename Length Filler (fill to a multiple of 4)
        fm.skip(calculatePadding(filenameLength, 4));

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[realNumFiles] = resource;

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
   Decompresses an archive, where the whole archive is compressed.
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

      long arcSize = fm.getLength();

      int compLength = (int) arcSize;

      // read the footer
      fm.seek(arcSize - 4);

      int decompLength = fm.readInt();
      if (decompLength < arcSize) {
        // decomp length is less than the comp length;
        return fm;
      }

      fm.seek(0); // back to the start

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_GZip exporter = Exporter_GZip.getInstance();
      exporter.open(fm, compLength, decompLength);

      for (int i = 0; i < decompLength; i++) {
        if (exporter.available()) {
          decompFM.writeByte(exporter.read());
        }
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(0);
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
  @SuppressWarnings("unused")
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);

      // 4 - Header
      fm.writeString("CMPR");

      //Loop 1 to calculate sizes

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      int totalLengthOfHeader = 8;
      int totalLengthOfDirectory = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        int fileLength = (int) resource.getDecompressedLength();
        fileLength += calculatePadding(fileLength, 4);
        totalLengthOfData += fileLength;

        int nameLength = resource.getName().length();
        nameLength += calculatePadding(nameLength, 4);
        totalLengthOfDirectory += 12 + nameLength;
      }

      // 4 - Archive Length (-8 header)
      fm.writeInt(totalLengthOfData + totalLengthOfDirectory);

      // Loop 2 - Build archive
      // for each file
      //   X - File Data
      //   0-3 - Padding to a multiple of 4 bytes
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm, 4);

      // Loop 3 - Build directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 0;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        int nameLength = name.length();

        long length = resources[i].getDecompressedLength();

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - Data Offset
        fm.writeInt(currentPos);

        // 4 - Filename Length
        fm.writeInt(nameLength);

        // X - Filename
        fm.writeString(name);

        // 1-3 - Filename Length Filler (fill to a multiple of 4)
        int paddingSize = calculatePadding(nameLength, 4);
        for (int j = 0; j < paddingSize; j++) {
          fm.writeByte(0);
        }

        currentPos += length;
        currentPos += calculatePadding(length, 4);
      }

      // 4 - Directory Offset
      fm.writeInt(totalLengthOfData);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}