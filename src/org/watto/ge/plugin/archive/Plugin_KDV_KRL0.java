
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
public class Plugin_KDV_KRL0 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_KDV_KRL0() {

    super("KDV_KRL0", "KDV_KRL0");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("kdv");
    setGames("Commanche Enhanced");
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
      if (fm.readString(4).equals("KRL0")) {
        rating += 50;
      }

      // Archive Size
      if (fm.readInt() == fm.getLength() - 8) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("KRL0")) {
        rating += 5;
      }

      // Archive Size
      if (fm.readInt() == fm.getLength() - 8) {
        rating += 5;
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

      // 4 - Header (KRL0) //note the zero, not an O
      // 4 - archive size [-8]
      // 4 - Header (KDV0) // once again, it is a zero
      // 4 - archive size [-8]
      fm.skip(16);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Unknown
      // 4 - DirLength [-8]
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(16);

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset [-8]
        long offset = fm.readInt() - 8;
        FieldValidator.checkOffset(offset, arcSize);

        int currentPos = (int) fm.getOffset();
        long length = fm.readInt() - 8 - offset;
        if (i == numFiles - 1) {
          length = (int) fm.getLength() - offset;
        }
        fm.seek(currentPos);

        String filename = Resource.generateFilename(i);

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

      // 4 - Header (KRL0) //note the zero, not an O
      // 4 - archive size (not including this and the line above. ie -8)
      // 4 - Header (KDV0) // once again, it is a zero
      // 4 - archive size [-8]
      // 4 - numFiles
      // 4 - Unknown
      // 4 - DirLength [-8]
      // 4 - Unknown
      // 4 - Unknown
      fm.writeBytes(src.readBytes(36));

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 36 + (4 * numFiles) + 8;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Data Offset
        fm.writeInt((int) offset);

        offset += length;
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