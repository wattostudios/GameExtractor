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
public class Plugin_DAT_XFLE3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_XFLE3() {

    super("DAT_XFLE3", "DAT_XFLE3");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Harvester");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("XFLE")) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  String filenamePrefix = null;

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
      filenamePrefix = null;

      FileManipulator fm = new FileManipulator(path, false, 148); // short quick reads

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        boolean noFilename = false;

        // 4 - File Header (XFLE)
        fm.skip(4);

        // 128 - Filename (null terminated, filled with junk) (starts with "1:\" or "3:\" etc)
        String filename = fm.readNullString(128);
        //FieldValidator.checkFilename(filename);
        int slashPos = filename.indexOf(":\\");
        if (slashPos > 0) {
          if (filenamePrefix == null) {
            filenamePrefix = filename.substring(0, slashPos + 2);
          }
          filename = filename.substring(slashPos + 2);
        }
        if (filename.length() == 0) {
          noFilename = true;
          filename = Resource.generateFilename(realNumFiles);
        }

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        fm.skip(8);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        if (noFilename) {
          resources[realNumFiles].addProperty("HasNoFilename", true);
        }

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
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

      if (filenamePrefix == null) {
        filenamePrefix = "1:\\";
      }

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Header (XFLE)
        fm.writeString("XFLE");

        // 128 - Filename (null terminated, filled with junk) (starts with "3:\")
        String noFilename = resource.getProperty("HasNoFilename");
        if (noFilename != null && !noFilename.equals("")) {
          for (int p = 0; p < 128; p++) {
            fm.writeByte(0);
          }
        }
        else {
          String filename = filenamePrefix + resource.getName();
          if (filename.length() > 128) {
            filename = filename.substring(0, 128);
          }
          fm.writeString(filename);

          int paddingSize = calculatePadding(filename.length(), 128);

          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }

        // 4 - File Offset
        fm.writeInt(fm.getOffset() + 16);

        // 8 - File Length
        fm.writeLong(decompLength);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // X - File Data
        write(resource, fm);
        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
