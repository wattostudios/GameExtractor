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
import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_WINGSPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_WINGSPAK() {

    super("PAK_WINGSPAK", "PAK_WINGSPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Soldner: Secret Wars");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("bld", "ddb", "fnc", "fnt", "map", "py", "seg", "sms", "stmp", "stmps", "ttmm", "v2d"); // LOWER CASE

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
      if (fm.readString(8).equals("WINGSPAK")) {
        rating += 50;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (WINGSPAK)
      // 4 - null
      fm.skip(12);

      // 4 - Directory Offset
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Unknown (6)
      fm.skip(4);

      // Loop through directory
      realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        boolean success = readDirectory(fm, path, resources, "", 1);
        if (!success) {
          return null;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // now go through and get the decompressed lengths
      fm.getBuffer().setBufferSize(4);

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();
        fm.relativeSeek(offset);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        resource.setLength(resource.getLength() - 4);
        resource.setDecompressedLength(decompLength);
        resource.setOffset(offset + 4);
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
  
   **********************************************************************************************
   **/

  public boolean readDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, int numSubFiles) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      long arcSize = fm.getLength();

      for (int i = 0; i < numSubFiles; i++) {
        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 1 - Entry Type
        byte entryType = fm.readByte();
        if (entryType == 1) {
          // directory

          // 4 - Number of Files/Sub-Directories under this Directory
          int numEntries = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkNumFiles(numSubFiles);

          readDirectory(fm, path, resources, dirName + filename + "\\", numEntries);
        }
        else if (entryType == 0) {
          // file

          // 4 - null
          fm.skip(4);

          // 4 - File Offset
          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - null
          fm.skip(4);

          // 4 - Compressed Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          // 4 - Compression Flag? (1)
          fm.skip(4);

          filename = dirName + filename;
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else {
          ErrorLogger.log("[PAK_WINGSPAK] Unknown entry type: " + entryType);
          return false;
        }

      }

      return true;

    }
    catch (Throwable t) {
      logError(t);
      return false;
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
