/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import java.util.Hashtable;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.Hex;
import org.watto.io.StringHelper;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_P5CK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_P5CK_2() {

    super("PAK_P5CK_2", "PAK_P5CK_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("TimeSplitters: Future Perfect");
    setExtensions("pak");
    setPlatforms("PS2", "GameCube");

    setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("gct", "GCT Image", FileType.TYPE_IMAGE),
        new FileType("dsp", "DSP Audio", FileType.TYPE_AUDIO),
        new FileType("mss", "MSS Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("P5CK")) {
        rating += 50;
      }

      fm.skip(4);

      // Directory Length
      if (FieldValidator.checkNumFiles(fm.readInt() / 16)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      try {
        getDirectoryFile(fm.getFile(), "c2n");
        rating += 5; // GameCube prefers this plugin
      }
      catch (Throwable t) {
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  boolean hasHashTable = false;

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

      // 4 - Header (P5CK)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 2036 - null Padding to offset 2048
      long dirOffset = arcSize - dirLength;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int numFiles = dirLength / 16;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // If this is GameCube, it has a separate c2n file that provides the filenames - read that in first
      hasHashTable = false;
      Hashtable<Integer, String> hashMap = null;
      try {
        File c2nFile = getDirectoryFile(path, "c2n");
        hasHashTable = true;

        hashMap = new Hashtable<Integer, String>(numFiles);

        FileManipulator nameFM = new FileManipulator(c2nFile, false);
        byte[] nameFMBytes = nameFM.readBytes((int) nameFM.getLength());
        nameFM.close();

        nameFM = new FileManipulator(new ByteBuffer(nameFMBytes));
        ManipulatorBuffer nameBuffer = nameFM.getBuffer();

        for (int i = 0; i < numFiles; i++) {
          nameFM.skip(2); // 0x

          String hash = nameFM.readString(8);
          nameFM.skip(2); // 2 spaces in between
          int hashValue = IntConverter.convertBig(new Hex(hash));

          String filename = StringHelper.readTerminatedString(nameBuffer, (byte) 10);

          hashMap.put(hashValue, filename);
        }

        nameFM.close();
      }
      catch (Throwable t) {
        // don't worry about it
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        String filename = null;

        // 4 - Hash?
        if (hasHashTable) {
          filename = hashMap.get(fm.readInt());

          if (filename == null) {
            filename = Resource.generateFilename(i);
          }
        }
        else {
          fm.skip(4);
          filename = Resource.generateFilename(i);
        }

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (hasHashTable) {
      return resource.getExtension();
    }

    if ((headerInt1 == 1 || headerInt1 == 5 || headerInt1 == 3 || headerInt1 == 7) && (headerInt2 == 1 || headerInt2 == 2 || headerInt2 == 3)) {
      return "gct";
    }

    return null;
  }

}
