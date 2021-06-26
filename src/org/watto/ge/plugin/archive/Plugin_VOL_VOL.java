
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VOL_VOL extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_VOL_VOL() {

    super("VOL_VOL", "VOL_VOL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Red Baron 3D");
    setExtensions("vol", "ted"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("VOL ")) {
        rating += 50;
      }

      fm.skip(4);

      // Header
      if (fm.readString(4).equals("volh")) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (VOL )
      // 4 - Hash?
      // 4 - Header (volh)
      // 4 - Unknown (128) (BIG)

      // 4 - Header (vols)
      fm.skip(20);

      // 4 - Filename Directory Length (including padding and the next field) (XOR byte 4 with 128)
      byte[] bytes = fm.readBytes(4);
      bytes[3] -= 128;
      int dirOffset = IntConverter.convertLittle(bytes) + 24;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Length (excluding padding)
      int namesOffset = 28;
      fm.seek(dirOffset);

      // 4 - Header (voli)
      fm.skip(4);

      // 4 - Details Directory Length (including the next field) (XOR byte 4 with 128)
      bytes = fm.readBytes(4);
      bytes[3] -= 128;
      int numFiles = ((IntConverter.convertLittle(bytes)) / 14) - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory

      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the first filename in the filename directory)
        int nameOffset = fm.readInt() + namesOffset;
        FieldValidator.checkOffset(nameOffset, arcSize);
        nameOffsets[i] = nameOffset;

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length?
        // 2 - Unknown
        fm.skip(6);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, 0);

        TaskProgressManager.setValue(i);
      }

      // get the filenames
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        if (filename.equals("")) {
          realNumFiles = i;
          break;
        }

        resources[i].setName(filename);
      }

      resources = resizeResources(resources, realNumFiles);
      numFiles = realNumFiles;

      // get the file lengths
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resources[i].getOffset());

        // 4 - File Header
        System.out.println(fm.readString(4));

        // 4 - File Length (XOR byte 4 with 128)
        bytes = fm.readBytes(4);
        bytes[3] -= 128;
        int length = IntConverter.convertLittle(bytes);
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        resource.setLength(length);
        resource.setOffset(fm.getOffset());
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
