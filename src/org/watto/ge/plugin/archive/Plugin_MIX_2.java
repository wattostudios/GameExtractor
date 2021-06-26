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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZWX;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MIX_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MIX_2() {

    super("MIX_2", "MIX_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Evolution GT",
        "SCAR Squadra Corse Alfa Romeo");
    setExtensions("mix");
    setPlatforms("PC");

    setCanScanForFileTypes(true);

    setTextPreviewExtensions("collision", "key", "script", "txt1"); // LOWER CASE

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

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      //ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("UNLZWX");
      ExporterPlugin exporter = Exporter_LZWX.getInstance();

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - null
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 20 - null
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Encrypted Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Encrypted Filename
        fm.skip(filenameLength);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 1 - null
        // 8 - Unknown
        fm.skip(9);

        String filename = Resource.generateFilename(i);

        if (decompLength != length) {
          // compressed
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // uncompressed

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
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

    if (headerInt1 == 827611220) {
      return "txt1";
    }
    else if (headerInt1 == 22) {
      return "partimages";
    }
    else if (headerInt1 == 706752303 || headerInt1 == 1633311292 || headerInt1 == 1128874331 || headerInt1 == 791621423 || headerInt1 == 2035511387 || headerInt1 == 2035576923) {
      return "script";
    }
    else if (headerInt1 == 1868786002 || headerInt1 == 1766993711) {
      return "key";
    }
    else if (headerInt1 == 1819042135) {
      return "collision";
    }
    else if (headerInt1 == 1701602643) {
      return "scene";
    }
    else if (headerInt1 == 1701008219) {
      return "scenedb";
    }
    else if (headerInt1 == 1869367131) {
      return "glowheader";
    }
    else if (headerInt1 == 1937064795) {
      return "customblend";
    }
    else if (headerInt1 == 1634027611) {
      return "header";
    }
    else if (headerInt1 == 1634882651) {
      return "trackmap";
    }
    else if (headerInt1 == 1987212611) {
      return "curvegraph";
    }
    else if (headerInt1 == 1347703356) {
      return "rtprofiler";
    }
    else if (headerInt1 == 1717978459) {
      return "effect";
    }
    else if (headerInt1 == 1162494543) {
      return "object";
    }
    else if (headerInt1 == 1685024347) {
      return "node";
    }
    else if (headerInt1 == 1767984475) {
      return "main";
    }
    else if (headerShort1 == 1025) {
      return "car";
    }
    else if (headerInt1 == 1598702657) {
      return "adj";
    }
    else if (headerInt1 == 71718210) {
      return "buf";
    }
    else if (headerInt1 == 1699940874) {
      return "pvs";
    }
    else if (headerInt1 == 1768843597) {
      return "texdef";
    }
    else if (headerInt1 == 1819047238) {
      return "fmtf";
    }
    else if (headerInt1 == 1381254477) {
      return "metrics";
    }

    return null;
  }

}
