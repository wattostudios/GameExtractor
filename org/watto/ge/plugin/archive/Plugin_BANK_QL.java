
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPluginException;
import org.watto.component.WSPopup;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BANK_QL extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BANK_QL() {

    super("BANK_QL", "BANK_QL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Fire Captain: Fire Department 2");
    setExtensions("bank", "pak");
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
      if (fm.readString(2).equals("QL")) {
        rating += 50;
      }
      else {
        return 0;
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

      File zipPath = new File(path.getAbsolutePath() + ".zip");

      FileManipulator fm = new FileManipulator(path, false);
      FileManipulator outfm = new FileManipulator(new File(path.getAbsolutePath() + ".zip"), true);

      TaskProgressManager.setMaximum((int) fm.getLength());

      while (fm.getOffset() < fm.getLength()) {
        // 2 - Header (QL)
        fm.readString(2);
        outfm.writeString("PK");

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        outfm.writeInt(entryType);

        if (entryType == 1311747) {
          // File Entry

          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          outfm.writeBytes(fm.readBytes(12));

          // 4 - Compressed File Size
          long length = fm.readInt();
          outfm.writeInt((int) length);

          // 4 - Decompressed File Size
          outfm.writeBytes(fm.readBytes(4));

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          outfm.writeInt(filenameLength);

          // X - Filename
          outfm.writeBytes(fm.readBytes(filenameLength));

          // X - File Data
          for (int i = 0; i < length; i++) {
            outfm.writeByte(fm.readByte());
          }

        }
        else if (entryType == 513) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          outfm.writeBytes(fm.readBytes(22));

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          outfm.writeInt(filenameLength);

          // 10 - null
          // 4 - File Offset (points to QL for this file in the directory)
          outfm.writeBytes(fm.readBytes(14));

          // X - Filename
          outfm.writeBytes(fm.readBytes(filenameLength));

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          outfm.writeBytes(fm.readBytes(16));
        }
        else {
          // bad header
          throw new WSPluginException("Bad Zip Header");
        }

        TaskProgressManager.setValue((int) fm.getOffset());

      }

      fm.close();
      outfm.close();

      WSPopup.showMessage("ZipConvertSuccess", false);

      Resource[] resources = new Resource[1];

      //path,id,name,offset,length,decompLength,exporter
      resources[0] = new Resource(zipPath, zipPath.getName(), 0, (int) zipPath.length());

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
