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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MPAK_MPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MPAK_MPAK() {

    super("MPAK_MPAK", "MPAK_MPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Battlestations: Midway",
        "Battlestations: Pacific");
    setExtensions("mpak");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

    setTextPreviewExtensions("ats", "def", "pel", "pes", "pfv", "pfx", "scn"); // LOWER CASE

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
      if (fm.readString(4).equals("MPAK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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
      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (MPAK)
      fm.skip(4);

      // 4 - Length of TOC directory
      // 4 - Length of STOC directory
      long dirOffset = 16 + fm.readInt() + fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 2 - Number Of Files in the TOC
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number Of Files in the STOC
      fm.skip(2);

      // 4 - Table Of Contents Header (TOC + (byte)0)
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File Entry Length [+11]
        fm.skip(2);

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 1 - Compression Flag? (1)
        int compressionFlag = fm.readByte();

        // 1 - Filename Length (not including terminator)
        int filenameLength = fm.readByte();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 1 - null Filename Terminator
        fm.skip(1);

        // 2 - Number of Blocks
        short numBlocks = fm.readShort();
        FieldValidator.checkNumFiles(numBlocks);

        // 4 - File Offset (relative to the RAWD header (ie first file = 4))
        long offset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(offset, arcSize);

        if (compressionFlag == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // compressed 

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        // other versions of the same file?
        fm.skip((numBlocks - 1) * 4);

        TaskProgressManager.setValue(i);
      }

      // Now analyse the MWZ files
      fm.getBuffer().setBufferSize(4);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String filename = resource.getName();

        int mwzPos = filename.indexOf(".mwz");
        if (mwzPos > 0) {
          // separately compressed
          filename = filename.substring(0, mwzPos);

          long length = resource.getLength() - 4;
          long offset = resource.getOffset();

          fm.seek(offset);

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          offset += 4;

          resource.setOffset(offset);
          resource.setDecompressedLength(decompLength);
          resource.setLength(length);
          resource.setExporter(exporterZLib);
          resource.setName(filename);
          resource.setOriginalName(filename);
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

}
