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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.archive.datatype.CAB_MSCF_Folder;
import org.watto.ge.plugin.resource.Resource_CAB_MSCF;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAB_MSCF extends ArchivePlugin {

  // the fileReserveSize in the current archive of this type. Needed for file extraction
  static int fileReserveSize = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int getFileReserveSize() {
    return fileReserveSize;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CAB_MSCF() {

    super("CAB_MSCF", "Microsoft Cabinet Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Microsoft Cabinet");
    setExtensions("cab"); // MUST BE LOWER CASE
    setPlatforms("PC");

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
      if (fm.readString(4).equals("MSCF")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // null
      if (FieldValidator.checkEquals(fm.readInt(), 0)) {
        rating += 5;
      }

      // Archive Size
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (FieldValidator.checkEquals(fm.readInt(), 0)) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // 4 - Header (MSCF)
      // 4 - Reserved (null)
      // 4 - Archive Length
      // 4 - Reserved (null)
      // 4 - Offset to the first CFFILE
      // 4 - Reserved (null)
      // 1 - Minor Version (3)
      // 1 - Major Version (1)
      fm.skip(26);

      // 2 - Number of Folders
      int numFolders = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numFolders);

      // 2 - Number of Files
      int numFiles = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Flags
      short flags = fm.readShort();

      // 2 - Cabinet Set ID Number
      // 2 - Sequential Number of this Cabinet file in a Set
      fm.skip(4);

      int folderReserveSize = 0;
      fileReserveSize = 0;
      if ((flags & 4) == 4) {
        // 2 - Header Reserve Size
        short headerReserveSize = fm.readShort();

        // 1 - Folder Reserve Size
        folderReserveSize = ByteConverter.unsign(fm.readByte());

        // 1 - File Reserve Size
        fileReserveSize = ByteConverter.unsign(fm.readByte());

        // X - Reserve Data (length = HeaderReserveSize)
        fm.skip(headerReserveSize);
      }

      if ((flags & 1) == 1) {
        // 0-255 - Previous CAB Filename
        // 1 - null Filename Terminator
        fm.readNullString();
        // 0-255 - Previous Disk Name
        // 1 - null Disk Name Terminator
        fm.readNullString();
      }

      if ((flags & 2) == 2) {
        // 0-255 - Next CAB Filename
        // 1 - null Filename Terminator
        fm.readNullString();
        // 0-255 - Next Disk Name
        // 1 - null Disk Name Terminator
        fm.readNullString();
      }

      // CFFOLDER DIRECTORY
      CAB_MSCF_Folder[] folders = new CAB_MSCF_Folder[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Offset to the first CFDATA in this Folder
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Number of CFDATA blocks in this Folder
        int numBlocks = ShortConverter.unsign(fm.readShort());

        // 2 - Compression Format for each CFDATA in this Folder (1 = MSZIP)
        short compression = fm.readShort();

        if ((flags & 4) == 4) {
          // X - Reserve Data (length = FolderReserveSize)
          fm.skip(folderReserveSize);
        }

        folders[i] = new CAB_MSCF_Folder(offset, numBlocks, compression, fileReserveSize);
      }

      // Go through all the Folders and work out the blocks
      long currentOffset = fm.getOffset();

      int oldBufferSize = fm.getBuffer().getBufferSize();
      fm.getBuffer().setBufferSize(8 + fileReserveSize); // so we only read the block headers - nice and quick

      for (int i = 0; i < numFolders; i++) {
        folders[i].calculateBlocks(fm);
      }

      fm.getBuffer().setBufferSize(oldBufferSize); // ready for the next part of the reading
      fm.seek(currentOffset);

      // CFFILE DIRECTORY

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Uncompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Offset in the Uncompressed CFDATA for the Folder this file belongs to (relative to the start of the Uncompressed CFDATA for this Folder)
        int offset = fm.readInt();

        // 2 - Folder ID (starts at 0)
        short folderID = fm.readShort();
        FieldValidator.checkRange(folderID, 0, numFolders);

        // work out where the first block for this file is located
        CAB_MSCF_Folder folder = folders[folderID];
        /*
        int blockNumber = folder.getBlockForOffset(offset);
        
        // when decompressing the first block, need to discard these bytes from the uncompressed data, as these
        // bytes belong to the previous file in the folder

        int blockOffset = folder.getBlockOffset(blockNumber);
        
        int blockDecompLengthBefore = folder.getBlockDecompLengthBefore(blockNumber);
        int blockDiscardBytes = offset - blockDecompLengthBefore;
        */
        int blockOffset = folder.getOffset();
        int blockDiscardBytes = offset;

        // 2 - File Date
        // 2 - File Time
        fm.skip(4);

        // 2 - File Attributes
        short fileAttributes = fm.readShort();

        String filename = "";
        if ((fileAttributes & 64) == 64) {
          // X - Filename (Unicode)
          // 2? - null Filename Terminator
          filename = fm.readNullUnicodeString();
        }
        else {
          // X - Filename (ASCII)
          // 1 - null Filename Terminator
          filename = fm.readNullString();
        }
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource_CAB_MSCF(path, filename, blockOffset, blockDiscardBytes, decompLength, folder.getExporter());

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
