
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_4() {

    super("WAD_4", "WAD_4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Land Of Legends");
    setExtensions("wad");
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
      if (ByteConverter.unsign(fm.readByte()) == 206 && ByteConverter.unsign(fm.readByte()) == 202 && ByteConverter.unsign(fm.readByte()) == 239 && ByteConverter.unsign(fm.readByte()) == 190) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 1) {
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

      // 4 - Header? (206,202,239,190)
      // 4 - Version (1)
      // 4 - Unknown (158)
      fm.skip(12);

      // 1 - Reader Package Name Length (41)
      // 41 - Reader Package Name (System.Resources.ResourceReader, mscorlib)
      fm.skip(ByteConverter.unsign(fm.readByte()));

      // 1 - Resource Set Package Name Length (115)
      // 115 - Resource Set Package Name (System.Resources.RuntimeResourceSet, mscorlib, Version=1.0.5000.0, Culture=neutral, PublicKeyToken=b77a5c561934e089)
      fm.skip(ByteConverter.unsign(fm.readByte()));

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 1 - Byte Array Package Name Length (93)
      // 93 - Byte Array Package Name (System.Byte[], mscorlib, Version=1.0.5000.0, Culture=neutral, PublicKeyToken=b77a5c561934e089)
      // 4 - Padding to Offset 280 (using repeating string "PAD");
      fm.seek(280);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // for each file
      // 4 - Hash?

      // for each file
      // 4 - Unknown
      fm.skip(numFiles * 8);

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // Loop through directory
      String[] names = new String[numFiles];
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        // X - Filename (unicode text)
        names[i] = fm.readUnicodeString((int) (ByteConverter.unsign(fm.readByte()) / 2));

        // 4 - File Offset (relative to the start of the //FILE DATA)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 2 - null
        // 4 - Unknown (1)
        // 4 - Padding (all 255's)
        // 4 - Unknown (1)
        // 4 - null
        // 4 - Unknown (271)
        // 1 - null
        fm.skip(23);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 1 - File Start Marker (2)
        // X - File Data
        // 1 - File End Marker (11)
        long offset = fm.getOffset() + 1;

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
