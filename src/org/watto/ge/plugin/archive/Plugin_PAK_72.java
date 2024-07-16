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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_72 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_72() {

    super("PAK_72", "PAK_72");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Outrun 2006: Coast 2 Coast");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

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
      fm.skip(4);
      long arcSize = fm.getLength();

      // First File Length
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

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        if (length == -1) {
          // EOF
          break;
        }
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + ".wav";

        //path,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(11025, 16, 1);
        //Resource resource = new Resource(path, filename, offset, length);
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        if (resource.isReplaced()) {
          // changed file

          // 4 - Unknown
          fm.writeBytes(src.readBytes(4));

          // 4 - File Length
          int srcLength = src.readInt();
          fm.writeInt(resource.getLength());

          // X - File Data
          src.skip(srcLength);
          write(resource, fm);

        }
        else {
          // write exactly as it is in the source

          // 4 - Unknown
          fm.writeBytes(src.readBytes(4));

          // 4 - File Length
          int srcLength = src.readInt();
          fm.writeInt(srcLength);

          // X - File Data
          fm.writeBytes(src.readBytes(srcLength));
        }

        TaskProgressManager.setValue(i);

      }

      // 4 - EOF Marker (-1)
      fm.writeInt(-1);
      // 4 - EOF Marker (-1)
      fm.writeInt(-1);

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
