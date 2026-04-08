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
public class Plugin_CMP_MULT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CMP_MULT() {

    super("CMP_MULT", "CMP_MULT");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Moonbase Commander");
    setExtensions("cmp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (header.equals("MULT")) {
        rating += 50;

        long arcSize = fm.getLength();

        // Archive Size
        if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
          rating += 5;
        }

        // Header
        if (fm.readString(4).equals("WRAP")) {
          rating += 5;
        }

        // Archive Size
        if (IntConverter.changeFormat(fm.readInt()) + 8 == arcSize) {
          rating += 5;
        }
      }
      else if (header.equals("WRAP")) {
        rating += 50;

        long arcSize = fm.getLength();

        // Archive Size
        if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
          rating += 5;
        }

        // Header
        if (fm.readString(4).equals("OFFS")) {
          rating += 5;
        }

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

      // 4 - Header (MULT)
      String header = fm.readString(4);

      // 4 - Archive Length (BIG)
      // 4 - Header (WRAP)
      // 4 - Archive Length [+8] (BIG)
      // 4 - Header (OFFS)
      int relativeOffset = 16;
      if (header.equals("MULT")) {
        fm.skip(16);
      }
      else if (header.equals("WRAP")) {
        fm.skip(8);
        relativeOffset = 8;
      }
      else {
        return null;
      }

      // 4 - Directory Length (BIG) (including these 2 fields)
      int numFiles = (IntConverter.changeFormat(fm.readInt()) - 8) / 4;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset [+16] (LITTLE)
        int offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      // Go through and work out the file extensions
      fm.getBuffer().setBufferSize(4);
      fm.seek(4);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 4 - File Type
        String extension = fm.readString(4);

        String name = resource.getName() + "." + extension;
        resource.setName(name);
        resource.setOriginalName(name);
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

      long archiveSize = 16 + 8 + (numFiles * 4);
      for (int i = 0; i < numFiles; i++) {
        archiveSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Header (MULT)
      String srcType = src.readString(4);
      fm.writeString(srcType);

      if (srcType.equals("MULT")) {
        // 4 - Archive Length (BIG)
        fm.writeInt(IntConverter.changeFormat((int) archiveSize));
        src.skip(4);

        // 4 - Header (WRAP)
        fm.writeBytes(src.readBytes(4));

        // 4 - Archive Length [+8] (BIG)
        fm.writeInt(IntConverter.changeFormat((int) archiveSize - 8));
        src.skip(4);
      }
      else if (srcType.equals("WRAP")) {
        archiveSize -= 8;

        // 4 - Archive Length [+8] (BIG)
        fm.writeInt(IntConverter.changeFormat((int) archiveSize));
        src.skip(4);
      }

      // 4 - Header (OFFS)
      // 4 - Directory Length (BIG) (including these 2 fields)
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8 + (numFiles * 4);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File Offset [+16] (LITTLE)
        fm.writeInt(offset);

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
