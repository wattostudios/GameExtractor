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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PCK_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCK_5() {

    super("PCK_5", "PCK_5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Broken Age");
    setExtensions("pck"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("canim", "clump", "effect", "fnt", "id", "sdoc", "texparams", "list", "settings"); // LOWER CASE

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
      byte[] headerBytes = fm.readBytes(4);
      if (headerBytes[0] == 1 && headerBytes[1] == 2 && headerBytes[2] == 3 && headerBytes[3] == 4) {
        rating += 50;
      }

      fm.skip(14);

      long arcSize = fm.getLength();

      // File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Length
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // go to footer
      fm.seek(arcSize - 12);

      // 2 - Number of Files
      int numFiles = ShortConverter.unsign(fm.readShort());

      // 4 - Directory Length
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.relativeSeek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 20 - Unknown
        fm.skip(20);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        int length2 = fm.readInt();
        FieldValidator.checkLength(length2, arcSize);

        if (length != length2) {
          System.out.println("[PCK_5] " + length + " vs " + length2);
        }

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // 10 - Unknown
        fm.skip(10);

        // 4 - File Offset
        byte[] offsetBytes = fm.readBytes(4);
        long offset = IntConverter.unsign(IntConverter.convertLittle(offsetBytes));
        offset += 30; // skip the file header
        FieldValidator.checkOffset(offset, arcSize);

        // X - Filename (XOR with the first byte of the File Offset if > (byte)128, otherwise XOR with (byte)128)
        byte[] filenameBytes = fm.readBytes(filenameLength);
        int key = ByteConverter.unsign(offsetBytes[0]);
        if (key < 128) {
          key = 128;
        }
        for (int f = 0; f < filenameLength; f++) {
          filenameBytes[f] ^= key;
        }
        String filename = StringConverter.convertLittle(filenameBytes);

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
