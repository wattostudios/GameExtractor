
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DIC_DIC1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DIC_DIC1() {

    super("DIC_DIC1", "DIC_DIC1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Colin McRae DiRT");
    setExtensions("wia", "wip");
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

      File dirFile = new File(fm.getFilePath() + File.separator + "CMSamples.dic");
      if (!dirFile.exists()) {
        return 0;
      }
      rating += 25;

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

      File sourcePath = new File(path.getPath() + File.separator + "CMSamples.dic");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Heading (DIC1)
      // 8 - Unknown
      fm.skip(12);

      // 4 - Number Of External Files
      int numExternal = fm.readInt();
      FieldValidator.checkNumFiles(numExternal);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numExternal);

      String pathDir = path.getAbsolutePath() + File.separator;

      // Loop through directory
      int[] numbers = new int[numExternal];
      int[] offsets = new int[numExternal];
      String[] names = new String[numExternal];

      for (int i = 0; i < numExternal; i++) {
        // 4 - Directory Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Number Of Files in this external file
        int numFilesInDir = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInDir);
        numbers[i] = numFilesInDir;

        // 16 - Filename of external file (null) (no extension)
        String name = fm.readNullString(16);
        FieldValidator.checkFilename(name);
        names[i] = pathDir + name;
      }

      // Loop through directory
      int realNumFiles = 0;
      for (int j = 0; j < numExternal; j++) {
        int number = numbers[j];
        int offset = offsets[j];

        fm.seek(offset + number * 24);

        // 4 - External File Length
        int endLength = fm.readInt();
        FieldValidator.checkLength(endLength);

        // 4 - External File Extension (null) (eg. "WIP", "WIA")
        File namePath = new File(names[j] + "." + fm.readNullString(4));

        fm.seek(offset);

        int prevOffset = 0;

        for (int i = 0; i < number; i++) {
          // 4 - File Offset
          offset = fm.readInt();
          FieldValidator.checkOffset(offset);

          int length = 0;
          if (i != 0) {
            // set the length of the previous resource in this external file
            length = offset - prevOffset;
            resources[realNumFiles - 1].setLength(length);
          }

          // 4 - Unknown
          fm.skip(4);

          // 16 - Filename (null)
          String filename = fm.readNullString(16);
          FieldValidator.checkFilename(filename);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(namePath, filename, offset, length);
          realNumFiles++;

          prevOffset = offset;
        }

        // set the length of the last resource in this external file
        int length = endLength - prevOffset;
        resources[realNumFiles - 1].setLength(length);

        TaskProgressManager.setValue(j);
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

}
