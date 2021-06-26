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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GRP_GRP1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GRP_GRP1() {

    super("GRP_GRP1", "GRP_GRP1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wings Of Prey");
    setExtensions("grp"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("GRP1")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Archive Length [+16]
      if (fm.readInt() + 16 == arcSize) {
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

      //ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (GRP1)
      // 4 - File Data Offset [+16]
      // 4 - Unknown Offset/Length
      // 4 - Archive Length [+16]
      fm.skip(16);

      // 4 - Offset to the Filename Offset Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number of Filename Offset Entries
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);

      // 4 - Offset to the File Data Properties Directory
      int filePropertyDirOffset = fm.readInt();
      FieldValidator.checkOffset(filePropertyDirOffset, arcSize);

      // 4 - Number of File Data Property Entries
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Offset to the Unknown Properties Directory
      // 4 - Number of Unknown Property Entries
      fm.skip(8);

      // Loop through directory
      String[] names = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;

        TaskProgressManager.setValue(i);
      }

      fm.seek(filePropertyDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File ID (Incremental from 0)
        fm.skip(4);

        String filename = names[i];
        /*
        int starPos = filename.indexOf('*');
        if (starPos > 0) {
          filename = filename.substring(0, starPos);
        }
        filename += ".ddsx";
        */

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