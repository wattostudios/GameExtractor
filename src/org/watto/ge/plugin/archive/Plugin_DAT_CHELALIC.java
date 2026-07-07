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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_CHELALIC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_CHELALIC() {

    super("DAT_CHELALIC", "DAT_CHELALIC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("");
    setExtensions(""); // MUST BE LOWER CASE
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
      if (fm.readString(8).equals("CHELALIC")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (CHELALIC)
      fm.skip(8);

      // 4 - Number of Entries
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // READ THE ROOT

      // 4 - null
      // 4 - null
      fm.skip(8);

      // 4 - Number of Sub-Folders in this Folder
      fm.skip(4);

      // 4 - Number of Entries in this Folder
      int numFilesInFolder = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInFolder + 1); // +1 to allow no files in this folder

      // 4 - Folder Name Length (including padding)
      int nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // X - Folder Name
      // 0-3 - null Padding to a multiple of 4 bytes
      String folderName = fm.readNullString(nameLength);
      FieldValidator.checkFilename(folderName);

      readDirectory(path, fm, resources, "", arcSize, fileDataOffset, numFilesInFolder);

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
   * 
   **********************************************************************************************
   **/
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, long arcSize, int fileDataOffset, int numEntries) {
    try {

      for (int i = 0; i < numEntries; i++) {
        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Indicator (The first file in a sub-folder has the number of files in this sub-folder, otherwise is null)
        fm.skip(4);

        // 4 - Number of Entries in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder + 1); // +1 to allow no files in this folder

        // 4 - Filename Length (including padding)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        // 0-3 - null Padding to a multiple of 4 bytes
        String filename = fm.readNullString(filenameLength);
        FieldValidator.checkFilename(filename);

        filename = dirName + filename;

        if (numFilesInFolder == 0) {
          // file

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);

          realNumFiles++;
        }
        else {
          // folder
          readDirectory(path, fm, resources, filename + "\\", arcSize, fileDataOffset, numFilesInFolder);
        }

      }

      /*
      // 4 - null
      // 4 - null
      fm.skip(8);
      
      // 4 - Number of Sub-Folders in this Folder
      int numSubFolders = fm.readInt();
      FieldValidator.checkNumFiles(numSubFolders + 1); // +1 to allow no sub-folders
      
      // 4 - Number of Entries in this Folder
      int numFilesInFolder = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInFolder + 1); // +1 to allow no files in this folder
      
      // 4 - Folder Name Length (including padding)
      int nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);
      
      // X - Folder Name
      // 0-3 - null Padding to a multiple of 4 bytes
      String folderName = fm.readNullString(nameLength);
      FieldValidator.checkFilename(folderName);
      
      dirName += folderName + "\\";
      
      // Read the Sub-Folders
      for (int i = 0; i < numSubFolders; i++) {
        readDirectory(path, fm, resources, dirName, arcSize, fileDataOffset);
      }
      
      // Read the Files
      for (int i = 0; i < numFilesInFolder; i++) {
        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);
      
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
      
        // 4 - Indicator (The first file in a sub-folder has the number of files in this sub-folder, otherwise is null)
        // 4 - null
        fm.skip(8);
      
        // 4 - Filename Length (including padding)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);
      
        // X - Filename
        // 0-3 - null Padding to a multiple of 4 bytes
        String filename = fm.readNullString(filenameLength);
        FieldValidator.checkFilename(filename);
      
        filename = dirName + filename;
      
        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
      
        TaskProgressManager.setValue(realNumFiles);
      
        realNumFiles++;
      }
      */

    }
    catch (Throwable t) {
      logError(t);

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
