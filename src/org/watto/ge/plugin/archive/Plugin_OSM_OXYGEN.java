
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OSM_OXYGEN extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_OSM_OXYGEN() {

    super("OSM_OXYGEN", "OSM_OXYGEN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Oxygen Phone Manager");
    setExtensions("osm");
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
      if (fm.readString(24).equals("Oxygen Software SMS. v3.")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Actual SMS's
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Actual SMS's
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 24 - Header (Oxygen Software SMS. v3.)
      fm.skip(24);

      // 4 - Number Of Items In Archive
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Actual SMS's (ie some SMS's span over 1 message)
      // 4 - Number Of Actual SMS's (ie some SMS's span over 1 message)
      // 4 - null
      // 4 - Length of header data for each SMS (68)
      fm.skip(16);

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 1 - null
        // 4 - Offset to the null above
        // 4 - Message Number (incremental from 1)
        // 8 - Timestamp?
        // 3 - null
        // 4 - Unknown
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown
        // 12 - null
        // 4 - Unknown (76 for messages with multiple parts)
        // 4 - Number Of Message Parts (null for a single message)
        // 4 - Message Part Number (null for a single message, incremental from 1 for multiple parts)
        // 4 - SMS Station Phone Number Length?
        fm.skip(68);

        // 2 - Message Length
        long length = fm.readShort() * 2;
        FieldValidator.checkLength(length, arcSize);

        // 2 - Topic Length
        int topicLength = fm.readShort() * 2;
        FieldValidator.checkLength(topicLength, arcSize);

        // 2 - Original Sender Phone Number Length
        int phoneLength = fm.readShort();
        FieldValidator.checkLength(phoneLength, arcSize);

        // X - Original Sender Phone Number
        String phoneNumber = fm.readString(phoneLength);

        // X - Message (Unicode Text - length=messageLength*2)
        long offset = fm.getOffset();
        fm.skip(length);

        // X - Topic (Unicode Text - length=topicLength*2)
        String topic = new String(fm.readBytes(topicLength), "UTF-16LE");

        // 16 - SMS Station Phone Number (null Terminated)
        fm.skip(16);

        if (length > 0) {
          String filename = phoneNumber + " - " + topic + ".txt";
          //filename = FileManipulator.removeNonFilename(filename);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
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
