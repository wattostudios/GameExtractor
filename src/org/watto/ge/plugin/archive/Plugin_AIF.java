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
public class Plugin_AIF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AIF() {

    super("AIF", "AIF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("SAS: Anti-Terror Force");
    setExtensions("aif");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Entry Length (16)
      if (fm.readInt() == 16) {
        rating += 5;
      }

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // File Entry Dir Offset
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

  int realNumFolders = 0;

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

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;
      realNumFolders = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Directory Length (including all these fields) (not including padding)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Header Length (16)
      // 8 - null
      fm.relativeSeek(0); // back to the beginning, to read the directory into a buffer;

      int numFiles = (int) ((dirLength - 16) / 16);
      FieldValidator.checkNumFiles(numFiles);

      byte[] dirBytes = fm.readBytes(dirLength);
      FileManipulator dirFM = new FileManipulator(new ByteBuffer(dirBytes));

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      readDirectory(path, arcSize, fm, resources, null, 1, 16, 0, 0);

      dirFM.close();
      fm.close();

      resources = resizeResources(resources, realNumFiles);

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
  public void readDirectory(File path, long arcSize, FileManipulator fm, Resource[] resources, String dirName, int numFolders, int foldersOffset, int numFiles, int filesOffset) {
    try {

      long dirLength = fm.getLength();

      // Loop through the folders entries
      fm.seek(foldersOffset);
      int[] subFoldersOffsets = new int[numFolders];
      int[] subFilesOffsets = new int[numFolders];
      short[] numSubFolders = new short[numFolders];
      short[] numSubFiles = new short[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Name Hash? (null for the root entry)
        fm.skip(4);

        // 4 - Offset to Sub-Folder Entries in this Folder
        int subFoldersOffset = fm.readInt();
        FieldValidator.checkOffset(subFoldersOffset, dirLength);
        subFoldersOffsets[i] = subFoldersOffset;

        // 4 - Offset to File Entries in this Folder
        int subFilesOffset = fm.readInt();
        FieldValidator.checkOffset(subFilesOffset, dirLength);
        subFilesOffsets[i] = subFilesOffset;

        // 2 - Number of Sub-Folders in this Folder
        short numSubFolder = fm.readShort();
        FieldValidator.checkNumFiles(numSubFolder + 1); // allow no folders
        numSubFolders[i] = numSubFolder;

        // 2 - Number of Files in this Folder
        short numSubFile = fm.readShort();
        FieldValidator.checkNumFiles(numSubFile + 1); // allow no files
        numSubFiles[i] = numSubFile;
      }

      // process each sub-folder
      for (int i = 0; i < numFolders; i++) {
        int subFoldersOffset = subFoldersOffsets[i];
        int subFilesOffset = subFilesOffsets[i];
        short numSubFolder = numSubFolders[i];
        short numSubFile = numSubFiles[i];

        String subDirName = null;
        if (dirName == null) {
          subDirName = "Root\\";
        }
        else {
          realNumFolders++;
          subDirName = dirName + "Folder " + realNumFolders + "\\";
        }

        readDirectory(path, arcSize, fm, resources, subDirName, numSubFolder, subFoldersOffset, numSubFile, subFilesOffset);
      }

      // Loop through the files entries
      fm.seek(filesOffset);
      for (int i = 0; i < numFiles; i++) {
        // 4 - Name Hash? (null for the root entry)
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = dirName + Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(realNumFiles);
      }

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

    if (headerInt1 == 542332225) {
      return "aus";
    }
    else if (headerInt1 == 1196310860) {
      return "lang";
    }
    else if (headerInt1 == 843925844) {
      return "tm2";
    }
    else if (headerInt1 == 1397768760) {
      return "8bps";
    }
    else if (headerShort1 == 12079 || headerShort1 == 2573) {
      return "txt";
    }
    else if (headerInt1 == 1380013139 || headerInt1 == 1599231059 || headerInt1 == 1634222947 || headerInt1 == 1634751331 || headerInt1 == 1735287116 || headerInt1 == 1936943469) {
      return "txt";
    }
    else if (headerInt1 == 9460301) {
      return "dll";
    }
    else if (headerInt1 == 1 && headerInt2 == 1) {
      return "arc";
    }
    else if (headerInt1 == 131072) {
      return "tex";
    }

    return null;
  }

}
