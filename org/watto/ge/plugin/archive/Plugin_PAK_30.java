
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
import org.watto.ge.plugin.exporter.Exporter_Custom_PAK_30;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_30 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_30() {

    super("PAK_30", "PAK_30");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pak");
    setGames("Rush For The Bomb");
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

      fm.skip(8);

      // Header (PACK)
      String header = fm.readString(4);
      if (header.equals("PACK") || header.equals("RFTB")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      Exporter_Custom_PAK_30 exporter = Exporter_Custom_PAK_30.getInstance();

      long arcSize = fm.getLength();

      // 12 - Header (Sr + 26 27 13 10 135 10 + PACK)
      fm.skip(12);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      dirLength += 16;

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // DIRECTORY IS XOR WITH BYTE 66
      int realNumFiles = 0;
      String oldFilename = "";
      while (fm.getOffset() < dirLength) {
        // 1 - New Filename Length
        int fullNameLength = ByteConverter.unsign(fm.readByte()) ^ 66;

        // 1 - Append Filename size
        int filenameLength = ByteConverter.unsign(fm.readByte()) ^ 66;

        String filename = "";
        if (fullNameLength > 0) {
          int oldCopyNameLength = fullNameLength - filenameLength;
          if (oldCopyNameLength <= oldFilename.length() && oldCopyNameLength > 0) {
            filename = oldFilename.substring(0, oldCopyNameLength);
          }
          else {
            filename = oldFilename;
          }
        }

        // 1 - Filename size (including null)
        fm.skip(1);

        // X - Filename (XOR with a different value?)
        // 1 - null Filename Terminator
        byte[] filenameBytes = fm.readNullString().getBytes();
        for (int b = 0; b < filenameBytes.length; b++) {
          filenameBytes[b] ^= 66;
        }
        filename += new String(filenameBytes);
        FieldValidator.checkFilename(filename);

        if (realNumFiles == 0) {
          oldFilename = filename;
        }

        // 4 - Offset
        byte[] offsetBytes = fm.readBytes(4);
        offsetBytes[0] ^= 66;
        offsetBytes[1] ^= 66;
        offsetBytes[2] ^= 66;
        offsetBytes[3] ^= 66;
        long offset = IntConverter.convertLittle(offsetBytes);

        offset += dirLength;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        byte[] lengthBytes = fm.readBytes(4);
        lengthBytes[0] ^= 66;
        lengthBytes[1] ^= 66;
        lengthBytes[2] ^= 66;
        lengthBytes[3] ^= 66;
        long length = IntConverter.convertLittle(lengthBytes);

        FieldValidator.checkLength(length, arcSize);

        //if (length == 0){
        //  oldFilename += "/";
        //  }

        // 1 - null
        fm.skip(1);

        if (length > 0) {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);

          TaskProgressManager.setValue((int) fm.getOffset());
          realNumFiles++;
        }
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