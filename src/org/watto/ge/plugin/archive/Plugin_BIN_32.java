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
import org.watto.ErrorLogger;
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
public class Plugin_BIN_32 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_32() {

    super("BIN_32", "BIN_32");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("LEGO Super Mario");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("Android", "iOS");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("bin_tex", "Texture Image", FileType.TYPE_IMAGE));

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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - File Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dataOffset) {

        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);

        // 4 - Number of Files in this Directory
        int numFilesInDir = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInDir);

        for (int i = 0; i < numFilesInDir; i++) {

          // 4 - Unknown (16)
          fm.skip(4);

          // 4 - File ID?
          int fileID = fm.readInt();

          // 4 - File Offset (relative to the start of the File Data)
          int offset = fm.readInt() + dataOffset;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length (can be null)
          int length = fm.readInt();
          FieldValidator.checkLength(length + 1, arcSize); // +1 to allow nulls

          // 2 - Entry Type (1=File, 2=FileWithNullLength)
          short entryType = fm.readShort();

          // 2 - File Type ID?
          short fileType = fm.readShort();
          String extension = "." + fileType;

          if (fileType == 1 || fileType == 3) {
            extension = "." + fileType + ".wav";
          }
          else if (fileType == 4) {
            extension = "." + fileType + ".bin_tex";
          }

          if (entryType == 1) {
            String filename = Resource.generateFilename(fileID) + extension;

            if (fileType == 1) {
              // RAW Audio 8bit 8000 stereo unsigned

              //path,name,offset,length,decompLength,exporter
              Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
              resource.setAudioProperties(8000, (short) 8, (short) 2);
              resources[realNumFiles] = resource;
            }
            else if (fileType == 3) {
              // RAW Audio 8bit 8000 mono unsigned

              //path,name,offset,length,decompLength,exporter
              Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
              resource.setAudioProperties(8000, (short) 8, (short) 1);
              resources[realNumFiles] = resource;
            }
            else {
              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
            }
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
          else if (entryType == 2) {
            // ignore blanks
          }
          else {
            ErrorLogger.log("[BIN_32] Unrecognized entry type: " + entryType);
          }
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
