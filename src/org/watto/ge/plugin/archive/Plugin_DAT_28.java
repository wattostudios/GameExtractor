
package org.watto.ge.plugin.archive;

import java.io.File;
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
public class Plugin_DAT_28 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_28() {

    super("DAT_28", "DAT_28");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dat");
    setGames("Rome Total War: The Barbarian Invasion",
        "Medieval 2 Total War");
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

      getDirectoryFile(fm.getFile(), "idx");
      rating += 25;

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
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      addFileTypes();

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "idx");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      boolean filenames = true;
      int unknownBlockSize = 12;

      // X - Archive Type ("SND.PACK" / "SKEL.PACK")
      // 0-3 - Padding to a multiple of 4 bytes (using the bytes 209,154,234)
      String header = fm.readString(4);
      if (header.equals("SND.")) {
        return readSND(fm, path, arcSize);
      }
      else if (header.equals("SKEL")) {
        return readSKEL(fm, path, arcSize);
      }
      else if (header.equals("ANIM")) {
        return readANIM(fm, path, arcSize);
      }
      else if (header.equals("EVT.")) {
        return readEVT(fm, path, arcSize);
      }

      return null;

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
  public Resource[] readANIM(FileManipulator fm, File path, long arcSize) throws Exception {

    fm.seek(16);

    // 4 - Number Of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    fm.seek(20);

    Resource[] resources = new Resource[numFiles];
    TaskProgressManager.setMaximum(numFiles);

    for (int i = 0; i < numFiles; i++) {
      // 4 - Entry Length [+12]
      fm.skip(4);

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Unknown
      // 4 - Unknown
      // 1 - Unknown
      fm.skip(9);

      // X - Filename
      // 1 - null Filename Terminator
      String filename = fm.readNullString();
      FieldValidator.checkFilename(filename);

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(i);
    }

    fm.close();

    return resources;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] readEVT(FileManipulator fm, File path, long arcSize) throws Exception {

    fm.seek(16);

    // 4 - Number Of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    fm.seek(20);

    Resource[] resources = new Resource[numFiles];
    TaskProgressManager.setMaximum(numFiles);

    for (int i = 0; i < numFiles; i++) {
      // 4 - Unknown (4)
      fm.skip(4);

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Unknown (1/2/3/4)
      fm.skip(4);

      String filename = Resource.generateFilename(i);

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(i);
    }

    fm.close();

    return resources;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] readSKEL(FileManipulator fm, File path, long arcSize) throws Exception {

    fm.seek(16);

    // 4 - Number Of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    fm.seek(20);

    Resource[] resources = new Resource[numFiles];
    TaskProgressManager.setMaximum(numFiles);

    for (int i = 0; i < numFiles; i++) {
      // 4 - Filename Length (including null)
      int filenameLength = fm.readInt() - 1;
      FieldValidator.checkFilenameLength(filenameLength);

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // X - Filename
      // 1 - null Filename Terminator
      String filename = fm.readString(filenameLength);
      FieldValidator.checkFilename(filename);
      fm.skip(1);

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(i);
    }

    fm.close();

    return resources;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] readSND(FileManipulator fm, File path, long arcSize) throws Exception {

    fm.seek(12);

    // 4 - Number Of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    fm.seek(24);

    Resource[] resources = new Resource[numFiles];
    TaskProgressManager.setMaximum(numFiles);

    for (int i = 0; i < numFiles; i++) {

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Sound Sample Rate (22050)
      // 4 - Sound Quality (16)
      // 4 - Sound Channels (1)
      // 4 - File Type? (13)
      fm.skip(16);

      // X - Filename
      // 1 - null Filename Terminator
      String filename = fm.readNullString();
      FieldValidator.checkFilename(filename);

      // 3 - Padding
      fm.skip(3);

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(i);
    }

    fm.close();

    return resources;

  }

}