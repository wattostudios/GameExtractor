
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
public class Plugin_MIX extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_MIX() {

    super("MIX", "MIX");

    //         read write replace rename
    setProperties(true, true, true, false);

    setExtensions("mix");
    setGames("Command And Conquer");
    setPlatforms("PC");

    setFileTypes("aud", "Audio File",
        "tpl", "Template Image File",
        "mve", "Movie File");

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

      // Number Of Files
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Body Length
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

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - Number of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Length Of Body
      fm.skip(4);

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID
        int fileID = fm.readInt();

        // 4 - Data Offset
        long offset = fm.readInt() + 6 + (numFiles * 12);
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_FileID(path, fileID, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
      }

      for (int i = 0; i < numFiles; i++) {
        fm.seek(resources[i].getOffset());

        byte[] filehead = fm.readBytes(4);

        String filename = resources[i].getName();

        if (filehead[0] == 34 && filehead[1] == 86) {
          filename += ".aud";
        }
        else if (filehead[0] == 59 && filehead[1] == 32) {
          filename += ".ini";
        }
        else if (filehead[0] == 24 && filehead[1] == 0 && filehead[2] == 24 && filehead[3] == 0) {
          filename += ".tpl";
        }
        else if (new String(filehead).equals("FORM")) {
          filename += ".mve";
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

      // 2 - Number Of Files
      fm.writeShort((short) numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].getLength() >= 0) {
          totalLengthOfData += resources[i].getDecompressedLength();
        }
      }

      // 4 - Body Length
      fm.writeInt(totalLengthOfData);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 0;
      for (int i = 0; i < numFiles; i++) {
        int fileID = -1;
        if (resources[i] instanceof Resource_FileID) {
          fileID = (int) ((Resource_FileID) resources[i]).getID();
        }

        long length = resources[i].getDecompressedLength();

        // 4 - File ID
        fm.writeInt(fileID);

        // 4 - Data Offset
        fm.writeInt(currentPos);

        // 4 - File Length
        fm.writeInt((int) length);

        currentPos += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}