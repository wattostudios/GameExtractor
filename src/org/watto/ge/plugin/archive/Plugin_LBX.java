/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LBX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LBX() {

    super("LBX", "Master LBX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("lbx");
    setGames("Master of Magic",
        "Master of Orion",
        "Master of Orion 2");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xmi", "XMI Audio", FileType.TYPE_AUDIO),
        new FileType("voc", "Creative Voice Audio", FileType.TYPE_AUDIO));

    setCanScanForFileTypes(true);

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

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // 4 - Unknown (65197)
      fm.skip(4);

      // 2 - null
      if (fm.readShort() == 0) {
        rating += 5;
      }

      // Filename Dir Offset (2048)
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - numFiles
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 2 - Unknown
      // 4 - null
      fm.skip(6);

      String[] names = new String[numFiles];

      if (path.getName().toLowerCase().equals("sound.lbx")) {

        // 4 - Filename Directory Offset
        int filenameDirOffset = fm.readInt();

        // GO TO THE FILENAME DIRECTORY
        fm.seek(filenameDirOffset);

        for (int i = 0; i < numFiles; i++) {
          // 20 - Filename (null)
          names[i] = fm.readNullString(20);
        }

        fm.seek(12);

        numFiles -= 1;

      }

      else {
        for (int i = 0; i < numFiles; i++) {
          names[i] = Resource.generateFilename(i);
        }
      }

      for (int i = 0; i < numFiles; i++) {
        // 4 - Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      // 4 - Archive Length
      int archiveLength = fm.readInt();
      if (archiveLength != 0) {
        arcSize = archiveLength;
      }

      fm.close();

      calculateFileSizes(resources, arcSize);

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

    if (headerInt1 == 188079) {

      resource.setOffset(resource.getOffset() + 16);
      long length = resource.getLength() - 16;
      resource.setLength(length);
      resource.setDecompressedLength(length);

      return "voc";
    }
    else if (headerInt1 == 122543) {

      resource.setOffset(resource.getOffset() + 16);
      long length = resource.getLength() - 16;
      resource.setLength(length);
      resource.setDecompressedLength(length);

      return "xmi";
    }

    return null;
  }

}