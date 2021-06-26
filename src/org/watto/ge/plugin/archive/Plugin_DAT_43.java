
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_43 extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_43() {

    super("DAT_43", "DAT_43");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Turok: Dinosaur Hunter", "Turok 2: Seeds Of Evil");
    setExtensions("dat", "lss", "lsm"); // MUST BE LOWER CASE
    setPlatforms("PC");
    setEnabled(false);

    //setFileTypes("","",
    //             "",""
    //             );

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      //long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      readDirectory(fm, resources, path, 0, 0);

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
  public void readDirectory(FileManipulator fm, Resource[] resources, File path, int offset, int length) throws Exception {

    // 4 - Number Of Files/Sub-Directories In This Directory
    byte[] header = fm.readBytes(4);
    if (new String(header).substring(0, 3).equals("RNC")) {
      // file
      System.out.println("Found file at " + offset);

      String filename = Resource.generateFilename(realNumFiles);

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length);
      realNumFiles++;

      TaskProgressManager.setValue(realNumFiles);
    }
    else {
      // directory

      long arcSize = path.length();

      int numFiles = IntConverter.convertLittle(header);
      FieldValidator.checkNumFiles(numFiles + 1);

      System.out.println("Found directory at " + offset + " with " + numFiles + " files in it");

      if (numFiles == 0) {
        return;
      }

      int endOfThisDirectory = (numFiles + 2) * 4;

      // Loop through directory
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File/Directory Offset (relative to the start of this directory)
        int dirOffset = fm.readInt();
        if (dirOffset < endOfThisDirectory) {
          System.out.println("Bad directory at " + offset);
          return;
        }
        dirOffset += offset;
        FieldValidator.checkOffset(dirOffset, arcSize);
        offsets[i] = dirOffset;
      }

      for (int i = 1; i < numFiles; i++) {
        lengths[i - 1] = offsets[i] - offsets[i - 1];
      }

      // 4 - Offset to the end of this directory (relative to the start of this directory)
      lengths[numFiles - 1] = fm.readInt() - offsets[numFiles - 1];

      for (int i = 0; i < numFiles; i++) {
        int dirOffset = offsets[i];
        fm.seek(dirOffset);
        System.out.println("Reading directory at " + dirOffset);
        readDirectory(fm, resources, path, dirOffset, lengths[i]);
      }

      System.out.println("Finished reading directory at " + offset);

    }

  }

}
