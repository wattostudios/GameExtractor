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

import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_ARCH00_LTAR;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARCH00_LTAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARCH00_LTAR() {

    super("ARCH00_LTAR", "ARCH00_LTAR");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Condemned: Criminal Origins",
        "FEAR",
        "FEAR: Extraction Point",
        "FEAR: Perseus Mandate",
        "FEAR 2: Project Origin",
        "Middle Earth: Shadow Of Mordor",
        "Middle Earth: Shadow Of War",
        "SAS: Secure Tomorrow");
    setExtensions("arch00", "arch01", "arch05");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("anmtree00p", "Animation Tree", FileType.TYPE_OTHER),
        new FileType("mat00", "Surface Material", FileType.TYPE_OTHER),
        new FileType("gamdb00p", "Game Database", FileType.TYPE_OTHER),
        new FileType("strdb00p", "Skeleton Database", FileType.TYPE_OTHER),
        new FileType("fx00p", "Effects", FileType.TYPE_OTHER),
        new FileType("fx", "Effects Definition", FileType.TYPE_OTHER),
        new FileType("fxi", "Effects Definition", FileType.TYPE_OTHER),
        new FileType("fxd", "Effects Program", FileType.TYPE_OTHER),
        new FileType("fxo", "Effects Object", FileType.TYPE_OTHER),
        new FileType("txanm00", "Texture Animation", FileType.TYPE_OTHER),
        new FileType("crc", "CRC Check", FileType.TYPE_OTHER),
        new FileType("lip", "Animation Lip Sync", FileType.TYPE_OTHER),
        new FileType("matlib00p", "Material Library", FileType.TYPE_OTHER),
        new FileType("model00p", "3D Model", FileType.TYPE_MODEL));

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
      if (fm.readString(4).equals("LTAR")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Version
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // 4 - Filename Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Number Of Folders (including root)
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_Custom_ARCH00_LTAR.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (LTAR)
      // 4 - Version (3)
      fm.skip(8);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number Of Folders (including root)
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Unknown (0)
      // 4 - Unknown (1)
      // 16 - CRC?
      fm.skip(28);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Read the filename directory into memory for quick access
      byte[] filenameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      // Loop through the FILES directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the names directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);

        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString();

        // 8 - File Offset
        long offset = fm.readLong();

        // 8 - Compressed File Length
        long length = fm.readLong();

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();

        // 4 - Compression Flag (0=Uncompressed, 9=Chunked ZLib Compression)
        int compressionFlag = fm.readInt();

        if (length == 0) {
          // We need to keep the empty files here, because they're used in the directory name calculation later on.
          resources[i] = null;
        }
        else {
          FieldValidator.checkFilename(filename);
          FieldValidator.checkOffset(offset, arcSize);
          FieldValidator.checkLength(length, arcSize);

          if (compressionFlag == 0) {
            //path,id,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }
          else {
            //path,id,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      // Loop through the FOLDERS directory
      int currentFilePos = 0;
      for (int i = 0; i < numDirs; i++) {
        // 4 - Folder Name Offset (relative to the start of the names directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);

        nameFM.seek(filenameOffset);
        String folderName = nameFM.readNullString();
        //FieldValidator.checkFilename(folderName);

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 4 - Number of Files in this Folder
        int numFilesInDir = fm.readInt();
        FieldValidator.checkRange(numFilesInDir, 0, numFiles);

        int lastFileInDir = currentFilePos + numFilesInDir;

        for (; currentFilePos < lastFileInDir; currentFilePos++) {
          // so through and set the directory name
          Resource resource = resources[currentFilePos];
          if (resource == null) {
            continue;
          }
          String filename = folderName + "\\" + resource.getName();
          resource.setName(filename);
          resource.setOriginalName(filename);
        }
      }

      // Now filter out the nulls
      Resource[] oldResources = resources;
      resources = new Resource[realNumFiles];
      int currentResource = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = oldResources[i];
        if (resource != null) {
          resources[currentResource] = resource;
          currentResource++;
        }
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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 4 - Header (LTAR)
      // 4 - Version (3)
      fm.writeBytes(src.readBytes(8));

      // 4 - Filename Directory Length
      int srcFilenameDirLength = src.readInt();
      fm.writeInt(srcFilenameDirLength);

      // 4 - Number Of Folders (including root)
      int srcNumFolders = src.readInt();
      fm.writeInt(srcNumFolders);

      // 4 - Number Of Files
      int srcNumFiles = src.readInt();
      fm.writeInt(srcNumFiles);

      // 4 - Unknown (1)
      // 4 - Unknown (0)
      // 4 - Unknown (1)
      // 16 - CRC?
      fm.writeBytes(src.readBytes(28));

      // NAMES DIRECTORY
      fm.writeBytes(src.readBytes(srcFilenameDirLength));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 48 + srcFilenameDirLength + (srcNumFiles * 32) + (srcNumFolders * 16);
      int currentFile = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Offset (relative to the start of the names directory)
        fm.writeBytes(src.readBytes(4));

        // 8 - File Offset
        long srcOffset = src.readLong();

        // 8 - Compressed File Length
        long srcLength = src.readLong();

        // 8 - Decompressed File Length
        long srcDecompLength = src.readLong();

        // 4 - Compression Flag (0=Uncompressed, 9=Chunked ZLib Compression)
        int srcCompFlag = src.readInt();

        if (srcLength == 0) {
          // empty file, not in GE, so just write as is
          fm.writeLong(srcOffset);
          fm.writeLong(srcLength);
          fm.writeLong(srcDecompLength);
          fm.writeInt(srcCompFlag);
        }
        else {
          // normal file
          Resource resource = resources[currentFile];
          currentFile++;

          long length = resource.getDecompressedLength();

          fm.writeLong(offset);

          if (resource.isReplaced()) {
            // writing in an uncompressed format
            fm.writeLong(length);
            fm.writeLong(length);
            fm.writeInt(0);

            offset += length;
          }
          else {
            // write as it is in the original archive
            fm.writeLong(srcLength);
            fm.writeLong(srcDecompLength);
            fm.writeInt(srcCompFlag);

            offset += srcLength;
          }
        }

      }

      // FOLDER DETAILS DIRECTORY
      fm.writeBytes(src.readBytes(srcNumFolders * 16));

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.isReplaced()) {
          // write as a raw file
          write(resource, fm);
        }
        else {
          // write as it is in the original archive
          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
