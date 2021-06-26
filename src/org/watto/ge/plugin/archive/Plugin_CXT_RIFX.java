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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CXT_RIFX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CXT_RIFX() {

    super("CXT_RIFX", "CXT_RIFX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Carlsbad Caverns: The Guadalupe Mountains",
        "Last Call",
        "Macromedia Shockwave",
        "Tennis Without Limits");
    setExtensions("cxt", "dxr"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("edim", "JPEG Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("RIFX")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("MV93")) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("imap")) {
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

      // 4 - Header (RIFX)
      // 4 - Archive Length [+8]
      // 4 - Header (MV93)
      // 4 - Header (imap)
      fm.skip(16);

      // 4 - Directory Length (24) (not including these 2 fields)
      int dirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 12 - null
      fm.skip(dirLength);

      // 4 - Header (mmap)
      // 4 - Directory Length (not including these 2 fields)
      fm.skip(8);

      // 2 - Header Length (24)
      int headerLength = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkLength(headerLength);

      // 2 - Entry Length (20)
      int entryLength = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkLength(entryLength);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown
      fm.skip(headerLength - 8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());
        // 4 - File Type/Extension
        String extension = fm.readString(4);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        if (entryLength == 20) {
          fm.skip(4);
          if (fm.readInt() == -1) {

            // early break
            break;
          }
        }
        else {
          // 4 - Unknown (usually null)
          // 4 - Unknown (usually null)
          fm.skip(entryLength - 12);
        }

        if (length != 0) {
          String filename = Resource.generateFilename(realNumFiles) + "." + extension;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

      fm.getBuffer().setBufferSize(4);

      // Check the ediM files, which might be wrapped JPEG images
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        if (resource.getExtension().equalsIgnoreCase("ediM")) {
          fm.seek(resource.getOffset() + 14);
          if (fm.readString(4).equals("JFIF")) {
            resource.setOffset(resource.getOffset() + 8);
            long length = resource.getLength() - 8;
            resource.setLength(length);
            resource.setDecompressedLength(length);
          }
        }
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
