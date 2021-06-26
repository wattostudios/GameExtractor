
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_WAD_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_WAD_2() {

    super("WAD_WAD_2", "WAD_WAD_2");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Eragon");
    setExtensions("wad");
    setPlatforms("PC");

    setFileTypes("16", "Text Table",
        "128", "Audio",
        "4096", "Image");

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

      // 4 - Header
      if (fm.readString(4).equals("wad" + (byte) 0)) {
        rating += 50;
      }

      // 4 - Platform Header ("pc" + 2 nulls)
      if (fm.readString(4).equals("pc" + (byte) 0 + "" + (byte) 0)) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(12);

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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("wad" + null)
      // 4 - Platform Header ("pc" + 2 nulls)
      // 4 - Version (1)
      // 4 - Archive Size
      // 4 - Archive Size
      // 8 - null
      // 4 - Number Of Files in the ID Directory
      fm.skip(32);

      // 4 - Number Of Files in the Details Directory
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - ID Directory Offset (44)
      fm.skip(4);

      // 4 - Details Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Type ID? (16=Text Tables, 4096=Images, 128=Audio?)
        int fileType = fm.readInt();

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        int lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offsetPointerLocation = fm.getOffset();
        int offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = Resource.generateFilename(i) + "." + fileType;

        //path,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

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
