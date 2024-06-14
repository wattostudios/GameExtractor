/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DBU extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DBU() {

    super("DBU", "DBU");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Chicken Little");
    setExtensions("dbu"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("dv", "dst", "dsd"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      try {
        int size = Integer.parseInt(fm.readString(8).trim());
        FieldValidator.checkLength(size, fm.getLength());
        rating += 5;
      }
      catch (Throwable t) {
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

      // X - Contents File Size (Characters) (including these fields and the null padding)
      // 1 - Line Separator (char(10))
      String contentsLengthString = fm.readString(8).trim();
      int contentsLength = Integer.parseInt(contentsLengthString) - 8;
      FieldValidator.checkLength(contentsLength, arcSize);

      // X - Contents File (Characters)
      // 0-15 - null Padding to a multiple of 16 bytes
      fm.skip(contentsLength);

      // 2 - Unknown (48)
      // 2 - Unknown (0/69)
      // 4 - Unknown ("SCE_" or null)
      // 8 - DB Header ("DB" + 6x nulls)
      // 4 - Unknown (256)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(32);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Add the contents file

      //path,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, "contents.txt", 0, contentsLength);
      realNumFiles++;

      // Loop through directory
      String filename = "";
      while (fm.getOffset() < arcSize) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 2 - Flags (1)
        short flags = fm.readShort();

        // 54 - Unknown ("1000" + 50x nulls)
        fm.skip(54);

        if (flags == 1 && length == 256) {
          // 256 - Filename (null terminated, filled with nulls, with Directory String at the very end)
          filename = fm.readNullString(length);
        }
        else {
          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
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

      // CONTENTS FILE
      // X - Contents File Size (Characters) (including these fields and the null padding)
      // 1 - Line Separator (char(10))
      // X - Contents File (Characters)
      // 0-15 - null Padding to a multiple of 16 bytes
      Resource contentsResource = resources[0];
      write(contentsResource, fm);

      int contentsLength = (int) contentsResource.getLength();
      int contentsPadding = calculatePadding(contentsLength, 16);
      for (int p = 0; p < contentsPadding; p++) {
        fm.writeByte(0);
      }

      // X - Contents File Size (Characters) (including these fields and the null padding)
      // 1 - Line Separator (char(10))
      String contentsLengthString = src.readString(8).trim();
      contentsLength = Integer.parseInt(contentsLengthString) - 8;

      // X - Contents File (Characters)
      // 0-15 - null Padding to a multiple of 16 bytes
      src.skip(contentsLength);

      // 2 - Unknown (48)
      // 2 - Unknown (0/69)
      // 4 - Unknown ("SCE_" or null)
      // 8 - DB Header ("DB" + 6x nulls)
      // 4 - Unknown (256)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      fm.writeBytes(src.readBytes(32));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      int realNumFiles = 1; // start at 1 to skip the contents file

      long srcArcSize = src.getLength();
      while (src.getOffset() < srcArcSize) {

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        int srcLength = src.readInt();
        FieldValidator.checkLength(srcLength, srcArcSize);

        // 2 - Flags (1)
        short flags = src.readShort();

        if (flags == 1 && srcLength == 256) {
          // write the length and flags that we read earlier
          fm.writeInt(srcLength);
          fm.writeShort(flags);

          // 54 - Unknown ("1000" + 50x nulls)
          fm.writeBytes(src.readBytes(54));

          // 256 - Filename (null terminated, filled with nulls, with Directory String at the very end)
          fm.writeBytes(src.readBytes(256));
        }
        else {

          Resource resource = resources[realNumFiles];
          realNumFiles++;

          // write the new length, and then the flags that we read earlier
          long length = resource.getDecompressedLength();
          int filePadding = calculatePadding(length, 16);

          fm.writeInt(length + filePadding);
          fm.writeShort(flags);

          // 54 - Unknown ("1000" + 50x nulls)
          fm.writeBytes(src.readBytes(54));

          // X - File Data
          src.skip(srcLength);

          write(resource, fm);
          TaskProgressManager.setValue(src.getOffset());

          for (int p = 0; p < filePadding; p++) {
            fm.writeByte(0);
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
