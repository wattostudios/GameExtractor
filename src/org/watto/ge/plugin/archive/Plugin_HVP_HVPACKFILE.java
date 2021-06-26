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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HVP_HVPACKFILE extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HVP_HVPACKFILE() {

    super("HVP_HVPACKFILE", "HVP_HVPACKFILE");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Obscure", "Obscure (PS2) [Read Only]");
    setExtensions("hvp", "001");
    setPlatforms("PC", "PS2");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, int numFiles) throws Exception {
    long arcSize = fm.getLength();

    ExporterPlugin exporter = Exporter_ZLib.getInstance();

    for (int i = 0; i < numFiles; i++) {
      // 4 - Length Of Entry (including this field)
      fm.skip(4);

      // 1 - Entry Type Indicator 1 (0=dir, 1=file)
      int entryType = fm.readByte();

      //System.out.println((fm.getOffset() - 5) + "\t" + entryType);

      if (entryType == 0) {
        // Directory

        // 4 - null
        fm.skip(4);

        // 4 - Number Of Files/SubDirectories In This Directory
        int numFilesInDir = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFiles(numFilesInDir);

        // 4 - Directory Name Length
        // X - Directory Name
        String subDirName = fm.readString(IntConverter.changeFormat(fm.readInt()));

        analyseDirectory(fm, path, resources, dirName + subDirName + "\\", numFilesInDir);
      }

      else {
        // File

        // 4 - Compression Type (0=Uncompressed, 1=ZLib)
        int compressionType = IntConverter.changeFormat(fm.readInt());

        // 4 - Compressed Size
        long length = IntConverter.changeFormat(fm.readInt());

        // 4 - Decompressed Size
        int decompLength = IntConverter.changeFormat(fm.readInt());

        if (decompLength == 0) {
          length = 0;
        }

        //System.out.println(compressionType + "\t" + (length == decompLength));

        FieldValidator.checkLength(length, arcSize);
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (Hash or something?)
        fm.skip(4);

        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Length
        // X - Filename
        String filename = dirName + fm.readString(IntConverter.changeFormat(fm.readInt()));
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        if (compressionType == 0) {
          // no compression
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // ZLib compression
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        realNumFiles++;
        TaskProgressManager.setValue(realNumFiles);

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
      if (fm.readString(11).equals("HV PackFile")) {
        rating += 50;
      }

      // null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // RESET GLOBALS
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      // 11 - Header (HV PackFile)
      // 1 - null
      // 2 - Version Major? (3)
      // 2 - Version Minor? (1)
      fm.skip(16);

      // 4 - Number of Directories at the Root Level
      int numRootDirectories = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numRootDirectories);

      // 4 - Number Of Files and Directories
      int numFilesAndDirs = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFilesAndDirs);

      // 4 - Number Of Files only
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Length (not including all these archive header fields)
      // 8 - Unknown
      fm.skip(12);

      //long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      /*
      if (numFiles == numFilesAndDirs) {
        fm.seek(32);
        analyseDirectory(fm, path, resources, "", numFiles);
      }
      else {
        analyseDirectory(fm, path, resources, "", 1);
      }
      //while (realNumFiles<numFiles){
      //  analyseDirectory(fm,path,resources,"",1);
      //  }
      */

      realNumFiles = 0;
      analyseDirectory(fm, path, resources, "", numRootDirectories);

      //resources = resizeResources(resources, realNumFiles);

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

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 11 - Header (HV PackFile)
      // 1 - null
      // 2 - Version Major? (3)
      // 2 - Version Minor? (1)
      // 4 - Number of Directories at the Root Level
      fm.writeBytes(src.readBytes(20));

      // 4 - Number Of Files and Directories
      int numFilesAndDirs = IntConverter.changeFormat(src.readInt());
      fm.writeInt(IntConverter.changeFormat(numFilesAndDirs));

      // 4 - Number Of Files only
      fm.writeBytes(src.readBytes(4));

      // 4 - Directory Length (not including all these archive header fields)
      int dirLength = IntConverter.changeFormat(src.readInt());
      fm.writeInt(IntConverter.changeFormat(dirLength));

      // 8 - Unknown
      fm.writeBytes(src.readBytes(8));

      fm.setLength(dirLength + 40);
      fm.seek(dirLength + 40);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      //long[] compressedLengths = write(exporter, resources, fm);
      // If the compression hasn't changed, and the file hasn't been replaced, just copy straight from the source to the destination.
      // If the file has been replaced, just copy it straight from the file as well, because this archive format supports uncompressed files.
      // The end result is, use Exporter_Default for everything!
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        Resource dummyResource = new Resource(resource.getSource(), "", resource.getOffset(), resource.getLength());
        write(dummyResource, fm);
        TaskProgressManager.setValue(i);
      }

      fm.seek(40);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dirLength + 40;
      int realNumFiles = 0;
      for (int i = 0; i < numFilesAndDirs; i++) {

        // 4 - Length Of Entry (including this field)
        fm.writeBytes(src.readBytes(4));

        // 1 - Entry Type (0=dir, 1=file)
        int entryType = src.readByte();
        fm.writeByte(entryType);

        if (entryType == 0) {
          // Directory

          // 4 - null
          fm.writeBytes(src.readBytes(4));

          // 4 - Number Of Files/SubDirectories In This Directory
          fm.writeBytes(src.readBytes(4));

          // 4 - Directory Name Length
          int nameLength = IntConverter.changeFormat(src.readInt());
          fm.writeInt(IntConverter.changeFormat(nameLength));

          // X - Directory Name
          fm.writeBytes(src.readBytes(nameLength));
        }

        else {
          // File
          Resource resource = resources[realNumFiles];

          if (resource.isReplaced()) {
            // 4 - Compression Type (0=Uncompressed, 1=ZLib)
            // forced to uncompressed
            fm.writeInt(0);
            src.skip(4);

          }
          else {
            // 4 - Compression Type (0=Uncompressed, 1=ZLib)
            // copy whatever it currently uses
            fm.writeBytes(src.readBytes(4));
          }

          long length = resource.getLength();
          long decompLength = resource.getDecompressedLength();

          // 4 - Compressed Size
          fm.writeInt(IntConverter.changeFormat((int) length));
          src.skip(4);

          // 4 - Decompressed Size
          fm.writeInt(IntConverter.changeFormat((int) decompLength));
          src.skip(4);

          // 4 - Hash?
          fm.writeBytes(src.readBytes(4));

          // 4 - File Offset
          fm.writeInt(IntConverter.changeFormat((int) offset));
          src.skip(4);

          // 4 - Filename Length
          int nameLength = IntConverter.changeFormat(src.readInt());
          fm.writeInt(IntConverter.changeFormat(nameLength));

          // X - Filename
          fm.writeBytes(src.readBytes(nameLength));

          offset += length;
          realNumFiles++;
        }

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("cre") || extension.equalsIgnoreCase("lvl") || extension.equalsIgnoreCase("sen") || extension.equalsIgnoreCase("sub")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}