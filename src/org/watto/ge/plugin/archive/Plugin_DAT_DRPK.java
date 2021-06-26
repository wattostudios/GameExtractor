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
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_DRPK;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_DRPK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_DRPK() {

    super("DAT_DRPK", "DAT_DRPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Inquisitor");
    setExtensions("dat"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("DRPK")) {
        rating += 50;
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
      //ExporterPlugin exporter = new Exporter_XOR(74);
      ExporterPlugin exporter = new Exporter_Custom_DAT_DRPK();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (DRPK)
      // 2 - null
      fm.seek(arcSize - 16);

      // Everything except the Archive Header is XOR with (byte)74
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 74));

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Folder Name Directory Length
      int folderDirLength = fm.readInt();
      FieldValidator.checkLength(folderDirLength, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Unknown
      long dirOffset = arcSize - folderDirLength - filenameDirLength - 16;
      fm.seek(dirOffset);

      // Read the Folder Name Directory and Filename Directory into buffers
      byte[] folderNameDir = fm.readBytes(folderDirLength);
      FileManipulator folderFM = new FileManipulator(new ByteBuffer(folderNameDir));

      byte[] filenameDir = fm.readBytes(filenameDirLength);
      FileManipulator fileFM = new FileManipulator(new ByteBuffer(filenameDir));

      // Now go through and read the directory
      dirOffset = arcSize - folderDirLength - filenameDirLength - 16 - (numFiles * 24);
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Folder Name Offset (relative to the start of the Folder Name Directory)
        int folderNameOffset = fm.readInt();
        FieldValidator.checkOffset(folderNameOffset, folderDirLength);

        folderFM.seek(folderNameOffset);
        String folderName = folderFM.readNullString() + "\\";

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        fileFM.seek(filenameOffset);
        String filename = folderName + fileFM.readNullString();
        //System.out.println(fm.getOffset() + "\t" + filename);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(i);
      }

      folderFM.close();
      fileFM.close();

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
