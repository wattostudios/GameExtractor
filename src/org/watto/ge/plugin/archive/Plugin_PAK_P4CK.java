
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_P4CK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_P4CK() {

    super("PAK_P4CK", "PAK_P4CK");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Second Sight");
    setExtensions("pak");
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
      if (fm.readString(4).equals("P4CK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 16)) {
        rating += 5;
      }

      // Filename Directory Offset
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      long arcSize = fm.getLength();

      // 4 - Header (P4CK)
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Offset Directory Length
      int numFiles = fm.readInt() / 16;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Length

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset + (numFiles * 16));

      // FILENAME DIRECTORY
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        names[i] = fm.readNullString();
        FieldValidator.checkFilename(names[i]);
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the directory)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
      long dirOffset = 16;
      int filenameDirLength = 0;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        long padding = 16 - (length % 16);
        if (padding == 16) {
          padding = 0;
        }

        dirOffset += length + padding;

        filenameDirLength += resources[i].getNameLength() + 1;
      }

      // Write Header Data

      // 4 - Header (P4CK)
      fm.writeString("P4CK");

      // 4 - Directory Offset
      fm.writeInt((int) dirOffset);

      // 4 - Offset Directory Length
      fm.writeInt(numFiles * 16);

      // 4 - Filename Directory Length
      fm.writeInt((int) filenameDirLength);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        // X - File Data
        write(resources[i], fm);

        long padding = 16 - (resources[i].getLength() % 16);
        if (padding == 16) {
          padding = 0;
        }

        // 1-16 - null
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16;
      int filenameOffset = numFiles * 16;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 - Filename Offset (relative to the start of the directory)
        fm.writeInt((int) filenameOffset);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - null
        fm.writeInt((int) 0);

        filenameOffset += resources[i].getNameLength() + 1;

        long padding = 16 - (length % 16);
        if (padding == 16) {
          padding = 0;
        }

        offset += length + padding;

      }

      // WRITE FILENAME DIRECTORY
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        fm.writeString(resources[i].getName());

        // 1 - null
        fm.writeByte(0);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
