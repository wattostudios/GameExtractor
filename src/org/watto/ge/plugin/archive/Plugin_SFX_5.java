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
import java.util.Arrays;
import java.util.HashMap;

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
public class Plugin_SFX_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SFX_5() {

    super("SFX_5", "SFX_5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("SEGA Rally Revo");
    setExtensions("sfx"); // MUST BE LOWER CASE
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

      fm.skip(188);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readInt() == 1) {
        rating += 5;
      }

      if (fm.readString(2).equals("PC")) {
        rating += 50;
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

      // 192 - Archive Description (null terminated, filled with nulls)
      // 4 - Unknown (1)
      // 20 - Platform Name (PC) (null terminated, filled with nulls)
      // 20 - Archive Creation Date (null terminated, filled with nulls)
      // 20 - Content Type (SoundBank File) (null terminated, filled with nulls)
      fm.seek(256);

      int numFiles = 0;

      // for each directory (4)
      // 4 - Directory Offset
      // 4 - Number of Files in this Directory
      int earliestOffset = (int) arcSize;
      int numDirs = 0;
      int[] dirOffsets = new int[100]; // guess max
      int[] dirNumFiles = new int[100]; // guess max
      while (fm.getOffset() < earliestOffset) {
        // 4 - Directory Offset
        int offset = fm.readInt();

        if (offset == 0) {
          // end of directory
          break;
        }

        FieldValidator.checkOffset(offset, arcSize);
        dirOffsets[numDirs] = offset;

        if (offset < earliestOffset) {
          earliestOffset = offset;
        }

        // 4 - Number of Files in this Directory
        int numFilesInDir = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInDir);
        dirNumFiles[numDirs] = numFilesInDir;

        numFiles += numFilesInDir;

        numDirs++;
      }

      FieldValidator.checkNumFiles(numFiles);

      if (numDirs == 4) {
        // we want directory 3, which contains the file details, and directory 4 which contains the file data

        numFiles = dirNumFiles[2];

        Resource[] resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // First read the file offsets from dir 4
        fm.seek(dirOffsets[3]);

        int[] offsets = new int[numFiles];

        for (int i = 0; i < numFiles; i++) {
          // 192 - Filename (null terminated, filled with nulls)
          // 4 - File ID?
          fm.skip(196);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        // now read the file properties from dir 3
        fm.seek(dirOffsets[2]);

        int[] propertyOffsets = new int[numFiles];
        for (int i = 0; i < numFiles; i++) {
          // 192 - Filename (null terminated, filled with nulls)
          // 4 - File ID?
          fm.skip(196);

          // 4 - File Offset
          int propertyOffset = fm.readInt();
          FieldValidator.checkOffset(propertyOffset, arcSize);
          propertyOffsets[i] = propertyOffset;
        }

        // now go and read the individual file properties
        for (int i = 0; i < numFiles; i++) {
          fm.relativeSeek(propertyOffsets[i]);

          long offset = offsets[i] + 192; // skip the 192-byte filename on the file

          // 192 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(192);
          FieldValidator.checkFilename(filename);

          if (filename.startsWith("M:\\")) {
            filename = filename.substring(3);
          }

          // 4 - Unknown (1)
          fm.skip(4);

          // 4 - Audio Frequency (eg 16000)
          int frequency = fm.readInt();
          FieldValidator.checkRange(frequency, 0, 100000);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - Other Data
          //path,name,offset,length,decompLength,exporter
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
          resource.setAudioProperties(frequency, 16, 1); // 16-bit mono
          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }

        fm.close();

        return resources;

      }
      else {
        // anything else, just process normally

        Resource[] resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // capture all the offsets so that we can work out the file sizes
        int[] offsets = new int[numFiles + numDirs + 1];
        int numOffsets = 0;
        for (int i = 0; i < numDirs; i++) {
          offsets[numOffsets] = dirOffsets[i];
          numOffsets++;
        }
        offsets[numOffsets] = (int) arcSize;
        numOffsets++;

        // Loop through directory
        int realNumFiles = 0;
        for (int d = 0; d < numDirs; d++) {
          int numFilesInDir = dirNumFiles[d];

          fm.seek(dirOffsets[d]);

          for (int i = 0; i < numFilesInDir; i++) {

            // 192 - Filename (null terminated, filled with nulls)
            String filename = fm.readNullString(192);
            FieldValidator.checkFilename(filename);

            // 4 - File ID?
            fm.skip(4);

            // 4 - File Offset
            int offset = fm.readInt();
            FieldValidator.checkOffset(offset, arcSize);

            offsets[numOffsets] = offset;
            numOffsets++;

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset);

            TaskProgressManager.setValue(realNumFiles);
            realNumFiles++;
          }
        }

        // work out the file sizes
        Arrays.sort(offsets);

        HashMap<Integer, Integer> sizes = new HashMap<Integer, Integer>(numOffsets);
        for (int i = 0; i < numOffsets - 1; i++) {
          sizes.put(offsets[i], offsets[i + 1] - offsets[i]);
        }

        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];
          int length = sizes.getOrDefault((int) resource.getOffset(), 0);
          resource.setLength(length);
          resource.setDecompressedLength(length);
        }

        fm.close();

        return resources;
      }

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
