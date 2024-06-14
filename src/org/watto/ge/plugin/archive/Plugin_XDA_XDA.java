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

import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XDA_XDA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XDA_XDA() {

    super("XDA_XDA", "XDA_XDA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Summon Night Granthese");
    setExtensions("xda"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pac", "PAC Archive", FileType.TYPE_ARCHIVE),
        new FileType("vb", "VAG Audio", FileType.TYPE_AUDIO),
        new FileType("tex", "TEX Image", FileType.TYPE_IMAGE));

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

      fm.skip(16);

      // Header
      if (fm.readString(4).equals("XDA" + (char) 0)) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      fm.skip(8);

      // Archive Size
      if (fm.readInt() == arcSize) {
        rating += 5;
      }

      fm.skip(12);

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

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

  int numNames = 0;
  int realNumNames = 0;
  String[] names = null;

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

      ExporterPlugin exporter = Exporter_Custom_VAG_Audio.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown (128)
      // 4 - Unknown (32)
      // 4 - Unknown (129)
      // 4 - Unknown (12)
      // 4 - Header ("XDA" + null)
      // 4 - Unknown (130)
      // 4 - Unknown (12)
      // 4 - Archive Length
      // 4 - Unknown (1000)
      // 4 - Unknown (28)
      // 4 - Padding Multiple (2048)
      // 4 - Filename Directory Length
      fm.skip(48);

      // 4 - Filename Directory Offset (2048)
      int nameDirOffset = fm.readInt();
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 4 - Details Directory Length
      fm.skip(4);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (51)
      // 4 - Unknown (1988)
      // 1980 - Padding to offset 2048
      fm.seek(nameDirOffset);

      // 4 - Section Type (1001)
      // 4 - Section Length
      // 4 - Offset to the end of this Section
      fm.skip(12);

      // 4 - Number of Names
      numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      realNumNames = 0;

      names = new String[numNames];

      while (realNumNames < numNames) {
        long offset = fm.getOffset();

        // 4 - Name Type (1002=Folder, 1003=File)
        int nameType = fm.readInt();

        if (nameType == 1002) {
          // 4 - Length of this entry and all the sub-entries within this folder
          int blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(12);

          // 4 - Folder Name Length (including null padding)
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Folder Name
          // 0-3 - null Padding to a multiple of 4 bytes
          String name = fm.readNullString(nameLength) + "\\";

          long endOffset = offset + blockLength;

          processFolder(fm, arcSize, endOffset, name);
        }
        else if (nameType == 1003) {
          // 4 - Entry Length
          fm.skip(4);

          // 4 - File ID?
          int fileID = fm.readInt();

          // 4 - Filename Length (including null padding)
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Filename
          // 0-3 - null Padding to a multiple of 4 bytes
          String name = fm.readNullString(nameLength);

          names[fileID] = name;
          realNumNames++;
        }
        else {
          ErrorLogger.log("[XDA_XDA] Unknown Name Type: " + nameType);
          return null;
        }
      }

      fm.seek(dirOffset);

      // 4 - Section Type (1020)
      // 4 - Section Length
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (including padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        String filename = names[i];

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resources[i] = resource;

        if (filename.endsWith("vb")) {
          resource.addProperty("Frequency", "44100");
          resource.setExporter(exporter);
        }

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
  
  **********************************************************************************************
  **/
  public void processFolder(FileManipulator fm, long arcSize, long endOffset, String dirName) {
    try {
      while (fm.getOffset() < endOffset) {
        long offset = fm.getOffset();

        // 4 - Name Type (1002=Folder, 1003=File)
        int nameType = fm.readInt();

        if (nameType == 1002) {
          // 4 - Length of this entry and all the sub-entries within this folder
          int blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(12);

          // 4 - Folder Name Length (including null padding)
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Folder Name
          // 0-3 - null Padding to a multiple of 4 bytes
          String name = fm.readNullString(nameLength);

          name = dirName + name + "\\";

          long subEndOffset = offset + blockLength;

          processFolder(fm, arcSize, subEndOffset, name);
        }
        else if (nameType == 1003) {
          // 4 - Entry Length
          fm.skip(4);

          // 4 - File ID?
          int fileID = fm.readInt();

          // 4 - Filename Length (including null padding)
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Filename
          // 0-3 - null Padding to a multiple of 4 bytes
          String name = fm.readNullString(nameLength);

          name = dirName + name;

          names[fileID] = name;
          realNumNames++;
        }
        else {
          ErrorLogger.log("[XDA_XDA] Unknown Name Type: " + nameType);
          return;
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
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
