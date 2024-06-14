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
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FLX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FLX() {

    super("FLX", "FLX");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("flx");
    setGames("Crusader: No Remorse", "Crusader: No Regret");
    setPlatforms("PC");

    //setCanScanForFileTypes(true);

    setFileTypes(new FileType("asfx", "ASFX Audio", FileType.TYPE_AUDIO));

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

      // 80 - Header (all 26's)
      if (fm.readInt() == 437918234) {
        rating += 50;
      }

      fm.seek(128);

      long arcSize = fm.getLength();

      // File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Length
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 128 - Unknown
      fm.seek(128);

      int realNumFiles = 0;
      boolean hasNextFile = true;
      while (hasNextFile && fm.getOffset() < arcSize) {
        // 4 - fileOffset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - fileLength
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        /*
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(11025, 8, 1);
        resource.setSigned(false);
        resources[i] = resource;
        */

        TaskProgressManager.setValue(offset);

        if (offset + length >= arcSize) {
          hasNextFile = false;
        }

        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      // go through, find the audio files, set them as WAV-convertable
      //fm.getBuffer().setBufferSize(32); // short quick reads

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();

        fm.seek(offset);

        // 4 - Header (ASFX)
        if (fm.readInt() == 1481003841) {
          // ASFX audio

          // 4 - Audio Data Length
          // 8 - null
          // 2 - Unknown
          fm.skip(14);

          // 2 - Frequency (11025 or 22050)
          int frequency = ShortConverter.unsign(fm.readShort());

          // 4 - null
          // 4 - Unknown
          // 2 - Unknown
          // 2 - Unknown

          offset += 32;

          long length = resource.getLength();
          length -= 32;

          Resource_WAV_RawAudio resourceWAV = new Resource_WAV_RawAudio(path, resource.getName() + ".wav", offset, length);
          resourceWAV.setAudioProperties(frequency, 8, 1);
          resourceWAV.setSigned(false);
          resources[i] = resourceWAV;

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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 128 - Unknown
      fm.writeBytes(src.readBytes(128));

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 128 + (numFiles * 8);
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) length);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        ExporterPlugin realExporter = resource.getExporter();
        resource.setExporter(exporterDefault); // so when we extract the audio files (when they're not replaced), they don't prepend the WAV header to them
        write(resource, fm);
        resource.setExporter(realExporter);

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}