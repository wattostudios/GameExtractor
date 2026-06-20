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

import org.watto.Language;
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
public class Plugin_A extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_A() {

    super("A", "A");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Fist of the North Star: Ken's Rage");
    setExtensions("a", "b", "c"); // MUST BE LOWER CASE
    setPlatforms("XBox 360");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("g1t", "G1T Texture Archive", FileType.TYPE_ARCHIVE),
        new FileType("kshl", "KSHL Shader", FileType.TYPE_OTHER));

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

      // 4 - Header (0,7,125,249)
      if (fm.readInt() == -109246720) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      if (IntConverter.changeFormat(fm.readInt()) == 2048) {
        rating += 5;
      }

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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (0,7,125,249)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding Multiple (2048)
      int paddingMultple = IntConverter.changeFormat(fm.readInt());

      // 8 - null
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset [*2048]
        int offset = IntConverter.changeFormat(fm.readInt()) * paddingMultple;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 8 - null
        fm.skip(8);

        String filename = Resource.generateFilename(i);

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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      int offset = 20 + (numFiles * 16);

      int dirPadding = calculatePadding(offset, 2048);
      offset += dirPadding;

      // Write Header Data

      // 4 - Header (0,7,125,249)
      fm.writeInt(-109246720);

      // 4 - Number of Files
      fm.writeInt(IntConverter.changeFormat(numFiles));

      // 4 - Padding Multiple (2048)
      fm.writeInt(IntConverter.changeFormat(2048));

      // 8 - null
      fm.writeLong(0);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset [*2048]
        fm.writeInt(IntConverter.changeFormat(offset / 2048));

        // 4 - File Length
        fm.writeInt(IntConverter.changeFormat((int) decompLength));

        // 8 - null
        fm.writeLong(0);

        offset += decompLength;
        offset += calculatePadding(offset, 2048);
      }

      // X - null Padding to a multiple of 2048 bytes
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files (with padding)
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm, 2048);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerInt1 == 1196699975) {
      return "g1t";
    }
    else if (headerInt1 == 1279808331) {
      return "kshl";
    }
    else if (headerInt1 == 100663296) {
      return "strings";
    }
    else if (headerInt1 == 1296380231) {
      return "g1em";
    }
    else if (headerInt1 == 1598571095) {
      return "wbh";
    }
    else if (headerInt1 == 1598308951) {
      return "wbd";
    }
    else if (headerInt1 == 33554432) {
      return "script";
    }
    else if (headerInt1 == 50349881) {
      return "9g";
    }
    else if (headerInt1 == 539963145) {
      return "justlook";
    }
    else if (headerInt1 == 83886080) {
      return "mission5";
    }
    else if (headerInt1 == 251658240) {
      return "mission15";
    }

    return null;
  }

}
