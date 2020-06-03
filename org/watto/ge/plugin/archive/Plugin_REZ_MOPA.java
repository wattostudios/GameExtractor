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
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_REZ_MOPA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_REZ_MOPA() {

    super("REZ_MOPA", "REZ_MOPA");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Medal Of Honor: Pacific Assault");
    setExtensions("rez");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("MOPA")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header 2
      if (fm.readString(8).equals("LZSSRLE ")) {
        rating += 5;
      }

      // Header 3
      if (fm.readString(4).equals("NONE")) {
        rating += 5;
      }

      fm.skip(4);

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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // If QuickBMS is available, use it to perform the decompression
      ExporterPlugin exporterLZSS = Exporter_Default.getInstance();
      ExporterPlugin exporterRLE = Exporter_Default.getInstance();
      if (QuickBMSHelper.checkAndShowPopup() != null) {
        exporterLZSS = new Exporter_QuickBMS_Decompression("mohlzss");
        exporterRLE = new Exporter_QuickBMS_Decompression("mohrle");
      }

      long arcSize = fm.getLength();

      // 4 - Header (MOPA)
      // 4 - Version (1)
      fm.skip(8);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 8 - Header 2 (LZSSRLE )
      // 4 - Header 3 (NONE)
      // 4 - Unknown (1)
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // JUMP TO THE FILENAME DIRECTORY
      fm.skip(numFiles * 16);

      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        //System.out.println(filename);
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      // JUMP TO THE NORMAL DIRECTORY
      fm.seek(32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();

        // 3 - Filename Offset [+24]
        fm.skip(3);

        // 1 - Compression Type [>>5]
        int compressionType = fm.readByte() >> 5;

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        if (compressionType == 1) {
          resources[i].setExporter(exporterLZSS);
        }
        else if (compressionType == 2) {
          resources[i].setExporter(exporterRLE);
        }
        //else {
        //  // No Compression
        //}

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("hag") || extension.equalsIgnoreCase("csv") || extension.equalsIgnoreCase("hal") || extension.equalsIgnoreCase("hat") || extension.equalsIgnoreCase("hi") || extension.equalsIgnoreCase("tik") || extension.equalsIgnoreCase("vfx")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dataOffset = 32 + numFiles * 16;
      for (int i = 0; i < numFiles; i++) {
        dataOffset += resources[i].getNameLength() + 1;
      }

      // Write Header Data

      // 4 - Header (MOPA)
      // 4 - Version (1)
      // 4 - Directory Length 
      // 4 - Compression Type 1 ("LZSS")
      // 4 - Compression Type 2 ("RLE ")
      // 4 - Compression Type 3 ("NONE")
      // 4 - Unknown (1)
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(32));

      // Write Details Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dataOffset;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long length = resource.getLength();
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - Decompressed Length
        fm.writeInt(decompLength);
        src.skip(4);

        // 3 - Filename Offset [+24]
        fm.writeBytes(src.readBytes(3));

        // 1 - Compression Type [>>5]
        if (resource.isReplaced()) {
          // force this file to "NONE" compression
          fm.writeByte(0);
          src.skip(1);

          // change the compLength to be the same as the decompLength, so we write the correct value down later
          length = decompLength;
        }
        else {
          fm.writeBytes(src.readBytes(1));
        }

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        offset += length;
      }

      // Write Filename Directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        fm.writeString(src.readNullString());
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.isReplaced()) {
          // if the file HAS been replaced, copy it raw from the replaced file on the PC
          write(resource, fm);
        }
        else {
          // if the file HASN'T been replaced, just copy it raw (Exporter_Default). This will effectively keep it compressed, if it's already compressed.
          ExporterPlugin oldExporter = resource.getExporter();

          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(oldExporter);
        }
        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
