
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VIV_BIG4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIV_BIG4() {

    super("VIV_BIG4", "VIV_BIG4");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Need For Speed Underground 2",
        "Lord Of The Rings: The Battle For Middle Earth",
        "Medal Of Honor: European Assault");
    setPlatforms("PC", "XBox");
    setExtensions("viv", "big");
    setEnabled(false); // DISABLED, BECAUSE WANT TO USE THE BIG_BIGF ONE INSTEAD, WHICH IS THE SAME BUT MORE UPDATED

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
      if (fm.readString(4).equals("BIG4")) {
        rating += 50;
      }

      // Archive Size
      if (IntConverter.changeFormat(fm.readInt()) == fm.getLength()) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
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

      // 4 - Header (BIG4)
      // 4 - Archive Size
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Size
      fm.skip(4);

      long arcSize = fm.getLength();

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 Bytes - Data Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 Bytes - File Size
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // X Bytes - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

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
      int directorySize = 16;
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        directorySize += 8 + resources[i].getNameLength() + 1;

        long length = resources[i].getDecompressedLength();
        filesSize += length;

        // padding for the file
        long padding = 64 - (length % 64);
        if (padding == 64) {
          padding = 0;
        }
        filesSize += padding;

      }

      int dirPadding = 64 - (directorySize % 64);
      if (dirPadding == 64) {
        dirPadding = 0;
      }
      directorySize += dirPadding;

      int archiveSize = filesSize + directorySize;

      // Write Header Data

      // 4 - Header (BIG4)
      fm.writeString("BIG4");

      // 4 - Archive Size
      fm.writeInt(IntConverter.convertBig(archiveSize));

      // 4 - Number Of Files
      fm.writeInt(IntConverter.convertBig(numFiles));

      // 4 - Directory Size
      fm.writeInt(IntConverter.convertBig(directorySize));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = directorySize;
      for (int i = 0; i < numFiles; i++) {
        // 4 Bytes - Data Offset
        fm.writeInt(IntConverter.convertBig((int) offset));

        // 4 Bytes - File Size
        fm.writeInt(IntConverter.convertBig((int) resources[i].getDecompressedLength()));

        // X Bytes - Filename (null)
        fm.writeNullString(resources[i].getName());
      }

      // directory padding
      for (int i = 0; i < dirPadding; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        write(resources[i], fm);

        long padding = 64 - (resources[i].getDecompressedLength() % 64);
        if (padding == 64) {
          padding = 0;
        }

        for (int j = 0; j < padding; j++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
