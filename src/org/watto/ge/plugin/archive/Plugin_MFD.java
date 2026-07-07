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
public class Plugin_MFD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MFD() {

    super("MFD", "MFD");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("187 Ride Or Die");
    setExtensions("mfd");
    setPlatforms("XBox");

    setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("btx", "Texture Image", FileType.TYPE_IMAGE));

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
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      fm.skip(4);

      // first file offset
      if (FieldValidator.checkEquals(fm.readInt(), 4 + (numFiles * 12))) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID?
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

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

      // 4 - Number Of Files
      fm.writeInt(numFiles);
      src.skip(4);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 4 + (12 * numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File ID?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        offset += length;
        offset += calculatePadding(offset, 4);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm, 4);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerInt1 == 21068104) {
      return "hya";
    }
    else if (headerInt1 == 1481916974) {
      return "btx";
    }
    else if (headerInt1 == 1393167666 || headerInt1 == 1393167665) {
      return "snd";
    }
    else if (headerInt1 == 1347633759) {
      return "bsp";
    }
    else if (headerInt1 == 1178874701) {
      return "m3d";
    }
    else if (headerInt1 == 1178874696) {
      return "h3d";
    }
    else if (headerInt1 == 1162170964) {
      return "tree";
    }
    else if (headerInt1 == 1111769668 || headerInt1 == 1111769695) {
      return "db";
    }
    else if (headerInt1 == 1175063857 || headerInt1 == 1175063858 || headerInt1 == 1175063861) {
      return "fx";
    }
    else if (headerInt1 == 0 && headerInt3 == 16777215) {
      return "mesh";
    }
    else if (headerInt2 == 1296649793) {
      return "anim";
    }
    else if (headerInt1 == 5132122) {
      return "zon";
    }
    else if (headerInt1 == 1380533087 || headerInt1 == 1380533075) {
      return "ric";
    }
    else if (headerInt1 == 1195787081) {
      return "icfg";
    }

    return null;
  }

}
