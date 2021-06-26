
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZSS_Old;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_001_2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_001_2() {

    super("001_2", "001_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Trek: 25th Anniversary",
        "Star Trek: Judgement Rights");
    setExtensions("001");
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

      getDirectoryFile(fm.getFile(), "dir");
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();
      ExporterPlugin exporter = Exporter_LZSS_Old.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "dir");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      int numFiles = (int) (fm.getLength() / 14);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - Filename (null)
        String filename = fm.readNullString(8);
        FieldValidator.checkFilename(filename);

        // 3 - Filename Extension (null)
        String ext = fm.readNullString(3);
        //FieldValidator.checkFilename(ext);

        names[i] = filename + "." + ext;
        //System.out.println(names[i]);

        // 3 - File Offset
        byte byte1 = fm.readByte();
        byte byte2 = fm.readByte();
        byte byte3 = fm.readByte();

        byte[] offsetBytes = new byte[] { byte1, byte2, byte3, 0 };
        offsets[i] = IntConverter.convertLittle(offsetBytes);
        //offsets[i] = ByteConverter.unsign(fm.readByte())<<24 + ByteConverter.unsign(fm.readByte())<<16 + ByteConverter.unsign(fm.readByte())<<8;
        //System.out.println(offsets[i]);
        //FieldValidator.checkOffset(offsets[i],arcSize);
      }

      fm.close();

      fm = new FileManipulator(path, false);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 2 - Decompressed File Size
        int decompLength = fm.readShort();

        // 2 - File Size
        long length = fm.readShort();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();

        String filename = names[i];

        if (decompLength == length) {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

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
