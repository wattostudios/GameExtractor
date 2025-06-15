/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_DLL;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_53 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_53() {

    super("PAK_53", "PAK_53");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("You Green Elephant");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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

      long arcSize = fm.getLength();

      // File Length
      if (FieldValidator.checkLength(fm.readInt() - 1, arcSize)) { // -1 will fail it if the length is 0
        rating += 5;
      }

      // File Length
      if (FieldValidator.checkLength(fm.readInt() - 1, arcSize)) { // -1 will fail it if the length is 0
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

      ExporterPlugin exporter = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      boolean[] needsExporter = new boolean[numFiles];
      while (fm.getOffset() < arcSize) {

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        if (decompLength == length) {
          // not compressed
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          needsExporter[realNumFiles] = false;
        }
        else {
          // JCALG compression
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          needsExporter[realNumFiles] = true;
        }
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      resources = resizeResources(resources, realNumFiles);

      /*
      // If QuickBMS is available, use it to perform the decompression
      // We have this at the end, so that if this isn't a compatible archive, it failed somewhere above *before* asking for QuickBMS
      if (QuickBMSHelper.checkAndShowPopup() != null) {
        exporter = new Exporter_QuickBMS_Decompression("JCALG");
      
        // now go through and set the QuickBMS exporter
        for (int i = 0; i < realNumFiles; i++) {
          if (needsExporter[i]) {
            resources[i].setExporter(exporter);
          }
        }
      }
      */
      exporter = new Exporter_QuickBMS_DLL("JCALG");

      // now go through and set the QuickBMS exporter
      for (int i = 0; i < realNumFiles; i++) {
        if (needsExporter[i]) {
          resources[i].setExporter(exporter);
        }
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
