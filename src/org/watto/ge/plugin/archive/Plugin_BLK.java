/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BLK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BLK() {

    super("BLK", "BLK");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("blk");
    setGames("Apache Longbow");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("raw", "RAW Audio", FileType.TYPE_AUDIO));

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

      if (fm.readInt() == 0) {
        rating += 5;

        if (fm.readInt() == 1) {
          rating += 5;
        }

        if (fm.readByte() == 10) {
          rating += 5;
        }
      }
      else {
        if (fm.readInt() == 0) {
          rating += 5;
        }

        if (fm.readInt() == 1) {
          rating += 5;
        }

        if (fm.readByte() == 10) {
          rating += 5;
        }
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

      FileManipulator fm = new FileManipulator(path, false, 8); // small quick reads

      long arcSize = fm.getLength();

      // 4 - Unknown (optional field)
      // 4 - null
      // 4 - Unknown (1)
      // 1 - Unknown (10)
      if (fm.readInt() == 0) {
        fm.skip(5);
      }
      else {
        fm.skip(9);
      }

      int numFiles = Archive.getMaxFiles(4);
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 8) {

        // 4 - File ID (incremental from 1)
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + ".raw";

        //path,id,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(22050, 8, 1);
        resources[realNumFiles] = resource;

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Unknown (optional field)
      // 4 - null
      // 4 - Unknown (1)
      // 1 - Unknown (10)
      int testField = src.readInt();
      fm.writeInt(testField);

      if (testField == 0) {
        fm.writeBytes(src.readBytes(5));
      }
      else {
        fm.writeBytes(src.readBytes(9));
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // 4 - File ID (incremental from 1)
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        int srcLength = src.readInt();
        fm.writeInt(resource.getDecompressedLength());

        // X - File Data
        src.skip(srcLength);

        ExporterPlugin oldExporter = resource.getExporter();
        resource.setExporter(exporterDefault);

        write(resource, fm);

        resource.setExporter(oldExporter);

        TaskProgressManager.setValue(i);
      }

      // 4 - End of Archive Marker (-1)
      fm.writeInt(-1);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}