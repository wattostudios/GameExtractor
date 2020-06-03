
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
public class Plugin_RBX extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RBX() {

    super("RBX", "3D Ultra RBX");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("3D Ultra Cool Radio Control Racers",
        "3D Ultra Mini Golf",
        "Mission Force: Cyber Storm");
    setExtensions("rbx");
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

      // Header (158 154 169 11)
      if (fm.readString(4).equals(new String(new byte[] { (byte) 158, (byte) 154, (byte) 169, (byte) 11 }))) {
        rating += 50;
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

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (158 154 169 11)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 12 - Filename (null)
        String filename = fm.readNullString(12);

        // 4 - File Offset
        long offset = fm.readInt() + 4;
        offsets[i] = offset;
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      // Calculate the file sizes
      java.util.Arrays.sort(offsets);

      for (int i = 0; i < numFiles - 1; i++) {
        Resource resource = resources[i];
        resource.setLength(offsets[i + 1] - offsets[i]);
        resource.setDecompressedLength(resource.getLength());
        FieldValidator.checkLength(resources[i].getLength(), arcSize);
      }
      resources[numFiles - 1].setLength(arcSize - offsets[numFiles - 1]);
      resources[numFiles - 1].setDecompressedLength(resources[numFiles - 1].getLength());

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

      // 4 - Header (158 154 169 11)
      fm.writeByte(158);
      fm.writeByte(154);
      fm.writeByte(169);
      fm.writeByte(11);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8 + (numFiles * 16);
      for (int i = 0; i < numFiles; i++) {
        // 12 Bytes - Filename (null)
        fm.writeNullString(resources[i].getName(), 12);

        // 4 Bytes - Data Offset
        fm.writeInt((int) offset);

        offset += resources[i].getDecompressedLength();
      }

      // To fix a weird offset thingy where each file's offset is off by 4
      fm.writeInt(0);

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
