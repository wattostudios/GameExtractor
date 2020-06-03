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
import org.watto.ge.plugin.exporter.Exporter_Custom_DLL_MZ_BMP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.BooleanArrayConverter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DLL_MZ extends ArchivePlugin {

  int realNumFiles = 0;
  int resourceOffset = 0;

  long furthestOffset = 0;
  long earliestOffset = 0;

  ExporterPlugin exporterBMP = Exporter_Custom_DLL_MZ_BMP.getInstance();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DLL_MZ() {

    super("DLL_MZ", "DLL_MZ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("DLL/EXE Program Resources");
    setExtensions("dll", "exe", "msstyles");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long dirOffset) throws Exception {
    fm.seek(dirOffset);
    long arcSize = fm.getLength();

    // 4 - Characteristics? (null)
    // 4 - Timestamp? (null)
    // 4 - Version Number? (null)
    fm.skip(12);

    // 2 - Number Of Names Entries
    int numNames = fm.readShort();

    // 2 - Number Of ID Entries (Different Languages?)
    int numIDs = fm.readShort();

    int numFiles = numNames + numIDs;
    FieldValidator.checkNumFiles(numFiles);

    // Loop through directory
    for (int i = 0; i < numFiles; i++) {
      // 4 - Resource Name OR ID (ignore bit#32)
      String filename = "";
      byte[] resNameBytes = fm.readBytes(4);
      boolean[] typeByteBits = BooleanArrayConverter.convertLittle(resNameBytes[3]);
      //System.out.println(typeByteBits[0] + " " + typeByteBits[1] + " " + typeByteBits[2] + " " + typeByteBits[3] + " " + typeByteBits[4] + " " + typeByteBits[5] + " " + typeByteBits[6] + " " + typeByteBits[7]);
      if (typeByteBits[0]) {
        // Offset To Resource Name (relative to the start of the resource table)
        typeByteBits[0] = false;
        resNameBytes[3] = ByteConverter.convertLittle(typeByteBits);

        long filenameOffset = IntConverter.convertLittle(resNameBytes) + resourceOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);

        long curPos = fm.getOffset();
        fm.seek(filenameOffset);

        // 2 - Filename Length [*2 for unicode];
        int filenameLength = fm.readShort() * 2;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        filename = new String(fm.readBytes(filenameLength), "UTF-16LE");

        long currentOffset = fm.getOffset();
        if (currentOffset > furthestOffset) { // for calculating the real offsets later on
          furthestOffset = currentOffset;
        }

        fm.seek(curPos);
      }
      else {
        // The first 2 bytes are the resource ID, the last 2 bytes are null
        filename = "" + ShortConverter.convertLittle(new byte[] { resNameBytes[0], resNameBytes[1] });
      }

      filename = dirName + filename.replace('_', '.');

      // 4 - Offset To Resource (relative to the start of the resource table)
      byte[] offDetailsBytes = fm.readBytes(4);
      boolean[] offTypeByteBits = BooleanArrayConverter.convertLittle(offDetailsBytes[3]);
      if (offTypeByteBits[0]) {
        // offset points to a nested resource table (repeat from "//RESOURCE TABLE")
        offTypeByteBits[0] = false;
        offDetailsBytes[3] = ByteConverter.convertLittle(offTypeByteBits);

        long nestOffset = IntConverter.convertLittle(offDetailsBytes) + resourceOffset;
        FieldValidator.checkOffset(nestOffset, arcSize);

        long curPos = fm.getOffset();
        analyseDirectory(fm, path, resources, filename + "/", nestOffset);
        fm.seek(curPos);
      }
      else {
        // offset points to the details for this resource
        offTypeByteBits[0] = false;
        offDetailsBytes[3] = ByteConverter.convertLittle(offTypeByteBits);

        long detailsOffset = IntConverter.convertLittle(offDetailsBytes) + resourceOffset;
        FieldValidator.checkOffset(detailsOffset, arcSize);

        long curPos = fm.getOffset();
        fm.seek(detailsOffset);

        // 4 - File Offset
        long offset = fm.readInt();
        //FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - null
        //fm.skip(8);

        fm.seek(curPos);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);

        if (offset < earliestOffset) { // for calculating the real file offsets later on
          earliestOffset = offset;
        }

        if (filename.toLowerCase().indexOf(".bmp") > 0) {
          resource.setExporter(exporterBMP);
          resource.setExtension("bmp");
          resource.setOriginalName(resource.getName()); // so it doesn't think the file has been renamed
        }
        else if (filename.toLowerCase().indexOf(".ini") > 0) {
          resource.setExtension("ini");
          resource.setOriginalName(resource.getName()); // so it doesn't think the file has been renamed
        }
        else if (filename.indexOf("CABINET") > 0) {
          // This is a Cabinet 3.0 embedded in the EXE, rather than a newer CAB which is stored at the end of the EXE
          filename = path.getName() + ".cab";
          resource.setName(filename);
          resource.setOriginalName(filename); // so it doesn't think the file has been renamed
        }

        resources[realNumFiles] = resource;

        TaskProgressManager.setValue(fm.getOffset());
        realNumFiles++;
      }

    }

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
      if (fm.readString(2).equals("MZ")) {
        rating += 50;
      }

      fm.skip(58);

      long arcSize = fm.getLength();

      // PE Header Offset
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

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;
      resourceOffset = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      // 2 - Header (MZ)
      // 2 - Number of Fields following??? (3)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(60);

      // 4 - Offset to PE Header
      int peOffset = fm.readInt();
      FieldValidator.checkOffset(peOffset, arcSize);

      // X - MS-DOS Program
      fm.seek(peOffset);

      // PE HEADER

      // 4 - Header (PE)
      fm.skip(4);

      // IMAGE_FILE_HEADER
      // 2 - Target Platform (332 = 32-bit Intel)
      fm.skip(2);

      // 2 - Number Of Section Tables
      short numSections = fm.readShort();
      FieldValidator.checkNumFiles(numSections);

      // 4 - Created Timestamp
      // 4 - Offset To Symbol Table (usually null)
      // 4 - Number Of Symbols In The Symbol Table
      fm.skip(12);

      // 2 - Length of IMAGE_OPTIONAL_HEADER
      int optHeaderLength = fm.readShort();
      FieldValidator.checkLength(optHeaderLength, arcSize);

      // 2 - Characteristics (EXE Attributes)
      fm.skip(optHeaderLength + 2);

      // SECTION TABLES
      int resourceLength = 0; // length of the resources section
      int furthestSectionOffset = 0; // the furthest section in the EXE file
      int furthestSectionLength = 0; // the furthest section in the EXE file
      for (int i = 0; i < numSections; i++) {
        // 8 - Section Name (null terminated) (.rsrc = Resource)
        String sectionName = fm.readNullString(8);
        if (sectionName.equals(".rsrc")) {
          // found it - just store the details for now, as we want to check other things later before iterating through the resources directory
          // 4 - Virtual Length (length of data - kinda)
          // 4 - Virtual Address (offset to data - kinda)
          fm.skip(8);

          // 4 - Length Of Raw Data (multiple of FileAlignment)
          resourceLength = fm.readInt();
          FieldValidator.checkLength(resourceLength);

          // 4 - Offset To Raw Data (multiple of FileAlignment)
          resourceOffset = fm.readInt();
          FieldValidator.checkOffset(resourceOffset, arcSize);

          TaskProgressManager.setMaximum(resourceLength + resourceOffset);

          if (resourceOffset > furthestSectionOffset) {
            furthestSectionOffset = resourceOffset;
            furthestSectionLength = resourceLength;
          }

          // 4 - Offset To Relocations
          // 4 - Offset To Line Numbers
          // 2 - Number Of Relocations
          // 2 - Number Of Line Numbers
          // 4 - Characteristics
          fm.skip(16);
        }
        else {
          // ignore - only want resources
          // However, we need to still find out if this is the furthest section in the EXE...

          // 4 - Virtual Length (length of data - kinda)
          // 4 - Virtual Address (offset to data - kinda)
          fm.skip(8);

          // 4 - Length Of Raw Data (multiple of FileAlignment)
          int currentLength = fm.readInt();

          // 4 - Offset To Raw Data (multiple of FileAlignment)
          int currentOffset = fm.readInt();

          if (currentOffset > furthestSectionOffset) {
            furthestSectionOffset = currentOffset;
            furthestSectionLength = currentLength;
          }

          // 4 - Offset To Relocations
          // 4 - Offset To Line Numbers
          // 2 - Number Of Relocations
          // 2 - Number Of Line Numbers
          // 4 - Characteristics
          fm.skip(16);
        }
      }

      // Now that we have found the end of the EXE file, look to see whether there is a resource stuck there.
      // ie for Self-Extracting EXE files, the Microsoft Cabinet CAB file is here.
      if (furthestSectionOffset > 0) {
        int endOfExe = furthestSectionOffset + furthestSectionLength;
        if (endOfExe < arcSize) {
          // go to the end of the EXE
          fm.seek(endOfExe);

          long appendedOffset = fm.getOffset();
          long appendedLength = arcSize - appendedOffset;

          // see if there is a resource here
          if (appendedOffset + 4 < arcSize) {

            // 4 - Unknown Data Length
            int unknownDataLength = fm.readInt();
            appendedOffset += 4 + unknownDataLength;
            if (appendedOffset < arcSize) {
              fm.seek(appendedOffset);

              if (appendedOffset + 4 < arcSize) {
                // 4 - Header (MSCF)
                String header = fm.readString(4);
                if (header.equals("MSCF")) {
                  // Found a Cabinet File appended to the EXE

                  //path,id,name,offset,length,decompLength,exporter
                  resources[realNumFiles] = new Resource(path, path.getName() + ".cab", appendedOffset, appendedLength);
                  realNumFiles++;
                }
              }
            }
          }
        }
      }

      // Now that we've analyzed for appended resource, we can go through the resources within the EXE
      if (resourceLength != 0) {

        // Reset the globals
        earliestOffset = arcSize;
        furthestOffset = 0;

        analyseDirectory(fm, path, resources, "", resourceOffset);

        fm.close();

        // NOW we need to change all the resource offsets - they're too high, need to make them relative to the end of the directories
        // The FurthestOffset is the end of the directories
        // The EariestOffset is the offset of the first file in the EXE, as reported in the offset tables
        // The EarliestOffset reported in the offset tables is HIGHER than the FurthestOffset as calculated by reading the directories
        // The difference needs to be subtracted from all offsets in all resources
        furthestOffset += 2; // there's a 2-null byte between directory and file data
        long difference = earliestOffset - furthestOffset; // yeah i know, the naming makes it seem the wrong way around, but this is right!

        for (int i = 0; i < realNumFiles; i++) {
          Resource resource = resources[i];
          long offset = resource.getOffset();
          resource.setOffset(offset - difference);

          // Then we need to see if there's a Cabinet 3.0 embedded in the EXE - if there is, rename it appropriately, like we do in newer CAB
          // versions (above) where the CAB is just appended to the end of the EXE rather than being embedded in it.

          System.out.println("Old: " + offset + "\tNew: " + resource.getOffset() + "\tName: " + resource.getName());
        }

        resources = resizeResources(resources, realNumFiles);
        return resources;
      }

      fm.close();

      return null;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
