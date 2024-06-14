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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DFU extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DFU() {

    super("DFU", "DFU");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Enter The Matrix");
    setExtensions("dfu"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(16);

      if (fm.readInt() == 32) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // load the second archive details as well
      String secondFilePath = path.getAbsolutePath();
      int dotPos = secondFilePath.lastIndexOf('.');
      secondFilePath = secondFilePath.substring(0, dotPos) + "_B" + secondFilePath.substring(dotPos);
      File secondFile = new File(secondFilePath);

      if (!secondFile.exists()) {
        return null;
      }

      long secondFileLength = secondFile.length();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (32)
      fm.skip(20);

      int dataOffset = 24 + (numFiles * 16);
      dataOffset += calculatePadding(dataOffset, 2048);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash
        fm.skip(4);

        // 4 - Compressed File Length (not including null padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the File Data)
        long offset = IntConverter.unsign(fm.readInt()) + dataOffset;

        File arcPath = path;
        if (offset >= arcSize) {
          offset -= arcSize;
          arcPath = secondFile;

          FieldValidator.checkOffset(offset, secondFileLength);
        }
        else {
          FieldValidator.checkOffset(offset, arcSize);
        }

        // 4 - Decompressed File Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(arcPath, filename, offset, length, decompLength);
        if (length != decompLength) {
          resource.setExporter(exporter);
        }
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

    if (headerInt1 == 1144148816) {
      return "ps2d";
    }
    else if (headerInt1 == 1179403647) {
      return "elf";
    }
    else if (headerInt1 == 0 && headerInt2 == 0 && headerInt3 == 0) {
      resource.setExporter(Exporter_Custom_VAG_Audio.getInstance());
      /*
      if (resource.getDecompressedLength() >= 192640) {
        resource.addProperty("Frequency", 44100);
        resource.addProperty("Channels", 2);
      }
      */
      return "vag";
    }
    else if (headerInt1 == 0 && (headerInt2 != 0 && (headerInt3 == headerInt2 * 4))) {
      return "lang";
    }
    else if (headerInt1 == 1193300813 || headerInt1 == 542262095 || headerInt1 == 544695630 || headerInt1 == 1920229709 || headerInt1 == 1920298819 || headerInt1 == 1768125772 || headerInt1 == 1869508429 || headerInt1 == 1886547789 || headerInt1 == 1634300481 || headerInt1 == 1634300737 || headerInt1 == 1651341651 || headerInt1 == 1684107842 || headerInt1 == 1699240264) {
      return "font";
    }
    else if (headerInt1 == 1414483778) {
      return "boot";
    }
    else if (headerInt1 == 1024) {
      return "img_arc";
    }
    else if (headerInt2 == 32 && (headerInt3 == 3038883 || headerInt3 == 4056264)) {
      return "mesh";
    }
    else if (headerInt1 == 9) {
      return "mesh";
    }
    else if (headerInt1 == 2003 || headerInt1 == 2004 || headerInt1 == 1944) {
      return "tls";
    }
    else if (headerInt1 == 100 || headerInt1 == 108 || headerInt1 == 124 || headerInt1 == 88 || headerInt1 == 72) {
      return "mission";
    }
    else if (headerInt1 == 39 || headerInt1 == 40 || headerInt1 == 41 || headerInt1 == 42) {
      return "level";
    }
    else if (headerInt1 == 3038883 || headerInt1 == 22875 || headerInt1 == 4028509 || headerInt1 == 4031811 || headerInt1 == 4056264 || headerInt1 == 5021374) {
      return "dcn";
    }
    else if (headerInt1 == 1918986339 || headerInt1 == 1918988403) {
      return "txt";
    }
    else if (headerInt1 == 808464432 || headerInt1 == 808464433) {
      return "car";
    }

    return null;
  }

}
