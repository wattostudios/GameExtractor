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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_HMMSYS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_HMMSYS() {

    super("PAK_HMMSYS", "PAK_HMMSYS");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Rising Kingdoms",
        "Imperivm: Great Battles of Rome");
    setExtensions("pak");
    setPlatforms("PC");

    setTextPreviewExtensions("vs", "bl", "xls"); // LOWER CASE

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
      if (fm.readString(15).equals("HMMSYS PackFile")) {
        rating += 50;
      }

      fm.skip(17);

      long arcSize = fm.getLength();

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 16 - Header ("HMMSYS PackFile" + (byte)10)
      // 4 - Unknown (26)
      // 12 - null
      fm.skip(32);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Length
      fm.skip(4);

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      String prevFilename = "";

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // 1 - Previous Filename Reuse Length
        int prevFilenameLength = ByteConverter.unsign(fm.readByte());

        int namePartLength = filenameLength - prevFilenameLength;

        // X - Filename Part (length = filenameLength - previousFilenameReuseLength)
        String filename = "";
        if (prevFilenameLength > 0) {
          filename = prevFilename.substring(0, prevFilenameLength);
        }
        filename += fm.readString(namePartLength);
        prevFilename = filename;

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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

      // Write Header Data

      // 16 - Header ("HMMSYS PackFile" + (byte)10)
      // 4 - Unknown (26)
      // 12 - null
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(36));

      // 4 - Details Directory Length
      int dirLength = src.readInt();
      fm.writeInt(dirLength);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int offset = 40 + dirLength + (4 * numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(src.readByte());
        fm.writeByte(filenameLength);

        // 1 - Previous Filename Reuse Length
        int prevFilenameLength = ByteConverter.unsign(src.readByte());
        fm.writeByte(prevFilenameLength);

        // X - Filename Part (length = filenameLength - previousFilenameReuseLength)
        int namePartLength = filenameLength - prevFilenameLength;
        fm.writeBytes(src.readBytes(namePartLength));

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        offset += length;
      }

      // HASH DIRECTORY
      // for each file
      // 4 - Unknown Hash
      fm.writeBytes(src.readBytes(numFiles * 4));

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
