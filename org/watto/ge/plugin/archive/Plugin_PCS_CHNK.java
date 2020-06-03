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
public class Plugin_PCS_CHNK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PCS_CHNK() {

    super("PCS_CHNK", "PCS_CHNK");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Driver 3",
        "Driver: Parallel Lines");
    setExtensions("cpr", "d3s", "dam", "gfx", "map", "mec", "mpc", "pcs", "pmu", "vgt", "vvs", "vvv", "sp");
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
      if (fm.readString(4).equals("CHNK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Unknown (3)
      if (fm.readInt() == 3) {
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

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (CHNK)
      // 4 - Archive Size
      fm.skip(8);

      // 4 - Number Of Chunks
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (3)
      fm.skip(4);

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Chunk Type Header (MDPC, PCSL, etc)
        byte[] codeBytes = fm.readBytes(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Chunk Type Code Number?
        String ext = "";
        if (fm.readShort() == 4096) {
          // header is not human-readable
          ext = "." + IntConverter.convertLittle(codeBytes);
        }
        else {
          // header is human-readable
          ext = "." + new String(codeBytes);
        }

        // 2 - Chunk Type Specific Number?
        fm.skip(2);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i) + ext;

        //path,id,name,offset,length,decompLength,exporter
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

      long archiveSize = 0;

      long fileOffset = 16 + (numFiles * 16);
      long paddingSize = 4096 - (fileOffset % 4096);
      if (paddingSize != 4096) {
        fileOffset += paddingSize;
      }

      archiveSize += fileOffset;

      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        archiveSize += length;

        paddingSize = 4096 - (length % 4096);
        if (paddingSize != 4096) {
          archiveSize += paddingSize;
        }
      }

      // Write Header Data

      // 4 - Header (CHNK)
      fm.writeBytes(src.readBytes(4));

      // 4 - Archive Size
      fm.writeInt((int) archiveSize);
      src.skip(4);

      // 4 - Number Of Chunks
      // 4 - Unknown (3)
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Chunk Type Header (MDPC, PCSL, etc)
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt((int) fileOffset);
        src.skip(4);

        // 2 - Chunk Type Code Number?
        // 2 - Chunk Type Specific Number?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        fm.writeInt((int) length);
        src.skip(4);

        fileOffset += length;

        paddingSize = 4096 - (length % 4096);
        if (paddingSize != 4096) {
          fileOffset += paddingSize;
        }
      }

      byte[] paddingBytes = new byte[] { (byte) 161, (byte) 21, (byte) 192, (byte) 222 };

      // 0-4095 - Padding (repeated 161,21,192,222) to a multiple of 4096 bytes
      paddingSize = 4096 - (fm.getOffset() % 4096);
      if (paddingSize != 4096) {
        for (int i = 0, j = 0; i < paddingSize; i++, j++) {
          if (j == 4) {
            j = 0;
          }
          fm.writeByte(paddingBytes[j]);
        }
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // Write Files
      for (int i = 0; i < numFiles; i++) {
        write(resources[i], fm);

        long length = resources[i].getDecompressedLength();
        paddingSize = 4096 - (length % 4096);

        if (paddingSize != 4096) {
          for (int k = 0, j = 0; k < paddingSize; k++, j++) {
            if (j == 4) {
              j = 0;
            }
            fm.writeByte(paddingBytes[j]);
          }
        }

      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
