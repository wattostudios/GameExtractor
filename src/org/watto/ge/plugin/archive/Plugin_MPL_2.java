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
public class Plugin_MPL_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MPL_2() {

    super("MPL_2", "MPL_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Muppet Treasure Island");
    setExtensions("mpl"); // MUST BE LOWER CASE
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

      if (fm.readShort() == 1) {
        rating += 5;
      }

      fm.skip(8);

      if (fm.readInt() == 14) {
        rating += 5;
      }

      if (fm.readInt() == 1002) {
        rating += 5;
      }

      fm.skip(30);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // find all the archives

      int numArchives = 1; // 1 = this current archive
      File[] archiveFiles = new File[20]; // guess max 20
      long[] archiveLengths = new long[20]; // guess max 20
      archiveFiles[0] = path;
      archiveLengths[0] = path.length();

      String baseName = path.getAbsolutePath();
      baseName = baseName.substring(0, baseName.length() - 5);

      for (int i = 2; i < 20; i++) {
        File archiveFile = new File(baseName + i + ".MPX");
        if (archiveFile.exists() && archiveFile.isFile()) {
          archiveFiles[numArchives] = archiveFile;
          archiveLengths[numArchives] = archiveFile.length();
          numArchives++;
        }
        else {
          break; // no more archives
        }
      }

      // 2 - Version (1)
      // 2 - Unknown ((bytes)165,165)
      // 4 - Unknown
      // 2 - null
      // 4 - Block 1 Offset (14)

      // BLOCK 1
      // 4 - ID (1002)
      // 4 - Unknown
      // 4 - Unknown
      // 2 - null

      // 2 - null
      // 4 - Block 2 Offset (34)

      // BLOCK 2
      // 4 - ID (1000)
      // 4 - Unknown
      // 4 - Unknown
      // 2 - null
      fm.skip(48);

      // 4 - Number of Streams
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown (1)
      // 2 - Number of Segments
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 16 - Stream Name (null terminated, filled with nulls) ("assetStream", "sceneStream",...)
        String extension = fm.readNullString(16);
        FieldValidator.checkFilename(extension);

        // 4 - Unknown
        // 4 - Stream Abbreviation ("eStr")
        fm.skip(8);

        // 2 - Archive File Number
        short archiveNumber = (short) (fm.readShort() - 1); // -1 because archive numbers start at 1
        FieldValidator.checkRange(archiveNumber, 0, numArchives);

        File archiveFile = archiveFiles[archiveNumber];
        long arcSize = archiveLengths[archiveNumber];

        // 4 - Stream Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Stream Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i) + "." + extension;

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(archiveFile, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

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
