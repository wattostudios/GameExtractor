
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKG extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG() {

    super("PKG", "PKG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Space Rangers 2");
    setExtensions("pkg");
    setPlatforms("PC");

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

      // Version (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Entry Length (158)
      if (fm.readInt() == 158) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Version (4)
      fm.skip(4);

      readDirectory(fm, path, resources, "", arcSize, exporter);

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
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long arcSize, ExporterPlugin exporter) throws Exception {

    // 4 - Directory Length (including all sub-directories)
    fm.skip(4);

    // 4 - Number Of entries in this directory
    int numEntries = fm.readInt();
    FieldValidator.checkNumFiles(numEntries);

    // 4 - Entry Length (158)
    fm.skip(4);

    // Loop through directory
    int[] types = new int[numEntries];
    long[] offsets = new long[numEntries];
    long[] lengths = new long[numEntries];
    String[] filenames = new String[numEntries];

    for (int i = 0; i < numEntries; i++) {
      // 4 - Entry Length
      fm.skip(4);

      // 4 - Decompressed Length (or unknown, if it is a sub-directory entry)
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);
      lengths[i] = length;

      // 63 - Directory Name (null terminated, filled with junk) (Uppercase)
      fm.skip(63);

      // 63 - Directory Name (null terminated, filled with junk) (Mixed Case)
      String filename = fm.readNullString(63);
      FieldValidator.checkFilename(filename);
      filenames[i] = filename;

      // 4 - Entry Type? (3=Sub-Directory, 2=Compressed File, 1=Uncompressed File)
      int type = fm.readInt();
      types[i] = type;

      // 4 - Entry Type? (3=Sub-Directory, 2=Compressed File, 1=Uncompressed File)
      // 8 - null
      fm.skip(12);

      // 4 - Offset To Entry (either File or Sub-Directory)
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);
      offsets[i] = offset;

      // 4 - Unknown (null for File entries)
      fm.skip(4);
    }

    // jump to the offsets
    for (int i = 0; i < numEntries; i++) {
      int type = types[i];

      fm.seek(offsets[i]);

      if (type == 3) {
        // sub directory
        readDirectory(fm, path, resources, "" + filenames[i] + "\\", arcSize, exporter);
      }
      else if (type == 2) {
        // file (compressed)
        // 4 - Compressed Length (including all these 4 fields)
        long length = fm.readInt() - 16;
        FieldValidator.checkLength(length, arcSize);

        long decompLength = lengths[i];

        // 4 - Unknown
        // 4 - Compression Header (ZL02)
        // 4 - Unknown (65536)
        // X - File Data (ZLib Compressed)
        long offset = fm.getOffset() + 12;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, dirName + filenames[i], offset, length, decompLength, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }
      else if (type == 1) {
        // file (uncompressed)
        // 4 - Compressed Length (including all these 4 fields)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset() + 4;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, dirName + filenames[i], offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }
    }

  }

}
