/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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

import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SARC_SARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SARC_SARC() {

    super("SARC_SARC", "SARC_SARC");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Just Cause",
        "Just Cause 2",
        "Mad Max");
    setExtensions("sarc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("lod"); // LOWER CASE

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

      // 4 - Unknown (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("SARC")) {
        rating += 50;
      }

      // 4 - Unknown (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
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

      // 4 - Unknown (4)
      // 4 - Header (SARC)
      // 4 - Unknown (2)
      fm.skip(12);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dirLength) {

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readNullString(filenameLength);
        String originalFilename = filename;
        if (filename.startsWith("C:\\")) {
          filename = filename.substring(3);
        }

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.addProperty("OriginalFilename", originalFilename); // so we can store it properly when write()
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(fm.getOffset());
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long directorySize = 0;
      for (int i = 0; i < numFiles; i++) {
        int entrySize = 12 + resources[i].getNameLength();
        entrySize += calculatePadding(entrySize, 4);
        directorySize += entrySize;
      }

      // Write Header Data

      // 4 - Unknown (4)
      fm.writeInt(4);

      // 4 - Header (SARC)
      fm.writeString("SARC");

      // 4 - Unknown (2)
      fm.writeInt(2);

      // 4 - Directory Length
      fm.writeInt((int) directorySize);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16 + directorySize;
      offset += calculatePadding(offset, 16);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        String filename = resource.getProperty("OriginalFilename");
        if (filename == null || filename.equals("")) {
          filename = resource.getName();
        }

        int filenameLength = filename.length();
        int filenamePadding = calculatePadding(filenameLength, 4);

        // 4 - Filename Length (including null padding)
        fm.writeInt(filenameLength + filenamePadding);

        // X - Filename
        fm.writeString(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        for (int p = 0; p < filenamePadding; p++) {
          fm.writeByte(0);
        }

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength;
        offset += calculatePadding(offset, 4);
      }

      // 0-15 - null Padding to a multiple of 16 bytes
      int dirPadding = calculatePadding(fm.getOffset(), 16);
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // 0-3 - null Padding to a multiple of 4 bytes
        int filePadding = calculatePadding(resource.getDecompressedLength(), 4);
        for (int p = 0; p < filePadding; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
