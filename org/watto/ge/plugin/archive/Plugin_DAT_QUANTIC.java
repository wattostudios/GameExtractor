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

import java.io.File;
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
public class Plugin_DAT_QUANTIC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_QUANTIC() {

    super("DAT_QUANTIC", "DAT_QUANTIC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Fahrenheit");
    setExtensions("dat", "d01", "d02", "d03", "d04");
    setPlatforms("PS2", "PC");

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

      getDirectoryFile(fm.getFile(), "idm");
      rating += 25;

      // Header
      String header = fm.readString(8);
      if (header.equals("QUANTICD") && fm.readString(12).equals("REAMTABINDEX")) { // IDM file
        rating += 50;
      }
      else if (header.equals("COM_CONT")) { // DAT
        rating += 50;
      }
      else if (header.equals("PARTITIO")) { // D01/3/4 FILE
        rating += 50;
      }
      else if (header.equals("DBRAW___")) { // D02 FILE
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

      // RESETTING THE GLOBAL VARIABLES

      // Get the Index file
      File sourcePath = getDirectoryFile(path, "idm");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // Work out how many data files there are
      int numDatFiles = 0;
      File[] datFiles = new File[10]; // max of 10 files
      long[] datFileLengths = new long[10];

      File mainDatFile = getDirectoryFile(path, "dat");
      if (!mainDatFile.exists()) {
        return null;
      }

      datFiles[0] = mainDatFile;
      datFileLengths[0] = mainDatFile.length();
      numDatFiles++;

      for (int i = 1; i < 10; i++) {
        try {
          File datFile = getDirectoryFile(path, "d0" + i);
          if (datFile.exists()) {
            datFiles[i] = datFile;
            datFileLengths[i] = datFile.length();
            numDatFiles++;
          }
          else {
            break; // no more files
          }
        }
        catch (Throwable t) {
          // just need to catch the Exception from getDirectoryFile() when it doesn't exist
        }
      }

      // 20 - Header (QUANTICDREAMTABIDMEM)
      // 4 - Unknown (85)
      fm.skip(24);

      // 4 - Number of Header Blocks (7)
      int numHeaderBlocks = fm.readInt();
      FieldValidator.checkRange(numHeaderBlocks, 0, 20);

      // 4 - Unknown (2094)
      // 4 - Unknown (1)
      // 4 - Unknown (92)

      // for each header block
      //   4 - Block ID
      //   4 - Block Value 1
      //   4 - Block Value 2

      // 4 - null
      fm.skip(16 + numHeaderBlocks * 12);

      int numFiles = (int) ((fm.getLength() - fm.getOffset() - 20) / 16);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID?
        fm.skip(4);

        // 4 - File Offset
        long offset = IntConverter.unsign(fm.readInt());

        // 4 - File Length
        long length = fm.readInt();

        // 1 - Data File ID (0=*.dat, 1=*.d01, 2=*.d02, etc.)
        int datFileID = fm.readByte();
        FieldValidator.checkRange(datFileID, 0, numDatFiles - 1);

        // 3 - Unknown
        fm.skip(3);

        File datFile = datFiles[datFileID];
        long arcSize = datFileLengths[datFileID];

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(datFile, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

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

    if (headerInt1 == 1095909956) {
      return "dbraw";
    }
    else if (headerInt1 == 1096040772) {
      return "databank";
    }
    else if (headerInt1 == 1312899652) {
      return "dbankidx";
    }
    else if (headerInt1 == 1414676816) {
      return "partition";
    }
    else if (headerInt1 == 1598902083) {
      return "com_cont";
    }

    return null;
  }

}
