
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
public class Plugin_BF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BF() {

    super("BF", "BF");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("bf");
    setGames("Prince of Persia: Sands of Time",
        "Prince of Persia: Warrior Within",
        "Prince Of Persia: The Two Thrones");
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

      long arcSize = fm.getLength();

      // Directory Size
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // First File Length
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

      long arcSize = fm.getLength();

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      int relOffset = dirLength + 4;
      while (fm.getOffset() < dirLength) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - Data Offset
        long offset = relOffset + fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        // 1 - null
        String filename = fm.readString(filenameLength - 1);
        fm.skip(1);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

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

      // calc sizes
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalDirLength = 0;
      for (int i = 0; i < numFiles; i++) {
        totalDirLength += resources[i].getName().length() + 21;
      }

      // 4 - dirLength
      fm.writeInt((int) totalDirLength);
      src.skip(4);

      long offset = 0;

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        String name = resources[i].getName();

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 4 - Data Offset
        // 4 - File Length
        src.skip(8);
        fm.writeInt((int) offset);
        fm.writeInt((int) length);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 4 - Filename Length (including null)
        int oldFilenameLength = src.readInt();
        fm.writeInt(name.length() + 1);

        // X - Filename
        // 1 - null
        src.skip(oldFilenameLength);
        fm.writeString(name);
        fm.writeByte(0);

        offset += length;
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