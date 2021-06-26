/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAD_12;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_12 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_12() {

    super("WAD_12", "WAD_12");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Croc: Legend of the Gobbos");
    setExtensions("wad"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      getDirectoryFile(fm.getFile(), "idx");
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

      Exporter_Custom_WAD_12 exporterByte = new Exporter_Custom_WAD_12();
      exporterByte.setByteLength(1);
      Exporter_Custom_WAD_12 exporterWord = new Exporter_Custom_WAD_12();
      exporterWord.setByteLength(2);

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "idx");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      String comma = ",";
      int realNumFiles = 0;
      while (fm.getOffset() < dirSize) {
        if (fm.getOffset() + 6 >= dirSize) {
          break; // end of index file
        }

        // X - Filename
        // 1 - Comma Separator (,)
        String filename = "";
        String letter = fm.readString(1);
        while (!letter.equals(comma)) {
          filename += letter;
          letter = fm.readString(1);
        }

        // X - File Offset
        // 1 - Comma Separator (,)
        String offsetString = "";
        letter = fm.readString(1);
        while (!letter.equals(comma)) {
          offsetString += letter;
          letter = fm.readString(1);
        }
        long offset = Long.parseLong(offsetString);
        FieldValidator.checkOffset(offset, arcSize);

        // X - Compressed File Length
        // 1 - Comma Separator (,)
        String compLengthString = "";
        letter = fm.readString(1);
        while (!letter.equals(comma)) {
          compLengthString += letter;
          letter = fm.readString(1);
        }
        long length = Long.parseLong(compLengthString);
        FieldValidator.checkLength(length, arcSize);

        // X - Decompressed File Length
        // 1 - Comma Separator (,)
        String decompLengthString = "";
        letter = fm.readString(1);
        while (!letter.equals(comma)) {
          decompLengthString += letter;
          letter = fm.readString(1);
        }
        long decompLength = Long.parseLong(decompLengthString);
        FieldValidator.checkLength(decompLength);

        // 1 - Compression Flag ("w" or "b" = compressed, "u" = uncompressed)
        String compressionType = fm.readString(1);

        // 2 - End of Entry Indicator ((bytes)13,10)
        fm.skip(2);

        //path,name,offset,length,decompLength,exporter
        if (compressionType.equals("u")) {
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }
        else if (compressionType.equals("w")) {
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporterWord);
        }
        else if (compressionType.equals("b")) {
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporterByte);
        }
        else {
          System.out.println("[WAD_12] Unknown Compression Type: " + compressionType);
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
