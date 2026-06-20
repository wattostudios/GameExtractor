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

import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FAC_FARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FAC_FARC() {

    super("FAC_FARC", "FAC_FARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sunday vs Magazine: Shūketsu! Chōjō Daikessen");
    setExtensions("fac"); // MUST BE LOWER CASE
    setPlatforms("PSP");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("at3", "AT3 Audio", FileType.TYPE_AUDIO),
        new FileType("fab", "FAB Archive", FileType.TYPE_ARCHIVE),
        new FileType("gim", "GIM Image", FileType.TYPE_IMAGE),
        new FileType("gimx", "GIM Image", FileType.TYPE_IMAGE));

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

      getDirectoryFile(fm.getFile(), "fah");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "fah");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (FARC)
      // 4 - Unknown (256)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<Long, String> hashMap = new HashMap<Long, String>(numFiles);
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "FAC_FARC" + File.separatorChar + "filenames.txt");
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }

          int separatorPos = name.indexOf(' ');
          if (separatorPos <= 0) {
            continue; // no separator between hash and filename
          }

          String hash = name.substring(0, separatorPos);
          name = name.substring(separatorPos + 1);

          // Convert the unsigned hash to a signed hash
          Long longHash = Long.parseUnsignedLong(hash);

          hashMap.put(longHash, name);
        }
        hashFM.close();
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Hash
        long hash = fm.readLong();

        // X - Filename (null)
        String filename = hashMap.get(hash);
        if (filename == null) {
          filename = Resource.generateFilename(i);
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

    String extension = resource.getExtension();
    if (extension != null && !extension.equals("")) {
      return extension;
    }

    if (headerInt1 == 1129464134) { // FARC
      return "fab";
    }
    else if (headerInt1 == 1263421507) { // CHNK
      return "chnk";
    }
    else if (headerInt1 == 1413768545) { // aeDT
      return "dat";
    }
    else if (headerInt1 == 1480938566) { // FTEX
      return "tex";
    }
    else if (headerInt1 == 1129464148) { // TARC
      return "tpk";
    }
    else if (headerInt1 == 1145589840) { // PPHD
      return "phd";
    }
    else if (headerInt1 == 1178947664) { // PPEF
      return "pef";
    }
    else if (headerInt1 == 1414090053) { // EMIT
      return "emit";
    }
    else if (headerInt1 == 1213285715) { // SEQH
      return "spk";
    }
    else if (headerInt1 == 1481592135) { // GMOX
      return "gmo";
    }

    else if (headerBytes[1] == 76 && headerBytes[2] == 117 && headerBytes[3] == 97 && headerBytes[4] == 81) { // luaQ
      return "lua";
    }

    else if (headerInt2 == 7564905) { // ins
      return "res";
    }

    else if (headerInt1 == 1970169183) { // _enum
      return "bin";
    }

    else if (headerInt1 == 1 && headerInt3 == 0) {
      return "gimx";
    }

    else if (headerInt1 == 0 && headerInt2 == 0 && headerInt3 == 0) {
      return "pbd";
    }

    else if (headerInt1 == -1920774525 || headerInt1 == -1971106173 || headerInt1 == -1987873661 || headerInt1 == -2021430653 || headerInt1 == -2138881661 || headerInt1 == 1250125443 || headerInt1 == 1635017060) {
      return "cpk";
    }

    return null;
  }

}
