
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_3() {

    super("WAD_3", "WAD_3");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Wipeout Pure");
    setExtensions("wad");
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

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Length
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Length (duplicate)
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 4;
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

      // 4 - Version (1)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        fm.skip(4);

        String filename = Resource.generateFilename(i);

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

      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      long offset = 8 + (numFiles * 16);
      long padding = 64 - (offset % 64);
      if (padding < 64) {
        offset += padding;
      }

      // Write Header Data
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // 4 - Version (1)
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 - Hash?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        // 4 - File Length
        // 4 - File Length
        src.skip(12);
        fm.writeInt((int) offset);
        fm.writeInt((int) length);
        fm.writeInt((int) length);

        // determine padding size
        long paddingSize = 64 - (length % 64);
        if (paddingSize == 64) {
          paddingSize = 0;
        }

        offset += length + paddingSize;
      }

      if (padding < 64) {
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }
      }

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        // X - File
        write(resources[i], fm);

        // 0-63 - padding
        long paddingSize = 64 - (resources[i].getLength() % 64);
        if (paddingSize < 64) {
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
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
