/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RDA_RESOURCEFILE11 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RDA_RESOURCEFILE11() {

    super("RDA_RESOURCEFILE11", "RDA_RESOURCEFILE11");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("Castle Strike");
    setExtensions("rda"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("bit", "bkg", "dat", "gfx", "inc", "ldg", "old", "prj", "ptn", "rnd", "sbt", "vsh", "test", "abl", "cmm", "dlg", "lvl", "wld", "fig", "fnt", "hef", "map", "pef"); // LOWER CASE

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
      if (fm.readString(18).equals("Resource File V1.1")) {
        rating += 50;
      }

      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(230);

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 18 - Header (Resource File V1.1)
      // 238 - null Padding to offset 256
      fm.skip(256);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 256 - Filename (null terminated, filled with nulls)
        //String filename = fm.readNullString(256);
        String filename = null;
        byte[] bytes = fm.readBytes(256);

        for (int b = 0; b < 256; b++) {
          if (bytes[b] == 0) {
            byte[] nameBytes = new byte[b];
            System.arraycopy(bytes, 0, nameBytes, 0, b);
            filename = new String(nameBytes, "ISO-8859-2");
            break;
          }
        }

        if (filename == null) {
          filename = new String(bytes, "ISO-8859-2");
        }
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Flags? (&1 = Compressed)
        int flags = fm.readInt();

        // 4 - Timestamp
        int timestamp = fm.readInt();

        Resource resource;
        if ((flags & 1) == 1) {
          // compressed

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength);
        }
        resource.addProperty("Timestamp", timestamp);
        resource.addProperty("Flags", flags);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 18 - Header (Resource File V1.1)
      fm.writeString("Resource File V1.1");

      // 238 - null Padding to offset 256
      for (int p = 0; p < 238; p++) {
        fm.writeByte(0);
      }

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 256 + 4 + (numFiles * 276);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        long compLength = resource.getLength();

        // 256 - Filename (null terminated, filled with nulls)
        String filename = resource.getName();
        if (filename.length() > 256) {
          filename = filename.substring(0, 256);
        }
        byte[] filenameBytes = filename.getBytes("ISO-8859-2");
        fm.writeBytes(filenameBytes);
        int padding = 256 - filename.length();
        if (padding > 0) {
          for (int p = 0; p < padding; p++) {
            fm.writeByte(0);
          }
        }

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - Compressed Length
        fm.writeInt((int) compLength);

        // 4 - Decompressed Length
        fm.writeInt((int) decompLength);

        int flags = 4;
        int timestamp = 0;
        try {
          flags = Integer.parseInt(resource.getProperty("Flags"));
          timestamp = Integer.parseInt(resource.getProperty("Timestamp"));
        }
        catch (Throwable t) {
        }

        // 4 - Flags? (&1 = Compressed)
        fm.writeInt(flags);

        // 4 - Timestamp
        fm.writeInt(timestamp);

        offset += compLength;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        ExporterPlugin originalExporter = resource.getExporter();
        resource.setExporter(exporterDefault); // so we keep unedited files in their original compressed format

        write(resource, fm);

        resource.setExporter(originalExporter);

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
