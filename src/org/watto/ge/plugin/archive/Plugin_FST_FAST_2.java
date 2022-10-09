/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FST_FAST_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FST_FAST_2() {

    super("FST_FAST_2", "FST_FAST_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Crazy Machines 2");
    setExtensions("fst"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ftexture", "FTexture Image", FileType.TYPE_IMAGE),
        new FileType("fsample", "FSample Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("FAST")) {
        rating += 50;
      }

      int version1 = fm.readShort();
      int version2 = fm.readShort();

      if (version1 == 0 && version2 == 3) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FAST)
      // 2 - Unknown (0)
      // 2 - Unknown (3)
      // 8 - Unknown
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      fm.skip(4);

      // 8 - Details Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - Number of Names
      int numNames = (int) fm.readLong();
      FieldValidator.checkNumFiles(numNames);

      // 8 - Names Directory Offset
      long nameDirOffset = fm.readLong();
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 8 - Number of File Types
      int numTypes = (int) fm.readLong();
      FieldValidator.checkNumFiles(numTypes);

      // 8 - File Types Directory Offset
      long typeDirOffset = fm.readLong();
      FieldValidator.checkOffset(typeDirOffset, arcSize);

      // 1 - Filename Length
      int filenameLength = ByteConverter.unsign(fm.readByte());

      // X - Filename (not including extension)
      fm.skip(filenameLength);

      long relativeOffset = fm.getOffset();

      String[] types = new String[numTypes];
      String[] names = new String[numNames];

      fm.seek(typeDirOffset);

      for (int i = 0; i < numTypes; i++) {
        // 4 - Type Name Length
        int length = fm.readInt();
        FieldValidator.checkFilenameLength(length);

        // 4 - Unknown (1)
        fm.skip(4);

        // X - Type Name (eg FMesh, FFont, FBlob, ...)
        String type = fm.readString(length);
        types[i] = type;
      }

      fm.seek(nameDirOffset);

      for (int i = 0; i < numNames; i++) {
        // 4 - Name Length
        int length = fm.readInt();
        FieldValidator.checkFilenameLength(length);

        // 4 - Unknown (1)
        fm.skip(4);

        // X - Name
        String name = fm.readString(length);
        names[i] = name;
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int offsetChange = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Name ID
        int nameID = fm.readInt();
        FieldValidator.checkRange(nameID, 0, numNames);
        String filename = names[nameID];

        // 4 - File Type ID
        int typeID = fm.readInt();
        FieldValidator.checkRange(typeID, 0, numTypes);
        filename += "." + types[typeID];

        // 8 - Unknown (-1)
        // 4 - null
        fm.skip(12);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        if (i == 0) { // sometimes the names in the header are wrong, so the offsets are all wrong by a factor
          int difference = (int) (offset - relativeOffset);
          if (difference > 0) {
            offsetChange = difference;
            ErrorLogger.log("[FST_FAST_2] Adjusting offset by " + offsetChange);
          }
        }
        offset -= offsetChange;

        // 8 - Compressed File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (0/10/16/..)
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Check for compression
      fm.getBuffer().setBufferSize(1);
      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());
        if (fm.readByte() == 120) { // ZLib Compression
          resource.setExporter(exporter);
          resource.setDecompressedLength(resource.getLength() * 250); // to trick the Viewer_FST_FAST_2_FTEXTURE to render a DXT image without existing early (as we don't actually know the decompressed length)
        }
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

      // 4 - Header (FAST)
      // 2 - Unknown (0)
      // 2 - Unknown (3)
      // 8 - Unknown
      // 4 - Number of Files
      // 4 - Unknown (1)
      fm.writeBytes(src.readBytes(24));

      // 8 - Details Directory Offset
      long dirOffset = src.readLong();
      fm.writeLong(dirOffset);

      // 8 - Number of Names
      int numNames = (int) src.readLong();
      fm.writeLong(numNames);

      // 8 - Names Directory Offset
      long nameDirOffset = src.readLong();
      fm.writeLong(nameDirOffset);

      // 8 - Number of File Types
      int numTypes = (int) src.readLong();
      fm.writeLong(numTypes);

      // 8 - File Types Directory Offset
      long typeDirOffset = src.readLong();
      fm.writeLong(typeDirOffset);

      // 1 - Filename Length
      int filenameLength = src.readByte();
      fm.writeByte(filenameLength);

      // X - Filename (not including extension)
      fm.writeBytes(src.readBytes(filenameLength));

      long offset = fm.getOffset(); // for writing the directory later...

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      long[] compressedLengths = new long[numFiles];
      ExporterPlugin exporterZLib = new Exporter_ZLib();
      ExporterPlugin exporterDefault = new Exporter_Default();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.isReplaced()) {
          // compress the changed
          compressedLengths[i] = write(exporterZLib, resource, fm);
        }
        else {
          // if it's compressed and unchanged, it keeps it compressed, and just copies byte-by-byte rather than recompressing 
          ExporterPlugin origExporter = resource.getExporter();
          resource.setExporter(exporterDefault);

          write(resource, fm);

          resource.setExporter(origExporter);
          compressedLengths[i] = resource.getLength();
        }
        TaskProgressManager.setValue(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // NAMES DIRECTORY
      long newNameDirOffset = fm.getOffset();

      src.seek(nameDirOffset);
      for (int i = 0; i < numNames; i++) {
        // 4 - Name Length
        int nameLength = src.readInt();
        fm.writeInt(nameLength);

        // 4 - Unknown (1)
        fm.writeBytes(src.readBytes(4));

        // X - Name
        fm.writeBytes(src.readBytes(nameLength));
      }

      // FILE TYPES DIRECTORY
      long newTypeDirOffset = fm.getOffset();

      src.seek(typeDirOffset);
      for (int i = 0; i < numTypes; i++) {
        // 4 - Type Name Length
        int nameLength = src.readInt();
        fm.writeInt(nameLength);

        // 4 - Unknown (1)
        fm.writeBytes(src.readBytes(4));

        // X - Type Name (eg FMesh, FFont, FBlob, ...)
        fm.writeBytes(src.readBytes(nameLength));
      }

      // DETAILS DIRECTORY
      long newDirOffset = fm.getOffset();

      src.seek(dirOffset);
      for (int i = 0; i < numFiles; i++) {
        long length = compressedLengths[i];

        // 4 - File Name ID
        // 4 - File Type ID
        // 8 - Unknown (-1)
        // 4 - null
        fm.writeBytes(src.readBytes(20));

        // 8 - File Offset
        src.skip(8);
        fm.writeLong(offset);

        // 8 - Compressed File Length
        src.skip(8);
        fm.writeLong(length);

        // 4 - Language ID? (0=German, 9=English, ...)
        fm.writeBytes(src.readBytes(4));

        offset += length;
      }

      // GO BACK TO THE BEGINNING AND WRITE THE CORRECT DIR OFFSETS
      fm.seek(24);

      // 8 - Details Directory Offset
      fm.writeLong(newDirOffset);

      fm.skip(8);

      // 8 - Names Directory Offset
      fm.writeLong(newNameDirOffset);

      fm.skip(8);

      // 8 - File Types Directory Offset
      fm.writeLong(newTypeDirOffset);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
