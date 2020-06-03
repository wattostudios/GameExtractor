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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_P_DFPF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_P_DFPF() {

    super("P_DFPF", "P_DFPF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Brutal Legend");
    setExtensions("~p"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setEnabled(false); // Too complex to be worried about it

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

      getDirectoryFile(fm.getFile(), "~h");
      rating += 25;

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "~h");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = fm.getLength();

      // 4 - Header (dfpf)
      // 4 - Version? (5) (LITTLE ENDIAN)
      // 4 - null
      fm.skip(12);

      // 4 - Folder Names Directory Offset (2048)
      int folderNamesDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(folderNamesDirOffset, dirSize);

      // 4 - null
      fm.skip(4);

      // 4 - Filename Directory Offset
      int filenameDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(filenameDirOffset, dirSize);

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Filename Directory Length
      int filenameDirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(filenameDirLength, dirSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 12 - null
      // 4 - Unknown
      // 4 - null
      fm.skip(24);

      // 4 - Details Directory Offset
      int detailsDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(detailsDirOffset, dirSize);

      // 4 - null
      // 4 - Padding Directory Offset
      // 4 - null
      // 4 - Header File Length [+16]
      // 4 - Unknown (1)
      // 4 - Unknown
      // 0-2047 - null Padding to a multiple of 2048 bytes
      fm.seek(filenameDirOffset);

      String[] filenames = new String[numFiles];
      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        filenames[i] = filename;
      }

      fm.seek(detailsDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 3 - Compressed File Length
        byte[] lengthBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
        int length = IntConverter.convertBig(lengthBytes);

        // 2 - Flags
        short flags = ShortConverter.changeFormat(fm.readShort());

        // 3 - Decompressed File Length ???
        fm.skip(3);

        // 3 - File Offset
        byte[] offsetBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
        int offset = IntConverter.convertBig(offsetBytes);

        // 1 - VALUEX
        int valueX = fm.readByte();

        // 3 - NAMEX
        byte[] nameXBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
        int nameX = IntConverter.convertBig(nameXBytes);

        // 1 - Compression Type
        fm.skip(1);

        int decompLength = (((valueX & 7) << 24) | nameX);

        if ((flags & 0x8000) == 0x8000) {
          length <<= 3;
          offset <<= 2;
        }
        else {
          decompLength >>= 4;
        }

        offset <<= 5;
        valueX >>= 3;
        offset |= valueX;

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);
        FieldValidator.checkLength(decompLength);

        String filename = filenames[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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
