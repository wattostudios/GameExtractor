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
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MIX_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MIX_3() {

    super("MIX_3", "MIX_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ducati: 90th Anniversary",
        "MotoGP 15",
        "MXGP");
    setExtensions("mix"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("efx", "script"); // LOWER CASE

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

      // 4 - Unknown (1)
      if (fm.readInt() == 1) {
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

      // 4 - Unknown (1)
      if (fm.readInt() == 1) {
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

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (1)
      // 16 - null
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (encrypted)
        fm.skip(filenameLength);
        String filename = Resource.generateFilename(i);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 2 - Compression Flag? (1)
        short compressionFlag = fm.readShort();

        // 8 - Unknown
        fm.skip(8);

        if (compressionFlag == 1) {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          //path,name,offset,length,decompLength,exporter
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
      return "txt";
    }
    else if (headerInt1 == 1262634067) {
      return "sdbk";
    }
    else if (headerInt1 == 1145979479) {
      return "xwb";
    }
    else if (headerInt1 == 1129464135) {
      return "garchive";
    }
    else if (headerInt1 == 1179861592) {
      return "xbsf";
    }
    else if (headerInt1 == 1464685400) {
      return "xsmw";
    }
    else if (headerInt1 == 1480999228) {
      return "afx";
    }
    else if (headerInt1 == 1701008219) {
      return "scenedb";
    }
    else if (headerInt1 == 1852131163 || headerInt1 == 1415801172 || headerInt1 == 1633906540 || headerInt1 == 1668183398 || headerInt1 == 1852143173 || headerInt1 == 1869833554 || headerInt1 == 1885434439 || headerInt1 == 1948265773 || headerInt1 == 1970365810 || headerShort1 == 11565 || headerShort1 == 2573) {
      return "script";
    }
    else if (headerInt1 == 1852133948) {
      return "xml";
    }
    else if (headerInt1 == 801094639) {
      return "efx";
    }
    else if (headerInt1 == 541278552) {
      return "xac";
    }
    else if (headerInt1 == 541938520) {
      return "xsm";
    }
    else if (headerInt1 == 542330701) {
      return "mos";
    }
    else if (headerInt1 == 1179862872) {
      return "xgsf";
    }
    else if (headerInt1 == 1986351676) {
      return "xml";
    }
    else if (headerInt1 == 154202) {
      return "zz";
    }
    else if (headerInt1 == 1019198447 || headerInt1 == 1836405308 || headerInt1 == 1851870268 || headerInt1 == 1920226108) {
      return "xml";
    }
    else if (headerInt1 == 1413693756) {
      return "actionmap";
    }
    else if (headerInt1 == 1634882651) {
      return "trackmap";
    }
    else if (headerInt1 == 1918980156) {
      return "particles";
    }
    else if (headerInt1 == 1598702657) {
      return "adj";
    }
    else if (headerInt1 == 155604290) {
      return "buf";
    }

    return null;
  }

}
