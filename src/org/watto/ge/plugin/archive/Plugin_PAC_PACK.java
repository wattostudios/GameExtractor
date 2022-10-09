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
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_PAC_PACK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAC_PACK() {

    super("PAC_PACK", "PAC_PACK");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Lego Alpha Team");
    setExtensions("pac"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("dds", "DDS Audio File", FileType.TYPE_AUDIO));

    setTextPreviewExtensions("puz"); // LOWER CASE

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
      if (fm.readString(4).equals("PACK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PACK)
      // 4 - Archive Length
      // 4 - null
      // 4 - File Data Offset
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - null
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Hash?
        fm.skip(8);

        if (filename.toLowerCase().endsWith(".dds")) {
          // Audio File

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource_WAV_RawAudio(path, filename, offset, length);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(16);
      // Convert all the DDS files into WAV audio
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource instanceof Resource_WAV_RawAudio) {
          fm.seek(resource.getOffset());

          Resource_WAV_RawAudio resourceWAV = (Resource_WAV_RawAudio) resource;

          // 2 - Unknown (1)
          fm.skip(2);

          // 2 - Channels (1/2)
          short channels = fm.readShort();

          // 4 - Frequency (22050)
          int frequency = fm.readInt();

          // 4 - Max Frequency? (44100)
          // 2 - Unknown (2/4)
          fm.skip(6);

          // 2 - Bitrate? (16)
          short bitrate = fm.readShort();

          resource.setOffset(fm.getOffset());
          resourceWAV.setAudioProperties(frequency, bitrate, channels);

          long length = resource.getDecompressedLength() - 16;
          resource.setLength(length);
          resource.setDecompressedLength(length);
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

      long headerSize = 20 + 4; // +4 for the null between the directory and the file data 
      long directorySize = 0;
      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long length = resource.getDecompressedLength();
        if (resource instanceof Resource_WAV_RawAudio) {
          length += 16;
        }

        filesSize += length;
        directorySize += 20 + resource.getNameLength() + 1;
      }
      long archiveSize = headerSize + directorySize + filesSize;

      // Write Header Data

      // 4 - Header (PACK)
      fm.writeBytes(src.readBytes(4));

      // 4 - Archive Length
      fm.writeInt(archiveSize);
      src.skip(4);

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Offset
      long dataOffset = headerSize + directorySize;
      fm.writeInt(dataOffset);
      src.skip(4);

      // 4 - Number of Files
      fm.writeBytes(src.readBytes(4));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dataOffset;
      long[] originalOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();
        if (resource instanceof Resource_WAV_RawAudio) {
          length += 16;
        }

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeString(resource.getName());
        fm.writeByte(0);
        src.readNullString();

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        // 4 - File Length
        fm.writeInt(offset);
        fm.writeInt(length);

        originalOffsets[i] = src.readInt();
        src.skip(4);

        // 8 - Hash?
        fm.writeBytes(src.readBytes(8));

        offset += length;
      }

      // 4 - null
      fm.writeInt(0);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      ExporterPlugin defaultExporter = Exporter_Default.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        ExporterPlugin oldExporter = resource.getExporter();
        if (resource instanceof Resource_WAV_RawAudio) {
          resource.setExporter(defaultExporter); // so that it doesn't replace by appending the WAV header

          // write the 16 bytes of header for this file
          src.seek(originalOffsets[i]);
          fm.writeBytes(src.readBytes(16));
        }

        write(resource, fm);
        TaskProgressManager.setValue(i);

        resource.setExporter(oldExporter);
      }

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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
