
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
import org.watto.ge.plugin.resource.Resource_FileID;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MAP extends ArchivePlugin {

  String[] names;
  int version = 1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MAP() {

    super("MAP", "MAP");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("map");
    setGames("Halo");
    setPlatforms("PC");

    setFileTypes("spr", "Object Sprite");

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

      // Version
      int version = fm.readInt();
      if (version == 1 || version == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Filename Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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
      names = new String[0];
      version = 1;

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Version
      fm.skip(4);

      // 4 - Filename Offset
      int filenameOffset = fm.readInt();
      FieldValidator.checkOffset(filenameOffset, arcSize);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      names = new String[numFiles];

      // Loop through filename directory
      fm.seek(filenameOffset);
      for (int j = 0; j < numFiles; j++) {
        // X - Filename (null terminated)
        names[j] = fm.readNullString();
        FieldValidator.checkFilename(names[j]);
      }

      // Loop through data directory
      fm.seek(dirOffset);
      for (int i = 0; i < numFiles; i++) {
        // 4 - file ID?
        int fileID = fm.readInt();

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_FileID(path, fileID, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      for (int i = 0; i < numFiles; i++) {

        fm.seek(resources[i].getOffset());

        String filename = resources[i].getName();
        String header = fm.readString(4);
        if (header.equals("OggS")) {
          filename += ".ogg";
        }
        else {
          filename += ".unk";
        }

        resources[i].setName(filename);

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

      // 4 - Version
      fm.writeInt((int) version);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      int totalLengthOfDirectory = numFiles * 12;
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].getLength() >= 0) {
          totalLengthOfData += resources[i].getDecompressedLength();
        }
      }

      // 4 - Filename Directory Offset
      fm.writeInt(16 + totalLengthOfData + totalLengthOfDirectory);

      // 4 - Data Directory Offset
      fm.writeInt(16 + totalLengthOfData);

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 16;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        long fileID = -1;
        if (resources[i] instanceof Resource_FileID) {
          fileID = ((Resource_FileID) resources[i]).getID();
        }

        // 4 - File ID
        fm.writeInt((int) fileID);

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - Data Offset
        fm.writeInt((int) currentPos);

        currentPos += length;
      }

      // Loop 4 - Build filename directory
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();

        if (name.indexOf(".ogg") > -1) {
          name.substring(0, name.length() - 4);
        }

        // X - Filename
        fm.writeNullString(name);

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}