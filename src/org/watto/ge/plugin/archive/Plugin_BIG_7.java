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
import java.util.HashMap;

import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TemporarySettings;
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_7 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_7() {

    super("BIG_7", "BIG_7");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Blood Omen: Legacy of Kain");
    setExtensions("big"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("tim", "TIM Image", FileType.TYPE_IMAGE),
        new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

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

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      fm.skip(8);

      if (FieldValidator.checkEquals(fm.readInt(), ((numFiles + 1) * 12) + 4)) {
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

      //ExporterPlugin exporter = Exporter_Custom_DEGOB.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<Integer, String> hashMap = new HashMap<Integer, String>(numFiles);
      try {
        File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "BIG_7" + File.separatorChar + "filenames.txt");
        if (hashFile.exists()) {
          int hashFileLength = (int) hashFile.length();

          char equalsChar = '=';

          FileManipulator hashFM = new FileManipulator(hashFile, false);
          while (hashFM.getOffset() < hashFileLength) {
            String name = hashFM.readLine();
            if (name.equals("")) {
              break; // EOF
            }

            int equalPos = name.indexOf(equalsChar);
            if (equalPos > 0) {
              String hash = name.substring(0, equalPos);
              name = name.substring(equalPos + 1);

              int hashInt = Integer.parseInt(hash);

              hashMap.put(hashInt, name);
            }

          }
          hashFM.close();
        }
      }
      catch (Throwable t) {
        ErrorLogger.log("[BIG_7] Problem reading filename hashes");
        ErrorLogger.log(t);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash
        int hashInt = fm.readInt();

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = hashMap.get(hashInt);
        if (filename == null) {
          //System.out.println(hashInt);
          filename = Resource.generateFilename(i);
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        /*
        String extension = FilenameSplitter.getExtension(filename).toLowerCase();
        if (extension.equals("ctm") || extension.equals("cmp")) {
          // compressed file
          resources[i].setExporter(exporter);
        }
        */

        TaskProgressManager.setValue(i);
      }

      fm.close();

      // Render the TIM images with the right colors
      TemporarySettings.set("SwapRedBlue", true);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt(numFiles);
      src.skip(4);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 4 + (12 * numFiles) + 12;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Hash
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        offset += length;
      }

      // 12 - null End of Directory Marker
      for (int p = 0; p < 12; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // 4 - Version? (100)
      fm.writeInt(100);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("vag")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "FFMPEG_Audio_WAV");
    }
    return null;
  }

}
