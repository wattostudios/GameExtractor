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

import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.Hex;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_78 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_78() {

    super("DAT_78", "DAT_78");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Legacy of Kain: Defiance");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("vrm", "VRM Image Archive", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      if (fm.getFile().getName().toLowerCase().equals("bigfile.dat")) {
        rating += 25;
      }
      else {
        rating -= 25;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      if (fm.readShort() == 257) { // bytes (1,1)
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

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      fm.skip(2);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      String[] names = null;

      HashMap<String, String> hashMap = new HashMap<String, String>(numFiles);
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "DAT_78" + File.separatorChar + "legacyofkain_defiance.txt");
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

            hashMap.put(hash, name);
          }

        }
        hashFM.close();

        names = new String[numFiles];
        setCanScanForFileTypes(false);
      }

      // HASH DIRECTORY
      if (names == null) {
        fm.skip(numFiles * 4);
      }
      else {
        for (int i = 0; i < numFiles; i++) {
          // 4 - Hash
          Hex hash = fm.readHex(4);
          names[i] = hashMap.get(hash.toString());
        }
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);
        if (names != null) {
          filename = names[i];
          if (filename == null) {
            filename = Resource.generateFilename(i);
          }
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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

    if (headerInt1 == 1380013857) {
      return "war";
    }
    else if (headerInt1 == 1632849222) {
      return "fms";
    }
    else if (headerInt1 == 1632849478) {
      return "fns";
    }
    else if (headerInt1 == 22050 || headerInt1 == 44100 || (headerInt2 == -1 && ((headerInt1 > 21950 && headerInt1 < 22300) || (headerInt1 > 44000 && headerInt1 < 44200)))) {
      resource.setOffset(resource.getOffset() + 2048);
      resource.setLength(resource.getLength() - 2048);
      return "rawaudio";
    }
    else if (headerInt1 == 561016425) {
      return "inp";
    }
    else if (headerInt1 == 561214797) {
      return "mus";
    }
    else if (headerInt1 == 846358881) {
      return "air";
    }

    return null;
  }

}
