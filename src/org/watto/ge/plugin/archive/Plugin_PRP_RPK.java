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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PRP_RPK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PRP_RPK() {

    super("PRP_RPK", "PRP_RPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Overlord 2");
    setExtensions("prp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

    setEnabled(false); // NOT WORKING YET - TOO COMPLEX

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
      if (fm.readString(3).equals("RPK") && fm.readByte() == 0) {
        rating += 50;
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

      // 4 - Header (RPK + null)
      // 2 - Unknown (6)
      // 2 - Unknown (70)
      // 4 - Unknown (Same as another field further below)
      // 4 - Archive Size [+192]
      // 160 - Archive Filename (no extension) (null terminated)
      // 1 - Length Of Hash [(value-130)*2]
      fm.skip(177);

      // 4 - Number Of Entries in Directory 1
      int numDir1Entries = fm.readInt();
      FieldValidator.checkNumFiles(numDir1Entries);

      // 2 - Unknown (16)
      // 10 - Hash?
      fm.skip(12);

      // for each entry
      //   4 - Entry ID
      //   4 - Entry Offset
      fm.skip(numDir1Entries * 8);

      // 4 - Unknown (257)
      // 4 - Unknown
      // 1 - Unknown (4)
      // 4 - Unknown
      // 4 - Unknown (2499)
      // 4 - Unknown (1)
      fm.skip(21);

      // 4 - Description Length
      int descriptionLength = fm.readInt();
      FieldValidator.checkFilenameLength(descriptionLength + 1); // +1 to allow empty string

      // X - Description
      fm.skip(descriptionLength);

      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (163)
      fm.skip(16);

      // 4 - Archive Filename Length
      int archiveNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(archiveNameLength);

      // X - Archive Filename (no extension)
      fm.skip(archiveNameLength);

      // DIRECTORY 2
      int checkByte = fm.readByte();
      while (checkByte == 3) {
        // 1 - Unknown (3)
        // read above

        // 2 - Unknown (20)
        // 4 - Unknown
        fm.skip(6);

        // 4 - Name Length
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow empty string

        // X - Name
        fm.skip(nameLength);

        // 4 - Unknown (257)
        // 4 - Unknown (257)
        fm.skip(8);

        // read the next checkbyte
        checkByte = fm.readByte();
      }

      // 4 - Unknown
      fm.skip(3); // we already read 1 byte for the checkByte

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long dataOffset = fm.getOffset() + numFiles * 8 + 4;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File ID (incremental from 1)
        fm.skip(4);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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
