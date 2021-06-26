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
import org.watto.ge.plugin.exporter.Exporter_LZ4_Framed;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VPPPC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VPPPC() {

    super("VPPPC", "VPPPC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Agents of Mayhem");
    setExtensions("vpp_pc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("vpkg", "vint_proj"); // LOWER CASE

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

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Folders
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Archive Size
      if (fm.readLong() == arcSize) {
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

      ExporterPlugin exporter = Exporter_LZ4_Framed.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Unknown
      // 8 - Unknown
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Folders?
      fm.skip(4);

      // 4 - Details Directory Length
      int nameDirOffset = fm.readInt() + 120;
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 4 - Names Directory Length
      int nameDirLength = fm.readInt();
      FieldValidator.checkLength(nameDirLength, arcSize);

      // 8 - Archive Length
      // 8 - Unknown
      // 8 - File Data Length
      // 8 - Unknown
      fm.skip(32);

      // 8 - File Data Offset
      long dataOffset = fm.readLong();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 48 - Description (or null)

      fm.seek(nameDirOffset);
      byte[] nameBytes = fm.readBytes(nameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.seek(120);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Filename Offset (relative to the start of the names directory)
        long filenameOffset = fm.readLong();
        FieldValidator.checkOffset(filenameOffset, nameDirLength);

        // 8 - Directory Name Offset (relative to the start of the names directory)
        long dirNameOffset = fm.readLong();
        FieldValidator.checkOffset(dirNameOffset, nameDirLength);

        nameFM.seek(dirNameOffset);
        String filename = nameFM.readNullString();
        nameFM.seek(filenameOffset);
        filename += nameFM.readNullString();

        if (filename.endsWith("_pc")) {
          filename = filename.substring(0, filename.length() - 3);
        }
        if (filename.startsWith("..\\")) {
          filename = filename.substring(3);
        }

        // 8 - File Data Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize + 1); // to allow for files that are at the length of the archive

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        // 8 - Compressed File Length (or -1 if not compressed)
        long length = fm.readLong();
        if (length == -1) {
          length = decompLength;
        }
        FieldValidator.checkLength(length, arcSize);

        // 2 - Compression Flag (0=uncompressed, 1=compressed)
        short compressionFlag = fm.readShort();

        // 2 - Unknown (1)
        // 4 - null
        fm.skip(6);

        //path,name,offset,length,decompLength,exporter
        if (compressionFlag == 0) {
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

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
