
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKG_CRSR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG_CRSR() {

    super("PKG_CRSR", "PKG_CRSR");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Galleon: Island Of Mystery");
    setExtensions("pkg");
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("CRSR")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // first file offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      long arcSize = (int) fm.getLength();

      // 4 - Header (CRSR)
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Archive Size [+12]
      // 4 - null
      fm.skip(16);

      // 4 - First File Offset [+4]
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 2 - Directory Name Length (including null)
      short dirNameLength = (short) (fm.readShort() - 1);
      FieldValidator.checkFilenameLength(dirNameLength);

      // X - Directory Name
      String dirName = fm.readString(dirNameLength) + "\\";

      // 1 - null Filename Terminator
      fm.skip(1);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      String prevFilename = "";
      String ext = "";
      int realNumFiles = 0;
      boolean endOfDirectory = false;
      while (!endOfDirectory) {
        // 1 - Reuse Filename Length
        int reuseLength = ByteConverter.unsign(fm.readByte());

        if (reuseLength == 255 || fm.getOffset() >= firstFileOffset) {
          endOfDirectory = true;
        }
        else {
          // X - Filename Part
          // 1 - Filename Part Terminator (byte 129)
          String filenamePart = "";
          int filenameByte = ByteConverter.unsign(fm.readByte());
          while (filenameByte != 129) {
            filenamePart += (char) filenameByte;
            filenameByte = ByteConverter.unsign(fm.readByte());
          }

          if (realNumFiles == 0 && reuseLength == 0) {
            // X - File Extension
            // 1 - File Extension Terminator (byte 243)
            ext = "";
            int extByte = ByteConverter.unsign(fm.readByte());
            while (extByte != 243) {
              ext += (char) extByte;
              extByte = ByteConverter.unsign(fm.readByte());
            }

          }

          // 4 - File Offset
          long offsetPointerLocation = fm.getOffset();
          long offsetPointerLength = 4;

          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long lengthPointerLocation = fm.getOffset();
          long lengthPointerLength = 4;

          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          String filename = prevFilename.substring(0, prevFilename.length() - reuseLength) + filenamePart;
          prevFilename = filename;

          filename = dirName + filename + "." + ext; // do this here after saving the prevFilename

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

          TaskProgressManager.setValue(offset);
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
