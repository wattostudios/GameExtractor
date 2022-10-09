/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import java.util.Arrays;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WB() {

    super("WB", "WB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Die Hard Trilogy 2");
    setExtensions("wb"); // MUST BE LOWER CASE
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

      // 2 - Unknown (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 2 - Number of Names
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Unknown (1)
      fm.skip(2);

      // 2 - Number of Names
      short numNames = fm.readShort();

      String[] names = new String[numNames];
      int[] nameIDs = new int[numNames];
      int[] nameIDsSorted = new int[numNames];
      for (int i = 0; i < numNames; i++) {
        // 8 - Filename (null terminated, filled with nulls)
        String name = fm.readNullString(8);
        FieldValidator.checkFilename(name);
        names[i] = name;

        // 4 - First File ID with this name?
        int nameID = fm.readInt();
        nameIDs[i] = nameID;
        nameIDsSorted[i] = nameID;
      }

      Arrays.sort(nameIDsSorted);

      // 4 - Unknown (4)
      fm.skip(4);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number of 8-Byte Entries
      short num8Byte = fm.readShort();
      FieldValidator.checkNumFiles(num8Byte + 1); // +1 to allow for 0 entries

      // 4 - End-Directory Block Size (60)
      int dataOffset = fm.readInt() + 12 + numFiles * 72 + num8Byte * 8;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Archive Length (kinda)
      // 4 - null
      // 4 - Unknown (14)
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int currentNameIndex = 0;
      int currentNameID = nameIDsSorted[currentNameIndex];
      String currentName = "";
      for (int n = 0; n < numNames; n++) {
        if (nameIDs[n] == currentNameID) {
          currentName = names[n] + File.separatorChar;
          break;
        }
      }

      int audioCounter = 1;
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (-1)
        // 8 - null
        // 4 - Unknown (100)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Flags (16 = padding entry, not a real file)
        fm.skip(36);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + dataOffset;

        // 4 - File Length
        int length = fm.readInt();

        // 4 - Bitrate? (16)
        int bitrate = fm.readInt();

        // 4 - Frequency (22050/11025)
        int frequency = fm.readInt();

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Flags
        fm.skip(20);

        if (length == 0) {
          // padding entry, not a real file
          continue;
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        //String filename = currentName + "Audio" + audioCounter + ".wav";
        String filename = Resource.generateFilename(realNumFiles) + ".wav";

        //path,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(frequency, bitrate, 1);
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(i);

        audioCounter++;

        if (i >= currentNameID && i + 1 < numFiles && currentNameIndex + 1 < numNames) {
          // move to the next nameID
          currentNameIndex++;
          currentNameID = nameIDsSorted[currentNameIndex];
          for (int n = 0; n < numNames; n++) {
            if (nameIDs[n] == currentNameID) {
              currentName = names[n] + File.separatorChar;
              audioCounter = 1;
              break;
            }
          }
        }
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

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
