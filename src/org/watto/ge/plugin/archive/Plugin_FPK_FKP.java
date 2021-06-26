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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_GZip;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FPK_FKP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FPK_FKP() {

    super("FPK_FKP", "FPK_FKP");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("FLOCK!");
    setExtensions("fpk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("pgl", "Language File", FileType.TYPE_DOCUMENT));

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

      // 4 - Header (FKP.)
      if (fm.readString(4).equals("FKP.")) {
        rating += 50;
      }

      // 4 - Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Header Length (16)
      if (fm.readInt() == 16) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("pgl")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      ExporterPlugin gzipExporter = Exporter_GZip.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FKP.)
      fm.skip(4);

      // 4 - Version (1)
      int version = fm.readInt();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Header Length (16)
      fm.skip(4);

      // Set the archive properties
      Resource_Property[] properties = new Resource_Property[1];
      properties[0] = new Resource_Property("Version", version);
      setProperties(properties);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 48 - Filename (null terminated, filled with byte 204)
        String filename = fm.readNullString(48);
        FieldValidator.checkFilename(filename);

        // 4 - Hash?
        // 4 - null
        fm.skip(8);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // PGM and PGA files are GZip-compressed
        String fileExtension = FilenameSplitter.getExtension(filename);
        if (fileExtension.equalsIgnoreCase("pgm") || fileExtension.equalsIgnoreCase("pga")) {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, length, gzipExporter);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
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

      long offset = 16 + numFiles * 64;

      int directoryPadding = calculatePadding(offset, 2048);
      offset += directoryPadding;

      // Write Header Data

      // 4 - Header (FKP.)
      // 4 - Version (1)
      // 4 - Number Of Files
      // 4 - Header Length (16)
      fm.writeBytes(src.readBytes(16));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 48 - Filename (null terminated, filled with byte 204)
        String filename = resources[i].getName();
        int filenameLength = filename.length();
        if (filename.length() > 48) {
          filename = filename.substring(0, 48);
          filenameLength = 48;
        }
        fm.writeString(resources[i].getName());
        if (filenameLength < 48) {
          fm.writeByte(0);
          filenameLength++;

          while (filenameLength < 48) {
            fm.writeByte(204);
            filenameLength++;
          }
        }

        src.skip(48);

        // 4 - Hash?
        // 4 - null (or a value that seems to relate to the file type - like-type files have the same value)
        fm.writeBytes(src.readBytes(8));

        // 4 - File Length
        fm.writeInt(length);

        // 4 - File Offset
        fm.writeInt(offset);

        src.skip(8);

        offset += length;

        int filePadding = calculatePadding(length, 2048);
        offset += filePadding;
      }

      // 0-2047 - Padding (byte 205) to a multiple of 2048 bytes
      for (int p = 0; p < directoryPadding; p++) {
        fm.writeByte(205);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin defaultExporter = Exporter_Default.getInstance();

      for (int i = 0; i < numFiles; i++) {
        // Some files use GZip compression. For "replacing", we just want to force normal reading/writing of files,
        // so temporarily remove the exporter and replace it later on
        Resource resource = resources[i];
        ExporterPlugin realExporter = resource.getExporter();
        resource.setExporter(defaultExporter);

        // X - File Data
        write(resource, fm);

        resource.setExporter(realExporter);

        // 0-2047 - Padding (byte 205) to a multiple of 2048 bytes
        int filePadding = calculatePadding(resource.getDecompressedLength(), 2048);
        for (int p = 0; p < filePadding; p++) {
          fm.writeByte(205);
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

  /**
   **********************************************************************************************
   * Sets up the default properties for a new Archive of this type
   **********************************************************************************************
   **/
  @Override
  public void setDefaultProperties(boolean force) {
    if (force || properties.length == 0) {
      properties = new Resource_Property[1];
      properties[0] = new Resource_Property("Version", 1);
    }
  }

}
