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
import java.util.Arrays;

import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_Offset;
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
public class Plugin_RCF_ATGCORE_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RCF_ATGCORE_3() {

    super("RCF_ATGCORE_3", "RCF_ATGCORE_3");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Prototype");
    setExtensions("rcf");
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
      if (fm.readString(23).equals("ATG CORE CEMENT LIBRARY")) {
        rating += 50;
      }

      fm.skip(13);

      // Directory Offset
      if (fm.readInt() == 60) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(12);

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

      // RESETTING THE GLOBAL VARIABLES

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 32 - Header ("ATG CORE CEMENT LIBRARY" + nulls to fill)
      // 4 - Unknown
      fm.skip(36);

      // 4 - Directory Offset (60)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      // 4 - null
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(filenameDirOffset + 8);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - File Data Padding Multiple (2048)
        // 4 - null
        fm.skip(12);

        // 4 - Filename Length (including null terminator)
        int filenameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        names[i] = fm.readString(filenameLength);

        // 4 nulls
        fm.skip(4);

      }

      Resource[] resources = new Resource[numFiles];
      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        int hash = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.addProperty("Hash", hash);
        resources[i] = resource;

        sorter[i] = new ResourceSorter_Offset(resource);

        TaskProgressManager.setValue(i);
      }

      // Sort the resources by Offset, so we can then assign the correct names
      Arrays.sort(sorter);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = sorter[i].getResource();
        String name = names[i];
        resource.setName(name);
        resource.setOriginalName(name);

        // put them in the right order in the proper array too
        resources[i] = resource;
      }

      // go through and determine compression
      fm.getBuffer().setBufferSize(16); // small quick reads

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        // 2 - Compression Header (RZ)
        if (!fm.readString(2).equals("RZ")) {
          continue; // not compressed
        }

        // 6 - null
        fm.skip(6);

        // 8 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        fm.skip(4);

        // X - File Data
        resource.setOffset(fm.getOffset());
        resource.setLength(resource.getLength() - 16);
        resource.setDecompressedLength(decompLength);
        resource.setExporter(exporter);

        String name = resource.getName();

        resource.addProperty("OriginalName", name);

        if (name.endsWith(".rz")) {
          name = name.substring(0, name.length() - 3);
          resource.setName(name);
          resource.setOriginalName(name);
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();

      // Write Header Data

      // 32 - Header ("ATG CORE CEMENT LIBRARY" + nulls to fill)
      // 4 - Unknown
      // 4 - Directory Offset (60)
      fm.writeBytes(src.readBytes(40));

      // 4 - Directory Length
      int srcDirLength = src.readInt();
      fm.writeInt(srcDirLength);

      // 4 - Offset To Filename Directory
      fm.writeBytes(src.readBytes(4));

      // 4 - Filename Directory Length
      int srcNameDirLength = src.readInt();
      fm.writeInt(srcNameDirLength);

      // 4 - null
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(8));

      long offset = 60 + srcDirLength;
      offset += calculatePadding(offset, 2048);
      offset += srcNameDirLength;
      offset += calculatePadding(offset, 2048);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long length = resource.getDecompressedLength();
        if (!resource.isReplaced()) {
          if (resource.isCompressed()) {
            length = resource.getLength() + 16; // keep it compressed, and add the 16-byte compression header
          }
        }

        // 4 - Hash?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        offset += length;
        offset += calculatePadding(offset, 2048);
      }

      // X - null Padding to a multiple of 2048 bytes
      int paddingSize = calculatePadding(fm.getOffset(), 2048);
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }
      src.skip(paddingSize);

      // write the filename directory (which is unchanged)
      fm.writeBytes(src.readBytes(srcNameDirLength));

      // X - null Padding to a multiple of 2048 bytes
      paddingSize = calculatePadding(fm.getOffset(), 2048);
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }
      src.skip(paddingSize);

      // Write Files
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        long currentOffset = fm.getOffset();
        offsets[i] = currentOffset;

        Resource resource = resources[i];

        if (resource.isReplaced()) {
          // need to find out whether the original file was compressed or not. If so, we need to compress this one
          String originalName = resource.getProperty("OriginalName");
          boolean originalCompressed = false;
          if (originalName != null) {
            if (originalName.endsWith(".rz")) {
              originalCompressed = true;
            }
          }

          if (originalCompressed) {
            // write compressed

            // 2 - Compression Header (RZ)
            fm.writeString("RZ");

            // 6 - null
            fm.writeShort(0);
            fm.writeInt(0);

            // 8 - Decompressed Length
            fm.writeInt(resource.getDecompressedLength());
            fm.writeInt(0);

            // X - File Data (ZLib)
            //long compressedLength = write(exporterZLib, resource, fm);
            write(exporterZLib, resource, fm);
          }
          else {
            // write raw
            write(resource, fm);
          }
        }
        else {

          String originalName = resource.getProperty("OriginalName");
          boolean originalCompressed = false;
          if (originalName != null) {
            if (originalName.endsWith(".rz")) {
              originalCompressed = true;
            }
          }

          if (originalCompressed) {
            // want to keep it compressed, so we need to wind back 16 bytes (and increase length by 16) so as to include the compression header
            ExporterPlugin originalExporter = resource.getExporter();
            resource.setOffset(resource.getOffset() - 16);
            resource.setLength(resource.getLength() + 16);
            resource.setExporter(exporterDefault);

            // write it exactly as per the source
            write(resource, fm);

            // put the details back again
            resource.setOffset(resource.getOffset() + 16);
            resource.setLength(resource.getLength() - 16);
            resource.setExporter(originalExporter);
          }
          else {
            // original file, not compressed = write raw
            write(resource, fm);
          }
        }

        lengths[i] = fm.getOffset() - currentOffset;

        // X - null Padding to a multiple of 2048 bytes
        paddingSize = calculatePadding(fm.getOffset(), 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);

      }

      // now we have the offsets and lengths (which we really don't know until the files have been compressed)
      // so now go back and write them into the directory
      fm.seek(60);

      for (int i = 0; i < resources.length; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        fm.writeInt(offsets[i]);

        // 4 - File Length
        fm.writeInt(lengths[i]);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
