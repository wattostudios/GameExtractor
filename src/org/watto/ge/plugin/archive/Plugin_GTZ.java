/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GTZ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GTZ() {

    super("GTZ", "GTZ");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("From Dusk Till Dawn");
    setExtensions("gtz"); // MUST BE LOWER CASE
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

      if (fm.readInt() == 1) {
        rating += 5;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readInt() == 1) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

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

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false, 256); // small fast reads

      long arcSize = fm.getLength();

      // 4 - Unknown (1)
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      readDirectory(path, fm, resources, "", arcSize);

      resources = resizeResources(resources, realNumFiles);

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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String baseDirName, long arcSize) {
    try {
      ExporterPlugin exporterZlib = Exporter_ZLib.getInstance();

      //System.out.println("Dir at " + fm.getOffset() + "\t" + baseDirName);

      // 4 - Number of Files in this Directory
      int numFilesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInDir + 1); // +1 to allow nulls

      // 4 - Number of Sub-Directories in this Directory
      int numFoldersInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFoldersInDir + 1); // +1 to allow nulls

      // 1 - Directory Name Length
      int nameLength = ByteConverter.unsign(fm.readByte());

      // X - Directory Name
      String dirName = fm.readString(nameLength);

      // read the files in this directory

      for (int i = 0; i < numFilesInDir; i++) {
        // 1 - Unknown (2)
        fm.skip(1);

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = baseDirName + dirName + File.separatorChar + fm.readString(filenameLength);

        // 1 - null
        // 4 - Hash?
        fm.skip(5);

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Data Offset
        fm.skip(4);

        // X - File Data (ZLib Compression)
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporterZlib);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      // read the folders in this directory

      for (int i = 0; i < numFoldersInDir; i++) {
        readDirectory(path, fm, resources, baseDirName + dirName + File.separatorChar, arcSize);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  long[] compOffsetPos = new long[0];

  long[] compLengths = new long[0];

  int replacedFilesCount = 0;

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

      // RESET GLOBALS
      realNumFiles = 0;
      compOffsetPos = new long[numFiles];
      compLengths = new long[numFiles];
      replacedFilesCount = 0;

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // 4 - Unknown (1)
      fm.writeBytes(src.readBytes(4));

      replaceDirectory(resources, path, fm, src);

      // go back and write the compressed lengths
      for (int i = 0; i < replacedFilesCount; i++) {
        fm.seek(compOffsetPos[i]);
        fm.writeInt(compLengths[i]);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public void replaceDirectory(Resource[] resources, File path, FileManipulator fm, FileManipulator src) {
    try {

      ExporterPlugin exporterZlib = Exporter_ZLib.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Number of Files in this Directory
      int numFilesInDirectory = src.readInt();
      fm.writeInt(numFilesInDirectory);

      // 4 - Number of Sub-Directories in this Directory
      int numFoldersInDirectory = src.readInt();
      fm.writeInt(numFoldersInDirectory);

      // 1 - Directory Name Length
      int nameLength = ByteConverter.unsign(src.readByte());
      fm.writeByte(nameLength);

      // X - Directory Name
      fm.writeBytes(src.readBytes(nameLength));

      for (int i = 0; i < numFilesInDirectory; i++) {

        Resource resource = resources[realNumFiles];
        long length = resource.getDecompressedLength();

        // 1 - Unknown (2)
        fm.writeBytes(src.readBytes(1));

        // 1 - Filename Length
        nameLength = ByteConverter.unsign(src.readByte());
        fm.writeByte(nameLength);

        // X - Filename
        fm.writeBytes(src.readBytes(nameLength));

        // 1 - null
        fm.writeByte(0);
        src.skip(1);

        // 4 - Hash?
        fm.writeBytes(src.readBytes(4));

        if (resource.isReplaced()) {
          // replaced

          // 4 - Compressed Length
          compOffsetPos[replacedFilesCount] = fm.getOffset();

          fm.writeInt(length); // will come back and write the compressed offset here later
          int originalLength = src.readInt();

          // 4 - Decompressed Length
          fm.writeInt(length);
          src.skip(4);

          // 4 - File Data Offset
          src.skip(4);
          fm.writeInt(fm.getOffset());

          // X - File Data (ZLib Compression)
          long compLength = write(exporterZlib, resource, fm);
          compLengths[replacedFilesCount] = compLength;

          replacedFilesCount++;

          src.skip(originalLength);
        }
        else {
          // original

          // 4 - Compressed Length
          // 4 - Decompressed Length
          fm.writeBytes(src.readBytes(8));

          // 4 - File Data Offset
          src.skip(4);
          fm.writeInt(fm.getOffset());

          // X - File Data (ZLib Compression)
          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);

          src.skip(resource.getLength());
        }

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;

      }

      for (int i = 0; i < numFoldersInDirectory; i++) {
        replaceDirectory(resources, path, fm, src);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
