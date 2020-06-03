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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_QuickBMSWrapper;
import org.watto.io.FileManipulator;
import org.watto.io.Hex;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
NOTE: THIS PLUGIN IS FOR TESTING ONLY - this isn't actually used. The correct plugin for running
QuickBMS scripts is ScriptArchivePlugin_QuickBMS
**********************************************************************************************
**/
public class Plugin_QuickBMSWrapper extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_QuickBMSWrapper() {

    super("QuickBMSWrapper", "Quick BMS Wrapper Plugin");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("QuickBMS");
    setExtensions("QuickBMS"); // MUST BE LOWER CASE
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

      // get the QuickBMS Executable
      String quickbmsPath = QuickBMSHelper.checkAndShowPopup();
      if (quickbmsPath == null) {
        return null;
      }

      // Build a script for doing the decompression
      String scriptPath = "C:\\_WATTOz\\____Development_Stuff\\_____quickbms\\weather_lord.bms";
      if (scriptPath == null) {
        // problem generating the script file
        return null;
      }

      ProcessBuilder pb = new ProcessBuilder(quickbmsPath, "-l", scriptPath, path.getAbsolutePath());

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_ReadingArchive"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      Process convertProcess = pb.start();
      //int returnCode = convertProcess.waitFor(); // wait for QuickBMS to finish
      int returnCode = 0;
      BufferedReader outputReader = new BufferedReader(new InputStreamReader(convertProcess.getInputStream()));

      ExporterPlugin exporter = new Exporter_QuickBMSWrapper(scriptPath);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];

      if (returnCode == 0) {
        String outputLine = outputReader.readLine();

        while (outputLine != null) {
          // the line is structured like this...
          //   offset     length     name
          // ... need to split it up and convert it into Resources
          String[] splitLine = outputLine.trim().split("\\s+", 3);
          if (splitLine != null && splitLine.length == 3) {
            String offsetHex = splitLine[0];
            int offset = IntConverter.convertBig(new Hex(offsetHex));

            int length = 0;
            try {
              length = Integer.parseInt(splitLine[1]);
            }
            catch (Throwable t) {
            }

            String filename = splitLine[2];

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
            realNumFiles++;
          }

          outputLine = outputReader.readLine();
        }

        // finished - return the resources
        TaskProgressManager.stopTask();

        resources = resizeResources(resources, realNumFiles);
        return resources;

      }

      // Stop the task
      TaskProgressManager.stopTask();

      return null;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
