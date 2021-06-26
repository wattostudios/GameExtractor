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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CPK_CPK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CPK_CPK_2() {

    super("CPK_CPK_2", "CPK_CPK_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sonic the Hedgehog 4");
    setExtensions("cpk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("CPK ")) {
        rating += 50;
      }

      // 4 - Unknown (255)
      if (fm.readInt() == 255) { // this will prioritise it over the CPK_CPK plugin, and in read() we will deny if the TOC block is missing
        rating += 5;
      }

      fm.skip(8);

      // Header
      if (fm.readString(4).equals("@UTF")) {
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

      // 4 - Header (CPK )
      // 4 - Unknown (255)
      // 4 - Header Length [+20]
      // 4 - null
      // 4 - Header (@UTF)
      // X - Stuff
      // 0-2047 - null Padding to a multiple of 2048 bytes
      fm.seek(2048);

      // 4 - Header (TOC )
      if (!fm.readString(4).equals("TOC ")) {
        return null; // this one REQUIRES a TOC block. The other plugin CPK_CPK doesn't.
      }

      // 4 - Unknown (255)
      // 4 - Table Of Contents Length  (not including these 4 header fields, or the padding)
      // 4 - null
      // 4 - Header (@UTF)
      // 4 - Table of Contents Length (not including the padding) [+24]
      fm.skip(20);

      // 4 - Details Directory Offset (relative to the start of the Table of Contents) [+24]
      int dirOffset = IntConverter.changeFormat(fm.readInt()) + 24 + 2048;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown
      // 4 - Table of Contents Length (not including the padding) [+24]
      // 4 - Number of File Properties
      // 2 - Unknown
      // 2 - Unknown
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      int nameDirOffset = dirOffset + (numFiles * 24);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the Names Directory)
        int nameOffset = IntConverter.changeFormat(fm.readInt()) + nameDirOffset;
        FieldValidator.checkOffset(nameOffset, arcSize);
        nameOffsets[i] = nameOffset;

        // 4 - File Length (including all the file header fields)
        int length = IntConverter.changeFormat(fm.readInt()) - 8;
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - File Length (including all the file header fields)
        // 4 - null
        fm.skip(8);

        // 4 - File Offset (relative to the start of Table Of Contents)
        int offset = IntConverter.changeFormat(fm.readInt()) + 2048 + 8;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File ID (incremental from 0)
        fm.skip(4);

        TaskProgressManager.setValue(i);
      }

      // Loop through the filename directory
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(nameOffsets[i]); // Relative seek (as we will usually be in the right place already)

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        int offset = offsets[i];
        int length = lengths[i];

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

}
