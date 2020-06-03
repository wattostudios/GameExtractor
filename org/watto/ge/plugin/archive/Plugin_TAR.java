
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TAR extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_TAR() {

    super("TAR", "Generic TAR Archive");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Generic TAR Archive",
        "Heroes Of The Pacific",
        "Atlantis");
    setExtensions("tar", "gpk");
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 100 - Filename
        String filename = fm.readNullString(100);

        if (filename.length() == 0) {
          fm.skip(412);
        }
        else {
          FieldValidator.checkFilename(filename);

          // 8 - Mode
          // 8 - UID
          // 8 - GID
          fm.skip(24);

          // 12 - File Length
          String lengthString = fm.readString(12).trim();
          long length = Integer.decode("0" + lengthString).intValue();
          FieldValidator.checkLength(length, arcSize);

          // 12 - Last Modification Time
          // 8 - Checksum
          // 1 - Link Flag
          // 100 - Linked Filename
          // 8 - Magic Number
          // 32 - uName
          // 32 - gName
          // 8 - Major Version Number
          // 8 - Minor Version Number
          // 167 - null Padding to make this file header have length 512
          fm.skip(376);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          // 0-511 - null Padding to a multiple of 512 bytes
          int padding = (int) (512 - (length % 512));
          if (padding < 512) {
            fm.skip(padding);
          }

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        int checksum = 32; // for some reason, always off by 32

        // 100 - Filename
        String name = resource.getName();
        fm.writeNullString(name, 100);

        for (int n = 0; n < name.length(); n++) {
          checksum += name.charAt(n);
        }

        // 8 - Mode
        // 8 - UID
        // 8 - GID
        fm.writeString("   777 ");
        fm.writeByte((byte) 0);
        fm.writeString("     0 ");
        fm.writeByte((byte) 0);
        fm.writeString("     0 ");
        fm.writeByte((byte) 0);

        checksum += 773; // (32 * 4) + (55 * 3) + (32 * 6 * 2) + (48 * 2)

        // 12 - File Length
        String length = Long.toOctalString(decompLength);
        if (length.length() > 11) {
          length = length.substring(0, 11);
        }
        while (length.length() < 11) {
          length = " " + length;
        }
        length += " ";

        fm.writeString(length);

        for (int n = 0; n < length.length(); n++) {
          checksum += length.charAt(n);
        }

        // 12 - Last Modification Time
        fm.writeString("00000000000 ");
        checksum += 560; // 48 * 11 + 32
        //fm.writeString("10603725260 ");
        //checksum += 592;

        // 8 - Checksum
        checksum += 272; // 32 * 7 + 48 (48 for the link flag below)
        String checkOctal = Integer.toOctalString(checksum);
        if (checkOctal.length() > 6) {
          checkOctal = checkOctal.substring(0, 6);
        }
        while (checkOctal.length() < 6) {
          checkOctal = " " + checkOctal;
        }
        checkOctal += " ";

        fm.writeString(checkOctal);
        fm.writeByte((byte) 0);

        // 1 - Link Flag
        fm.writeString("0");

        // 100 - Linked Filename
        // 8 - Magic Number
        // 32 - uName
        // 32 - gName
        // 8 - Major Version Number
        // 8 - Minor Version Number
        // 167 - null Padding to make this file header have length 512
        fm.writeBytes(new byte[355]);

        // X - File Data
        write(resource, fm);

        // 0-511 - null Padding to a multiple of 512 bytes
        int padding = (int) (512 - (decompLength % 512));
        if (padding < 512) {
          fm.writeBytes(new byte[padding]);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
