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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HOGG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HOGG() {

    super("HOGG", "HOGG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Champions Online",
        "Pirates of the Burning Sea");
    setExtensions("hogg", "pig"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("adb", "Avatar Database", FileType.TYPE_OTHER),
    //    new FileType("ade", "Avatar Appearance", FileType.TYPE_OTHER),
    //    new FileType("apl", "Avatar Database", FileType.TYPE_OTHER));

    setTextPreviewExtensions("adb", "ade", "apl", "cminfo", "danim", "deps", "fmod_dsp", "font", "fontsettings", "fp", "fph", "hdo", "hlsl", "itl", "light", "lightingmodel", "manifest", "materialprofile", "modinfo", "ms", "mtl", "obj", "op", "phl", "rminfo", "texword", "trc", "unused", "vhl", "vp", "zeni_snap"); // LOWER CASE

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
      byte[] headerBytes = fm.readBytes(8);
      if (headerBytes[0] == 13 && ByteConverter.unsign(headerBytes[1]) == 240 && ByteConverter.unsign(headerBytes[2]) == 173 && ByteConverter.unsign(headerBytes[3]) == 222 && headerBytes[4] == 10 && headerBytes[5] == 0 && headerBytes[6] == 0 && headerBytes[7] == 4) {
        rating += 50;
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

      fm.skip(4);

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 8 - Header (13,240,173,222,10,0,0,4)
      fm.skip(8);

      // 4 - Details Directory Length (including padding)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Decompression Directory Length (including padding)
      int decompDirLength = fm.readInt();
      FieldValidator.checkLength(decompDirLength, arcSize);

      // 4 - null
      fm.skip(4);

      // 4 - Filename Directory Length (including padding)
      int nameDirLength = fm.readInt();
      FieldValidator.checkLength(nameDirLength, arcSize);

      // 1024 - null Padding
      fm.skip(1024);

      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int dirPieceLength = 64500;
      int detailsDirOffset = 65560;

      if (FilenameSplitter.getExtension(path).equalsIgnoreCase("pig")) {
        // uses smaller offsets
        dirPieceLength = 3084;
        detailsDirOffset = 4144;
      }

      //
      //
      // READ IN THE DIRECTORY PIECE, PROCESSED AT THE END
      //
      //
      byte[] dirPiece = fm.readBytes(dirPieceLength);

      //
      //
      // PROCESS THE ARCHIVE AS NORMAL
      //
      //

      // X - null Padding to offset 65560
      fm.seek(detailsDirOffset);

      // Loop through directory
      long endDirectory = fm.getOffset() + dirLength;
      while (fm.getOffset() < endDirectory) {
        // 8 - File Data Offset
        long offset = fm.readLong();
        /*
        if (offset == 0) {
          // no more files
          break;
        }
        */
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        if (length == -1) {
          // no more files
          break;
        }
        FieldValidator.checkLength(length, arcSize);

        // 8 - Hash/CRC?
        // 4 - null
        // 4 - Unknown (65534)
        fm.skip(16);

        // 4 - File ID (incremental from 0)
        int fileID = fm.readInt();
        FieldValidator.checkRange(fileID, 0, numFiles);

        /*
        String filename = names[fileID];
        if (filename == null || filename.length() <= 0) {
          filename = Resource.generateFilename(fileID);
        }
        */
        String filename = Resource.generateFilename(fileID);
        //System.out.println(fileID + "\tname");

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      fm.seek(endDirectory);

      // Loop through directory
      endDirectory = fm.getOffset() + decompDirLength;
      for (int i = 0; i < realNumFiles; i++) {
        // 4 - File ID (incremental from 0)
        /*
        int fileID = fm.readInt();
        FieldValidator.checkRange(fileID, 0, numFiles);
        System.out.println(fileID + "\tdecomp");
        */
        fm.skip(4);
        int fileID = i;

        // 4 - Unknown (-1)
        fm.skip(4);

        // 4 - Decompressed Length (0 if not a compressed file)
        int decompLength = fm.readInt();
        if (decompLength != 0) {
          FieldValidator.checkLength(decompLength);

          Resource resource = resources[fileID];
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
        }

        // 4 - null
        fm.skip(4);
      }

      resources = resizeResources(resources, realNumFiles);

      //
      //
      // Now, File#1 is the DataList, so extract it and process it for all the filenames
      //
      //
      Resource dataListResource = resources[0];
      int dataListLength = (int) dataListResource.getLength();
      int dataListDecompLength = (int) dataListResource.getDecompressedLength();

      if (dataListLength == dataListDecompLength) {
        // dataList isn't compressed - still want to extract it and read over it, so the code further down still works without modification
        fm.seek(dataListResource.getOffset());

        byte[] dataListDecompBytes = fm.readBytes(dataListDecompLength);

        fm.close();
        fm = new FileManipulator(new ByteBuffer(dataListDecompBytes));
      }
      else {
        // dataList is ZLib compressed - need to decompress it

        fm.close();

        exporter.open(dataListResource);
        byte[] dataListDecompBytes = new byte[dataListDecompLength];
        for (int i = 0; i < dataListDecompLength; i++) {
          if (exporter.available()) {
            dataListDecompBytes[i] = (byte) exporter.read();
          }
        }
        exporter.close();

        fm = new FileManipulator(new ByteBuffer(dataListDecompBytes));
      }

      // 4 - null
      // 4 - Number of Filenames?
      // 4 - DataList Name Length (10)
      // 10 - DataList Name ("?DataList" + null);
      fm.skip(22);

      // Loop through directory
      int fileNum = 1; // start at 1, so we skip the DataList file at offset 0
      int numFilenamesRemaining = realNumFiles - 1; // so we know how many to read from the dirPiece later on. -1 to skip the DataList file
      while (fm.getOffset() < dataListDecompLength) {
        //System.out.println(fm.getOffset());
        if (numFilenamesRemaining <= 0) {
          break;
        }

        // 4 - Filename Length (including null)
        int filenameLength = fm.readInt();

        if (filenameLength == 0) {// Pirates --> shared.pig

          String filename = Resource.generateFilename(fileNum);

          Resource resource = resources[fileNum];
          resource.setName(filename);
          resource.setOriginalName(filename);
          fileNum++;
          numFilenamesRemaining--;

          continue;
        }

        // Here, need to read the next 4 bytes as a number, but don't move the file pointer.
        // This is needed to determine whether this entry is a filename or file properties.
        // If these 4 bytes have the same value as the FilenameLength, then this entry is
        // file properties, with length FilenameLength. Otherwise, these 4 bytes are the
        // first 4 characters of the filename.
        byte[] testBytes = fm.readBytes(4);
        int testInt = IntConverter.convertLittle(testBytes);
        if (testInt == filenameLength) {
          // X - File Properties (Varying in structure. Length=FilenameLength)
          fm.skip(testInt - 4);

          //System.out.println(">\tPADDING");
        }
        else {

          FieldValidator.checkFilenameLength(filenameLength);
          // X - Filename
          // 1 - null Filename Terminator
          String filename = StringConverter.convertLittle(testBytes) + fm.readNullString(filenameLength - 4);

          Resource resource = resources[fileNum];
          resource.setName(filename);
          resource.setOriginalName(filename);
          fileNum++;
          numFilenamesRemaining--;

          //System.out.println(filename);
        }
      }

      fm.close();

      //
      //
      // Now go back to the beginning, start processing the rest of the directory at the start of the archive
      //
      //

      fm = new FileManipulator(new ByteBuffer(dirPiece));
      while (fm.getOffset() < dirPieceLength) { // 64500 is the max size of the dir piece
        if (numFilenamesRemaining <= 0) {
          // early exit
          break;
        }
        //System.out.println(fm.getOffset());

        // 1 - Unknown (1)
        int entryType = fm.readByte();
        if (entryType != 1) {
          // early exit
          break;
        }

        // 4 - File ID
        //int fileID = fm.readInt();
        fm.skip(4);

        // 4 - Filename Length (including null)
        int filenameLength = fm.readInt();

        // Here, need to read the next 4 bytes as a number, but don't move the file pointer.
        // This is needed to determine whether this entry is a filename or file properties.
        // If these 4 bytes have the same value as the FilenameLength, then this entry is
        // file properties, with length FilenameLength. Otherwise, these 4 bytes are the
        // first 4 characters of the filename.
        byte[] testBytes = fm.readBytes(4);
        int testInt = IntConverter.convertLittle(testBytes);
        if (testInt == filenameLength) {
          // X - File Properties (Varying in structure. Length=FilenameLength)
          fm.skip(testInt - 4);

          //System.out.println(">\tPADDING");
        }
        else {

          FieldValidator.checkFilenameLength(filenameLength);
          // X - Filename
          // 1 - null Filename Terminator
          String filename = StringConverter.convertLittle(testBytes) + fm.readNullString(filenameLength - 4);

          Resource resource = resources[fileNum];
          resource.setName(filename);
          resource.setOriginalName(filename);
          fileNum++;
          numFilenamesRemaining--;

          //System.out.println(filename);
        }

      }

      fm.close();

      //
      //
      // Finally, set the Datalist filename
      //
      //
      String dataListFilename = "dataList.dir";
      dataListResource.setName(dataListFilename);
      dataListResource.setOriginalName(dataListFilename);

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
