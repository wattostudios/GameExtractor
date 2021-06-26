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
public class Plugin_PAK_DPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_DPAK() {

    super("PAK_DPAK", "PAK_DPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("LEGO Creator Knights Kingdom");
    setExtensions("pak"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("DPAK")) {
        rating += 50;
      }

      fm.skip(4);

      // 4 - File ID Directory Offset (24)
      if (fm.readInt() == 24) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - File Details Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Filename Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

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

  int realNumFiles = 0;

  long arcSize = 0;

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

      arcSize = fm.getLength();

      // 4 - Header (DPAK)
      // 2 - null
      // 2 - Version? (1)
      // 4 - File ID Directory Offset (24)
      fm.skip(12);

      // 4 - File Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      int dirLength = filenameDirOffset - dirOffset;
      int filenameDirLength = fileDataOffset - filenameDirOffset;

      int numFiles = dirLength / 8;
      FieldValidator.checkNumFiles(numFiles);

      // read in the directories
      fm.seek(dirOffset);
      byte[] dirBytes = fm.readBytes(dirLength);
      FileManipulator dirFM = new FileManipulator(new ByteBuffer(dirBytes));

      fm.seek(filenameDirOffset);
      byte[] nameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      // now read and process the file IDs

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      readDirectory(fm, path, resources, 24, "", dirOffset, dirFM, filenameDirOffset, nameFM);

      if (resources[0] == null) {
        // didn't actually read anything - not a valid archive
        return null;
      }

      // close everything up
      dirFM.close();
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
  
  **********************************************************************************************
  **/
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, int entryOffset, String dirName, int dirOffset, FileManipulator dirFM, int filenameDirOffset, FileManipulator nameFM) {
    try {

      fm.seek(entryOffset);

      // 4 - Number of Entries in this Folder
      int numEntries = fm.readInt();
      FieldValidator.checkNumFiles(numEntries);

      String[] names = new String[numEntries];
      for (int e = 0; e < numEntries; e++) {
        // 4 - File/Folder Name Offset
        int nameOffset = fm.readInt() - filenameDirOffset;
        FieldValidator.checkOffset(nameOffset, arcSize);

        nameFM.seek(nameOffset);
        // X - File/Folder Name
        // 1 - null Name Terminator
        //String name = nameFM.readNullString();
        String name = nameFM.readNullString(1024); // force a max size, to stop it reading forever in some invalid archives
        FieldValidator.checkFilename(name);
        names[e] = name;
      }

      int[] offsets = new int[numEntries];
      for (int e = 0; e < numEntries; e++) {
        // 4 - Entry Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[e] = offset;
      }

      // Now read the file entries, and/or process the folder entries
      for (int e = 0; e < numEntries; e++) {
        int offset = offsets[e];

        if (offset >= dirOffset) {
          // If Offset >= DetailsDirectoryOffset, it's an offset to a file.
          offset -= dirOffset;
          dirFM.seek(offset);

          // 4 - File Length
          int length = dirFM.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset
          offset = dirFM.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          String filename = dirName + names[e];

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
        else {
          // If Offset < DetailsDirectoryOffset, it's an offset to the "NumberOfEntriesInThisFolder" for the Folder
          readDirectory(fm, path, resources, offset, dirName + names[e] + "\\", dirOffset, dirFM, filenameDirOffset, nameFM);
        }
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
