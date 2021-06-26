
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
public class Plugin_001_TCLF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_001_TCLF() {

    super("001_TCLF", "001_TCLF");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Twilight");
    setExtensions("001");
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
      if (fm.readString(4).equals("TCLF")) {
        rating += 50;
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

      // 4 - Header (TLCF)
      fm.skip(4);

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int i = 0;
      while (fm.getOffset() < arcSize) {
        // 32 - Filename (null)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        // 4 - fileSize
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        long offset = (int) fm.getOffset();
        fm.skip(length);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue((int) fm.getOffset());
        i++;
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

      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // Write Header Data

      // 4 - Header (TCLF)
      fm.writeString("TCLF");
      src.skip(4);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        // 32 - Filename (null)
        fm.writeNullString(resources[i].getName(), 32);
        src.skip(32);

        // 4 Bytes - File Size
        fm.writeInt((int) resources[i].getDecompressedLength());
        int oldLength = src.readInt();

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // X - File
        write(resources[i], fm);
        src.skip(oldLength);

      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
