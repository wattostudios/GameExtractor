/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TemporarySettings;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
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
public class Plugin_WAD_WADH_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_WADH_3() {

    super("WAD_WADH_3", "WAD_WADH_3");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("NHL 2K3");
    setExtensions("wad"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("cmn", "CMN Archive", FileType.TYPE_ARCHIVE),
        new FileType("pvr", "Playstation TIM2 Image", FileType.TYPE_IMAGE),
        new FileType("eng", "ENG Language File", FileType.TYPE_DOCUMENT),
        new FileType("db", "Database File", FileType.TYPE_DOCUMENT));

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
      if (fm.readString(4).equals("WADH")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // File Directory Offset
      if (FieldValidator.checkOffset(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Entry Type (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Folder Name Offset
      if (FieldValidator.checkOffset(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // This Offset
      if (FieldValidator.checkEquals(fm.getOffset(), fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int firstFilenameOffset = 0;

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

      // 4 - Header (WADH)
      fm.skip(4);

      // 4 - File Data Offset [+8]
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      byte[] nameBytes = fm.readBytes(dataOffset);
      fm.relativeSeek(8);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      int numFiles = Archive.getMaxFiles();
      realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      firstFilenameOffset = dataOffset;
      while (fm.getOffset() < firstFilenameOffset) {
        readDirectory(path, fm, nameFM, "", resources, arcSize);
      }

      resources = resizeResources(resources, realNumFiles);

      TemporarySettings.set("ForcePaletteStriping", true); // so the image viewer will force striping of the color palette (in Viewer_TM2_TIM2_4)
      //TemporarySettings.set("SwapRedBlue", true); // so the image viewer will force striping of the color palette (in Viewer_TM2_TIM2_4)

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
   
   **********************************************************************************************
   **/
  public void readDirectory(File path, FileManipulator fm, FileManipulator nameFM, String dirName, Resource[] resources, long arcSize) {
    try {

      //System.out.println(fm.getOffset());

      // 4 - Entry Type (0=File, 1=Folder)
      int entryType = fm.readInt();

      if (entryType == 1) {
        // Folder

        // 4 - Folder Name Offset [+8]
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(nameFM.getLength());

        nameFM.seek(filenameOffset);
        String thisDirName = nameFM.readNullString();

        if (thisDirName.length() == 0) {
          // root dir
        }
        else {
          thisDirName = dirName + thisDirName + "\\";
        }

        if (filenameOffset < firstFilenameOffset) {
          firstFilenameOffset = filenameOffset;
        }

        // 4 - This Offset
        fm.skip(4);

        // 4 - Number of Entries in this Folder
        int numSubEntries = fm.readInt();
        FieldValidator.checkNumFiles(numSubEntries);

        // for each entry in this folder
        int[] offsets = new int[numSubEntries];
        for (int i = 0; i < numSubEntries; i++) {
          //   4 - offset to sub-entry (pointer to a file or a folder entry)
          int offset = fm.readInt() + 8;
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        for (int i = 0; i < numSubEntries; i++) {
          fm.relativeSeek(offsets[i]);
          readDirectory(path, fm, nameFM, thisDirName, resources, arcSize);
        }

      }
      else if (entryType == 0) {
        // File
        // 4 - Filename Offset [+8]
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(nameFM.getLength());

        if (filenameOffset < firstFilenameOffset) {
          firstFilenameOffset = filenameOffset;
        }

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        nameFM.seek(filenameOffset);
        String filename = dirName + nameFM.readNullString();

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }
      else {
        ErrorLogger.log("[WAD_WADH_3] Unknown entry type: " + entryType);
        firstFilenameOffset = 0;
        return;
      }

    }
    catch (

    Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (WADH)
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Offset [+8]
      int dataOffset = src.readInt();
      fm.writeInt(dataOffset);

      int firstFilenameOffset = dataOffset;

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      int realNumFiles = 0;
      long offset = dataOffset + 8;
      while (src.getOffset() < firstFilenameOffset) {
        // 4 - Entry Type (0=File, 1=Folder)
        int entryType = src.readInt();
        fm.writeInt(entryType);

        if (entryType == 1) {
          // 4 - Folder Name Offset [+8]
          int filenameOffset = src.readInt();
          fm.writeInt(filenameOffset);

          if (filenameOffset < firstFilenameOffset) {
            firstFilenameOffset = filenameOffset;
          }

          // 4 - This Offset
          fm.writeBytes(src.readBytes(4));

          // 4 - Number of Entries in this Folder
          int numEntries = src.readInt();
          fm.writeInt(numEntries);

          // for each entry in this folder
          //   4 - offset to sub-entry (pointer to a file or a folder entry)
          fm.writeBytes(src.readBytes(numEntries * 4));
        }
        else if (entryType == 0) {
          // 4 - Filename Offset [+8]
          int filenameOffset = src.readInt();
          fm.writeInt(filenameOffset);

          if (filenameOffset < firstFilenameOffset) {
            firstFilenameOffset = filenameOffset;
          }

          Resource resource = resources[realNumFiles];
          realNumFiles++;

          // 4 - File Offset
          fm.writeInt(offset);
          src.skip(4);

          // 4 - File Length
          int length = (int) resource.getDecompressedLength();
          fm.writeInt(length);
          src.skip(4);

          offset += length;
        }
      }

      // Filename Directory
      int filenameDirLength = dataOffset - firstFilenameOffset;
      FieldValidator.checkLength(filenameDirLength, src.getLength());
      fm.writeBytes(src.readBytes(filenameDirLength));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
