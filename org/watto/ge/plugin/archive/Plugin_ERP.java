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
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ERP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ERP() {

    super("ERP", "ERP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alter Ego");
    setExtensions("erp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (1)
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Unknown
        fm.skip(2);

        // 64 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(64);
        if (filename.equals("")) {
          filename = Resource.generateFilename(i);
        }
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(20);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String name = resource.getName();
        if (FilenameSplitter.getExtension(name).equals("esa")) {
          // Audio file (Ogg or Wav)
          fm.seek(resource.getOffset() + 12);

          // 4 - Audio Header
          String audioHeader = fm.readString(4);
          if (audioHeader.equals("RIFF")) {
            // WAV
            resource.setOffset(resource.getOffset() + 12);

            int length = (int) (resource.getLength() - 12);
            resource.setLength(length);
            resource.setDecompressedLength(length);

            name = name.substring(0, name.length() - 3) + "wav";
            resource.setName(name);
            resource.setOriginalName(name);
          }
          else if (audioHeader.equals("OggS")) {
            // OGG
            resource.setOffset(resource.getOffset() + 12);

            int length = (int) (resource.getLength() - 12);
            resource.setLength(length);
            resource.setDecompressedLength(length);

            name = name.substring(0, name.length() - 3) + "ogg";
            resource.setName(name);
            resource.setOriginalName(name);
          }

        }
        // don't worry about EIM images - they're handled by a Viewer_ERP_EIM_EIM
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
