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

import org.watto.component.WSPluginException;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLibX;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAB_ISC_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CAB_ISC_3() {

    super("CAB_ISC_3", "InstallShield CAB Archive (multiple CAB with single HDR Index) - CAB_ISC_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("InstallShield");
    setExtensions("cab"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      try {
        // try to get a *.HDR file with the same filename
        getDirectoryFile(fm.getFile(), "hdr");
        rating += 25;
      }
      catch (WSPluginException e) {
        // If it fails, see if we can find file #1 for this block of files, then get the *.HDR for that
        String filename = fm.getFile().getAbsolutePath();
        int dotPos = filename.lastIndexOf(".") - 1; // -1 to strip off the number just before the "."
        if (dotPos > 0) {
          filename = filename.substring(0, dotPos) + "1.hdr"; // we want to find the *.HDR attached to the first file
          if (new File(filename).exists()) {
            // found a "xxxx1.hdr" file!
            rating += 25;
          }
        }
      }

      // Header
      if (fm.readString(4).equals("ISc(")) {
        rating += 50;
      }

      // 4 - Unknown (16801804)
      if (fm.readInt() == 16801804) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      fm.skip(4);

      // 4 - File Data Offset
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLibX.getInstance();
      //ExporterPlugin exporter = Exporter_ZLibX.getInstance();
      //ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      //long arcSize = (int) path.length();
      long arcSize = path.length();

      File sourcePath = null;
      String basePathSubstring = "";

      try {
        // try to get a *.HDR file with the same filename
        sourcePath = getDirectoryFile(path, "hdr");
      }
      catch (WSPluginException e) {
        // If it fails, see if we can find file #1 for this block of files, then get the *.HDR for that
        String filename = path.getAbsolutePath();
        int dotPos = filename.lastIndexOf(".") - 1; // -1 to strip off the number just before the "."
        if (dotPos > 0) {
          basePathSubstring = filename.substring(0, dotPos);
          filename = basePathSubstring + "1.hdr"; // we want to find the *.HDR attached to the first file
          if (new File(filename).exists()) {
            // found a "xxxx1.hdr" file!
            sourcePath = new File(filename);
          }
        }
      }

      if (sourcePath == null) {
        return null;
      }

      // now work out how many datafiles we have (NOTE: We're only having a MAX of 9, as we've only ever seen "2" anyway);
      File[] dataFiles = new File[10]; // 10, because the datafiles are identified 1-9, not 0-8
      for (int i = 0; i < 10; i++) {
        File datafile = new File(basePathSubstring + (i) + ".cab");
        if (datafile.exists()) {
          dataFiles[i] = datafile;
        }
        else {
          dataFiles[i] = null;
        }
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = sourcePath.length();

      // 4 - File Header ("ISc(")
      // 4 - Unknown (16801804)
      // 4 - null
      fm.skip(12);

      // 4 - Strings Directory Offset (512)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, dirSize);

      // 4 - Strings Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength);

      fm.seek(dirOffset);

      // 4 - English Strings Tables Offset
      // 4 - null
      // 4 - Unknown Length/Offset
      // 4 - Strings Directory Length
      // 4 - null
      // 4 - Files Directory Length
      // 4 - Files Directory Length
      fm.skip(28);

      // 4 - Number of Directories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);
      // 4 - null
      // 4 - Length of Directories Offset Section (numDirectories * 4)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // Now go to the start of the FILES DIRECTORY
      dirOffset += dirLength;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int[] dirNameOffsets = new int[numDirectories];
      for (int i = 0; i < numDirectories; i++) {
        // 4 - Directory Name Offset (relative to the start of the files directory)
        int dirNameOffset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(dirNameOffset, arcSize);
        dirNameOffsets[i] = dirNameOffset;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      int[] directoryIDs = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 2 - Entry Type (12=Unknown, 4=File, 0=Setup-related File)
        short entryType = fm.readShort();
        if (entryType != 4) {
          // not a file - skip it
          fm.skip(85);
          continue;
        }

        // 8 - Decompressed File Length (or null if not a file)
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        // 8 - Compressed File Length (or null if not a file)
        long length = fm.readLong();
        FieldValidator.checkLength(length);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset);

        // 16 - Checksum/Hash/?
        // 2 - Unknown
        // 2 - Unknown
        // 4 - Unknown
        // 8 - null
        fm.skip(32);

        // 4 - Filename Offset (relative to the start of the files directory)
        int filenameOffset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(filenameOffset, dirSize);
        filenameOffsets[realNumFiles] = filenameOffset;

        // 2 - Directory ID (zero-based ID to one of the directories listed at the start of the files directory)
        short directoryID = fm.readShort();
        FieldValidator.checkRange(directoryID, 0, numDirectories);
        directoryIDs[realNumFiles] = directoryID;

        // 4 - Unknown Flags
        // 4 - Unknown
        // 13 - null
        fm.skip(21);

        // 2 - Data File Number? (1/2)
        short datafileNumber = fm.readShort();
        FieldValidator.checkRange(datafileNumber, 0, 9); // NOTE: Again, we're forcing a MAX of 9 here
        File dataFile = dataFiles[datafileNumber];

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(dataFile, "", offset, length, decompLength, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      // Now loop through and get the directory names
      String[] dirNames = new String[numDirectories];
      for (int i = 0; i < numDirectories; i++) {
        fm.seek(dirNameOffsets[i]);

        // X - Directory Name (null)
        String filename = fm.readNullString();
        if (!filename.equals("")) {
          FieldValidator.checkFilename(filename); // don't want to check "", but want to check all others
          filename += "\\"; // the first dirName is the root (empty), so don't want a slash on the end of it
        }

        dirNames[i] = filename;
      }

      // Now loop through the filenames directory, and set the directory name as well
      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // now add the directory name
        filename = dirNames[directoryIDs[i]] + filename;

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
        resource.forceNotAdded(true);
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

}
