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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.resource.Resource_FileID;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_TEX extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_TEX() {

    super("PAK_TEX", "PAK_TEX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sacred");
    setExtensions("pak");
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
      String header = fm.readString(3);

      // Type ID
      int id = fm.readByte();

      if ((header.equals("TEX") && id == 3) || (header.equals("CIF") && id == 0) || (header.equals("WPN") && id == 8) || (header.equals("SND") && id == 1) || (header.equals("ITM") && id == 5) || (header.equals("ITM") && id == 3)) {
        rating += 70;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      // 3 - Header ()
      String header = fm.readString(3);

      // 1 - Type ID
      int id = fm.readByte();

      if (header.equals("TEX") && id == 3) {

        Resource[] resources = readDirectory(path, fm);
        readTexFilenames(fm, resources);
        fm.close();

        return resources;

      }
      else if (header.equals("CIF") && id == 0) {
        return readCif(path, fm);
      }
      else if (header.equals("WPN") && id == 8) {
        return readWpn(path, fm);
      }
      else if (header.equals("SND") && id == 1) {

        Resource[] resources = readDirectory(path, fm);

        // check if they're WAV audio files
        fm.getBuffer().setBufferSize(4);
        int numFiles = resources.length;
        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];
          long offset = resource.getOffset();
          fm.seek(offset);
          int headerInt = fm.readInt();

          if (headerInt == 0) { // padding at the start of some files
            for (int p = 0; p < 1024; p++) {
              int byteValue = fm.readByte();
              if (byteValue != 0) {
                // found the real start of the file
                offset = fm.getOffset() - 1;
                byte[] headerBytes = new byte[] { (byte) byteValue, fm.readByte(), fm.readByte(), fm.readByte() };
                headerInt = IntConverter.convertLittle(headerBytes);

                resource.setOffset(offset);
                break;
              }
            }
          }

          String name = resource.getName();
          if (headerInt == 1179011410) {
            name += ".wav";
          }
          else if (headerInt == -1070531585 || headerInt == -1068434433 || headerInt == -1064240129 || headerInt == -1066337281) {
            name += ".mp3";
          }

          resource.setName(name);
          resource.setOriginalName(name);

        }

        //System.out.println(resources.length);

        fm.close();

        return resources;

      }
      else if (header.equals("ITM") && id == 5) {

        Resource[] resources = readDirectory(path, fm);
        readItm5Filenames(fm, resources);
        fm.close();

        return resources;

      }
      else if (header.equals("ITM") && id == 3) {

        Resource[] resources = readDirectory(path, fm);
        readItm3Filenames(fm, resources);
        fm.close();

        return resources;

      }
      else {
        return null;
      }
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
  public Resource[] readCif(File path, FileManipulator fm) {
    try {

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      //long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        long offset = (int) fm.getOffset();
        long length = 64;

        fm.skip(64);

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

  **********************************************************************************************
  **/
  public Resource[] readDirectory(File path, FileManipulator fm) {
    try {

      // 4 - Number Of Files (not all are used)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // X - null
      fm.seek(256);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;
      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Type
        int id = fm.readInt();

        // 4 - Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Size
        long length = fm.readInt();

        if (length > 0) {
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource_FileID(path, id, filename, offset, length);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

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
  public void readItm3Filenames(FileManipulator fm, Resource[] resources) {
    try {

      for (int i = 0; i < resources.length; i++) {
        fm.seek(resources[i].getOffset());

        // 32 - Filename
        String filename = fm.readNullString(32);

        resources[i].setName(filename);
        resources[i].setOriginalName(filename);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void readItm5Filenames(FileManipulator fm, Resource[] resources) {
    try {

      for (int i = 0; i < resources.length; i++) {
        if ((resources[i] instanceof Resource_FileID) && (((Resource_FileID) resources[i]).getID() == 2)) {
          fm.seek(resources[i].getOffset() + 56);

          // 32 - Filename
          String filename = fm.readNullString(32);

          resources[i].setName(filename);
          resources[i].setOriginalName(filename);
        }
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void readTexFilenames(FileManipulator fm, Resource[] resources) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      for (int i = 0; i < resources.length; i++) {
        long offset = resources[i].getOffset();

        fm.seek(offset);

        // 32 - Filename
        String filename = fm.readNullString(32);

        // 2 - X Image Size
        // 2 - Y Image Size
        // 4 - Unknown
        // 40 - null padding

        resources[i].setName(filename);
        resources[i].setOriginalName(filename);
        resources[i].setOffset(offset + 80);

        // TEST - this won't work until the DecompressedLength is known!
        //resources[i].setDecompressedLength();
        resources[i].setExporter(exporter);

      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource[] readWpn(File path, FileManipulator fm) {
    try {

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      //long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        long offset = (int) fm.getOffset();
        long length = 322;

        fm.skip(322);

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

}
