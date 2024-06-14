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
import java.util.HashMap;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BSA_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BSA_4() {

    super("BSA_4", "BSA_4");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Fallout 3",
        "Fallout: New Vegas");
    setExtensions("bsa"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("BSA" + (char) 0)) {
        rating += 50;
      }

      int version = fm.readInt();
      if (version == 103 || version == 104) {
        rating += 5;
      }

      if (fm.readInt() == 36) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Folders
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 3)) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("BSA" + null)
      // 4 - Version (104)
      fm.skip(8);

      // 4 - Header Length (36)
      int folderDirOffset = fm.readInt();
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Archive Flags
      int archiveFlags = fm.readInt();

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 3);

      // 4 - Total Length of all Folder Names (including their padding bytes)
      int totalFolderNameLength = fm.readInt();
      FieldValidator.checkLength(totalFolderNameLength, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - File Type Flags
      fm.skip(4);

      boolean archiveCompressionFlag = (((archiveFlags >> 2) & 1) == 1);
      boolean embeddedNamesFlag = (((archiveFlags >> 8) & 1) == 1);

      // read the filenames directory
      int filenameDirOffset = folderDirOffset + (numFolders * 16) + totalFolderNameLength + numFolders + (numFiles * 16);
      fm.seek(filenameDirOffset);

      byte[] filenameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));
      nameFM.seek(0);

      fm.seek(folderDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] numFilesInFolders = new int[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 8 - Folder Name Hash
        fm.skip(8);

        // 4 - Number of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder);

        numFilesInFolders[i] = numFilesInFolder;

        // 4 - Filename Offset (kinda)
        fm.skip(4);
      }

      // Loop through directory
      int realNumFiles = 0;
      boolean[] compressed = new boolean[numFiles];
      for (int i = 0; i < numFolders; i++) {
        // 1 - Folder Name Length (including null terminator)
        int folderNameLength = ByteConverter.unsign(fm.readByte());

        // X - Folder Name
        // 1 - null Folder Name Terminator
        String folderName = fm.readNullString(folderNameLength);
        FieldValidator.checkFilename(folderName);
        folderName += "\\";

        int numFilesInFolder = numFilesInFolders[i];

        for (int f = 0; f < numFilesInFolder; f++) {
          // 8 - Filename Hash
          fm.skip(8);

          // 4 - File Length (bit 30 = compression is the opposite of what is in the archive flags, bit 31 = internal check)
          int length = fm.readInt();

          int swapCompression = ((length >> 30) & 1);
          if (swapCompression == 1) {
            compressed[realNumFiles] = !archiveCompressionFlag;
          }
          else {
            compressed[realNumFiles] = archiveCompressionFlag;
          }

          length &= 1073741823;
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          String filename = nameFM.readNullString();
          FieldValidator.checkFilename(filename);

          filename = folderName + filename;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);

          realNumFiles++;
        }

      }

      fm.getBuffer().setBufferSize(256);

      // Loop through compressions
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (embeddedNamesFlag) {
          long offset = resource.getOffset();
          fm.relativeSeek(offset);

          // 1 - Filename Length
          // X - Filename (including Folder Path)
          int filenameLength = ByteConverter.unsign(fm.readByte());

          String filename = fm.readString(filenameLength);

          offset += filenameLength + 1;
          long length = resource.getLength() - (filenameLength + 1);

          resource.setOffset(offset);
          resource.setLength(length);
          resource.setDecompressedLength(length);
          resource.addProperty("EmbeddedNameLength", filenameLength);
          resource.addProperty("EmbeddedName", filename);
        }

        if (compressed[i]) {
          long offset = resource.getOffset();
          fm.relativeSeek(offset);

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          offset += 4;

          resource.setOffset(offset);
          resource.setLength(resource.getLength() - 4);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header ("BSA" + null)
      // 4 - Version (104)
      // 4 - Header Length (36)
      fm.writeBytes(src.readBytes(12));

      // 4 - Archive Flags
      int srcArchiveFlags = src.readInt();
      fm.writeInt(srcArchiveFlags);

      // 4 - Number of Folders
      int srcNumFolders = src.readInt();
      fm.writeInt(srcNumFolders);

      // 4 - Number of Files
      int srcNumFiles = src.readInt();
      fm.writeInt(srcNumFiles);

      // 4 - Total Length of all Folder Names (including their padding bytes)
      int srcTotalNameLength = src.readInt();
      fm.writeInt(srcTotalNameLength);

      // 4 - Filename Directory Length
      int srcFilenameDirLength = src.readInt();
      fm.writeInt(srcFilenameDirLength);

      // 4 - File Type Flags
      fm.writeBytes(src.readBytes(4));

      // for each folder
      int[] numFilesInFolders = new int[srcNumFolders];
      for (int i = 0; i < srcNumFolders; i++) {
        // 8 - Folder Name Hash
        fm.writeBytes(src.readBytes(8));

        // 4 - Number of Files in this Folder
        int numFilesInFolder = src.readInt();
        numFilesInFolders[i] = numFilesInFolder;
        fm.writeInt(numFilesInFolder);

        // 4 - Filename Offset (kinda)
        fm.writeBytes(src.readBytes(4));
      }

      boolean archiveCompressionFlag = (((srcArchiveFlags >> 2) & 1) == 1);
      boolean embeddedNamesFlag = (((srcArchiveFlags >> 8) & 1) == 1);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // Some archives (eg Textures) don't store files in the right order, so we need to store them here with their offset
      // so we can find them when we need to write them later on, in the right order.
      HashMap<Integer, Integer> resourceMap = new HashMap<Integer, Integer>(numFiles);

      long offset = 36 + (srcNumFolders * 16) + srcTotalNameLength + srcNumFolders + (srcNumFiles * 16) + srcFilenameDirLength;

      // for each folder
      int[] originalLengths = new int[numFiles];
      boolean[] originalCompressedFlags = new boolean[numFiles];
      int realNumFiles = 0;

      for (int i = 0; i < srcNumFolders; i++) {
        // 1 - Folder Name Length (including null terminator)
        int folderNameLength = ByteConverter.unsign(src.readByte());
        fm.writeByte(folderNameLength);

        // X - Folder Name
        // 1 - null Folder Name Terminator
        fm.writeBytes(src.readBytes(folderNameLength));

        // for each file in this folder
        int numFilesInFolder = numFilesInFolders[i];
        for (int f = 0; f < numFilesInFolder; f++) {
          // 8 - Filename Hash
          fm.writeBytes(src.readBytes(8));

          Resource resource = resources[realNumFiles];
          int length = 0;
          if (resource.isReplaced()) {
            // we're storing replaced files as UNCOMPRESSED files

            // 4 - File Length (bit 30 = compression is the opposite of what is in the archive flags, bit 31 = internal check)
            length = (int) resource.getDecompressedLength();

            int srcLength = src.readInt();
            originalLengths[realNumFiles] = srcLength & 1073741823;
            boolean srcCompressed = (((srcLength >> 30) & 1) == 1);
            originalCompressedFlags[realNumFiles] = srcCompressed;

            if (embeddedNamesFlag) {
              // we still need to store the embedded name
              length += Integer.parseInt(resource.getProperty("EmbeddedNameLength")) + 1; // +1 for the filename length field
            }

            if (archiveCompressionFlag) {
              // we need to add the flag to this file, to say that this file is UNCOMPRESSED
              int lengthWithFlag = length | (1 << 30);
              fm.writeInt(lengthWithFlag);
            }
            else {
              fm.writeInt(length);
            }

          }
          else {
            // 4 - File Length (bit 30 = compression is the opposite of what is in the archive flags, bit 31 = internal check)
            int srcLength = src.readInt();
            length = srcLength & 1073741823;
            originalLengths[realNumFiles] = length;
            originalCompressedFlags[realNumFiles] = (((srcLength >> 30) & 1) == 1);

            fm.writeInt(srcLength);
          }

          // 4 - File Offset
          int srcOffset = src.readInt();
          resourceMap.put(srcOffset, realNumFiles);
          realNumFiles++;

          fm.writeInt(offset);

          offset += length;
        }
      }

      // for each filename
      //   X - Filename
      //   1 - null Filename Terminator
      fm.writeBytes(src.readBytes(srcFilenameDirLength));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        //Resource resource = resources[i];

        // Find the right resource details (length, filename length, etc) based on the offset
        //int srcOffset = (int) src.getOffset();
        //int resourceNum = resourceMap.get(srcOffset);
        int resourceNum = i;

        Resource resource = resources[resourceNum];

        int originalLength = originalLengths[resourceNum];
        boolean originalCompressedFlag = originalCompressedFlags[resourceNum];

        if (embeddedNamesFlag) {
          /*
          int filenameLength = Integer.parseInt(resources[resourceNum].getProperty("EmbeddedNameLength"));
          
          // 1 - Filename Length
          // X - Filename (including Folder Path)
          fm.writeBytes(src.readBytes(filenameLength));
          */

          int filenameLength = Integer.parseInt(resource.getProperty("EmbeddedNameLength"));
          fm.writeByte(filenameLength);

          String filename = resource.getProperty("EmbeddedName");
          fm.writeString(filename);

          src.skip(filenameLength + 1);

          originalLength -= (filenameLength + 1);

        }

        if (resource.isReplaced()) {
          // Replaced
          // We are storing replaced files UNCOMPRESSED

          if ((archiveCompressionFlag && !originalCompressedFlag) || !archiveCompressionFlag && originalCompressedFlag) {
            // 4 - Decompressed Length (skip in the SRC only)
            src.skip(4);

            originalLength -= 4;
          }

          write(resource, fm);

          src.skip(originalLength);
        }
        else {
          // original file

          if ((archiveCompressionFlag && !originalCompressedFlag) || !archiveCompressionFlag && originalCompressedFlag) {
            // 4 - Decompressed Length
            //fm.writeBytes(src.readBytes(4));
            fm.writeInt(resource.getDecompressedLength());
            src.skip(4);

            originalLength -= 4;
          }

          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);

          src.skip(originalLength);
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
