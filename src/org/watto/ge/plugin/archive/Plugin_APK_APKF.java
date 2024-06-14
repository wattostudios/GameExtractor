/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_APK_APKF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_APK_APKF() {

    super("APK_APKF", "APK_APKF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Spider-Man 3",
        "Kung Fu Panda");
    setExtensions("apk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("APKF")) {
        rating += 50;
      }

      if (fm.readLong() == 263) {
        rating += 5;
      }

      if (fm.readInt() == -1) {
        rating += 5;
      }

      if (fm.readInt() == 2) {
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (APKF)
      // 4 - Unknown (263)
      // 4 - null
      // 4 - Padding (-1)
      // 4 - Unknown (2)
      fm.skip(20);

      // 8 - Properties Directory Offset [+20]
      int propertiesOffset = fm.readInt() + 20;
      FieldValidator.checkOffset(propertiesOffset, arcSize);
      fm.skip(4);

      int maxNumTypes = 50; // guess max

      int numTypes = 0;
      int numFiles = 0;

      String[] types = new String[maxNumTypes];
      int[] numEntriesOfType = new int[maxNumTypes];

      // 4 - Data Type (TEX + null, MAT + null, MESH)
      String typeCode = fm.readNullString(4);
      while (typeCode != null && !typeCode.equals("")) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Offset To The First Entry Of This Type in the Details Directory (relative to this field offset)
        fm.skip(12);

        // 4 - Number Of Files of this type
        int numFilesOfType = fm.readInt();
        FieldValidator.checkNumFiles(numFilesOfType);

        // 4 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(8);

        types[numTypes] = typeCode;
        numEntriesOfType[numTypes] = numFilesOfType;
        numFiles += numFilesOfType;

        numTypes++;

        // Read next type code
        // 4 - Data Type (TEX + null, MAT + null, MESH)
        typeCode = fm.readNullString(4);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      //long filenameDirRelOffset = fm.getOffset();

      long[] filenameOffsets = new long[numFiles];
      long[] propertiesLengths = new long[numFiles];
      long[] fileLengths = new long[numFiles];

      // READ THE LENGTHS
      int realNumFiles = 0;
      long totalPropertiesLength = 0;
      for (int t = 0; t < numTypes; t++) {
        String type = types[t];
        int numEntries = numEntriesOfType[t];

        for (int i = 0; i < numEntries; i++) {
          long filenameDirRelOffset = fm.getOffset();
          // 4 - Filename Offset (relative to the start of this field)
          long filenameOffset = fm.readInt() + filenameDirRelOffset;
          FieldValidator.checkOffset(filenameOffset, arcSize);
          filenameOffsets[realNumFiles] = filenameOffset;

          // 4 - Hash?
          fm.skip(4);

          // 4 - Properties Directory Entry Length (TEX=68, MESH=244/808/404, MAT=704/160, ...)
          int propertiesLength = fm.readInt();
          FieldValidator.checkLength(propertiesLength, arcSize);

          // if (entry is TEX || entry is MESH){
          if (type.equals("TEX") || type.equals("MESH")) {
            // 4 - File Data Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);
            fileLengths[realNumFiles] = length;
          }

          if (type.equals("TEX")) {
            // TEX doesn't have padding, for some reason
          }
          else {
            // Properties are padded to a multiple of 16 bytes, but not for the last entry of each type!
            if (i != numEntries - 1) {
              propertiesLength += calculatePadding(propertiesLength, 16);
            }
          }

          propertiesLengths[realNumFiles] = propertiesLength;

          totalPropertiesLength += propertiesLength;

          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);
        }
      }

      // READ THE FILENAMES
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      // find the start of the actual property entries
      fm.relativeSeek(propertiesOffset);

      // 4 - Header (IMG + null)
      // 4 - null
      // 8 - Unknown (16)
      // 4 - Properties Directory Length
      // 4 - Unknown (36/32)
      // 4 - Header (PHYS)
      // 4 - null
      fm.skip(32);

      // 8 - First Entry Offset (relative to the start of the Properties Directory (64) [-16]
      propertiesOffset += fm.readInt() - 16;
      fm.skip(4);

      // 4 - File Data Length
      fm.skip(4);

      // 4 - Directory Length (including this field, but not including any of the fields before this one)
      long fileDataOffset = fm.getOffset() + fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // X - null Padding so the Offset is a multiple of 16 bytes
      propertiesOffset += calculatePadding(propertiesOffset, 16);

      // BUILD THE OFFSETS AND STORE THE FILES
      long fileDataOffsetOld = propertiesOffset + totalPropertiesLength;
      FieldValidator.checkOffset(fileDataOffsetOld, arcSize);

      long offset = fileDataOffset;
      long currentPropertiesOffset = propertiesOffset;
      realNumFiles = 0;
      for (int t = 0; t < numTypes; t++) {
        String type = types[t];
        int numEntries = numEntriesOfType[t];

        for (int i = 0; i < numEntries; i++) {
          String filename = names[realNumFiles] + "." + type;
          long propertiesLength = propertiesLengths[realNumFiles];

          if (type.equals("TEX") || type.equals("MESH")) {
            // We have a separate file stored in the File Data, so we want this entry to be the Properties first, followed by the file data
            long length = fileLengths[realNumFiles];

            long[] blockOffsets = new long[] { currentPropertiesOffset, offset };
            long[] blockLengths = new long[] { propertiesLength, length };

            BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterDefault, blockOffsets, blockLengths, blockLengths);

            FieldValidator.checkOffset(offset, arcSize); // check the offset is valid

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, offset, length, length, blockExporter);
            resource.addProperty("PropertiesLength", propertiesLength);
            resources[realNumFiles] = resource;
            realNumFiles++;

            TaskProgressManager.setValue(realNumFiles);

            offset += length;
          }
          else {
            // just store the data from the properties directory

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, currentPropertiesOffset, propertiesLength);
            realNumFiles++;
          }

          currentPropertiesOffset += propertiesLength;
        }
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
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
