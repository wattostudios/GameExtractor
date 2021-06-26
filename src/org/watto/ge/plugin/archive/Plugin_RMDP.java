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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RMDP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RMDP() {

    super("RMDP", "RMDP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alan Wake");
    setExtensions("rmdp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

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

      // 1 - Version? (1)
      fm.skip(1);

      // 4 - Number of Folders
      fm.skip(4);

      // 4 - Number of Names
      int numFolders = IntConverter.changeFormat(fm.readInt()) - 1;
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Names Directory Length
      int nameDirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(nameDirLength, arcSize);

      // 156 - Junk to offset 173?
      fm.seek(173);

      // skip over the directories so we can get the filenames
      fm.skip(numFolders * 28 + numFiles * 40);

      byte[] nameDirBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      fm.seek(173);

      String[] folderNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Hash?
        // 4 - Unknown
        fm.skip(8);

        // 4 - Parent Folder ID
        int parentID = IntConverter.changeFormat(fm.readInt());

        // 4 - Unknown
        fm.skip(4);

        // 4 - Folder Name Offset (relative to the start of the Names Directory)
        int nameOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(nameOffset, nameDirLength);

        // X - Name (null terminated)
        nameFM.relativeSeek(nameOffset);
        String folderName = nameFM.readNullString();

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        parentID--;
        if (parentID >= 0 && parentID < numFolders) {
          folderName = folderNames[parentID] + folderName;
        }
        folderName += "\\";

        folderNames[i] = folderName;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        // 4 - Unknown
        fm.skip(8);

        // 4 - Parent Folder ID
        int parentID = IntConverter.changeFormat(fm.readInt());

        // 4 - Unknown
        fm.skip(4);

        // 4 - Filename Offset (relative to the start of the Names Directory)
        int nameOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(nameOffset, nameDirLength);

        // X - Name (null terminated)
        nameFM.seek(nameOffset);
        String filename = nameFM.readNullString();

        // 8 - File Offset
        byte[] offsetBytes = fm.readBytes(8);
        long offset = LongConverter.convertBig(offsetBytes);
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        parentID--;
        if (parentID >= 0 && parentID < numFolders) {
          filename = folderNames[parentID] + filename;
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
