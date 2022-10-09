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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UP_UDPS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UP_UDPS() {

    super("UP_UDPS", "UP_UDPS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Airfix Dogfighter");
    setExtensions("up"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("UDSP")) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 24)) {
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

      // 4 - Header (UDSP)
      // 4 - Version? (bytes 1,1,0,0)
      fm.skip(8);

      // 4 - Folders Directory Length
      int folderDirLength = fm.readInt();
      FieldValidator.checkLength(folderDirLength, arcSize);

      // 4 - Folders Directory Offset
      int folderDirOffset = fm.readInt();
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Names Directory Length
      int namesDirLength = fm.readInt();
      FieldValidator.checkLength(namesDirLength, arcSize);

      // 4 - Names Directory Offset
      int namesDirOffset = fm.readInt();
      FieldValidator.checkOffset(namesDirOffset, arcSize);

      // 4 - Files Directory Length
      int filesDirLength = fm.readInt();
      FieldValidator.checkLength(filesDirLength, arcSize);

      // 4 - Files Directory Offset
      int filesDirOffset = fm.readInt();
      FieldValidator.checkOffset(filesDirOffset, arcSize);

      int numFolders = folderDirLength / 24;
      FieldValidator.checkNumFiles(numFolders);

      int numFiles = filesDirLength / 24;
      FieldValidator.checkNumFiles(numFiles);

      // Read in the names directory
      fm.seek(namesDirOffset);
      byte[] nameBytes = fm.readBytes(namesDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      // Read in the files directory
      fm.seek(filesDirOffset);
      byte[] fileBytes = fm.readBytes(filesDirLength);
      FileManipulator fileFM = new FileManipulator(new ByteBuffer(fileBytes));

      // process the folders, and in turn, process the files
      fm.relativeSeek(folderDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFolders; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - Folder Name Offset (relative to the start of the Names Directory)
        int folderNameOffset = fm.readInt();
        FieldValidator.checkOffset(folderNameOffset, namesDirLength);

        nameFM.seek(folderNameOffset);
        String dirName = nameFM.readNullString();

        // 4 - Unknown
        // 4 - Unknown (0/2)
        fm.skip(8);

        // 4 - Number of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder + 1); // +1 to allow empty folders

        // 4 - Offset to the First File Entry for this Folder (relative to the start of the Files Directory)
        int folderFilesOffset = fm.readInt();
        FieldValidator.checkOffset(folderFilesOffset, filesDirLength + 1); // +1 to allow files at the end of the list, which are empty folders

        // process each file in this folder
        fileFM.seek(folderFilesOffset);

        for (int j = 0; j < numFilesInFolder; j++) {
          // 4 - Unknown
          fileFM.skip(4);

          // 4 - Filename Offset (relative to the start of the Names Directory)
          int filenameOffset = fileFM.readInt();
          FieldValidator.checkOffset(filenameOffset, namesDirLength);

          nameFM.seek(filenameOffset);
          String filename = dirName + nameFM.readNullString();

          // 4 - Compression Flag (1=compressed, 0=uncompressed)
          int compressed = fileFM.readInt();

          // 4 - Decompressed File Length
          int decompLength = fileFM.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          int length = fileFM.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset
          int offset = fileFM.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          //path,name,offset,length,decompLength,exporter
          if (compressed == 1) {
            // compressed
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          }
          else {
            // decompressed
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);
        }

      }

      nameFM.close();
      fileFM.close();
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
