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
import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.SubsetExporterWrapper;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RSB_RSB1 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RSB_RSB1() {

    super("RSB_RSB1", "RSB_RSB1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Bejeweled 3",
        "Zuma's Revenge");
    setExtensions("rsb"); // MUST BE LOWER CASE
    setPlatforms("ps3", "xbox 360");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("caf", "Core Audio File", FileType.TYPE_AUDIO),
        new FileType("ptx", "PTX Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("rsb1")) {
        rating += 50;
      }

      // Unknown (3)
      if (IntConverter.changeFormat(fm.readInt()) == 3) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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
  @SuppressWarnings("unused")
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

      // 4 - Header (1bsr)
      // 4 - Unknown (3)
      // 4 - null
      // 4 - File Data Offset
      // 4 - Unknown
      // 4 - Directory 0 Offset
      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Number of Entries in Directory 0
      fm.skip(44);

      // 4 - Directory 3 Offset
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown
      // 4 - Number of Directory 1 Entries
      // 4 - Directory 1 Offset
      // 4 - Directory 1 Entry Size (1156)
      // 4 - Directory 2 Length
      // 4 - Directory 2 Offset
      fm.skip(24);

      // 4 - Number of Directory 3 Entries
      int numGroups = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numGroups);

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      String[] groupNames = new String[numGroups];
      int[] groupOffsets = new int[numGroups];
      int[] groupLengths = new int[numGroups];
      for (int i = 0; i < numGroups; i++) {
        // 128 - Resource Group Name (null terminated, filled with nulls)
        String groupName = fm.readNullString(128);
        groupNames[i] = groupName;

        // 4 - Resource Group Data Offset
        int groupOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(groupOffset, arcSize);
        groupOffsets[i] = groupOffset;

        // 4 - Resource Group Data Length (including Padding)
        int groupLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(groupLength, arcSize);
        groupLengths[i] = groupLength;

        // 4 - Resource Group ID (incremental from 0)
        // 4 - Unknown (1)
        // 4 - Header Padding Multiple? (4096)
        // 4 - Resource Group Data Padding Multiple? (4096)
        // 12 - null
        // 4 - Resource Group Data Padding? (4096)
        // 4 - Compressed Resource Group Data Length (including Padding after the resource group)
        // 4 - Decompressed Resource Group Data Length
        // 20 - null
        // 4 - Unknown (1)
        // 4 - Resource Group ID (incremental from 0)
        fm.skip(68);
      }

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numGroups; i++) {
        long groupDirOffset = groupOffsets[i];

        fm.seek(groupDirOffset);
        //System.out.println("Group:\t" + groupDirOffset + "\t" + groupNames[i]);

        long relDataOffset = groupDirOffset;

        // 4 - Resource Group Header (pgsr)
        // 4 - Unknown (3)
        // 8 - null
        // 4 - Unknown (1)
        fm.skip(20);

        // 4 - Data Offset? (4096)
        relDataOffset += IntConverter.changeFormat(fm.readInt());

        // 4 - Data Offset? (4096)
        fm.skip(4);

        // 4 - Decompressed Data Length? (null if data is compressed)
        int decompBlockLength = IntConverter.changeFormat(fm.readInt());

        // 4 - Decompressed Data Length? (null if data is compressed)
        // 4 - null
        // 4 - File Data Offset (4096)
        fm.skip(12);

        // 4 - Compressed File Data Length (including Padding after the file)
        int compressedBlockLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(compressedBlockLength, arcSize);

        // 4 - Decompressed File Data Length
        int decompressedBlockLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompressedBlockLength);

        // 20 - null
        fm.skip(20);

        // 4 - Sub-File Directory Data Length
        int groupDataLength = IntConverter.changeFormat(fm.readInt());
        int groupEndOffset = groupDataLength;
        FieldValidator.checkLength(groupEndOffset, arcSize);

        // 4 - Sub-File Directory Offset (relative to the start of this file entry)
        groupDirOffset += IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(groupDirOffset, arcSize);

        groupEndOffset += groupDirOffset;
        FieldValidator.checkOffset(groupEndOffset, arcSize);

        // 4 - null
        // 4 - null
        // 4 - null
        fm.seek(groupDirOffset);

        String[] reuseNames = new String[50];
        int numReuseNames = 0;
        int[] reuseOffsets = new int[50];
        String currentReuseName = "";

        while (fm.getOffset() < groupEndOffset) {

          // first, go through and check expiry of short names
          long nameOffset = fm.getOffset();
          for (int n = 0; n < numReuseNames; n++) {
            if (reuseOffsets[n] == nameOffset) {
              numReuseNames = n + 1;
              break;
            }
          }
          if (numReuseNames != 0) {
            currentReuseName = reuseNames[numReuseNames - 1];
          }

          // X - Filename (if this is the first file in this resource group, it also includes the directory path) (each character consumes 4 bytes)
          // 4 - null Filename Terminator
          byte[] filenameBytes = new byte[512];
          int filenamePos = 0;

          for (int p = 0; p < 512; p++) { // 512 as a maximum filename length (guessed)
            byte[] bytes4 = fm.readBytes(4);
            if (bytes4[3] == 0) {
              break;
            }
            else {
              filenameBytes[filenamePos] = bytes4[3];
              if (bytes4[2] != 0) {
                // everything after this point will be replaced at a future point in time

                // get the filename until this point
                byte[] shortNameBytes = new byte[filenamePos];
                System.arraycopy(filenameBytes, 0, shortNameBytes, 0, filenamePos);
                String shortName = new String(shortNameBytes);
                shortName = currentReuseName + shortName; // prepend the existing short name

                // work out when it will be replaced
                byte[] expireOffsetBytes = new byte[] { bytes4[2], bytes4[1], bytes4[0], 0 };
                int expireOffset = IntConverter.convertLittle(expireOffsetBytes) * 4;
                expireOffset += groupDirOffset;

                // store the short name and the expiry offset
                reuseNames[numReuseNames] = shortName;
                reuseOffsets[numReuseNames] = expireOffset;
                numReuseNames++;
              }
              filenamePos++;
            }
          }
          byte[] oldBytes = filenameBytes;
          filenameBytes = new byte[filenamePos];
          System.arraycopy(oldBytes, 0, filenameBytes, 0, filenamePos);
          String filename = new String(filenameBytes);
          filename = currentReuseName + filename;  // prepend the existing short name

          //System.out.println("\t" + filename);

          // 4 - Compression Flag (1=compressed, 0=uncompressed)
          int compressed = IntConverter.changeFormat(fm.readInt());

          if (compressed == 1) {
            // zlib compression

            // if the whole resource group is compressed, we need to decompress the whole resource group first, to a temporary file, so
            // we can point to the actual 'individual' files within it

            // 4 - File Data Offset (relative to the start of the File Data for the files in this resource group)
            int relFileOffset = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkOffset(relFileOffset, arcSize);

            //   4 - Decompressed File Data Length
            int decompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(decompLength);

            //System.out.println(fm.getOffset());
            //   4 - Unknown (Image Format, if this resource is a PTX image (0=BARG4444, 1=RGB565))
            int imageFormat = IntConverter.changeFormat(fm.readInt());

            //   8 - null
            fm.skip(8);

            //   4 - Unknown (Image Width, if this resource is a PTX image)
            int imageWidth = IntConverter.changeFormat(fm.readInt());

            //   4 - Unknown (Image Height, if this resource is a PTX image)
            int imageHeight = IntConverter.changeFormat(fm.readInt());

            if (decompLength == decompressedBlockLength) {
              // a single file has been compressed - nothing fancy

              int offset = (int) (relDataOffset);
              FieldValidator.checkOffset(offset, arcSize);

              int compLength = groupDataLength;
              FieldValidator.checkLength(compLength, arcSize);

              //path,name,offset,length,decompLength,exporter
              Resource resource = new Resource(path, filename, offset, compLength, decompLength, exporter);
              resource.addProperty("Width", "" + imageWidth);
              resource.addProperty("Height", "" + imageHeight);
              resource.addProperty("ImageFormat", "ARGB");

              resources[realNumFiles] = resource;

            }
            else {
              // A single compressed file with multiple individual files within it

              //exporter,groupOffset,groupCompLength,groupDecompLength,offsetInGroupToFile,decompFileSizeInGroup
              SubsetExporterWrapper exporterGroup = new SubsetExporterWrapper(exporter, relDataOffset, compressedBlockLength, decompressedBlockLength, relFileOffset + decompBlockLength, decompLength);
              exporterGroup.setRawDataLengthAtStart(decompBlockLength);

              //if (relDataOffset == 42778624) {
              //System.out.println(relFileOffset);
              //System.out.println("L" + fm.getOffset());
              //}

              imageWidth = (decompLength / 4 / imageHeight);

              //path,name,offset,length,decompLength,exporter
              Resource resource = new Resource(path, filename, relDataOffset + relFileOffset + decompBlockLength, decompLength, decompLength, exporterGroup);
              resource.addProperty("Width", "" + imageWidth);
              resource.addProperty("Height", "" + imageHeight);
              resource.addProperty("ImageFormat", "ARGB");

              resources[realNumFiles] = resource;
            }

            realNumFiles++;

            TaskProgressManager.setValue(relDataOffset);
          }
          else if (compressed == 0) {
            // not compressed

            //if (relDataOffset == 42778624) {
            //  System.out.println("ZK" + fm.getOffset());
            //}

            // 4 - File Data Offset (relative to the start of the File Data for the files in this resource group)
            int offset = (int) (IntConverter.changeFormat(fm.readInt()) + relDataOffset);
            FieldValidator.checkOffset(offset, arcSize);

            //   4 - File Data Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
          else {
            ErrorLogger.log("[RSB_RSB1]: Unknown compression: " + compressed);
          }

        }

      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
