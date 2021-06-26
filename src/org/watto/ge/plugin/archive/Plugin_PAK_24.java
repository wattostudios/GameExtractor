
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_24 extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_24() {

    super("PAK_24", "PAK_24");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("True Crime 2");
    setExtensions("pak");
    setPlatforms("PS2");

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

      // File Entry Length (48 for first entry)
      if (fm.readShort() == 48) {
        rating += 5;
      }

      // File Offsets
      if (fm.readInt() == IntConverter.changeFormat(fm.readInt())) {
        rating += 10;
      }

      // File Lengths
      if (fm.readInt() == IntConverter.changeFormat(fm.readInt())) {
        rating += 10;
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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      //long arcSize = (int) fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      readDirectory(path, fm, resources, "");

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName) throws Exception {
    long arcSize = fm.getLength();

    // skipping over the 2 opening directories
    fm.skip(10);
    int dirLength = fm.readInt() - 96;
    fm.skip(82);

    //System.out.println(fm.getOffset() + " - " + endOffset + " - " + dirLength);

    // Loop through directory
    int readLength = 0;
    while (readLength < dirLength) {
      //System.out.println(fm.getOffset());
      // 2 - File Entry Length (first 2 = 48)
      int fileEntryLength = fm.readShort();
      while (fileEntryLength == 0) {
        fileEntryLength = fm.readShort();
        readLength += 2;
      }
      readLength += fileEntryLength;
      //System.out.println(readLength + " vs " + dirLength);

      // 4 - File Offset [*2048] (folder = folder entries offset [*2048])
      long offset = fm.readInt();
      if (offset < 0) {
        offset = 4294967296L + offset;
      }
      offset *= 2048;
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Offset [*2048] (folder = folder entries offset [*2048]) (BIG ENDIAN)
      fm.skip(4);

      // 4 - File Length (first 2 entries = length of this current folder data)
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - File Length (first 2 entries = length of this current folder data) (BIG ENDIAN)
      // 4 - Unknown (null for folders)
      // 4 - Unknown (33554432 for folders)
      // 2 - null
      // 2 - Unknown (1)
      // 2 - Unknown (1) (BIG ENDIAN)
      fm.skip(18);

      // 1 - Filename Length
      int filenameLength = ByteConverter.unsign(fm.readByte());

      // X - Filename
      String filename = "";
      boolean dirEntry = false;
      if (filenameLength <= 2) {
        filename = fm.readString(filenameLength);
        dirEntry = true;
        // 1 - null
        if ((filenameLength + 1) % 2 == 1) {
          fm.skip(1);
        }
      }
      else {
        filename = fm.readString(filenameLength - 2);
        String checkType = fm.readString(2);

        if (checkType.equals(";1")) {
          // file
          if ((filenameLength + 1) % 2 == 1) {
            fm.skip(1);
          }
        }
        else {
          // folder
          filename += checkType;
          dirEntry = true;
          // 1 - null
          if ((filenameLength + 1) % 2 == 1) {
            fm.skip(1);
          }
        }

      }

      filename = dirName + filename;

      // 2 - null
      // 4 - Unknown (BIG ENDIAN)
      // 4 - Unknown (16728)
      // 4 - null
      fm.skip(14);

      //System.out.println(filename);

      if (!dirEntry) {
        // file
        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }
      else {
        // folder
        long currentPos = fm.getOffset();
        fm.seek(offset);
        //System.out.println("jumping to " + filename + " at " + offset);
        readDirectory(path, fm, resources, filename + "\\");
        fm.seek(currentPos);
      }

    }

    //System.out.println("returning to previous folder - " + dirName);

  }

}
