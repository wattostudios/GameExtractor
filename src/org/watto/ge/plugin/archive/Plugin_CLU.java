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
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CLU extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CLU() {

    super("CLU", "CLU");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Broken Sword 2: The Smoking Mirror");
    setExtensions("clu");
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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Unknown (8)
      if (fm.readLong() == 8) {
        rating += 5;
      }

      // null
      if (fm.readShort() == 0) {
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

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int numFiles = (int) ((arcSize - dirOffset) / 8);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        offsets[i] = offset;
        lengths[i] = length;
      }

      fm.getBuffer().setBufferSize(32);// short quick reads

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        long length = lengths[i];

        if (length == 0) { // ignore empty files
          continue;
        }

        fm.seek(offset);

        // 1 - File Type?
        int fileType = fm.readByte();

        String filename = Resource.generateFilename(realNumFiles);
        if (fileType == 112) {
          // X - Filename
          // 1 - null Filename Terminator
          filename = fm.readNullString();
          int filenameLength = filename.length();

          offset += (filenameLength + 1);
          length -= (filenameLength + 1);
        }
        else {
          // 31 - Filename (null)
          filename = fm.readNullString(31);
          offset += 32;
          length -= 32;
        }

        String extension = "." + fileType;
        if (fileType == 101) {
          extension = ".101"; //image??
        }
        else if (fileType == 102) {
          extension = ".screen";
        }
        else if (fileType == 103) {
          extension = ".script";
        }
        else if (fileType == 104) {
          extension = ".grid";
        }
        else if (fileType == 105) {
          extension = ".global_vars";
        }
        else if (fileType == 107) {
          extension = ".run_list";
        }
        else if (fileType == 108) {
          extension = ".text";
        }
        else if (fileType == 109) {
          extension = ".screen_mngr";
        }
        else if (fileType == 110) {
          extension = ".new";
        }
        else if (fileType == 111) {
          extension = ".wav";
        }
        else if (fileType == 113) {
          extension = ".palette";
        }
        filename += extension;

        //System.out.println(filename);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      if (realNumFiles != numFiles) {
        resources = resizeResources(resources, realNumFiles);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 12;
      for (int i = 0; i < numFiles; i++) {
        dirOffset += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 8 - Directory Offset
      fm.writeLong(dirOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // 4 - This Offset
      fm.writeInt((int) fm.getLength());

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8;
      for (int i = 0; i < numFiles; i++) {
        long decompLength = resources[i].getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
