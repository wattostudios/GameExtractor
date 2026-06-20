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
import java.util.HashMap;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_47 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_47() {

    super("BIN_47", "BIN_47");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Need for Speed: Hot Pursuit 2");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("pck", "PCK Archive", FileType.TYPE_ARCHIVE));

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

      File dirFile = new File(fm.getFile().getParentFile().getAbsolutePath() + File.separatorChar + "ZDIR.BIN");
      if (dirFile.exists()) {
        rating += 50;
      }
      else {
        return 0;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles((int) dirFile.length() / 12)) {
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

      long arcSize = path.length();

      File sourcePath = new File(path.getParentFile().getAbsolutePath() + File.separatorChar + "ZDIR.BIN");
      if (!sourcePath.exists() || !sourcePath.isFile()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "BIN_47" + File.separatorChar + "filenames.txt");
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }

          // Calculate the hash
          name = name.toUpperCase();

          byte[] nameBytes = name.getBytes();
          int nameLength = nameBytes.length;

          int hash = 0xFFFFFFFF;
          for (int c = 0; c < nameLength; c++) {
            hash = 33 * hash + nameBytes[c];
          }

          hashMap.put(hash, name);
        }
        hashFM.close();
      }

      Resource[] resources = null;

      try {
        // first try Hot Pursuit 2

        int numFiles = (int) fm.getLength() / 12;
        FieldValidator.checkNumFiles(numFiles);

        resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 4 - Filename Hash
          int hash = fm.readInt();

          // 4 - File Offset [*2048]
          long offset = IntConverter.unsign(fm.readInt()) * 2048;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - Filename (null)
          String filename = hashMap.get(hash);
          if (filename == null) {
            filename = Resource.generateFilename(i);
          }

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);

        // try one of the newer games instead
        fm.relativeSeek(0);

        int numFiles = (int) fm.getLength() / 24;
        FieldValidator.checkNumFiles(numFiles);

        resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 4 - Filename Hash
          int hash = fm.readInt();

          // 4 - Archive ID
          fm.skip(4);

          // 4 - File Offset [*2048]
          long offset = IntConverter.unsign(fm.readInt()) * 2048;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Total Offset
          fm.skip(4);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Checksum
          fm.skip(4);

          // X - Filename (null)
          String filename = hashMap.get(hash);
          if (filename == null) {
            filename = Resource.generateFilename(i);
          }

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
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
