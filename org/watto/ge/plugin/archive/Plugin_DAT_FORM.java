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
public class Plugin_DAT_FORM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_FORM() {

    super("DAT_FORM", "DAT_FORM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("10 Second Ninja X",
        "Dead Ground",
        "Gloom",
        "HackyZack",
        "HellCat",
        "Knight Club",
        "Laggerjack",
        "Monster Slayers",
        "Rising Lords",
        "Spectrubes Infinity",
        "Starship Annihilator",
        "The Rare Nine");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("FORM")) {
        rating += 25;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("AUDO")) {
        rating += 25;
      }

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 16, arcSize)) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FORM)
      // 4 - Archive Length [+8]
      // 4 - Header 2 (AUDO)
      // 4 - Archive Length [+16]
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      long previousOffset = offsets[0] + 4; // +4 to skip the header field
      for (int i = 1; i < numFiles; i++) { // Note, starting at 1
        long thisOffset = offsets[i] + 4; // +4 to skip the header field
        long length = thisOffset - previousOffset - 4;// -4 to skip the header field

        int fileNum = i - 1;
        String filename = Resource.generateFilename(fileNum);

        //path,name,offset,length,decompLength,exporter
        resources[fileNum] = new Resource(path, filename, thisOffset, length);

        TaskProgressManager.setValue(fileNum);

        previousOffset = thisOffset;
      }

      // Create the last file
      int fileNum = numFiles - 1;
      long lastOffset = offsets[fileNum] + 4;// +4 to skip the header field
      long lastLength = arcSize - lastOffset;
      String lastFilename = Resource.generateFilename(fileNum);
      resources[fileNum] = new Resource(path, lastFilename, lastOffset, lastLength);

      fm.getBuffer().setBufferSize(4);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        String filename = resource.getName();

        String header = fm.readString(4);
        if (header.equals("RIFF")) {
          filename += ".wav";
        }
        else if (header.equals("OggS")) {
          filename += ".ogg";
        }

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

}
