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
import org.watto.ErrorLogger;
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate_XOR;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_50 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_50() {

    super("PAK_50", "PAK_50");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("PROJECT XINATRA");
    setExtensions("pak");
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
      if (ByteConverter.unsign(fm.readByte()) == 152 && ByteConverter.unsign(fm.readByte()) == 131) {
        rating += 50;
      }
      else {
        return 0;
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin xorExporter = new Exporter_XOR(200);
      ExporterPlugin multiExporter = new Exporter_Deflate_XOR(200);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // SET THE XOR VALUE
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 200));

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Header (PK)
        fm.skip(2);

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        if (entryType == 1311747 || entryType == 656387) {
          // File Entry

          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          fm.skip(12);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          if (length == decompLength) {
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, xorExporter);
          }
          else {
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, multiExporter);
          }
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 513) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(22);

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.skip(14);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else {
          // bad header
          String errorMessage = "[PAK_50]: Manual read: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6);
          if (realNumFiles >= 5) {
            // we found a number of files, so lets just return them, it might be a "prematurely-short" archive.
            ErrorLogger.log(errorMessage);
            break;
          }
          else {
            throw new WSPluginException(errorMessage);
          }
        }

      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
