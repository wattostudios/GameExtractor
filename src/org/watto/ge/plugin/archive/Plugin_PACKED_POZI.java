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
public class Plugin_PACKED_POZI extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PACKED_POZI() {

    super("PACKED_POZI", "PACKED_POZI");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("American McGee's Scrapland",
        "Clive Barker's Jericho");
    setExtensions("packed");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("fnt", "txa"); // LOWER CASE

    //setCanScanForFileTypes(true);

  }

  /**
  **********************************************************************************************
  Gets a blank resource of this type, for use when adding resources.
  This overrides the normal method, so that files are stored with the correct path separator slash.
  
  This is only used when adding files to an existing archive of this type. If this is a new archive,
  we don't know what plugin is going to be used until we save the archive, so we also have to cater
  for that in the write() method.
  **********************************************************************************************
  **/
  public Resource getBlankResource(File file, String name) {
    return new Resource(file, name.replace('\\', '/'));
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
      String header = fm.readString(4);
      if (header.equals("Pozi") || header.equals("BFPK")) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
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

  String archiveHeader = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (Pozi)
      archiveHeader = fm.readString(4);

      // 4 - null
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles + 1]; // to calculate the compressed lengths, and therefore work out which files are compressed
      offsets[numFiles] = arcSize;

      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      /*
      // now go through and look for compressed files
      fm.getBuffer().setBufferSize(5);
      
      for (int i = 0; i < numFiles; i++) {
        try {
          Resource resource = resources[i];
      
          long offset = resource.getOffset();
          fm.seek(offset);
      
          int decompLength = (int) resource.getDecompressedLength();
      
          // 4 - Compressed Length
          int length = fm.readInt();
      
          // 1 - ZLib Compression Header
          int compressionHeader = fm.readByte();
      
          if (compressionHeader == 120 && length < decompLength) {
            if (FieldValidator.checkLength(length, arcSize)) {
              offset += 4;
              resource.setOffset(offset);
              resource.setLength(length);
              resource.setDecompressedLength(decompLength);
              resource.setExporter(exporterZLib);
            }
          }
        }
        catch (Throwable t) {
          // not a compressed file, carry on
        }
      }
      */

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        int decompLength = (int) resource.getDecompressedLength();
        int compLength = (int) (offsets[i + 1] - offsets[i]);

        if (compLength != decompLength) {
          compLength -= 4; // to remove the compression header

          resource.setOffset(resource.getOffset() + 4);
          resource.setLength(compLength);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporterZLib);
        }
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
      long offset = 12;
      for (int i = 0; i < numFiles; i++) {
        offset += 12 + resources[i].getNameLength();
      }

      // Write Header Data

      // 4 - Header (Pozi)
      if (archiveHeader == null) {
        fm.writeString("Pozi");
      }
      else {
        fm.writeString(archiveHeader);
      }

      // 4 - null 
      fm.writeInt((int) 0);

      // 4 - Number Of Files 
      fm.writeInt((int) numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.isAdded()) {
          resource.setName(resource.getName().replace('\\', '/')); // forces the right file path slashes
        }

        String filename = resource.getName();
        long length = resource.getDecompressedLength();

        // 4 - Filename Length
        fm.writeInt((int) filename.length());

        // X - Filename
        fm.writeString(filename);

        // 4 - File Size
        fm.writeInt((int) length);

        // 4 - Data Offset
        fm.writeInt((int) offset);

        if (!resource.isReplaced() && resource.isCompressed()) {
          offset += (4 + resource.getLength());
        }
        else {
          offset += length;
        }
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (!resource.isReplaced() && resource.isCompressed()) {
          // 4 - Compressed Length
          fm.writeInt(resource.getLength());

          // X - Compressed Data
          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);
        }
        else {
          // write the uncompressed file
          write(resource, fm);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
