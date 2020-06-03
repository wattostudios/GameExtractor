
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
public class Plugin_HOG_HOG2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_HOG_HOG2() {

    super("HOG_HOG2", "HOG_HOG2");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("hog", "d3c", "d3m", "mn3");
    setGames("Descent 3");
    setPlatforms("PC");

    setFileTypes("osf", "Outrage Sound File",
        "ogf", "Outrage Graphic File",
        "msn", "Mission Information",
        "brf", "Briefing Information",
        "msg", "Text Messages",
        "d3l", "Descent 3 Level",
        "omf", "Programming Script");

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
      if (fm.readString(4).equals("HOG2")) {
        rating += 50;
      }

      // Number Of Files
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

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Offset To First File
      int firstOffset = fm.readInt();
      FieldValidator.checkOffset(firstOffset, arcSize);

      // 56 - Padding (byte values = 255)
      fm.skip(56);

      long offset = firstOffset;
      for (int i = 0; i < numFiles; i++) {
        // 36 - Filename
        String filename = fm.readNullString(36);
        FieldValidator.checkFilename(filename);

        // 4 - Blank
        fm.skip(4);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Timestamp
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        offset += length;
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

      // 4 - Header
      fm.writeString("HOG2");

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - Offset To First File
      fm.writeInt(numFiles * 48 + 68);

      // 56 - Padding (of 255 byte values)
      for (int j = 0; j < 56; j++) {
        fm.writeByte(255);
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int timestamp = (int) (java.util.Calendar.getInstance().getTimeInMillis());
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        // 36 - Filename
        fm.writeNullString(name, 36);

        // 4 - Blank
        fm.writeInt(0);

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - Timestamp
        fm.writeInt(timestamp);
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