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
import org.watto.component.WSPluginException;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TXD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TXD() {

    super("TXD", "TXD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sonic Heroes");
    setExtensions("txd"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes("tex", "Texture Image");

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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long length = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Entry Type
        int entryType = fm.readInt();

        if (entryType == 1) {
          // Entry Holder

          // 4 - Entry Length
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown
          fm.skip(4);

          // X - Data
          if (length == 4) {
            fm.skip(4);
          }
        }
        else if (entryType == 2) {
          // Empty
        }
        else if (entryType == 3) {
          // Blank

          // 4 - null
          // 4 - Unknown
          fm.skip(8);
        }
        else if (entryType == 21) {
          // Image Descriptor

          // 4 - Image Data Length (including these fields and the entry type field)
          // 4 - Unknown
          fm.skip(8);
        }
        else if (entryType == 22) {
          // Archive Header

          // 4 - Archive Length [+12]
          // 4 - Unknown
          fm.skip(8);
        }
        else if (entryType == 8) {
          // Image Data

          long offset = fm.getOffset() - 4;

          // 4 - Unknown
          fm.skip(4);

          // 64 - Filename (null terminated)
          String filename = fm.readNullString(64) + ".tex";

          fm.seek(offset + length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
        else {
          throw new WSPluginException("Invalid entry type");
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
