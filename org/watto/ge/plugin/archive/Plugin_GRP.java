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
public class Plugin_GRP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GRP() {

    super("GRP", "GRP");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Nibiru: Messenger Of The Gods",
        "Nibiru: Age Of Secrets",
        "The Black Mirror");
    setExtensions("grp");
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

      fm.skip(16);

      // Header Length (44)
      if (fm.readInt() == 44) {
        rating += 5;
      }

      fm.skip(4);

      // Version? (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files (including null padding directory entries)
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 16 - Unknown
      // 4 - Length Of Archive Header (44)
      // 4 - Archive Length?
      // 4 - Version? (2)
      fm.skip(28);

      // 4 - Number Of Files (NOT including the padding entries at the end of the directory)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Files (including padding entries at the end of the directory)
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // NOTE - the last 3 values are unknown
      int[] xorVals = new int[] { 169, 134, 137, 144, 149, 144, 137, 134, 223, 139, 134, 146, 223, 190, 184, 187, 172, 223, 137, 158, 141, 138, 149, 154, 197, 223, 183, 158, 156, 148, 0, 0, 0 };

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 33 - Encrypted Filename? (null)
        byte[] filenameBytes = fm.readBytes(33);
        String filename = "";
        for (int f = 0; f < 33; f++) {
          byte fnByte = filenameBytes[f];
          if (fnByte == 0) {
            f = 33;
          }
          else {
            filename += (char) ((byte) (fnByte ^ xorVals[f]));// + "*" + fnByte + " ";
          }
        }
        //filename = FileManipulator.removeNonFilename(filename);

        // 4 - File Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - null
        fm.skip(8);

        //String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

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

}
