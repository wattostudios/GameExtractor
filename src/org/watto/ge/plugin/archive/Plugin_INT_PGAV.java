/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
Ref: https://github.com/EdnessP/scripts/blob/main/jak-daxter/jak3-Xvagwad.py
**********************************************************************************************
**/
public class Plugin_INT_PGAV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_INT_PGAV() {

    super("INT_PGAV", "INT_PGAV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Jak 3");
    setExtensions("int", "com", "eng", "fre", "ger", "ita", "spa"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

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

      // Header
      if (fm.readString(4).equals("pGAV")) {
        rating += 50;
      }

      if (IntConverter.changeFormat(fm.readInt()) == 32) {
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
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      File dirFile = new File(path.getParent() + File.separatorChar + "VAGDIR.AYB");
      if (dirFile.exists() && dirFile.isFile()) {
        // OK
      }
      else {
        return null;
      }

      boolean intFile = false;
      if (path.getName().endsWith(".INT")) {
        intFile = true;
      }

      FileManipulator fm = new FileManipulator(dirFile, false);

      long arcSize = path.length();

      // 8 - Header (VGWADDIR)
      // 4 - Version (2)
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      char[] filenameChars = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-".toCharArray();
      int numFilenameChars = filenameChars.length;

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 8 - Entry
        long entry = fm.readLong();

        // Stereo = Entry >> 42 & 0x1
        int channels = 1;
        if (((entry >> 42) & 1) == 1) {
          channels = 2;
        }

        // IntEntry = Entry >> 43 & 0x1 (1=in the INT file, 0=in the language file like ENG, GER, etc.)
        boolean isInt = false;
        isInt = ((entry >> 43) & 1) == 1;

        if (isInt != intFile) {
          continue; // only show the files relevant for whether this is the INT file or one of the language-specific files
        }

        // Frequency = Entry >> 44 & 0xF
        int frequency = (int) ((entry >> 44) & 15);
        if (frequency == 2) {
          frequency = 16000;
        }
        else if (frequency == 3) {
          frequency = 24000;
        }
        else if (frequency == 4) {
          frequency = 32000;
        }
        else if (frequency == 6) {
          frequency = 48000;
        }
        else if (frequency == 8) {
          frequency = 44100;
        }
        else if (frequency == 12) {
          frequency = 22050;
        }
        else if (frequency == 15) {
          frequency = 36000;
        }
        else {
          frequency = 48000; // guess as default
        }

        // Offset = Entry >> 48 << 15
        int offset = (int) (((entry >> 48) & 0xFFFF) << 15);
        FieldValidator.checkOffset(offset, arcSize);

        long filenameVals = (entry & 0x3FFFFFFFFFFL);
        String filename = "";

        long tmpName = filenameVals & 0x1FFFFF;
        for (int idx = 0; idx < 8; idx++) {
          if (idx == 4) {
            tmpName = filenameVals >> 21;
          }

          int characterIndex = (int) (tmpName % numFilenameChars);
          filename = filenameChars[characterIndex] + filename; // reverse order

          tmpName = tmpName / numFilenameChars;
        }

        filename = filename.trim() + ".VAG";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        realNumFiles++;

        TaskProgressManager.setValue(i);
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
