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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PACK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PACK_2() {

    super("PACK_2", "PACK_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Brigador");
    setExtensions("pack"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Filename Offsets Directory Offset (64)
      if (fm.readInt() == 64) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 4;
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

      // 4 - Unknown
      // 4 - Unknown (148)
      // 4 - Unknown
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Offsets Directory Offset (64)
      int filenameOffsetsDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameOffsetsDirOffset, arcSize);

      // 4 - Directory 2 Offset
      fm.skip(4);

      // 4 - File Offsets Directory Offset
      int offsetsDirOffset = fm.readInt();
      FieldValidator.checkOffset(offsetsDirOffset);

      // 4 - File Lengths Directory Offset
      int lengthsDirOffset = fm.readInt();
      FieldValidator.checkOffset(lengthsDirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Offsets Directory 1 Offset
      int filenameDirLength = fm.readInt() - filenameDirOffset;
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number of Entries in Offsets Directory 1
      // 4 - Offsets Directory 1 Offset
      // 4 - Number of Entries in Offsets Directory 2
      // 4 - Offsets Directory 2 Offset
      // 4 - null
      // 4 - Unknown

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the filenames
      fm.seek(filenameDirOffset);

      byte[] filenameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      // read the offsets
      fm.seek(offsetsDirOffset);

      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        offsets[i] = offset;
        TaskProgressManager.setValue(i);
      }

      // read the lengths
      fm.seek(lengthsDirOffset);

      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        lengths[i] = length;
        TaskProgressManager.setValue(i);
      }

      // read the filenames and create the Resources
      fm.seek(filenameOffsetsDirOffset);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        int filenameOffset = fm.readInt() - filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        // X - Filename (null)
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        int length = lengths[i];
        int offset = offsets[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      nameFM.close();
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
