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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VOL_VOL_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VOL_VOL_3() {

    super("VOL_VOL_3", "VOL_VOL_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pro Pilot 99");
    setExtensions("vol"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("VOL ")) {
        rating += 50;
      }

      fm.skip(4);

      if (fm.readString(4).equals("volh")) {
        rating += 5;
      }

      if (fm.readShort() == 4) {
        rating += 5;
      }

      fm.skip(2);

      if (fm.readInt() == 16) {
        rating += 5;
      }

      if (fm.readString(4).equals("vols")) {
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

      //ExporterPlugin exporter = Exporter_LZH.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("VOL ")
      // 3 - File Data Offset [+8]
      // 1 - Endian Indicator (128)

      // 4 - Header (volh)
      // 3 - Block Size (4)
      // 1 - Endian Indicator (128)
      // 4 - Unknown (16)

      // 4 - Header (vols)
      fm.skip(24);

      // 3 - Filename Directory Length (not including these header fields, but yes including padding)
      // 1 - Endian Indicator (128)
      byte[] lengthBytes = fm.readBytes(4);
      lengthBytes[3] &= 127;

      int detailsDirOffset = (int) (fm.getOffset() + IntConverter.convertLittle(lengthBytes));
      FieldValidator.checkOffset(detailsDirOffset, arcSize);

      // 4 - Filename Directory Length (not including any header fields or padding)
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // X - Filename Directory
      byte[] nameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.relativeSeek(detailsDirOffset);

      // 4 - Header (voli)
      fm.skip(4);

      // 3 - Details Directory Length (not including these header fields, but yes including padding entries)
      // 1 - Endian Indicator (128)
      lengthBytes = fm.readBytes(4);
      lengthBytes[3] &= 127;

      int dirLength = IntConverter.convertLittle(lengthBytes);
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = dirLength / 14;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt();
        if (filenameOffset == -1) {
          break; // end of directory - padding file entry
        }
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString();

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 1 - Unknown (3)
        // 1 - Unknown (1)
        fm.skip(2);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, decompLength, decompLength);
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      fm.getBuffer().setBufferSize(8);// small quick reads

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 4 - Header (VBLK)
        fm.skip(4);

        // 3 - Compressed File Data Length (not including these header fields)
        // 1 - Endian Indicator (128)
        lengthBytes = fm.readBytes(4);
        lengthBytes[3] &= 127;

        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        resource.setLength(length);
        resource.setOffset(fm.getOffset());
        //resource.setExporter(new Exporter_LZH());
        //resource.setExporter(new Exporter_Custom_DynamixLZW());
      }

      //calculateFileSizes(resources, arcSize);

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
