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
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_TERF extends ArchivePlugin {

  boolean compressedArchive = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_TERF() {

    super("DAT_TERF", "DAT_TERF");

    //         read write replace rename
    setProperties(true, true, true, false);

    setGames("Madden 2004",
        "Madden 2005",
        "NFL Head Coach");
    setExtensions("dat");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("TERF")) {
        rating += 50;
      }

      // Header Size
      int headerSize = fm.readInt();
      if (headerSize == 64 || headerSize == 16 || headerSize == 2048 || headerSize == 128) {
        rating += 5;
      }

      // Unknown Constant
      if (fm.readInt() == 83886594) {
        rating += 5;
      }

      // Padding Size
      int paddingSize = fm.readShort();
      if (paddingSize == 64 || paddingSize == 4 || paddingSize == 2048 || paddingSize == 128) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      compressedArchive = false;

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (TERF)
      fm.skip(4);

      // 4 - Directory Offset (64)
      int headerSize = fm.readInt();
      if (headerSize == 64) {
        return read64(fm, path);
      }
      else if (headerSize == 128) {
        return read128(fm, path);
      }
      else if (headerSize == 16) {
        return read16(fm, path);
      }
      else if (headerSize == 2048) {
        return read2048(fm, path);
      }
      else {
        return readOther(fm, path);
      }

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * AN ARCHIVE WITH SOME COMPRESSED FILES
   **********************************************************************************************
   **/
  public Resource[] read128(FileManipulator fm, File path) {
    try {

      compressedArchive = true;

      ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("EA_MADDEN");

      long arcSize = fm.getLength();

      // 4 - Unknown (83886594)
      // 2 - File Padding Size (128)
      fm.skip(6);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 112 - null
      fm.skip(112);

      long dirOffset = fm.getOffset();

      // 4 - Directory Header (DIR1)
      String header = fm.readString(4);
      if (header.equals("HSH1")) {

        // 4 - Directory Length (including these 2 fields)
        int dirLength = fm.readInt() - 8;
        FieldValidator.checkLength(dirLength, arcSize);

        fm.skip(dirLength);

        dirOffset = fm.getOffset();

        // 4 - Directory Header (DIR1)
        fm.skip(4);
      }

      // 4 - Directory Length (including these 2 fields)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      long relOffset = (dirLength * 2) + dirOffset; // *2 to include the compressed directory

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (relative to the start of the FileDataHeader)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (or null)
        long length = fm.readInt();
        if (length == 0) {
          length = 128;
        }
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.seek(dirOffset + dirLength);

      // 4 - Compression Header (COMP)
      // 4 - Compression Length (including these 2 fields)
      fm.skip(8);

      // Compression Directory
      for (int i = 0; i < numFiles; i++) {

        boolean compressed = false;

        // 4 - Compression Tag (0=uncompressed, 5=compressed)
        int compTag = fm.readInt();
        if (compTag == 0) {
        }
        else if (compTag == 5) {
          compressed = true;
        }
        else {
          ErrorLogger.log("[DAT_TERF] Unknown compression type: " + compTag);
          return null;
        }

        // 4 - Decompressed Size (0=uncompressed)
        if (compressed) {
          Resource resource = resources[i];
          resource.setDecompressedLength(fm.readInt());
          resource.setExporter(exporter);
        }
        else {
          fm.skip(4);
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
   * AN ARCHIVE WITH SOME COMPRESSED FILES
   **********************************************************************************************
   **/
  public Resource[] read16(FileManipulator fm, File path) {
    try {

      compressedArchive = true;

      ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("EA_MADDEN");

      long arcSize = fm.getLength();

      // 4 - Unknown (83886594)
      // 2 - File Padding Size (4)
      fm.skip(6);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Header (DIR1)
      fm.skip(4);

      // 4 - Directory Length (including these 2 fields) // *2 to include the compressed directory
      int relOffset = (fm.readInt() * 2) + 16;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles * 2);

      // Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (relative to the start of the FileDataHeader)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (or null)
        long length = fm.readInt();
        if (length == 0) {
          length = 4;
        }
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // 4 - Compression Header (COMP)
      // 4 - Compression Length (including these 2 fields)
      fm.skip(8);

      // Compression Directory
      for (int i = 0; i < numFiles; i++) {

        boolean compressed = false;

        // 4 - Compression Tag (0=uncompressed, 5=compressed)
        int compTag = fm.readInt();
        if (compTag == 0) {
        }
        else if (compTag == 5) {
          compressed = true;
        }
        else {
          ErrorLogger.log("[DAT_TERF] Unknown compression type: " + compTag);
          return null;
        }

        // 4 - Decompressed Size (0=uncompressed)
        if (compressed) {
          Resource resource = resources[i];
          resource.setDecompressedLength(fm.readInt());
          resource.setExporter(exporter);
        }
        else {
          fm.skip(4);
        }

        TaskProgressManager.setValue(numFiles + i);
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
   * SOMETIMES COMPRESSED ARCHIVE, AN ARCHIVE WITH SOME COMPRESSED FILES
   **********************************************************************************************
   **/
  public Resource[] read2048(FileManipulator fm, File path) {
    try {

      compressedArchive = true;

      ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("EA_MADDEN");

      long arcSize = fm.getLength();

      // 4 - Unknown (83886594)
      // 2 - File Padding Size (2048)
      fm.skip(6);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2032 - null
      fm.skip(2032);

      long dirOffset = fm.getOffset();

      // 4 - Directory Header (DIR1)
      fm.skip(4);

      // 4 - Directory Length (including these 2 fields)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // Lets check whether there is a Compressed directory
      fm.seek(dirOffset + dirLength);
      if (fm.readString(4).equals("COMP")) {
        compressedArchive = true;
      }
      else {
        compressedArchive = false;
      }

      fm.seek(dirOffset + 8);

      long relOffset = 0;
      if (compressedArchive) {
        relOffset = (dirLength * 2) + dirOffset; // *2 to include the compressed directory
      }
      else {
        relOffset = dirLength + dirOffset;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (relative to the start of the FileDataHeader)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (or null)
        long length = fm.readInt();
        if (length == 0) {
          length = 2048;
        }
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      if (compressedArchive) {
        fm.seek(dirOffset + dirLength);

        // 4 - Compression Header (COMP)
        // 4 - Compression Length (including these 2 fields)
        fm.skip(8);

        // Compression Directory
        for (int i = 0; i < numFiles; i++) {

          boolean compressed = false;

          // 4 - Compression Tag (0=uncompressed, 5=compressed)
          int compTag = fm.readInt();
          if (compTag == 0) {
          }
          else if (compTag == 5) {
            compressed = true;
          }
          else {
            ErrorLogger.log("[DAT_TERF] Unknown compression type: " + compTag);
            return null;
          }

          // 4 - Decompressed Size (0=uncompressed)
          if (compressed) {
            Resource resource = resources[i];
            resource.setDecompressedLength(fm.readInt());
            resource.setExporter(exporter);
          }
          else {
            fm.skip(4);
          }

          TaskProgressManager.setValue(i);
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
   * AN ARCHIVE WITHOUT COMPRESSION
   **********************************************************************************************
   **/
  public Resource[] read64(FileManipulator fm, File path) {
    try {

      compressedArchive = false;

      long arcSize = fm.getLength();

      // 4 - Unknown (83886594)
      // 2 - File Padding Size (64)
      fm.skip(6);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 48 - null
      // 4 - Directory Header (DIR1)
      fm.skip(52);

      // 4 - Directory Length (including these 2 fields)
      int relOffset = fm.readInt() + 64;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (relative to the start of the FileDataHeader)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (or null)
        long length = fm.readInt();
        if (length == 0) {
          length = 64;
        }
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
   * Other archive formats
   **********************************************************************************************
   **/
  public Resource[] readOther(FileManipulator fm, File path) {
    try {

      compressedArchive = true;

      ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("EA_MADDEN");

      long arcSize = fm.getLength();

      // 4 - Unknown (83886594)
      fm.skip(4);

      // 2 - File Padding Size
      int paddingSize = fm.readShort();

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 112 - null
      // 4 - Directory Header (DIR1)
      fm.skip(116);

      // 4 - Directory Length (including these 2 fields) // *2 to include the compressed directory
      int relOffset = (fm.readInt() * 2) + paddingSize;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles * 2);

      // Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (relative to the start of the FileDataHeader)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (or null)
        long length = fm.readInt();
        if (length == 0) {
          length = paddingSize;
        }
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // 4 - Compression Header (COMP)
      if (!fm.readString(4).equals("COMP")) {
        // No compression information in this archive
        fm.close();
        return resources;
      }

      // 4 - Compression Length (including these 2 fields)
      fm.skip(4);

      // Compression Directory
      for (int i = 0; i < numFiles; i++) {

        boolean compressed = false;

        // 4 - Compression Tag (0=uncompressed, 5=compressed)
        int compTag = fm.readInt();
        if (compTag == 0) {
        }
        else if (compTag == 5) {
          compressed = true;
        }
        else {
          ErrorLogger.log("[DAT_TERF] Unknown compression type: " + compTag);
          return null;
        }

        // 4 - Decompressed Size (0=uncompressed)
        if (compressed) {
          Resource resource = resources[i];
          resource.setDecompressedLength(fm.readInt());
          resource.setExporter(exporter);
        }
        else {
          fm.skip(4);
        }

        TaskProgressManager.setValue(numFiles + i);
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
  @Override
  public void write(Resource[] resources, File path) {
    try {

      // write as compressed archive or not?
      String[] possibleValues = { "Compressed Files", "Decompressed Files" };

      String selectedValue = "";
      if (compressedArchive) {
        selectedValue = (String) javax.swing.JOptionPane.showInputDialog(null, "What type of archive is this? Don't change if you are unsure.", "Archive Type", javax.swing.JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
      }
      else {
        selectedValue = (String) javax.swing.JOptionPane.showInputDialog(null, "What type of archive is this? Don't change if you are unsure.", "Archive Type", javax.swing.JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[1]);
      }

      if (selectedValue.equals("Compressed Files")) {
        write16(resources, path);
      }
      else {
        write64(resources, path);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * SOME COMPRESSED FILES
   **********************************************************************************************
   **/
  public void write16(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        filesSize += length;

        long padding = 4 - (length % 4);
        if (padding < 4) {
          filesSize += padding;
        }
      }

      // Write Header Data

      // 4 - Header (TERF)
      fm.writeString("TERF");

      // 4 - Directory Offset (16)
      fm.writeInt((int) 16);

      // 4 - Unknown (83886594)
      fm.writeInt((int) 83886594);

      // 2 - File Padding Size (4)
      fm.writeShort((short) 4);

      // 2 - Number Of Files
      fm.writeShort((short) numFiles);

      // 4 - Directory Header (DIR1)
      fm.writeString("DIR1");

      // 4 - Directory Length (including these 2 fields)
      fm.writeInt(numFiles * 8 + 8);

      // Write Directory
      long offset = 12;
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 - Data Offset
        fm.writeInt((int) offset);

        // 4 - File Size
        fm.writeInt((int) length);

        offset += length;

        long padding = 4 - (length % 4);
        if (padding < 4) {
          offset += padding;
        }
      }

      // 4 - Compression Directory Header (COMP)
      fm.writeString("COMP");

      // 4 - Compression Directory Length (including these 2 fields)
      fm.writeInt(numFiles * 8 + 8);

      // Write Compression Directory
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].isCompressed()) {
          // 4 - Compression Tag
          fm.writeInt((int) 5);

          // 4 - Decompressed Size
          fm.writeInt((int) resources[i].getDecompressedLength());
        }
        else {
          // 4 - Compression Tag
          fm.writeInt((int) 0);

          // 4 - Decompressed Size
          fm.writeInt((int) 0);
        }
      }

      // 4 - File Data Header (DATA)
      fm.writeString("DATA");

      // 4 - File Data Length (including these 3 fields)
      fm.writeInt(filesSize + 12);

      // 4 - null
      fm.writeInt((int) 0);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        write(resources[i], fm);
        TaskProgressManager.setValue(i);

        long padding = 4 - (resources[i].getDecompressedLength() % 4);
        if (padding < 4) {
          for (int j = 0; j < padding; j++) {
            fm.writeByte(0);
          }
        }

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * NO COMPRESSION
   **********************************************************************************************
   **/
  public void write64(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        filesSize += length;

        long padding = 64 - (length % 64);
        if (padding < 64) {
          filesSize += padding;
        }
      }

      int dirSize = numFiles * 8 + 8;
      int dirPadding = 64 - (dirSize % 64);
      if (dirPadding == 64) {
        dirPadding = 0;
      }

      // Write Header Data

      // 4 - Header (TERF)
      fm.writeString("TERF");

      // 4 - Directory Offset (64)
      fm.writeInt((int) 64);

      // 4 - Unknown (83886594)
      fm.writeInt((int) 83886594);

      // 2 - File Padding Size (64)
      fm.writeShort((short) 64);

      // 2 - Number Of Files
      fm.writeShort((short) numFiles);

      // 48 - null
      for (int i = 0; i < 48; i++) {
        fm.writeByte(0);
      }

      // 4 - Directory Header (DIR1)
      fm.writeString("DIR1");

      // 4 - Directory Length (including these 2 fields)
      fm.writeInt(dirSize + dirPadding);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 64;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 - Data Offset
        fm.writeInt((int) offset);

        // 4 - File Size
        fm.writeInt((int) length);

        offset += length;

        long padding = 64 - (length % 64);
        if (padding < 64) {
          offset += padding;
        }
      }

      // 0-63 - Directory Padding
      for (int i = 0; i < dirPadding; i++) {
        fm.writeByte(0);
      }

      // 4 - File Data Header (DATA)
      fm.writeString("DATA");

      // 4 - File Data Length (including these 3 fields)
      fm.writeInt(filesSize + 64);

      // 56 - null
      for (int i = 0; i < 56; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        write(resources[i], fm);
        TaskProgressManager.setValue(i);

        long padding = 64 - (resources[i].getDecompressedLength() % 64);
        if (padding < 64) {
          for (int j = 0; j < padding; j++) {
            fm.writeByte(0);
          }
        }

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
