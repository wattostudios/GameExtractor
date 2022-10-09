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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SBK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SBK() {

    super("SBK", "SBK");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Driver 2");
    setExtensions("sbk"); // MUST BE LOWER CASE
    setPlatforms("PSX");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      if (fm.readInt() == 0) {
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

      ExporterPlugin exporterVAG = Exporter_Custom_VAG_Audio.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long dataOffset = 4 + (numFiles * 16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (relative to the start of the file)
        long offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Loop (1)
        int loop = fm.readInt();

        // 4 - Audio Frequency? (8000/12000)
        int frequency = fm.readInt();

        if (loop == 0) {
          length -= 16;
        }

        int decompLength = (length >> 4) * 28 * 2;

        String filename = Resource.generateFilename(i) + ".wav";

        //path,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length, decompLength);
        resource.addProperty("Loop", loop);
        resource.setAudioProperties(frequency, (short) 16, (short) 1, true);
        resource.setExporter(exporterVAG);

        resources[i] = resource;

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

        if (channels != 1) {
          ErrorLogger.log("[SBK] Converting WAV into VAG: Source is not MONO, conversion might not be successful.");
        }

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

        if (bitrate != 16) {
          ErrorLogger.log("[SBK] Converting WAV into VAG: Source is not 16-bit, conversion might not be successful.");
        }

        // X - Extra Data
        int extraLength = blockSize - 16;
        byte[] extraData = fm.readBytes(extraLength);
        resourceWAV.setExtraData(extraData);

        Resource dataResource = null;

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
          else if (header.equals("smpl")) {
            // 4 - Data Length
            int blockLength = fm.readInt();

            if (blockLength < 60) {
              fm.skip(blockLength);
            }
            else {
              // 4 Manufacturer  0 - 0xFFFFFFFF
              // 4 Product 0 - 0xFFFFFFFF
              // 4 Sample Period 0 - 0xFFFFFFFF
              // 4 MIDI Unity Note 0 - 127
              // 4 MIDI Pitch Fraction 0 - 0xFFFFFFFF
              // 4 SMPTE Format  0, 24, 25, 29, 30
              // 4 SMPTE Offset  0 - 0xFFFFFFFF
              fm.skip(28);

              // 4 Num Sample Loops  0 - 0xFFFFFFFF
              int loop = fm.readInt();
              resourceWAV.setProperty("Loop", "" + loop);

              // 4 Sampler Data  0 - 0xFFFFFFFF
              fm.skip(4);

              blockLength -= 36;
              fm.skip(blockLength);
            }
          }
          else if (header.equals("data")) {
            // 4 - Data Length 
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // prepare the resource, will extract it later on, after reading all the properties
            dataResource = new Resource(fileToReplaceWith, "", fm.getOffset(), length);

            fm.skip(length);
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

        // Now that we've read all the properties, go back and process the actual data

        if (dataResource != null) {
          // X - Raw Audio Data
          File destination = new File(fileToReplaceWith.getAbsolutePath() + "_ge_converted.raw");
          if (destination.exists()) {
            destination.delete();
          }

          // Read in the raw audio, convert it into VAG, then write it out.
          FileManipulator outFM = new FileManipulator(destination, true);
          dataResource.addProperty("Loop", resourceBeingReplaced.getProperty("Loop"));
          Exporter_Custom_VAG_Audio.getInstance().pack(dataResource, outFM);
          outFM.close();

          // DecompLength already set to the length of the WAV file
          // Length needs to be set to the length of the packed file
          long compressedLength = destination.length();
          resourceWAV.setLength(compressedLength);

          fm.close();

          return destination;
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

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getLength();

        int loop = 1;
        try {
          loop = Integer.parseInt(resource.getProperty("Loop"));
        }
        catch (Throwable t) {
        }

        int frequency = 8000;
        try {
          frequency = ((Resource_WAV_RawAudio) resource).getFrequency();
        }
        catch (Throwable t) {
        }

        // 4 - File Offset (relative to the start of the file)
        fm.writeInt((int) offset);

        if (loop == 0 && !resource.isReplaced()) { // if it's been replaced, we're already adding the length, don't want to add it again
          length += 16;
        }

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - Loop (1)
        fm.writeInt((int) loop);

        // 4 - Audio Frequency? (8000/12000)
        fm.writeInt((int) frequency);

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        ExporterPlugin origExporter = resource.getExporter();
        resource.setExporter(exporterDefault);

        int loop = 1;
        try {
          loop = Integer.parseInt(resource.getProperty("Loop"));
        }
        catch (Throwable t) {
        }

        long origLength = resource.getLength();
        long length = origLength;
        if (loop == 0 && !resource.isReplaced()) { // if it's been replaced, we're already adding the length, don't want to add it again
          length += 16;
        }

        resource.setLength(length);

        write(resource, fm);
        TaskProgressManager.setValue(i);

        resource.setLength(origLength);
        resource.setExporter(origExporter);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
