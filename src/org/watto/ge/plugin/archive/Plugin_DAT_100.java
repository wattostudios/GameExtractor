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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
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
public class Plugin_DAT_100 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_100() {

    super("DAT_100", "DAT_100");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Kill Switch");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("txd", "Texture Archive", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      // Header
      if (fm.readInt() == 1817) {
        rating += 50;
      }

      if (fm.readInt() == 12) {
        rating += 5;
      }

      fm.skip(4);

      if (fm.readString(10).equals("StopSystem")) {
        rating += 5;
      }

      fm.skip(2);

      if (fm.readInt() == 1798) {
        rating += 5;
      }

      if (fm.readInt() == 0) {
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

      FileManipulator fm = new FileManipulator(path, false, 512);

      long arcSize = fm.getLength();

      // 4 - Header (1817)
      // 4 - Chunk Size (12)
      // 4 - Version
      // 12 - String ("StopSystem" + null + "x")
      // 4 - Chunk Type (1798)
      // 4 - null
      // 4 - Version
      fm.skip(36);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 12) {

        // 4 - Chunk Type
        fm.skip(4);

        // 4 - Chunk Data Length (including the Chunk Data Header Fields)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Version
        fm.skip(4);

        // CHUNK DATA
        //   4 - GUID + Path Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        //   X - GUID + Path String {
        //     X - Filename (without extension or path)
        //     1 - null
        //     X - GUID
        //     1 - null
        //     X - Type String
        //     1 - null
        //     X - Source Filename (including path and extension)
        //     1 - null
        //     X - Source Path (without filename or extension)
        //     1 - null
        //     }
        long nextOffset = fm.getOffset() + filenameLength;
        fm.readNullString();
        fm.readNullString();
        fm.readNullString();
        String filename = fm.readNullString();
        fm.relativeSeek(nextOffset);

        if (filename.startsWith("M:\\")) {
          filename = filename.substring(3);
        }

        //   4 - Chunk Length?
        fm.skip(4);

        length -= (8 + filenameLength);

        //   X - Chunk Data
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      resources = resizeResources(resources, realNumFiles);

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
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false, 512);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (1817)
      // 4 - Chunk Size (12)
      // 4 - Version
      // 12 - String ("StopSystem" + null + "x")
      // 4 - Chunk Type (1798)
      // 4 - null
      // 4 - Version
      fm.writeBytes(src.readBytes(36));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Chunk Type
        fm.writeBytes(src.readBytes(4));

        // 4 - Chunk Data Length (including the Chunk Data Header Fields)
        // 4 - Version
        // 4 - GUID + Path Length
        int srcLength = src.readInt();
        int version = src.readInt();
        int filenameLength = src.readInt();

        fm.writeInt(length + 8 + filenameLength);
        fm.writeInt(version);
        fm.writeInt(filenameLength);

        // X - GUID + Path String
        fm.writeBytes(src.readBytes(filenameLength));

        // 4 - Chunk Length?
        src.skip(4);
        fm.writeInt(length);

        // X - Chunk Data
        write(resource, fm);

        srcLength -= (filenameLength + 8);
        src.skip(srcLength);

        TaskProgressManager.setValue(i);
      }

      // Write Footer Data

      // 4 - Chunk Type (1798)
      // 4 - null
      // 4 - Version
      fm.writeBytes(src.readBytes(12));

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
