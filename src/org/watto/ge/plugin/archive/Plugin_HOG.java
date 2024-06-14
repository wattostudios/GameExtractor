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
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HOG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HOG() {

    super("HOG", "HOG");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Syphon Filter: The Omega Strain",
        "Syphon Filter");
    setExtensions("hog");
    setPlatforms("PS2",
        "PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tim", "Playstation TIM Image", FileType.TYPE_IMAGE));

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

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Archive Header Length (20)
      if (fm.readInt() == 20) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Filename Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Offset (2048)
      if (fm.readInt() == 2048) {
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

      // CLEAR OUT THE PALETTES LOADED FROM A DIFFERENT HOG ARCHIVE
      PaletteManager.clear();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Unknown
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Archive Header Length (20)
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - First File Offset (2048)
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(filenameDirOffset);

      // Loop through filename directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      fm.seek(20);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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

      // 4 - Unknown (164,240,164,54 or 171,240,164,54)
      // 4 - Number Of Files
      // 4 - Archive Header Length (20)
      // 4 - Filename Directory Offset
      // 4 - File Data Offset (2048)
      fm.writeBytes(src.readBytes(20));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt(offset);

        offset += length;
        offset += calculatePadding(offset, 2048);
      }

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - Filename (null)
        fm.writeString(resource.getName());
        fm.writeByte(0);
      }

      // X - null Padding to a multiple of 2048 bytes
      int padding = calculatePadding(fm.getOffset(), 2048);
      for (int p = 0; p < padding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        // X - File Data
        write(resources[i], fm);

        // X - null Padding to a multiple of 2048 bytes
        padding = calculatePadding(fm.getOffset(), 2048);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
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
