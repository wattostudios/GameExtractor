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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Explode;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SSA_RASS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SSA_RASS() {

    super("SSA_RASS", "SSA_RASS");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("ssa");
    setGames("Empires: Dawn Of The Modern World",
        "Empire Earth");
    setPlatforms("PC");

    setFileTypes("scn", "Scenario",
        "dat", "Data File",
        "udf", "Unit Data File",
        "edf", "Effect Data File",
        "env", "Environment",
        "cem", "Object Model",
        "psh", "Pixel Shaders",
        "vsh", "Vertex Shaders",
        "scc", "Shortcut",
        "sdf", "Sound Data File",
        "sdd", "SDD Texture",
        "tai", "AI Script");

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
      if (fm.readString(4).equals("rass")) {
        rating += 50;
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 30)) {
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

      Exporter_Explode exporterExplode = Exporter_Explode.getInstance();
      Exporter_ZLib exporterZLib = Exporter_ZLib.getInstance();

      long arcSize = fm.getLength();

      // 4 - Header
      // 4 - Version (1)
      // 4 - Blank
      fm.skip(12);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = dirLength / 30;// guessed
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int i = 0;
      for (int j = 0; j < numFiles; j++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readNullString(filenameLength);
        //fm.skip(2);// 2 null bytes
        FieldValidator.checkFilename(filename);

        // 4 - Start Byte
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - End Byte
        int endByte = fm.readInt();

        // 4 - Unknown
        fm.skip(4);

        long length = endByte - offset + 1;
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(j);

        i++;
        //System.out.println(endByte + " of " + (arcSize-40));
        if (endByte >= arcSize - 40) {
          j = numFiles;
        }
      }

      resources = resizeResources(resources, i);

      fm.getBuffer().setBufferSize(12); // for quick reading

      for (int j = 0; j < i; j++) {
        Resource resource = resources[j];

        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Header
        String type = fm.readString(4);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();

        // 4 - null
        fm.skip(4);

        // X - Compressed File Data
        if (type.equals("PK01")) {
          // Explode Compression
          FieldValidator.checkLength(decompLength);

          resource.setOffset(offset + 12);
          resource.setLength(resource.getLength() - 12);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporterExplode);
        }
        else if (type.equals("ZL01")) {
          // ZLib Deflate Compression
          // TODO NOT TESTED
          FieldValidator.checkLength(decompLength);

          resource.setOffset(offset + 12);
          resource.setLength(resource.getLength() - 12);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporterZLib);
        }

        TaskProgressManager.setValue(j);
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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfDirectory = 0;
      for (int i = 0; i < numFiles; i++) {
        int nameLength = resources[i].getName().length() + 1;
        //if (nameLength % 2 == 1) { // things don't seem to be padded in the original archives
        //  nameLength++;
        //}
        totalLengthOfDirectory += 16 + nameLength;
      }

      // 4 - Header
      // 4 - Version (1)
      // 4 - Blank
      fm.writeBytes(src.readBytes(12));

      // 4 - Directory Length
      src.skip(4);
      fm.writeInt(totalLengthOfDirectory);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16 + totalLengthOfDirectory;

      //
      //
      //
      // need to work out if there are any additional properties stored in the archive, and if so, add their length to the offset
      src.seek(offset);

      // 4 - Number of Additional Properties (1)
      int numProperties = src.readInt();
      offset += 4;

      // for each property...
      for (int p = 0; p < numProperties; p++) {
        //   4 - Length of Property Name (including null terminator)
        //   X - Property Name
        //   1 - null Property Name Terminator
        int nameLength = src.readInt();
        src.skip(nameLength);
        offset += 4 + nameLength;

        //   4 - Length of Property Value (including null terminator)
        //   X - Property Value
        //   1 - null Property Value Terminator
        int valueLength = src.readInt();
        src.skip(valueLength);
        offset += 4 + valueLength;
      }

      // Now, back to the beginning of the source archive
      src.seek(16);
      //
      //
      //

      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // We don't have an explode compressor, so if the file hasn't changed, just copy it as-is
        if (fd.getExporter() instanceof Exporter_Explode) {
          length = fd.getLength() + 12; // include the 12 byte header
        }

        String name = fd.getName();

        // 4 - Filename Length
        // X - Filename (+ 1 or 2 null)
        src.skip(src.readInt());

        int nameLength = name.length() + 1;
        int numPadding = 1;
        //if (nameLength % 2 == 1) { // things don't seem to be padded in the original archives
        //  nameLength++;
        //  numPadding = 2;
        //}

        fm.writeInt(nameLength);
        // Some letters are different codepages, which messes up the lengths, so need to fix that here instead of just writeString()
        //fm.writeString(name);
        for (int c = 0; c < nameLength - 1; c++) {
          fm.writeByte((byte) name.charAt(c));
        }

        fm.writeByte(0);
        if (numPadding == 2) {
          fm.writeByte(0);
        }

        // 4 - Start Byte
        // 4 - End Byte
        src.skip(8);
        fm.writeInt((int) offset);
        fm.writeInt((int) (offset + length - 1));

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        offset += length;

      }

      // 4 - Number of Additional Properties (1)
      numProperties = src.readInt();
      fm.writeInt(numProperties);

      // for each property...
      for (int p = 0; p < numProperties; p++) {
        //   4 - Length of Property Name (including null terminator)
        int nameLength = src.readInt();
        fm.writeInt(nameLength);

        //   X - Property Name
        //   1 - null Property Name Terminator
        fm.writeBytes(src.readBytes(nameLength));

        //   4 - Length of Property Value (including null terminator)
        int valueLength = src.readInt();
        fm.writeInt(valueLength);

        //   X - Property Value
        //   1 - null Property Value Terminator
        fm.writeBytes(src.readBytes(valueLength));
      }

      // X - File Data

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // This will decompress all the files and write them as uncompressed (as we don't have an Explode compressor)
      //write(resources, fm);
      //
      // Instead, if the compressor is Explode, lets just leave it compressed if the file hasn't been changed
      Exporter_Default exporterDefault = Exporter_Default.getInstance();
      Exporter_Explode exporterExplode = Exporter_Explode.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.getExporter() instanceof Exporter_Explode) {
          resource.setExporter(exporterDefault); // use the default exporter for the straight copy of data
          resource.setOffset(resource.getOffset() - 12); // include the 12 byte header
          resource.setLength(resource.getLength() + 12); // include the 12 byte header

          write(resource, fm);

          resource.setExporter(exporterExplode); // set it all back again, in case we want to read this file from the archive again or something
          resource.setOffset(resource.getOffset() + 12);
          resource.setLength(resource.getLength() - 12);
        }
        else {
          write(resource, fm);
        }
        TaskProgressManager.setValue(i);
      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}