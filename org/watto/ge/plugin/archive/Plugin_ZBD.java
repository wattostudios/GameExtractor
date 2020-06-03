
package org.watto.ge.plugin.archive;

import java.io.File;
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
public class Plugin_ZBD extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ZBD() {

    super("ZBD", "Zipper Interactive ZBD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("zbd");
    setGames("Recoil",
        "Mech Warrior 3");
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
      if (fm.readInt() == 0) {
        rating += 15;
      }

      // Version
      int version = fm.readInt();
      if (version == 1 || version == 7) {
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
      // 4 - null
      fm.skip(4);

      // 4 - Version
      int version = fm.readInt();
      //System.out.println(version);

      Resource[] success = new Resource[0];
      if (version == 1) {
        success = read1(fm, path);
      }
      else if (version == 2) {
        //success = read2(fm,path);
      }
      else if (version == 7) {
        success = read7(fm, path);
      }
      else if (version == 15) {
        //success = read15(fm,path);
      }
      else if (version == 28) {
        //success = read28(fm,path);
      }

      fm.close();

      return success;

    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] read1(FileManipulator fm, File path) throws Exception {

    // 4 - Unknown
    fm.skip(4);

    // 4 - numFiles
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    // 8 - null
    fm.skip(8);

    long arcSize = fm.getLength();

    Resource[] resources = new Resource[numFiles];
    TaskProgressManager.setMaximum(numFiles);

    //int firstDataOffset = 16 + (40 * numFiles);
    for (int i = 0; i < numFiles; i++) {

      // 32 - Filename (null)
      String filename = fm.readNullString(32);
      FieldValidator.checkFilename(filename);

      // 4 - Data Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - Padding (all 255's)
      fm.skip(4);

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset);

      TaskProgressManager.setValue(i);
    }

    fm.close();

    calculateFileSizes(resources, arcSize);

    return resources;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] read15(FileManipulator fm, File path) throws Exception {

    // 4 - numFiles
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    Resource[] resources = new Resource[numFiles];

    // 4 - Directory Entry Length (36)
    fm.skip(4);

    // 4 - Directory Offset
    long dirOffset = fm.readInt();

    // 4 - Unknown
    // 4 - Unknown
    fm.skip(8);

    String[] names = new String[numFiles];
    for (int i = 0; i < numFiles; i++) {
      // 16 - Unknown
      fm.skip(16);

      // 20 - Filename (null)
      names[i] = fm.readNullString(20);
    }

    fm.seek(dirOffset); // just incase we are not there

    // 4 - Unknown
    // 4 - numFiles
    // 4 - numFiles
    // 4 - numFiles-1
    fm.skip(16);

    for (int i = 0; i < numFiles; i++) {

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - File ID
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      // 4 - null
      // 2 - File ID
      // 2 - File ID

      //path,id,name,offset,length,decompLength,exporter
      //resources[i] = new Resource(path,names[i],offset,length);

      //TaskProgressManager.setValue(offset);

      //offset += length;
    }

    fm.close();

    return resources;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] read7(FileManipulator fm, File path) throws Exception {

    // 4 - numFiles
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    Resource[] resources = new Resource[numFiles];

    long offset = 12 + (128 * numFiles);
    for (int i = 0; i < numFiles; i++) {
      // 120 - Filename (null)
      String filename = fm.readNullString(120);

      // 4 - Unknown
      fm.skip(4);

      // 4 - File Length
      long length = fm.readInt();

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(offset);

      offset += length;
    }

    fm.close();

    return resources;

  }

}