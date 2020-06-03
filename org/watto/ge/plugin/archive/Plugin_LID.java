
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_LID extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LID() {

    super("LID", "LID");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("lid");
    setGames("Skunny: Desert Raid",
        "Skunny: Lost In Space",
        "Skunny: Save Our Pizza",
        "Skunny: Back To The Forest",
        "Skunny Kart",
        "Skunny: In The Wild West");
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

      fm.skip(9);
      // 10 - Filename  - check for null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readInt() / 22;
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

      FileManipulator fm = new FileManipulator(path, false);

      // 10 - Filename (filled with nulls, if filename is too short)
      // 4 - File ID?
      // 4 - File Length
      fm.skip(18);

      // 4 - Data Offset
      int numFiles = fm.readInt() / 22 - 1;
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(0);

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 10 - Filename (null)
        String filename = fm.readNullString(10);
        FieldValidator.checkFilename(filename);

        // 4 - File ID?
        fm.skip(4);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = (22 * (numFiles + 1));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 10 - Filename (filled with nulls, if filename is too short)
        src.skip(10);
        fm.writeNullString(resources[i].getName(), 10);

        // 4 - File ID?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        // 4 - Data Offset
        src.skip(8);
        fm.writeInt((int) length);
        fm.writeInt((int) offset);

        offset += length;
      }

      for (int i = 0; i < 22; i++) {
        fm.writeByte(0);
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }

  }

}