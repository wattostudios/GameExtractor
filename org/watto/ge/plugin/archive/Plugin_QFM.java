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
import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_QFM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_QFM() {

    super("QFM", "QFM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Football Manager 2006");
    setExtensions("qfm");
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

      // Unknown (258)
      if (fm.readShort() == 258) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("mfq.")) {
        rating += 50;
      }

      // Unknown (3)
      if (fm.readShort() == 3) {
        rating += 5;
      }

      // Unknown (256)
      if (fm.readShort() == 256) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 2 - Unknown (258)
      // 4 - Header (mfq.)
      // 2 - Unknown (3)
      // 2 - Unknown (256)
      fm.skip(10);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      String dirName = "";
      while (fm.getOffset() < arcSize - 8) { // -8 to just skip a little bit at the end, for a footer or anything
        // 4 - Unknown (123456789)
        fm.skip(4);

        // 1 - Entry Type
        int entryType = fm.readByte();

        if (entryType == 1) {
          // back a directory
          if (dirName.equals("")) {
            // do nothing - already at the root
          }
          else {
            int slashPos = dirName.lastIndexOf("\\", dirName.length() - 2);
            if (slashPos > 0) {
              dirName = dirName.substring(0, slashPos + 1);
            }
            else {
              dirName = "";
            }
          }
          continue;
        }

        // 512 - Filename (unicode text) (null terminated, filled with nulls)
        //System.out.println(fm.getOffset());
        String filename = fm.readNullUnicodeString(256);
        FieldValidator.checkFilename(filename);

        // 2 - null Filename Terminator
        fm.skip(2);

        if (entryType == 0) {
          // directory
          dirName = dirName + filename + "\\";
          continue;
        }
        else if (entryType == 2) {
          // file
          // 4 - File Extension Length [*2 for unicode]
          int extensionLength = fm.readInt();
          FieldValidator.checkFilenameLength(extensionLength);

          // 8 - File Extension (unicode text) (including ".")
          String extension = fm.readUnicodeString(extensionLength);

          // 2 - null File Extension Terminator
          fm.skip(2);

          filename = dirName + filename + extension;

          // 4 - Compressed File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - File Data
          long offset = fm.getOffset();
          if (fm.readString(1).equals("x")) {
            // compressed

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
          }
          else {
            // uncompressed

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          fm.skip(length - 1); // -1 because we read 1 byte for compression checking

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
        else {
          ErrorLogger.log("[QFM] Unknown entry type: " + entryType);
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
