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
import java.util.Arrays;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WB() {

    super("WB", "WB");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Die Hard Trilogy 2");
    setExtensions("wb"); // MUST BE LOWER CASE
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

      // 2 - Unknown (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 2 - Number of Names
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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
  @SuppressWarnings("unused")
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

      // 2 - Unknown (1)
      fm.skip(2);

      // 2 - Number of Names
      short numNames = fm.readShort();

      String[] names = new String[numNames];
      int[] nameIDs = new int[numNames];
      int[] nameIDsSorted = new int[numNames];
      for (int i = 0; i < numNames; i++) {
        // 8 - Filename (null terminated, filled with nulls)
        String name = fm.readNullString(8);
        FieldValidator.checkFilename(name);
        names[i] = name;

        // 4 - First File ID with this name?
        int nameID = fm.readInt();
        nameIDs[i] = nameID;
        nameIDsSorted[i] = nameID;
      }

      Arrays.sort(nameIDsSorted);

      // 4 - Unknown (4)
      fm.skip(4);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number of 8-Byte Entries
      short num8Byte = fm.readShort();
      FieldValidator.checkNumFiles(num8Byte + 1); // +1 to allow for 0 entries

      // 4 - End-Directory Block Size (60)
      int dataOffset = fm.readInt() + 12 + numFiles * 72 + num8Byte * 8 + (int) fm.getOffset();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Archive Length (kinda)
      // 4 - null
      // 4 - Unknown (14)
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int currentNameIndex = 0;
      int currentNameID = nameIDsSorted[currentNameIndex];
      String currentName = "";
      for (int n = 0; n < numNames; n++) {
        if (nameIDs[n] == currentNameID) {
          currentName = names[n] + File.separatorChar;
          break;
        }
      }

      int audioCounter = 1;
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (-1)
        // 8 - null
        // 4 - Unknown (100)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Flags (16 = padding entry, not a real file)
        fm.skip(36);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + dataOffset;

        // 4 - File Length
        int length = fm.readInt();

        // 4 - Bitrate? (16)
        int bitrate = fm.readInt();

        // 4 - Frequency (22050/11025)
        int frequency = fm.readInt();

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Flags
        fm.skip(20);

        if (length == 0) {
          // padding entry, not a real file
          continue;
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        //String filename = currentName + "Audio" + audioCounter + ".wav";
        String filename = Resource.generateFilename(realNumFiles) + ".wav";

        //path,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(frequency, bitrate, 1);
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(i);

        audioCounter++;

        if (i >= currentNameID && i + 1 < numFiles && currentNameIndex + 1 < numNames) {
          // move to the next nameID
          currentNameIndex++;
          currentNameID = nameIDsSorted[currentNameIndex];
          for (int n = 0; n < numNames; n++) {
            if (nameIDs[n] == currentNameID) {
              currentName = names[n] + File.separatorChar;
              audioCounter = 1;
              break;
            }
          }
        }
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
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

      // Write Header Data

      // 2 - Unknown (1/3)
      fm.writeBytes(src.readBytes(2));

      // 2 - Number of Names
      int numNames = src.readShort();
      fm.writeShort(numNames);

      // for each entry
      //   8 - Filename (null terminated, filled with nulls)
      //   4 - Unknown
      fm.writeBytes(src.readBytes(numNames * 12));

      // 4 - Unknown (4)
      fm.writeBytes(src.readBytes(4));

      // 2 - Number of Files
      int srcNumFiles = src.readShort();
      fm.writeShort(srcNumFiles);

      // 2 - Number of 8-Byte Entries
      int num8Bytes = src.readShort();
      fm.writeShort(num8Bytes);

      // 4 - End-Directory Block Size (60)
      int endDirBlockLength = src.readInt();
      fm.writeInt(endDirBlockLength);

      // 4 - Archive Length (kinda)
      // 4 - null
      // 4 - Unknown (14)
      fm.writeBytes(src.readBytes(12));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      int currentFile = 0;
      int totalSrcLength = 0;
      for (int i = 0; i < srcNumFiles; i++) {

        // 4 - Unknown (-1)
        // 8 - null
        // 4 - Unknown (100)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown  
        // 4 - Flags (16 = padding entry, not a real file)
        fm.writeBytes(src.readBytes(36));

        // 4 - File Offset (relative to the start of the File Data)
        // 4 - File Length
        int srcOffset = src.readInt();
        int srcLength = src.readInt();

        if (srcLength == 0) {
          // padding entry - copy entirely from source
          fm.writeInt(srcOffset);
          fm.writeInt(srcLength);
          fm.writeBytes(src.readBytes(28));
          continue;
        }

        Resource resource = resources[currentFile];
        long length = resource.getDecompressedLength();

        fm.writeInt(offset);
        fm.writeInt(length);

        totalSrcLength += srcLength;

        // 4 - Bitrate? (16)
        fm.writeInt(((Resource_WAV_RawAudio) resource).getBitrate());
        src.skip(4);

        // 4 - Frequency (22050/11025)
        fm.writeInt(((Resource_WAV_RawAudio) resource).getFrequency());
        src.skip(4);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Flags
        fm.writeBytes(src.readBytes(20));

        offset += length;
        currentFile++;
      }

      // for each 8-byte entry
      //   4 - Unknown
      //   4 - Unknown
      fm.writeBytes(src.readBytes(num8Bytes * 8));

      // 60 - End of Directory Block
      fm.writeBytes(src.readBytes(endDirBlockLength));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin defaultExporter = Exporter_Default.getInstance();

      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
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

      src.skip(totalSrcLength);

      // FOOTER
      //   X - Unknown
      int footerLength = (int) (src.getLength() - src.getOffset());
      if (footerLength > 0) {
        fm.writeBytes(src.readBytes(footerLength));
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
