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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NITRO_NTRO extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NITRO_NTRO() {

    super("NITRO_NTRO", "NITRO_NTRO");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Spore Hero Arena",
        "The Sims 2");
    setExtensions("nitro_archive"); // MUST BE LOWER CASE
    setPlatforms("NDS");

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

      // 4 - Header (NTRO)
      if (fm.readString(4).equals("NTRO")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      //if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
      if (fm.readInt() == arcSize) {
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

      // 4 - Header (NTRO)
      // 4 - Archive Length
      // 4 - Unknown (3)
      // 4 - Unknown (14)
      // 4 - Unknown (4)
      // 4 - Unknown (48)
      // 4 - Unknown (15)
      // 4 - Unknown (180)
      fm.skip(32);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (16)
      // 4 - Unknown
      fm.skip(8);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Number of Files
      //int numFiles = fm.readInt();
      //FieldValidator.checkNumFiles(numFiles);
      fm.skip(4);

      int numFiles = (fileDataOffset - dirOffset) / 20;
      FieldValidator.checkNumFiles(numFiles);

      String[] names = new String[numFiles];
      if (dirOffset == 52) {
        // no filenames
        for (int i = 0; i < numFiles; i++) {
          names[i] = Resource.generateFilename(i);
        }
      }
      else {
        // filenames

        // 4 - Filename Directory Length
        fm.skip(4);

        // 4 - Filename Directory Offset
        int filenameDirOffset = fm.readInt();
        FieldValidator.checkOffset(filenameDirOffset, arcSize);

        // 4 - Number of Filenames
        int numNames = fm.readInt();
        FieldValidator.checkNumFiles(numNames);

        fm.seek(filenameDirOffset);

        for (int i = 0; i < numFiles; i++) {
          // 128 - Filename (filled with nulls)
          String filename = fm.readNullString(128);
          FieldValidator.checkFilename(filename);
          names[i] = filename;
        }
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 8 - Unknown
        fm.skip(8);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = names[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // if no filenames, go through and get the fileType for each file, and set that as the extension
      if (dirOffset == 52) {
        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];
          fm.seek(resource.getOffset() + 12);

          String filename = resource.getName();

          // 4 - File Type
          int fileType = fm.readInt();
          if (fileType == 0) {
            filename += ".save";
          }
          else if (fileType == 1) {
            filename += ".nitro_texture";
          }
          else if (fileType == 11) {
            filename += ".nitro_mesh";
          }
          else if (fileType == 14) {
            filename += ".nitro_archive";
          }
          else if (fileType == 17) {
            filename += ".nitro_skeleton";
          }
          else if (fileType == 19) {
            filename += ".nitro_animation";
          }
          else if (fileType == 32) {
            filename += ".nitro_map";
          }
          else if (fileType == 64) {
            filename += ".nitro_xml";
          }
          else if (fileType == 1024) {
            filename += ".nitro_script";
          }
          else if (fileType == 8192) {
            filename += ".nitro_vertices";
          }
          else if (fileType == 10240) {
            filename += ".nitro_text";
          }
          else if (fileType == 65536) {
            filename += ".lot";
          }
          else {
            filename += "." + fileType;
          }

          resource.setName(filename);
          resource.setOriginalName(filename); // so it doesn't think the filename has changed
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

}
