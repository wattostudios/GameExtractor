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
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_G3V0_2 extends ArchivePlugin {

  int realNumFiles = 0;
  int nextEntryType = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_G3V0_2() {

    super("PAK_G3V0_2", "PAK_G3V0_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Risen");
    setExtensions("pak");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
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

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("G3V0")) {
        rating += 50;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Archive Length
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown (1)
      // 4 - Header (G3V0)
      // 8 - null
      // 8 - Unknown (1)
      // 8 - File Data Offset
      fm.skip(32);

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - Archive Length
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      nextEntryType = 16;

      while (fm.getOffset() < arcSize - 4) {
        readDirectory(resources, fm, "", path, arcSize);
      }

      resources = resizeResources(resources, realNumFiles);

      // Now go through and change all the sounds to MP3s
      fm.getBuffer().setBufferSize(24);
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        String filename = resource.getName();
        if (FilenameSplitter.getExtension(filename).equals("_xsnd")) {
          long offset = resource.getOffset();
          fm.seek(offset);

          // 8 - File Header
          if (fm.readString(8).equals("GR01SN04")) {
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(8);

            // 4 - File Offset
            int relativeOffset = fm.readInt();
            FieldValidator.checkLength(relativeOffset, arcSize);

            long realLength = resource.getLength() - relativeOffset;
            long realOffset = offset + relativeOffset;

            resource.setOffset(realOffset);
            resource.setLength(realLength);
            resource.setDecompressedLength(realLength);

            filename += ".mp3";
            resource.setName(filename);
            resource.setOriginalName(filename);
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readDirectory(Resource[] resources, FileManipulator fm, String parentDirName, File path, long arcSize) throws Exception {

    ExporterPlugin exporter = Exporter_ZLib.getInstance();

    // 4 - Filename Length (not including null)
    int filenameLength = fm.readInt();
    FieldValidator.checkFilenameLength(filenameLength + 1); // +1 to allow for the root, which is empty

    // X - Filename
    String filename = fm.readString(filenameLength);

    // 1 - null Filename Terminator
    if (filenameLength != 0) {
      fm.skip(1);
    }

    long offset = 0;
    if (nextEntryType == 32) {
      // 8 - File Offset
      offset = fm.readLong();
      FieldValidator.checkOffset(offset, arcSize);
    }

    // 24 - Unknown
    fm.skip(24);

    // 2 - Entry Type (16=dir, 32/33/128=file)
    int entryType = fm.readShort();
    if (entryType != 16) {
      entryType = 32;
    }

    // 2 - Unknown (2=Compressed OR Directory, 6=Decompressed)
    fm.skip(2);

    if (entryType == 16) {
      // directory

      if (filenameLength != 0) { // so we don't add it to the root
        filename += "\\";
      }

      // 4 - Number of Entries in this Directory
      int numEntriesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numEntriesInDir + 1); // +1 to allow for zero sub-directories

      // 1 - Next Entry Type (16=dir, 32/33/128=file) (so you can know whether to read the FileOffset or not, in the next entry)
      nextEntryType = ByteConverter.unsign(fm.readByte());
      if (nextEntryType != 16) {
        nextEntryType = 32;
      }

      // 1 - Unknown (0/8)
      // 2 - Unknown (2)
      fm.skip(3);

      String dirName = parentDirName + filename;

      for (int i = 0; i < numEntriesInDir; i++) {
        readDirectory(resources, fm, dirName, path, arcSize);
      }

    }
    else if (entryType == 32) {
      // file

      // 4 - Unknown (0)
      // 4 - Unknown (2)
      fm.skip(8);

      // 4 - Compressed File Length
      int length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Decompressed File Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // 1 - Next Entry Type (16=dir, 32/33/128=file) (so you can know whether to read the FileOffset or not, in the next entry)
      nextEntryType = ByteConverter.unsign(fm.readByte());
      if (nextEntryType != 16) {
        nextEntryType = 32;
      }

      // 1 - Unknown (0/8)
      // 2 - Unknown (2)
      fm.skip(3);

      filename = parentDirName + filename;

      /*
      if (FilenameSplitter.getExtension(filename).equals("_xsnd")) {
        offset += 191;
        length -= 191;
        decompLength -= 191;
        filename += ".mp3";
      }
      */

      //path,name,offset,length,decompLength,exporter
      if (length != decompLength) {
        // compressed
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
      }
      else {
        // not compressed
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
      }

      TaskProgressManager.setValue(realNumFiles);
      realNumFiles++;

    }
    else {
      throw new WSPluginException("Invalid Entry Type: " + entryType);
    }

  }

}
