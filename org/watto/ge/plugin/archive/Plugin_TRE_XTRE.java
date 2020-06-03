
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TRE_XTRE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_TRE_XTRE() {

    super("TRE_XTRE", "TRE_XTRE");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Wing Commander 4: The Price Of Freedom");
    setExtensions("tre");
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
      if (fm.readString(4).equals("XTRE")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // ID Directory Offset
      if (fm.readInt() == 24) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (XTRE)
      // 4 - Version (1)
      // 4 - ID Directory Offset
      fm.skip(12);

      // 4 - Filename Directory Offset (if no filename table, this field equals the next field)
      int namesDirOffset = fm.readInt();
      FieldValidator.checkOffset(namesDirOffset, arcSize);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - File Data Offset
      fm.skip(4);

      int numFiles = (namesDirOffset - 24) / 8;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Checksum
        fm.skip(4);

        // 4 - Offset (into the filename directory (if exists) or into the files directory)
        long offset = fm.readInt();
        if (offset == -1) {
          // empty entry
        }
        else {
          FieldValidator.checkOffset(offset, arcSize);
          offsets[realNumFiles] = offset;
          realNumFiles++;
        }
      }

      String[] names = new String[realNumFiles];
      if (namesDirOffset != dirOffset) {
        // THERE IS A FILENAMES DIRECTORY
        // Loop through directory
        for (int i = 0; i < realNumFiles; i++) {
          fm.seek(offsets[i]);

          // 1 - Filename Length
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          String filename = fm.readString(filenameLength);
          if (filename.indexOf("..\\..\\") == 0) {
            filename = filename.substring(6);
          }
          names[i] = filename;

          // 4 - Offset (into the files directory)
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }
      }
      else {
        for (int i = 0; i < realNumFiles; i++) {
          names[i] = Resource.generateFilename(i);
        }
      }

      // Loop through directory
      long[] offsets2 = new long[realNumFiles];
      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - File Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets2[i] = offset; // so we can calculate file sizes

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, 0, 0, 0);

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);
      //calculateFileSizes(resources,arcSize);

      // Calculating file sizes...
      // 1. Sort the offsets
      java.util.Arrays.sort(offsets2);

      // 2. Determine the file lengths, and add into the hashtable using key of the file offset
      java.util.Hashtable<String, Long> lengths = new java.util.Hashtable<String, Long>(realNumFiles);
      for (int i = 0; i < realNumFiles - 1; i++) {
        long length = offsets2[i + 1] - offsets2[i];
        FieldValidator.checkLength(length, arcSize);

        lengths.put("" + offsets2[i], new Long(length));
      }

      long lastLength = arcSize - offsets2[realNumFiles - 1];
      FieldValidator.checkLength(lastLength, arcSize);

      lengths.put("" + offsets2[realNumFiles - 1], new Long(lastLength));

      // 3. Assign the file lengths based on the file offset
      for (int i = 0; i < realNumFiles; i++) {
        long length = lengths.get("" + resources[i].getOffset()).intValue();
        resources[i].setLength(length);
        resources[i].setDecompressedLength(length);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
