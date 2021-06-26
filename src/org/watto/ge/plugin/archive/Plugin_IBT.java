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
import org.watto.datatype.FileType;
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IBT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IBT() {

    super("IBT", "IBT");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("ibt");
    setGames("Thief 3: Deadly Shadows");
    setPlatforms("PC");

    setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("dxt", "DXT Image", FileType.TYPE_IMAGE),
        new FileType("phys", "Physics File", FileType.TYPE_OTHER));

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

      // Version
      if (fm.readShort() == 1) {
        rating += 5;
      }

      fm.skip(6);

      long arcSize = fm.getLength();

      // First Data Offset (dirLength)
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(8);

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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - Version (1)
      // 4 - Unknown
      // 2 - Unknown (null)
      // 4 - First Data Offset (dirLength)
      // 4 - File Data Length
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(24);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Unknown
      // 8 - Unknown
      // 8 - Unknown
      fm.skip(20);

      for (int i = 0; i < numFiles; i++) {

        // 4 - ID or Group type?
        fm.skip(4);

        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Length of padding after the file (this is not included in the file size)
        // 4 - File ID?
        // 4 - File Type? (66)
        // 4 - Unknown
        // 8 - Unknown
        // 8 - Unknown
        // 1 - Unknown (8) (filename length maybe???)
        fm.skip(33);

        // X - Filename
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // X - Padding to a length of 308 (using nulls)
        fm.skip(262 - filename.length());

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 1398360144) {
      return "phys";
    }
    else if (headerInt1 == 1415070721) {
      return "dxt";
    }

    return null;
  }

}