
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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZAP extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ZAP() {

    super("ZAP", "ZAP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Gorky Zero: Aurora Watching");
    setExtensions("zap");
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // skip to end of archive
      fm.seek(arcSize - 12);

      // 4 - Pointer To Directory End
      int dirEndOffset = fm.readInt();
      FieldValidator.checkOffset(dirEndOffset, arcSize);

      // Skip to dir END offset
      fm.seek(dirEndOffset + 4);

      // 4 - Offset To Directory Start
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // Skip to dir START offset
      fm.seek(dirOffset);

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dirEndOffset - 8) {
        // 4 - Unknown ID (usually 1)
        fm.skip(4);

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        String filename = "";
        int checkByte = fm.readByte();
        int checkByte2 = fm.readByte();
        if (checkByte2 == 0) {
          // skip over filenameLength-1 bytes, then...
          filenameLength = ShortConverter.convertLittle(new byte[] { (byte) filenameLength, (byte) checkByte });
          fm.skip(filenameLength - 3);
          filenameLength = ByteConverter.unsign(fm.readByte());
        }
        else {
          filename += (char) checkByte + "" + (char) checkByte2;
          filenameLength -= 2;
        }

        // X - Filename
        filename += fm.readString(filenameLength);

        // 4 - File Offset?
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Directory ID (for the directory the file belongs to)???
        // 4 - null
        fm.skip(8);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
