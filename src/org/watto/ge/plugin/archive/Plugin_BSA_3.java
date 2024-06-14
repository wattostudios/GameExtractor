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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BSA_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BSA_3() {

    super("BSA_3", "BSA_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Elder Scrolls 2: Daggerfall");
    setExtensions("bsa"); // MUST BE LOWER CASE
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

      // Number Of Files
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // Archive Type
      short type = fm.readShort();
      if (type == 256 || type == 512) {
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

      setCanScanForFileTypes(false);

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Archive Type (256=NameBased, 512=FileIDBased)
      short archiveType = fm.readShort();

      long dirOffset = 0;
      if (archiveType == 256) {
        dirOffset = arcSize - (numFiles * 18);
      }
      else if (archiveType == 512) {
        dirOffset = arcSize - (numFiles * 8);
      }
      else {
        return null;
      }

      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      if (archiveType == 256) {
        // NameBased

        long offset = 4;
        for (int i = 0; i < numFiles; i++) {

          // 12 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(12);
          FieldValidator.checkFilename(filename);

          // 2 - null
          fm.skip(2);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);

          offset += length;
        }
      }
      else if (archiveType == 512) {
        // FileIDBased

        setCanScanForFileTypes(true);

        long offset = 4;
        for (int i = 0; i < numFiles; i++) {
          // 2 - File ID
          int fileID = ShortConverter.unsign(fm.readShort());

          // 2 - null
          fm.skip(2);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(fileID);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);

          offset += length;
        }
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 892220022 || headerInt1 == 908997238 || headerInt1 == 925774454) {
      return "3d";
    }

    return null;
  }

}
