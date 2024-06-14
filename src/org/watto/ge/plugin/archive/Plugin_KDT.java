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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_KDT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_KDT() {

    super("KDT", "KDT");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Gorky 17");
    setExtensions("kdt", "dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("spr", "Texture Archive", FileType.TYPE_ARCHIVE));

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

      // 4 - Number of Files (including blank end-of-directory entry)
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 28 - null
      if (fm.readLong() == 0) {
        rating += 5;
      }
      if (fm.readLong() == 0) {
        rating += 5;
      }
      fm.skip(12);

      // 4 - File Offset
      if (FieldValidator.checkOffset(fm.readInt(), fm.getLength())) {
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

      // 4 - Number of Files (including blank end-of-directory entry)
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      // 28 - null
      fm.skip(28);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 112 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(112);
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
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

      long archiveSize = 32 + ((numFiles + 1) * 128);
      for (int i = 0; i < numFiles; i++) {
        int length = (int) resources[i].getDecompressedLength() + 1; // always has a null at the end of the file data
        length += calculatePadding(length, 32);
        archiveSize += length;
      }

      // Write Header Data

      // 4 - Number of Files (including blank end-of-directory entry)
      // 28 - null
      fm.writeBytes(src.readBytes(32));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 32 + (128 * (numFiles + 1));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - File Length (not including null padding)
        fm.writeInt(length);
        src.skip(4);

        // 4 - Unknown
        // 4 - Unknown
        // 112 - Filename (null terminated, filled with nulls)
        fm.writeBytes(src.readBytes(120));

        length += 1; // always a null at the end of each file data
        length += calculatePadding(length, 32);

        offset += length;
      }

      // 4 - End of File Data Offset
      fm.writeInt(archiveSize);

      // 124 - null End Of Directory Entry
      for (int p = 0; p < 124; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // 1 - null Terminator
        fm.writeByte(0);

        int paddingSize = calculatePadding(((int) resource.getDecompressedLength()) + 1, 32);

        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      // X - null Padding (optional)
      int sourceLength = (int) src.getLength();
      int outLength = (int) offset;
      int paddingSize = sourceLength - outLength;
      if (paddingSize > 0) {
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
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
