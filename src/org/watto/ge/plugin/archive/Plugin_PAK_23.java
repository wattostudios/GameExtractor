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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.io.StringHelper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_23 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_23() {

    super("PAK_23", "PAK_23");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Dead To Rights 2");
    setExtensions("pak");
    setPlatforms("PS2", "PC");

    setTextPreviewExtensions("scr", "vfx"); // LOWER CASE

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
      if (fm.readInt() == -1961496690) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Archive Header Size
      if (fm.readInt() == 16) {
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

      Exporter_LZO_MiniLZO exporter = Exporter_LZO_MiniLZO.getInstance();
      exporter.setCheckDecompressedLength(false);
      exporter.setForceDecompress(true);
      exporter.setUseActualDecompressedLength(true);

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (4108980338)
      // 4 - null
      // 4 - Archive Header Size (16)
      fm.skip(12);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (Sometimes including the drive letter "S:/", etc.)
        // 1 - Filename Terminator (using (byte)10)
        String filename = StringHelper.readTerminatedString(fm.getBuffer(), (byte) 10);
        FieldValidator.checkFilename(filename);

        int dotPos = filename.indexOf(':');
        if (dotPos > 0) {
          filename = filename.substring(dotPos + 2);
        }

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Length (0=uncompressed)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (length == 0) {
          // uncompressed

          length = decompLength;

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // lzo1x

          int numBlocks = length / 2048;
          if (numBlocks == 1) {
            //path,id,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {

            long[] blockOffsets = new long[numBlocks];
            long[] blockLengths = new long[numBlocks];
            long[] blockDecompLengths = new long[numBlocks];

            int blockOffset = offset;
            for (int b = 0; b < numBlocks; b++) {
              blockOffsets[b] = blockOffset;
              blockLengths[b] = 2048;
              blockDecompLengths[b] = decompLength;
              blockOffset += 2048;
            }

            BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);

            //path,id,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, blockExporter);
          }

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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // Write Header Data

      // 4 - Header (4108980338)
      // 4 - null
      // 4 - Archive Header Size (16)
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(16));

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // read the directory to get the original filenames, so we can then work out the first offset
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (Sometimes including the drive letter "S:/", etc.)
        // 1 - Filename Terminator (using (byte)10)
        StringHelper.readTerminatedString(src.getBuffer(), (byte) 10);

        // 4 - File Offset
        // 4 - File Length
        // 4 - Padding Multiple (16384 for most files, 0 for the last file)
        src.skip(12);
      }

      long offset = src.getOffset();
      src.relativeSeek(16);

      // read the directory to get the original filenames, so we can then work out the first offset
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (Sometimes including the drive letter "S:/", etc.)
        // 1 - Filename Terminator (using (byte)10)
        String filename = StringHelper.readTerminatedString(src.getBuffer(), (byte) 10);
        fm.writeString(filename);
        fm.writeByte(10);

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        Resource resource = resources[i];
        int length = (int) resource.getDecompressedLength();

        if (resource.isReplaced()) {
          // 4 - Decompressed File Length
          fm.writeInt(length);

          // 4 - File Length (0=uncompressed)
          fm.writeInt(0);

          src.skip(8);
        }
        else {
          // 4 - Decompressed File Length
          // 4 - File Length (0=uncompressed)
          fm.writeBytes(src.readBytes(8));

          length = (int) resource.getLength();
        }

        lengths[i] = length;

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.isReplaced()) {
          write(resource, fm);
        }
        else {
          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);
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
