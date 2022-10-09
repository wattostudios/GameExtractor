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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_KPKA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_KPKA() {

    super("PAK_KPKA", "PAK_KPKA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Capcom Arcade Stadium");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "TEX Image", FileType.TYPE_IMAGE));

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

      // Header
      if (fm.readString(4).equals("KPKA")) {
        rating += 50;
      }

      if (fm.readInt() == 4) {
        rating += 5;
      }

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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (KPKA)
      // 4 - Unknown (4)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Hash?
        fm.skip(8);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - Compressed File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        // 1 - Compression Flag (0=Uncompressed, 1=Compressed)
        int compressionFlag = fm.readByte();

        // 1 - Flags?
        // 6 - null
        // 8 - Hash?
        fm.skip(15);

        String filename = Resource.generateFilename(i);

        if (compressionFlag == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // deflate compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
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

    if (headerInt1 == 5919570) {
      return "rsz";
    }
    else if (headerInt1 == 5395285) {
      return "usr";
    }
    else if (headerInt1 == 5129043) {
      return "scn";
    }
    else if (headerInt1 == 4343376) {
      return "pfb";
    }
    else if (headerInt1 == 5784916) {
      return "tex";
    }
    else if (headerInt1 == 5460819) {
      return "sss";
    }
    else if (headerInt1 == 541476164) {
      return "def";
    }
    else if (headerInt1 == 5262668) {
      return "lmp";
    }
    else if (headerInt1 == 4605011) {
      return "sdf";
    }
    else if (headerInt1 == 4605005) {
      return "mdf";
    }
    else if (headerInt1 == 4476748) {
      return "lod";
    }
    else if (headerInt1 == 1920493157) {
      return "efxr";
    }
    else if (headerInt1 == 1751347827) {
      return "srch";
    }
    else if (headerInt1 == 1684238963) {
      return "srcd";
    }
    else if (headerInt1 == 1480938578) {
      return "rtex";
    }
    else if (headerInt1 == 1447904594) {
      return "remv";
    }
    else if (headerInt1 == 1431720750) {
      return "svu";
    }
    else if (headerInt1 == 1413699654) {
      return "fxct";
    }
    else if (headerInt1 == 1346980931) {
      return "clip";
    }
    else if (headerInt1 == 1330004550) {
      return "fbfo";
    }
    else if (headerInt1 == 1213416781) {
      return "mesh";
    }
    else if (headerInt1 == 1179992647) {
      return "gbuf";
    }
    else if (headerInt1 == 1178944579) {
      return "cdef";
    }
    else if (headerInt1 == 1162690893) {
      return "mame";
    }
    else if (headerInt1 == 1128353101) {
      return "mmac";
    }
    else if (headerInt1 == 1112690766) {
      return "nprb";
    }

    else if (headerInt2 == 1312900436) {
      return "tean";
    }
    else if (headerInt2 == 1380537671) {
      return "guir";
    }
    else if (headerInt2 == 1887007846) {
      return "ftyp";
    }
    else if (headerInt2 == 1414415945) {
      return "ifnt";
    }
    else if (headerInt2 == 1195787079) {
      return "gcfg";
    }
    else if (headerInt2 == 1414288198) {
      return "fslt";
    }
    else if (headerInt2 == 1196641607) {
      return "gmsg";
    }
    else if (headerInt2 == 1347240275) {
      return "samp";
    }

    return null;
  }

}
