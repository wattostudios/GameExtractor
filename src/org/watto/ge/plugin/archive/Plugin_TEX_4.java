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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TEX_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TEX_4() {

    super("TEX_4", "TEX_4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dead Rising 4");
    setExtensions("tex", "big"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("sfx"); // LOWER CASE

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
      byte[] headerBytes = fm.readBytes(4);
      if (headerBytes[0] == 6 && headerBytes[1] == 5 && headerBytes[2] == 4 && headerBytes[3] == 3) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      boolean isTexArchive = false;
      if (path.getName().contains(".tex")) {
        isTexArchive = true;
      }

      long arcSize = fm.getLength();

      // 4 - Header ((bytes)6,5,4,3)
      fm.skip(4);

      // 4 - File Data Offset
      // 4 - Archive Length
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Header Size (24)
      // 4 - Filename Directory Offset
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the archive)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - Hash?
        fm.skip(4);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Padding Size? (16)
        // 4 - Compression Flag? (1)
        fm.skip(8);

        if (length != decompLength) {
          offset += 4;
          length -= 4;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, "", offset, length, decompLength, exporter);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, "", offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        if (isTexArchive) {
          if (filename.indexOf('.') < 0) {
            // set a file extension
            filename += ".bct";
          }
        }

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
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
