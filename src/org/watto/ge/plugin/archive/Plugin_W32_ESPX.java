/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_W32_ESPX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_W32_ESPX() {

    super("W32_ESPX", "W32_ESPX");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Psi Ops");
    setExtensions("w32");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("ESPx")) {
        rating += 50;
      }

      // Version (6)
      if (fm.readInt() == 6) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Extension Dir Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Files Dir Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 24)) {
        rating += 5;
      }

      fm.skip(8);

      // Filename Dir Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (ESPx)
      // 4 - Version (6)
      // 4 - Unknown
      // 4 - Extension Directory Offset
      // 4 - Extension Directory Length
      fm.skip(20);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Files Directory Length
      int numFiles = fm.readInt() / 24;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Offset To ???
      // 4 - ??? Length
      fm.skip(8);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset);

      // 4 - Filename Directory Length
      // 4 - null
      fm.seek(dirOffset);

      // 4 - Number of Files
      numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Type/Extension (eg "afsm", "anim", etc)
        String extension = "." + fm.readString(4);

        // 4 - File Type ID (74=afsm, 4=anim, etc)
        fm.skip(4);

        // 4 - File Size
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Offset (relative to the start of the Filename directory)
        int filenameOffset = filenameDirOffset + fm.readInt();
        FieldValidator.checkOffset(filenameOffset);
        nameOffsets[i] = filenameOffset;

        // 4 - File Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, extension, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

        TaskProgressManager.setValue(i);
      }

      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        if (filename.length() == 0) {
          filename = Resource.generateFilename(i);
        }

        Resource resource = resources[i];
        resource.setFilename(filename); // note - setFilename so we still keep the extension!
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
