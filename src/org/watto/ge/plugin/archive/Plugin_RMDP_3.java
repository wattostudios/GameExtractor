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
public class Plugin_RMDP_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RMDP_3() {

    super("RMDP_3", "RMDP_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Control");
    setExtensions("rmdp"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "bin");
      rating += 25;

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

      File sourcePath = getDirectoryFile(path, "bin");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long arcSize = path.length();

      // 1 - Version? (0)
      // 4 - Unknown (9)
      fm.skip(5);

      // 4 - Number of Folders
      int numFolders = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - Unknown (1)
      fm.skip(8);

      // 4 - Names Directory Length
      int nameDirLength = fm.readInt();
      FieldValidator.checkLength(nameDirLength, arcSize);

      // skip over the directories so we can get the filenames
      fm.seek(sourcePath.length() - nameDirLength);

      byte[] nameDirBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      fm.seek(201);

      String[] folderNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Hash?
        // 8 - Folder ID?
        fm.skip(12);

        // 8 - Parent Folder ID
        long parentID = fm.readLong();

        // 4 - Unknown
        fm.skip(4);

        // 8 - Folder Name Offset (relative to the start of the Filename Directory) (-1 means the Root folder)
        long nameOffset = fm.readLong();
        FieldValidator.checkOffset(nameOffset, nameDirLength);

        // X - Name (null terminated)
        nameFM.seek(nameOffset);
        String folderName = nameFM.readNullString(nameDirLength) + "\\";

        // 8 - Folder ID of the first Sub-Folder in this Folder? (-1 for no sub-folders)
        // 8 - File ID of the first File in this Folder? (-1 for no files)
        fm.skip(16);

        parentID--;
        if (parentID >= 0 && parentID < numFolders) {
          folderName = folderNames[(int) parentID] + folderName;
        }

        folderNames[i] = folderName;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        // 8 - File ID? (or -1)
        fm.skip(12);

        // 8 - Folder ID?
        long parentID = fm.readLong();

        // 4 - Unknown
        fm.skip(4);

        // 8 - Filename Offset (relative to the start of the Filename Directory)
        long nameOffset = fm.readLong();
        FieldValidator.checkOffset(nameOffset, nameDirLength);

        // X - Name (null terminated)
        nameFM.seek(nameOffset);
        String filename = nameFM.readNullString(nameDirLength);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Hash?
        // 4 - Unknown
        fm.skip(12);

        parentID--;
        if (parentID >= 0 && parentID < numFolders) {
          filename = folderNames[(int) parentID] + filename;
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      nameFM.close();
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
