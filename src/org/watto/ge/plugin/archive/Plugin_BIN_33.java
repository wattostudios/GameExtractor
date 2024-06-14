/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_33 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_33() {

    super("BIN_33", "BIN_33");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Die Hard Trilogy 2");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      if (fm.readInt() == 2) {
        rating += 5;
      }

      if (fm.readInt() == -92286556) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      if (fm.readInt() == 44) {
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

      long length = path.length();

      FileManipulator fm;
      if (length < 2000000) {
        // load the whole file if it's smaller than 2MB (which they all are)
        // this is mainly to ensure we don't load full data for BIN files that are incorrectly matched to this plugin
        fm = new FileManipulator(path, false, (int) length);
      }
      else {
        fm = new FileManipulator(path, false);
      }

      long arcSize = fm.getLength();

      // 4 - Unknown (2)
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // basically just read through the full archive, find the entry headers, and use that to find the files

      // Loop through directory
      long offset = fm.getOffset();
      int realNumFiles = 0;
      while (offset < arcSize) {
        boolean found = false;

        // 4 - Entry Header (-92286556)
        if (fm.readByte() == -92) {
          if (fm.readByte() == -47) {
            if (fm.readByte() == 127) {
              if (fm.readByte() == -6) {
                // found a file header

                // 4 - Unknown (44)
                fm.skip(4);

                // 48 - Filename (null terminated, filled with nulls)
                String filename = fm.readNullString();
                if (filename.startsWith("K:\\")) {
                  filename = filename.substring(3);
                }

                resources[realNumFiles] = new Resource(path, filename, offset);
                realNumFiles++;

                offset = fm.getOffset();
                found = true;
              }
            }
          }
        }

        if (!found) {
          offset++;
        }

        fm.relativeSeek(offset);

        TaskProgressManager.setValue(offset);
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

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

      // Write Header Data

      // 4 - Unknown (2)
      fm.writeInt(2);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
