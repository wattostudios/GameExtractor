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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BNK_XBNK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BNK_XBNK() {

    super("BNK_XBNK", "BNK_XBNK");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Score Hero");
    setExtensions("bnk"); // MUST BE LOWER CASE
    setPlatforms("android");

    // Read in WAV audio files and convert them
    setCanConvertOnReplace(true);

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("XBNK")) {
        rating += 50;
      }

      // 2 - Version Major (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 2 - Version Minor (2)
      if (fm.readShort() == 2) {
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

      // 4 - Header (XBNK)
      // 2 - Version Major (1)
      // 2 - Version Minor (2)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] lengths = new int[numFiles];
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - null
        fm.skip(4);
        TaskProgressManager.setValue(i);
      }

      // quick reads
      fm.getBuffer().setBufferSize(20);
      fm.seek(1);

      // Loop through directory

      for (int i = 0; i < numFiles; i++) {
        int offset = offsets[i];
        int length = lengths[i];

        fm.seek(offset);

        // 2 - Codec
        short codec = fm.readShort();

        // 2 - Channels
        short channels = fm.readShort();

        // 4 - Frequency
        int frequency = fm.readInt();

        // 4 - Number of Samples
        int numSamples = fm.readInt();

        // 2 - Block Align
        short blockAlign = fm.readShort();

        // 2 - Bit Depth
        short bitrate = fm.readShort();

        String filename = Resource.generateFilename(i);

        if (codec >= 0xfff0) {
          // OGG
          // 4 - File Length?

          offset += 20;
          length -= 20;

          filename += ".ogg";

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

        }
        else if (codec == 2 || codec == 1) { // 1 is only to allow you to import converted RAW audio files
          // MS-ADPCM
          // 2 - Extra Data Length
          short extraLength = fm.readShort();
          // X - Extra Data
          byte[] extraData = fm.readBytes(extraLength);

          offset += 16 + 2 + extraLength;
          length -= (16 + 2 + extraLength);

          filename += ".wav";

          //path,name,offset,length,decompLength,exporter
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
          resource.setAudioProperties(frequency, bitrate, channels);
          resource.setCodec(codec);
          resource.setSamples(numSamples);
          resource.setBlockAlign(blockAlign);
          resource.setExtraData(extraData);
          resources[i] = resource;
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 4 - Header (XBNK)
      fm.writeString("XBNK");

      // 2 - Version Major (1)
      fm.writeShort(1);

      // 2 - Version Minor (2)
      fm.writeShort(2);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 12 + (numFiles * 12);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        if (resource instanceof Resource_WAV_RawAudio) {
          Resource_WAV_RawAudio resourceWAV = (Resource_WAV_RawAudio) resource;
          decompLength += 16;
          if (resource.getExtension().equals("ogg")) {
            decompLength += 4;
          }
          else {
            byte[] extraData = resourceWAV.getExtraData();
            if (extraData != null) {
              decompLength += extraData.length + 2;
            }
          }
        }

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // 4 - null
        fm.writeInt(0);

        offset += decompLength;
      }

      // Write Files
      ExporterPlugin defaultExporter = Exporter_Default.getInstance();
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource instanceof Resource_WAV_RawAudio) {
          Resource_WAV_RawAudio resourceWAV = (Resource_WAV_RawAudio) resource;

          // 2 - Codec
          short codec = resourceWAV.getCodec();
          fm.writeShort(codec);
          // 2 - Channels
          fm.writeShort(resourceWAV.getChannels());
          // 4 - Frequency
          fm.writeInt(resourceWAV.getFrequency());
          // 4 - Number of Samples
          int samples = resourceWAV.getSamples();
          if (samples == -1) {
            samples = 0;
          }
          fm.writeInt(samples);
          // 2 - Block Align
          fm.writeShort(resourceWAV.getBlockAlign());
          // 2 - Bit Depth
          fm.writeShort(resourceWAV.getBitrate());

          if (codec >= 0xfff0) {
            // OGG
            // 4 - File Length?
            fm.writeInt(resource.getDecompressedLength());
          }
          else if (codec == 2 || codec == 1) {
            // MS-ADPCM
            // 2 - Extra Data Length
            // X - Extra Data
            byte[] extraData = resourceWAV.getExtraData();
            if (extraData == null) {
              fm.writeShort(0);
            }
            else {
              fm.writeShort(extraData.length);
              fm.writeBytes(extraData);
            }
          }
        }

        if (resource.isReplaced()) {
          write(resource, fm);
        }
        else {
          // write the data raw from the archive (so we don't put the WAV header on it, just want the raw data)
          ExporterPlugin exporter = resource.getExporter();
          resource.setExporter(defaultExporter);
          write(resource, fm);
          resource.setExporter(exporter);
        }
        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   When replacing TEX images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a TEX image. All other files are replaced without conversion
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
