/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import java.util.Hashtable;
import org.itadaki.bzip2.BZIP_CRC32;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FS_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FS_3() {

    super("FS_3", "FS_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Fantastic 4", "Sopranos: Road to Respect");
    setExtensions("fs", "rs");
    setPlatforms("PC", "XBox", "PS2");

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
      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      fm.seek(arcSize - 4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset + 4);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength);
      fm.skip(filenameDirLength + 4);

      // 4 - Directory Length
      int numFiles = fm.readInt() / 16;
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(dirOffset + 8);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory

      Hashtable<Integer, String> names = new Hashtable<Integer, String>(numFiles);
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        BZIP_CRC32 crc = new BZIP_CRC32();
        byte[] filenameBytes = filename.getBytes();
        int filenameLength = filenameBytes.length;
        for (int b = 0; b < filenameLength; b++) {
          crc.updateCRC(filenameBytes[b]);
        }
        int crcResult = crc.getCRC();

        names.put(crcResult, filename);
        //System.out.println(filename + "\t" + crcResult);
      }

      fm.seek(dirOffset + 8 + filenameDirLength + 8);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Compressed Length (0 means the file is not compressed)
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        // 4 - File Offset [*2048]
        long offset = fm.readInt() * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Hash
        int filenameHash = fm.readInt();

        //String filename = names[i];
        String filename = names.get(filenameHash);
        if (filename == null) {
          filename = Resource.generateFilename(i);
        }

        if (compLength == 0) {
          // not compressed

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // compressed

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, compLength, length, exporter);
        }

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

    //if (headerInt1 == 26) {
    //  return "lvl";
    //}

    return null;
  }

}
