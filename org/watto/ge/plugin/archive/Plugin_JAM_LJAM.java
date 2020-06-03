
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_JAM_LJAM extends ArchivePlugin {

  int realNumFiles = 0;
  int readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_JAM_LJAM() {

    super("JAM_LJAM", "JAM_LJAM");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("jam");
    setGames("Lego Racers");
    setPlatforms("PC");

    setFileTypes("pcm", "PCM Audio File",
        "sbk", "SBK File",
        "srf", "SRF File",
        "lrs", "LRS File",
        "mab", "MAB File",
        "rab", "RAB File",
        "ccb", "CCB File",
        "rcb", "RCB File",
        "adb", "ADB Database",
        "bdb", "BDB Database",
        "ddb", "DDB Driver Database",
        "edb", "EDB Database",
        "gdb", "GDB Database",
        "idb", "IDB Information Database",
        "mdb", "MDB Model Database",
        "sdb", "SDB Skin/Structure Database",
        "tdb", "TDB Texture Database",
        "wdb", "WDB Car Database",
        "tgb", "TGB File",
        "ghb", "GHB File",
        "mib", "MIB File",
        "tib", "TIB File",
        "skb", "SKB File",
        "cmb", "CMB File",
        "emb", "EMB File",
        "tmb", "TMB File",
        "cpb", "CPB File",
        "spb", "SPB File",
        "crb", "CRB File",
        "lrb", "LRB File",
        "rrb", "RRB File",
        "trb", "TRB File",
        "msb", "MSB File",
        "lsb", "LSB File",
        "bvb", "BVB File",
        "evb", "EVB File",
        "pwb", "PWB File",
        "hzb", "HZB File");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long offset) throws Exception {
    fm.seek(offset);
    long arcSize = fm.getLength();

    // 4 - Number of Files (0 for directory)
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles + 1); // +1 to avoid errors with directories (ie 0=directory)

    if (numFiles == 0) {
      // Directory

      // 4 - Number of Directories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      for (int i = 0; i < numDirectories; i++) {
        fm.seek(offset + 8 + (i * 16));
        // 12 - Directory Name
        String subDirName = fm.readNullString(12);
        FieldValidator.checkFilename(subDirName);

        // 4 - Sub-Directory Offset
        long dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        analyseDirectory(fm, path, resources, dirName + File.separator + subDirName, dirOffset);

      }

    }

    else {
      // Files
      for (int i = 0; i < numFiles; i++) {
        // 12 - Filename
        String filename = dirName + "\\" + fm.readNullString(12);

        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

        realNumFiles++;
        TaskProgressManager.setValue(readLength);
        readLength += length;
      }

    }

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
      if (fm.readString(4).equals("LJAM")) {
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

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;
      readLength = 0;

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header

      int numFiles = Archive.getMaxFiles(4);// guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, "", 4);

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}