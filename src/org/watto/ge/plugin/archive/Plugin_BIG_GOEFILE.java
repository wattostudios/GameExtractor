/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
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
public class Plugin_BIG_GOEFILE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_GOEFILE() {

    super("BIG_GOEFILE", "BIG_GOEFILE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Blood Omen 2: Legacy of Kain");
    setExtensions("big"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("texture", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("sample", "Sample Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(7).equals("goefile")) {
        rating += 50;
      }
      fm.skip(1);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header ("goefile" + null)
      // 4 - Archive Length
      // 4 - Unknown (1)
      fm.skip(16);

      // 8 - Directory Header ("symlist" + null)
      // 4 - Directory Length (including all these header fields)
      // 4 - null
      fm.skip(16);

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // X - Name
        // 1 - null Name Terminator
        String name = fm.readNullString();
        //FieldValidator.checkFilename(name);
        names[i] = name;
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      long currentOffset = fm.getOffset();
      if (currentOffset % 2 == 1) {
        currentOffset++;
      }
      fm.getBuffer().setBufferSize(64); // small quick reads
      fm.seek(1); // to force buffer re-load to the smaller size
      fm.seek(currentOffset);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long startOffset = fm.getOffset();
        //System.out.println(startOffset);

        // 8 - File Type (null terminated, filled with nulls)
        String fileType = fm.readNullString(8);

        // 4 - File Length (including all these header fields)
        int lengthIncludingHeaders = fm.readInt();
        FieldValidator.checkLength(lengthIncludingHeaders, arcSize);

        // 4 - null
        fm.skip(4);

        // 2 - Filename ID (0-based index into Names Directory)
        int filenameID = fm.readShort();
        FieldValidator.checkRange(filenameID, 0, numNames);

        // 2 - Language ID (0-based index into Names Directory)
        int langID = fm.readShort();
        FieldValidator.checkRange(langID, 0, numNames);

        // 8 - symlist ("symlist" + null)
        // 4 - Unknown (20/28)
        // 4 - null
        fm.skip(16);

        // 4 - Number of 2-byte Values (can be null)
        int num2Bytes = fm.readInt();
        if (num2Bytes % 2 == 1) {
          num2Bytes++;
        }
        FieldValidator.checkRange(num2Bytes, 0, 5000);//guess

        fm.skip(num2Bytes * 2);

        // 4 - File Length (File Data only)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        int lengthPadding = calculatePadding(length, 4);

        // 8/12 - Unknown
        // X - File Data
        long offset = startOffset + (lengthIncludingHeaders - (length + lengthPadding));
        FieldValidator.checkOffset(offset, arcSize);

        //String filename = Resource.generateFilename(realNumFiles) + "." + fileType;
        String filename = names[filenameID] + "." + names[langID] + "." + fileType;

        if (fileType.equals("sample")) {
          //path,name,offset,length,decompLength,exporter
          fm.seek(offset + 4);

          // 4 - Frequency
          int frequency = fm.readInt();

          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset + 20, length - 20);
          resource.setAudioProperties(frequency, (short) 16, (short) 1, true);
          resources[realNumFiles] = resource;
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }
        realNumFiles++;

        fm.seek(offset + length + lengthPadding);

        TaskProgressManager.setValue(offset);
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
