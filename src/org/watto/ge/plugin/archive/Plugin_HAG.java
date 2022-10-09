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
import java.io.FilenameFilter;
import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileExtensionFilter;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HAG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HAG() {

    super("HAG", "HAG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Orion Burger",
        "Ripley's Believe It or Not!: The Riddle Of Master Lu");
    setExtensions("hag");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("raw", "RAW Audio", FileType.TYPE_AUDIO),
        new FileType("cod", "Walkbehind Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("ss", "SS Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("ssb", "SS Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("ssc", "SS Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("tt", "Location Bitmap Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("def"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      if (rating > 0) {
        // Only want to do this if we've already matched a HAG extension

        // Find any *.HAS file (there should only be 1)
        File parentFile = fm.getFile().getParentFile();
        File[] hasFiles = parentFile.listFiles((FilenameFilter) new FileExtensionFilter("has", "HAS"));
        if (hasFiles != null && hasFiles.length >= 1) {
          rating += 25;
        }
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      // Find any *.HAS file (there should only be 1)
      File parentFile = path.getParentFile();
      File[] hasFiles = parentFile.listFiles((FilenameFilter) new FileExtensionFilter("has", "HAS"));
      if (hasFiles == null || hasFiles.length <= 0) {
        return null;
      }

      File sourcePath = hasFiles[0];

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // skip through to read the archive directory first
      fm.skip(numFiles * 47);

      int numArchives = (int) ((sourcePath.length() - fm.getOffset()) / 34);
      FieldValidator.checkNumFiles(numArchives);

      File[] archiveFiles = new File[numArchives];
      long[] archiveLengths = new long[numArchives];
      String basePath = parentFile.getAbsolutePath() + File.separatorChar;
      for (int i = 0; i < numArchives; i++) {
        // 33 - Archive Name (null)
        String name = fm.readNullString(33);
        FieldValidator.checkFilename(name);

        // in Riddle of Master Lu, the archive names are *.IDX, need to convert them to *.HAG
        if (name.endsWith(".hag") || name.endsWith(".HAG")) {
          // ok already
        }
        else {
          int dotPos = name.lastIndexOf(".");
          String extension = name.substring(dotPos + 1);

          // work out if it's uppercase or lowercase
          if (extension.equals(extension.toUpperCase())) {
            // uppercase
            extension = ".HAG";
          }
          else {
            extension = ".hag";
          }

          name = name.substring(0, dotPos) + extension;
        }

        // 1 - Archive ID
        int nameID = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkRange(nameID, 0, numArchives);

        File archiveFile = new File(basePath + name);
        if (!archiveFile.exists()) {
          ErrorLogger.log("[HAG] Can't find HAG file " + name);
          return null;
        }
        archiveFiles[nameID] = archiveFile;
        archiveLengths[nameID] = archiveFile.length();
      }

      // back to the file details directory
      fm.relativeSeek(4);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 33 - Filename (null Terminated)
        String filename = fm.readNullString(33);
        if (filename.length() <= 0) {
          // empty entry
          fm.skip(14);
          continue;
        }
        FieldValidator.checkFilename(filename);

        filename = filename.replace('/', '_'); // some filenames contains a / in them, but aren't actually directories, so fix this

        // 1 - Archive Name ID
        int archiveID = ByteConverter.unsign(fm.readByte());

        File archiveFile = archiveFiles[archiveID];
        long arcSize = archiveLengths[archiveID];

        // 1 - Entry Type (0 for a blank file)
        fm.skip(1);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Entry Offset (if not =FileOffset, then ignore?)
        fm.skip(4);

        if (FilenameSplitter.getExtension(filename).equalsIgnoreCase("RAW")) {
          // Audio

          //path,id,name,offset,length,decompLength,exporter
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(archiveFile, filename, offset, length);
          resource.setAudioProperties(11025, 8, 1);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;
          realNumFiles++;
        }
        else {
          //path,id,name,offset,length,decompLength,exporter
          Resource resource = new Resource(archiveFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
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

    String extension = resource.getExtension();

    if (extension != null && extension.length() > 0) {

      if (extension.equals(" W_O #S")) { // a very specific file
        extension = "SS";
      }

      return extension; // already has an extension
    }

    if (headerInt1 == 1295274835) {
      return "SS";
    }
    else if (headerInt1 == 1296122696) {
      //return "MACH";
      return "WSS";
    }
    else if (headerInt1 == 1145132097) {
      //return "DATA";
      return "WSS";
    }
    else if (headerInt1 == 1397051733) {
      //return "SEQU";
      return "WSS";
    }

    return null;
  }

}
