
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
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DRS extends ArchivePlugin {

  int readSize = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DRS() {

    super("DRS", "DRS");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("drs");
    setGames("Age Of Empires",
        "Age Of Empires 2");
    setPlatforms("PC");

    setFileTypes("bin", "Binary Data (often text)",
        "slp", "ArtDesk SLP Image");

  }

  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, int infoOffset, String ext, long dirOffset, int numFiles) throws Exception {
    long arcSize = fm.getLength();

    int curPos = (int) fm.getOffset();
    fm.seek(dirOffset);

    for (int i = 0; i < numFiles; i++) {
      // 4 - File ID
      int fileID = fm.readInt();

      // 4 - Data Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      String filename = Resource.generateFilename(i) + "." + ext;

      //path,id,name,offset,length,decompLength,exporter
      resources[infoOffset] = new Resource_FileID(path, fileID, filename, offset, length);

      infoOffset++;

      readSize += length;
      TaskProgressManager.setValue(readSize);
    }

    fm.seek(curPos);

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
      if (fm.readString(36).equals("Copyright (c) 1997 Ensemble Studios.")) {
        rating += 50;
      }

      fm.skip(4);

      // Archive Name
      if (fm.readString(9).equals("1.00tribe")) {
        rating += 5;
      }

      fm.skip(7);

      // Number of Groups
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
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

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      readSize = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 36 - Header
      // 4 - Unknown (26) Number Of Groups?
      // 16 - Archive Name (1.00tribe       )
      fm.skip(56);

      // 4 - Number Of Groups
      int numGroups = fm.readInt();
      FieldValidator.checkNumFiles(numGroups);

      // 4 - Offset To First File Data
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = dirLength / 12;// guessed

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(readSize);

      int i = 0;
      int curFile = 0;
      for (int j = 0; j < numGroups; j++) {

        // 4 - File Type / Extension (reversed - 32 terminated)
        String fileExt = StringConverter.reverse(fm.readNullString(4));

        // 4 - Directory Offset
        long dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        // 4 - Number Of Files
        int tempNumFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        i += tempNumFiles;

        analyseDirectory(fm, path, resources, curFile, fileExt, dirOffset, tempNumFiles);
        curFile += tempNumFiles;
      }

      resources = resizeResources(resources, i);

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

      // 36 - Header
      fm.writeString("Copyright (c) 1997 Ensemble Studios.");

      // 4 - Unknown (26)
      fm.writeInt(26);

      // 16 - Archive Name
      fm.writeString("1.00tribe       ");

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // DETECT GROUPS,
      String[] groupNames = new String[10];
      int[] numFilesInGroup = new int[10];
      int[] fileGroupNum = new int[numFiles];
      int numGroups = 0;
      // Loop to calculate number of groups
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();

        if (name.lastIndexOf(".") > -1) {
          String ext = name.substring(name.lastIndexOf(".") + 1);

          if (ext.length() > 4) {
            ext = ext.substring(0, 4);
          }

          String extRev = "";
          if (ext.length() < 4) {
            for (int m = ext.length(); m < 4; m++) {
              extRev += " ";
            }
          }

          for (int m = ext.length() - 1; m > -1; m--) {
            extRev += ext.charAt(m);
          }

          boolean found = false;
          for (int j = 0; j < numGroups; j++) {
            if (groupNames[j].equals(extRev)) {
              found = true;
              numFilesInGroup[j]++;
              fileGroupNum[i] = j;
              j = numGroups;
            }
          }

          if (!found) {
            groupNames[numGroups] = extRev;
            numFilesInGroup[numGroups] = 0;
            fileGroupNum[i] = numGroups;
            numGroups++;
          }
        }

        else {
          boolean found = false;
          for (int j = 0; j < numGroups; j++) {
            if (groupNames[j].equals("llun")) {
              found = true;
              numFilesInGroup[j]++;
              fileGroupNum[i] = j;
              j = numGroups;
            }
          }

          if (!found) {
            groupNames[numGroups] = "llun";
            numFilesInGroup[numGroups] = 0;
            fileGroupNum[i] = numGroups;
            numGroups++;
          }

        }

      }

      // 4 - Number Of Groups
      fm.writeInt(numGroups);

      // 4 - Offset To First File Data
      fm.writeInt(numFiles * 12 + 64 + 12 * numGroups);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int numFilesSoFar = 0;
      for (int i = 0; i < numGroups; i++) {
        // 4 - File Type / Extension (reversed - 32 terminated)
        fm.writeString(groupNames[i]);

        // 4 - Directory Offset
        fm.writeInt(64 + 12 * i + 12 * (numFilesSoFar + 1));

        // 4 - Number Of Files
        fm.writeInt(numFilesInGroup[i] + 1);

        numFilesSoFar += numFilesInGroup[i];
      }

      int currentPos = numFiles * 12 + 64 + 12 * numGroups;
      for (int i = 0; i < numGroups; i++) {
        for (int j = 0; j < numFiles; j++) {
          if (fileGroupNum[j] == i) {

            int ID = -1;
            if (resources[j] instanceof Resource_FileID) {
              ID = (int) ((Resource_FileID) resources[j]).getID();
            }

            long length = resources[j].getDecompressedLength();

            // 4 - File ID
            if (ID > -1) {
              fm.writeInt(ID);
            }
            else {
              fm.writeInt(100000 + j);
            }

            // 4 - Data Offset
            fm.writeInt(currentPos);

            // 4 - File Length
            fm.writeInt((int) length);

            currentPos += length;

          }
        }
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numGroups; i++) {
        for (int j = 0; j < numFiles; j++) {
          if (fileGroupNum[j] == i) {

            write(resources[j], fm);

          }
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}