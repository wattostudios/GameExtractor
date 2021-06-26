
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
public class Plugin_E3_ESV1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_E3_ESV1() {

    super("E3_ESV1", "E3_ESV1");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Esoteria");
    setExtensions("e3");
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
      if (fm.readString(7).equals("ESv1.0 ")) {
        rating += 50;
      }

      fm.skip(1);

      // Number Of Files
      int numFiles = fm.readInt() + 1; // +1 for the null case
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

      FileManipulator fm = new FileManipulator(path, false);

      // 7 - Header (ESv1.0 )
      // 1 - Sub-type
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      if (numFiles == 0) {
        return readNullType(path, fm);
      }
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 64 - Filename (null)
        String filename = fm.readNullString(64);

        // 4 - Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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
  public Resource[] readNullType(File path, FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[1];
      TaskProgressManager.setMaximum(1);

      // 4 - Type Name Length (including null)
      int typeNameLength = fm.readInt();
      FieldValidator.checkLength(typeNameLength, 100);

      // NOTE: THE FOLLOWING 2 FIELDS DON'T APPEAR IF TypeNameLength=99
      String filename = "";
      if (typeNameLength == 99) {
        filename = Resource.generateFilename(0);
      }
      else {
        // X - Type Name
        // 1 - null
        filename = fm.readNullString(typeNameLength);
      }

      // 4 - null
      fm.skip(4);

      // 4 - Filename Length (sometimes including null)
      int filenameLength = fm.readInt();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Filename (null) (without extension)
      fm.skip(filenameLength);

      // 18 - null
      fm.skip(18);

      // X - File Data
      long offset = (int) fm.getOffset();
      long length = arcSize - offset;

      //path,id,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(1);

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

      // Write Header Data

      // 7 - Header (ESv1.0 )
      fm.writeString("ESv1.0 ");

      // 1 - Sub-type ((byte)96 or (byte)97 or (byte)99 or (byte)104 or (byte)105)
      fm.writeString("i");

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 12 + (numFiles * 72);
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 64 - Filename (null)
        fm.writeNullString(resources[i].getName(), 64);

        // 4 - Data Offset
        fm.writeInt((int) offset);

        // 4 - File Size
        fm.writeInt((int) length);

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
