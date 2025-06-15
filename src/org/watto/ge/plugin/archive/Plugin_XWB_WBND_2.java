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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XWB_WBND_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XWB_WBND_2() {

    super("XWB_WBND_2", "XWB_WBND_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Ghost Recon 2",
        "The Urbz: Sims in the City");
    setExtensions("xwb");
    setPlatforms("XBox");

    // Read in WAV audio files and convert them
    setCanConvertOnReplace(true);

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
      if (fm.readString(4).equals("WBND")) {
        rating += 50;
      }

      // Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Size Of Header 1 (40)
      if (fm.readInt() == 40) {
        rating += 5;
      }

      // Size Of Header 2 (36)
      if (fm.readInt() == 36) {
        rating += 5;
      }

      // Directory Offset (76)
      if (fm.readInt() == 76) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int paddingMultiple = 512; // read in from read(), used in replace()

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (WBND)
      // 4 - Version (2)
      // 4 - Size Of Header 1 (40)
      // 4 - Size Of Header 2 (36)
      fm.skip(16);

      // 4 - Offset To Details Directory (76)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length Of Details Directory
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Length Of Filename Directory
      fm.skip(4);

      // 4 - File Data Offset
      int firstFileOffset = fm.readInt();// + 8;
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - File Data Length
      // 2 - Unknown (1)
      // 2 - Unknown (0/1)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 16 - Archive Filename (null terminated, filled with null) (without extension)
      // 4 - Length Of Each Details Entry (24)
      // 4 - Length Of Each Filename Entry (64)
      fm.skip(24);

      // 4 - Padding Multiple (512/2048)
      paddingMultiple = fm.readInt();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(filenameDirOffset);

      // FILENAMES DIRECTORY
      String[] names = new String[numFiles];
      if (filenameDirOffset > 0) {
        for (int i = 0; i < numFiles; i++) {
          // 64 - Filename (null) (without extension)
          String filename = fm.readNullString(64);
          FieldValidator.checkFilename(filename);
          names[i] = filename;
        }
      }
      else {
        for (int i = 0; i < numFiles; i++) {
          names[i] = Resource.generateFilename(i);
        }
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Flags
        fm.skip(4);

        // 4 - Audio Details
        long audioDetails = IntConverter.unsign(fm.readInt());

        // 0 00000000 000111110100000000 010 01
        // | |        |                  |   |
        // | |        |                  |   wFormatTag
        // | |        |                  nChannels
        // | |        nSamplesPerSec
        // | wBlockAlign
        // wBitsPerSample

        int codec = (int) ((audioDetails) & ((1 << 2) - 1));
        int channels = (int) ((audioDetails >> (2)) & ((1 << 3) - 1));
        int frequency = (int) ((audioDetails >> (2 + 3)) & ((1 << 18) - 1));
        int align = (int) ((audioDetails >> (2 + 3 + 18)) & ((1 << 8) - 1));
        //int bits = (int) ((audioDetails >> (2 + 3 + 18 + 8)) & ((1 << 1) - 1));

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        FieldValidator.checkOffset(offset + length, arcSize + 100); // help to rule out some different formats (eg where the files are stored as OGG)

        // 8 - null
        fm.skip(8);

        String filename = names[i];

        short bitrate = 8;

        String extension = ".wav";
        if (codec == 0) { // WAVEBANKMINIFORMAT_TAG_PCM      0x0     // PCM data
          codec = 0x0001;
          bitrate = 8;
          align = (bitrate / 8) * channels;
        }
        else if (codec == 1) { // IMA ADPCM XBox 4-bit
          // Confirmed for The Urbz (XBox)
          codec = 0x0069;
          bitrate = 4;
          align = (align + 22) * channels;
        }
        else if (codec == 2) { // WAVEBANKMINIFORMAT_TAG_ADPCM    0x2     // ADPCM data
          codec = 0x0002;
          bitrate = 4;
          align = (align + 22) * channels;
        }
        else if (codec == 3) { // WAVEBANKMINIFORMAT_TAG_WMA      0x3     // WMA data
          extension = ".wma";
        }

        filename += extension;

        if (extension.equals(".wav")) {
          // WAV format
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
          resource.setAudioProperties(frequency, bitrate, (short) channels, true);
          resource.setCodec((short) codec);
          resource.setBlockAlign((short) align);
          resources[i] = resource;
        }
        else {
          // something else
          Resource resource = new Resource(path, filename, offset, length);
          resources[i] = resource;
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

      long fileDataLength = 0;
      for (int i = 0; i < numFiles; i++) {
        fileDataLength += resources[i].getDecompressedLength();
        fileDataLength += calculatePadding(fileDataLength, paddingMultiple);
      }

      // Write Header Data

      // 4 - Header (WBND)
      // 4 - Version (2)
      // 4 - Size Of Header 1 (40)
      // 4 - Size Of Header 2 (36)
      // 4 - Offset To Details Directory (76)
      // 4 - Length Of Details Directory
      // 4 - Offset To Filename Directory (null if this archive has no filename directory)
      fm.writeBytes(src.readBytes(28));

      // 4 - Length Of Filename Directory (null if this archive has no filename directory)
      int srcFilenameDirLength = src.readInt();
      fm.writeInt(srcFilenameDirLength);

      // 4 - File Data Offset
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Length
      src.skip(4);
      fm.writeInt(fileDataLength);

      // 2 - Unknown (1)
      // 2 - Unknown (0/1)
      // 4 - Number Of Files
      // 16 - Archive Filename (null terminated, filled with null) (without extension)
      // 4 - Length Of Each Details Entry (24)
      // 4 - Length Of Each Filename Entry (64)
      // 4 - Padding Multiple (512/2048)
      fm.writeBytes(src.readBytes(36));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Flags
        fm.writeBytes(src.readBytes(4));

        // 4 - Audio Details
        //int audioDetails = src.readInt();
        long audioDetails = IntConverter.unsign(src.readInt());

        if (resource instanceof Resource_WAV_RawAudio) {
          // set the correct frequency, in case it was imported and the frequency was changed
          int frequency = ((Resource_WAV_RawAudio) resource).getFrequency();
          audioDetails = (audioDetails & 4286578719l) | (frequency << 5);

          /*
          // checks
          int codec = (int) ((audioDetails) & ((1 << 2) - 1));
          int channels = (int) ((audioDetails >> (2)) & ((1 << 3) - 1));
          int newFrequency = (int) ((audioDetails >> (2 + 3)) & ((1 << 18) - 1));
          int align = (int) ((audioDetails >> (2 + 3 + 18)) & ((1 << 8) - 1));
          */

        }

        fm.writeInt((int) audioDetails);

        // 4 - File Offset (relative to the start of the file data)
        src.skip(4);
        fm.writeInt(offset);

        // 4 - File Length
        src.skip(4);
        fm.writeInt(length);

        // 8 - null
        fm.writeBytes(src.readBytes(8));

        offset += length;
        offset += calculatePadding(offset, paddingMultiple);
      }

      // FILENAME DIRECTORY
      if (srcFilenameDirLength != 0) {
        // for each file
        //   64 - Filename (null terminated, filled with null) (without extension)
        fm.writeBytes(src.readBytes(srcFilenameDirLength));
      }

      // X - null Padding to the Padding Multiple
      int padding = calculatePadding(fm.getOffset(), paddingMultiple);
      for (int p = 0; p < padding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      Exporter_Default exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        ExporterPlugin origExporter = resource.getExporter();
        resource.setExporter(exporterDefault);

        // X - File Data
        write(resource, fm);

        resource.setExporter(origExporter);

        // X - null Padding to the Padding Multiple
        padding = calculatePadding(resource.getDecompressedLength(), paddingMultiple);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
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
   When replacing RAW audio with a WAV file, the WAV header is stripped off. All other files are
   replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    try {
      if (resourceBeingReplaced.getExtension().equalsIgnoreCase("wav")) {
        // try to convert

        if (!FilenameSplitter.getExtension(fileToReplaceWith).equalsIgnoreCase("wav")) {
          // if the fileToReplaceWith has a WAV extension, need to read it in to a Resource_RawAudio_WAV.
          // otherwise replace as raw
          return fileToReplaceWith;
        }

        if (!(resourceBeingReplaced instanceof Resource_WAV_RawAudio)) {
          // resource must already be in the right format
          return fileToReplaceWith;
        }

        Resource_WAV_RawAudio resourceWAV = (Resource_WAV_RawAudio) resourceBeingReplaced;

        //
        //
        // if we're here, we want to open the WAV file to read in the properties, then store it on the resourceBeingReplaced
        //
        //

        long arcSize = fileToReplaceWith.length();

        FileManipulator fm = new FileManipulator(fileToReplaceWith, false);

        // 4 - Header (RIFF)
        if (!fm.readString(4).equals("RIFF")) {
          return fileToReplaceWith;
        }

        // 4 - Length
        fm.skip(4);

        // 4 - Header 2 (WAVE)
        // 4 - Header 3 (fmt )
        if (!fm.readString(8).equals("WAVEfmt ")) {
          return fileToReplaceWith;
        }

        // 4 - Block Size (16)
        int blockSize = fm.readInt();
        FieldValidator.checkLength(blockSize, arcSize);

        // 2 - Format Tag (0x0001)
        short codec = fm.readShort();
        resourceWAV.setCodec(codec);

        // 2 - Channels
        short channels = fm.readShort();
        resourceWAV.setChannels(channels);

        // 4 - Samples per Second (Frequency)
        int frequency = fm.readInt();
        resourceWAV.setFrequency(frequency);

        // 4 - Average Bytes per Second ()
        fm.skip(4);

        // 2 - Block Alignment (bits/8 * channels)
        short blockAlign = fm.readShort();
        resourceWAV.setBlockAlign(blockAlign);

        // 2 - Bits Per Sample (bits)
        short bitrate = fm.readShort();
        resourceWAV.setBitrate(bitrate);

        // X - Extra Data
        int extraLength = blockSize - 16;
        byte[] extraData = fm.readBytes(extraLength);
        resourceWAV.setExtraData(extraData);

        // 4 - Header (fact) or (data)
        while (fm.getOffset() < arcSize) {
          String header = fm.readString(4);
          if (header.equals("fact")) {
            // 4 - Data Length 
            fm.skip(4);

            // 4 - Number of Samples 
            int samples = fm.readInt();

            resourceWAV.setSamples(samples);
          }
          else if (header.equals("data")) {
            // 4 - Data Length 
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // X - Raw Audio Data
            File destination = new File(fileToReplaceWith.getAbsolutePath() + "_ge_converted.raw");
            if (destination.exists()) {
              destination.delete();
            }
            FileManipulator outFM = new FileManipulator(destination, true);

            outFM.writeBytes(fm.readBytes(length));

            outFM.close();
            fm.close();
            return destination;
          }
          else {
            // 4 - Data Length 
            int length = fm.readInt();
            try {
              FieldValidator.checkLength(length, arcSize);
            }
            catch (Throwable t) {
              fm.close();
              return fileToReplaceWith;
            }

            // X - Unknown
            fm.skip(length);
          }
        }

        fm.close();

        return fileToReplaceWith;
      }
      else {
        return fileToReplaceWith;
      }
    }
    catch (Throwable t) {
      return fileToReplaceWith;
    }
  }

}
