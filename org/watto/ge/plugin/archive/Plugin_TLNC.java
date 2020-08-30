
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TLNC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TLNC() {

    super("TLNC", "TLNC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Splinter Cell: Double Agent");
    setExtensions("tlnc", "ulnc", "lin"); // MUST BE LOWER CASE
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

      String extension = FilenameSplitter.getExtension(fm.getFile());
      if (extension.equals(extensions[0])) {
        rating += 25;

        getDirectoryFile(fm.getFile(), "hlnc");
        rating += 25;

      }
      else if (extension.equals(extensions[1])) {
        rating += 25;

        getDirectoryFile(fm.getFile(), "hlnc");
        rating += 25;

        return rating; // skip the numFiles check below
      }
      else if (extension.equals(extensions[2])) {
        rating += 25;

        getDirectoryFile(fm.getFile(), "hln");
        rating += 25;
      }
      else {
        return 0;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      long arcSize = (int) path.length();

      File sourcePath;
      FileManipulator fm;

      String pathName = path.getName();
      if (pathName.indexOf(".tlnc") > 0 || pathName.indexOf(".ulnc") > 0) {
        sourcePath = getDirectoryFile(path, "hlnc");
        fm = new FileManipulator(sourcePath, false);
      }
      else {
        sourcePath = getDirectoryFile(path, "hln");
        fm = new FileManipulator(sourcePath, false);
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(fm.getLength());

      // Loop through directory
      int realNumFiles = 0;

      while (fm.getOffset() < fm.getLength()) {
        // 4 - Unknown
        // 1 - Unknown (75)
        fm.skip(5);

        // 512 - Filename (unicode) (null terminated)
        String utxFilename = fm.readUnicodeString(256);
        for (int j = 0; j < 256; j++) {
          // remove the null characters at the end of the filename
          if (utxFilename.charAt(j) == (char) 0) {
            utxFilename = utxFilename.substring(0, j);
            j = 256;
          }
        }
        FieldValidator.checkFilename(utxFilename);

        if (utxFilename.length() > 2) {
          // remove the ../ at the beginning
          if (utxFilename.charAt(0) == '.' && utxFilename.charAt(1) == '.') {
            utxFilename = utxFilename.substring(3);
          }
        }

        // 1 - Unknown (75)
        // 4 - null
        // 1 - Unknown (75)
        // 4 - Hash?
        // 1 - Unknown (75)
        fm.skip(11);

        // 4 - Number of Images in this UTX File
        int numImages = fm.readInt();

        for (int j = 0; j < numImages; j++) {

          // 4 - Unknown
          // 1 - Unknown (75)
          // 4 - Unknown
          // 1 - Unknown (75)
          fm.skip(10);

          // 4 - Image Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 1 - Unknown (75)
          fm.skip(1);

          String filename = utxFilename + "(" + (j + 1) + ").Texture";

          //path,name,offset,length,decompLength,exporter
          //resources[realNumFiles] = new Resource(path,filename,offset,length);
          resources[realNumFiles] = new Resource(path, filename, offset);
          realNumFiles++;
        }

        // 1 - Unknown (75)
        fm.skip(1);

        TaskProgressManager.setValue(fm.getOffset());
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}
