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
import org.watto.datatype.FileType;
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
public class Plugin_AWB_AFS2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AWB_AFS2() {

    super("AWB_AFS2", "AWB_AFS2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Daemon x Machina",
        "Shenmue 3");
    setExtensions("awb"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("hca", "HCA Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("AFS2")) {
        rating += 50;
      }

      fm.skip(4);

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

      long arcSize = fm.getLength();

      // 4 - Header (AFS2)
      fm.skip(4);

      // 4 - Flags
      int flags = fm.readInt();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Global Alignment
      int globalAlign = fm.readShort();
      fm.skip(2);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int[] localAligns = new int[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - Local Alignment
        int localAlign = ShortConverter.unsign(fm.readShort());
        localAligns[i] = localAlign;

        TaskProgressManager.setValue(i);
      }

      if ((flags & 0x200) == 0x200) {
        // 2-bytes per field

        // 4 - File Offset
        int currentOffset = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkOffset(currentOffset, arcSize);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          int offset = currentOffset;

          // 4 - File Offset
          int nextOffset = ShortConverter.unsign(fm.readShort());

          // add padding
          if (globalAlign != 0) {
            offset += calculatePadding(offset, globalAlign);
          }
          else {
            offset += calculatePadding(offset, localAligns[i]);
          }
          FieldValidator.checkOffset(offset, arcSize);

          int length = nextOffset - offset;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i) + ".hca";

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
          currentOffset = nextOffset;
        }

      }
      else {
        // 4-bytes per field

        // 4 - File Offset
        int currentOffset = fm.readInt();
        FieldValidator.checkOffset(currentOffset, arcSize);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          int offset = currentOffset;

          // 4 - File Offset
          int nextOffset = fm.readInt();

          // add padding
          if (globalAlign != 0) {
            offset += calculatePadding(offset, globalAlign);
          }
          else {
            offset += calculatePadding(offset, localAligns[i]);
          }
          FieldValidator.checkOffset(offset, arcSize);

          int length = nextOffset - offset;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i) + ".hca";

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
          currentOffset = nextOffset;
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
