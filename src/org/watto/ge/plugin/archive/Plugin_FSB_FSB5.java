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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB5_MP3;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB5_OGG;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB_Audio;
import org.watto.ge.plugin.resource.Resource_FSB_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FSB_FSB5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FSB_FSB5() {

    super("FSB_FSB5", "FSB_FSB5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("fsb");
    setGames("SOMA",
        "The Sims FreePlay",
        "A Roll-Back Story",
        "Object Cleaning",
        "Splash Blast Panic",
        "Sure Footing",
        "Transistor");
    setPlatforms("PC");

    //setFileTypes("spr", "Object Sprite");

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

      // 4 - Header (FSB5)
      String header = fm.readString(4);
      if (header.equals("FSB5")) {
        rating += 50;
      }
      else {
        int headerInt = IntConverter.convertLittle(header.getBytes());
        if (headerInt == 1626646592) {
          // might be encrypted with standard password "DFm3t4lFTW"
          rating += 25;
          return rating;
        }
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  public static int CHUNK_CHANNELS = 1;

  public static int CHUNK_FREQUENCY = 2;

  public static int CHUNK_LOOP = 3;

  public static int CHUNK_XMASEEK = 6;

  public static int CHUNK_DSPCOEFF = 7;

  public static int CHUNK_XWMADATA = 10;

  public static int CHUNK_VORBIS = 11;

  public static int CODEC_NONE = 0;

  public static int CODEC_PCM8 = 1; // WAV

  public static int CODEC_PCM16 = 2; // WAV

  public static int CODEC_PCM24 = 3;

  public static int CODEC_PCM32 = 4; // WAV

  public static int CODEC_PCMFLOAT = 5;

  public static int CODEC_GCADPCM = 6;

  public static int CODEC_IMAADPCM = 7;

  public static int CODEC_VAG = 8;

  public static int CODEC_HEVAG = 9;

  public static int CODEC_XMA = 10;

  public static int CODEC_MPEG = 11; // MP3

  public static int CODEC_CELT = 12;

  public static int CODEC_AT9 = 13;

  public static int CODEC_XWMA = 14;

  public static int CODEC_VORBIS = 15; // OGG

  public static int CODEC_IT214 = 16;

  public static int CODEC_IT215 = 17;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      // THIS IS A SPECIAL CASE - WE HAVE SPLIT THE read() INTO A SEPARATE read(fm) METHOD, SO THAT WE CAN CALL IT
      // NORMALLY HERE, BUT ALSO CALL IT FROM Plugin_BANK_RIFF WHERE AN FSB5 FILE IS EMBEDDED PART-WAY THROUGH THE ARCHIVE.

      FileManipulator fm = new FileManipulator(path, false);

      Resource[] resources = read(fm);

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
  int fsbdec(int t) {
    return ((((((((t & 64) | (t >> 2)) >> 2) | (t & 32)) >> 2) | (t & 16)) >> 1) |
        (((((((t & 2) | (t << 2)) << 2) | (t & 4)) << 2) | (t & 8)) << 1));
  }

  /**
   **********************************************************************************************
   Decrypts an archive. If the decrypted file already exists, we use that, we don't re-decrypt.
   **********************************************************************************************
   **/
  public FileManipulator decryptArchive(FileManipulator fm, String password) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(0); // return to the start, ready for decryption

      int compLength = (int) fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      byte[] keyBytes = password.getBytes();
      int keyLength = keyBytes.length;
      int[] key = new int[keyLength];
      for (int i = 0; i < keyLength; i++) {
        key[i] = ByteConverter.unsign(keyBytes[i]);
      }

      int keyPos = 0;
      for (int i = 0; i < compLength; i++) {
        decompFM.writeByte(fsbdec(ByteConverter.unsign(fm.readByte())) ^ key[keyPos]);
        keyPos++;

        if (keyPos >= keyLength) {
          keyPos = 0;
        }
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] read(FileManipulator fm) {
    try {

      // THIS IS A SPECIAL CASE - WE HAVE SPLIT THE read() INTO A SEPARATE read(fm) METHOD, SO THAT WE CAN CALL IT
      // NORMALLY HERE, BUT ALSO CALL IT FROM Plugin_BANK_RIFF WHERE AN FSB5 FILE IS EMBEDDED PART-WAY THROUGH THE ARCHIVE.

      File path = fm.getFile();

      addFileTypes();

      boolean debug = Settings.getBoolean("DebugMode");

      long arcSize = fm.getLength();

      int realNumFiles = 0;
      int maxNumFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[maxNumFiles];
      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < arcSize - 32 && realNumFiles < maxNumFiles) { // This loop is to support Unity3D "resource" files that list multiple FSB5's in a single file
        long startOffset = fm.getOffset();

        // 4 - Header (FSB5)
        String header = fm.readString(4);
        int headerInt = IntConverter.convertLittle(header.getBytes());
        if (headerInt == 1626646592) {
          // might be encrypted with standard password "DFm3t4lFTW"
          // decrypt the archive

          FileManipulator decompFM = decryptArchive(fm, "DFm3t4lFTW");
          if (decompFM != null) {
            fm.close(); // close the original archive
            fm = decompFM; // now we're going to read from the decrypted file instead
            fm.seek(0); // go to the same point in the decrypted file as in the compressed file

            path = fm.getFile(); // So the resources are stored against the decrypted file
            header = fm.readString(4); // read the header, which was already read above in non-encrypted archives
          }
        }

        while (header.equals("SND ")) {
          // FSB file within

          // 4 - File Data Length
          int fileLength = fm.readInt();
          FieldValidator.checkLength(fileLength);

          // 0-31 - null Padding to a multiple of 32 bytes
          fm.skip(calculatePadding(fm.getOffset(), 32));

          // 4 - FSB header
          header = fm.readString(4);
        }

        // 4 - Version (1)
        fm.skip(4);

        // 4 - Number Of Files
        int numFiles = fm.readInt();
        try {
          FieldValidator.checkNumFiles(numFiles);
        }
        catch (Throwable t) {
          if (realNumFiles > 5) {
            // we have found a few files, so break early if there's a problem, rather than rejecting completely
            break;
          }
        }

        // 4 - Directory Length
        long dirOffset = startOffset + fm.readInt() + 60;
        FieldValidator.checkOffset(dirOffset, arcSize);

        // 4 - Name Length
        long nameLength = fm.readInt();
        FieldValidator.checkLength(nameLength, arcSize);

        long fileDataOffset = dirOffset + nameLength;
        FieldValidator.checkOffset(fileDataOffset, arcSize);

        // 4 - Data Length
        int dataLength = fm.readInt();
        FieldValidator.checkLength(dataLength);

        // 4 - Mode (Codec)
        int mode = fm.readInt();
        if (debug) {
          System.out.println("[Plugin_FSB_FSB5] Codec/Mode is " + mode);
        }

        // 4 - Flags
        int flags = fm.readInt();

        // 4 - null
        // 24 - Hash?
        fm.skip(28);

        // Process each file (sample)
        int[] channels = new int[numFiles];
        int[] frequencies = new int[numFiles];
        int[] chunkTypes = new int[numFiles];
        int[] crcs = new int[numFiles];

        int[] sampleLengths = new int[numFiles];
        int[] dataOffsets = new int[numFiles];
        for (int i = 0; i < numFiles; i++) {
          //000000000100001101010011011110 0000000000000000000000000000 1 1000 1
          long sampleDetails = fm.readLong();

          int nextChunk = (int) ((sampleDetails & 1));
          int frequencyMap = (int) ((sampleDetails >> 1) & 15);
          channels[i] = (int) ((sampleDetails >> 5) & 1) + 1;
          int dataOffset = (int) ((sampleDetails >> 6) & 268435455) * 16;
          int samples = (int) ((sampleDetails >> 34) & 1073741823);

          dataOffsets[i] = (int) (dataOffset + fileDataOffset);
          sampleLengths[i] = samples;

          // convert the frequency id to a real frequency
          if (frequencyMap == 1) {
            frequencies[i] = 8000;
          }
          else if (frequencyMap == 2) {
            frequencies[i] = 11000;
          }
          else if (frequencyMap == 3) {
            frequencies[i] = 11025;
          }
          else if (frequencyMap == 4) {
            frequencies[i] = 16000;
          }
          else if (frequencyMap == 5) {
            frequencies[i] = 22050;
          }
          else if (frequencyMap == 6) {
            frequencies[i] = 24000;
          }
          else if (frequencyMap == 7) {
            frequencies[i] = 32000;
          }
          else if (frequencyMap == 8) {
            frequencies[i] = 44100;
          }
          else if (frequencyMap == 9) {
            frequencies[i] = 48000;
          }

          while (nextChunk == 1) {
            //0001011 000000000000000011010000 0
            int chunkDetails = fm.readInt();

            nextChunk = (int) ((chunkDetails & 1));
            int chunkSize = (int) ((chunkDetails >> 1) & 16777215);
            int chunkType = (int) ((chunkDetails >> 25) & 127);
            chunkTypes[i] = chunkType;

            if (chunkType == CHUNK_VORBIS) {
              // 4 - CRC
              int crc = fm.readInt();
              crcs[i] = crc;

              if (!new File(Settings.getString("OGG_Headers_Path") + File.separator + "setupTable_" + IntConverter.unsign(crc) + ".ogg").exists()) {
                System.out.println("[Plugin_FSB_FSB5] Missing CRC " + IntConverter.unsign(crc) + " found in file " + path.getName());
              }

              // X - Unknown
              fm.skip(chunkSize - 4);
            }
            else if (chunkType == CHUNK_CHANNELS) {
              // 1 - Number of Channels
              fm.skip(1);
            }
            else if (chunkType == CHUNK_FREQUENCY) {
              // 4 - Frequency
              fm.skip(4);
            }
            else if (chunkType == CHUNK_LOOP) {
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(8);
            }
            else {
              // X - Unknown
              fm.skip(chunkSize);
            }

          }
        }

        // Get the FSB Audio exporter
        //ExporterPlugin exporter = Exporter_Custom_FSB_Audio.getInstance();
        ExporterPlugin exporterGeneric = Exporter_Custom_FSB_Audio.getInstance();
        ExporterPlugin exporterMP3 = Exporter_Custom_FSB5_MP3.getInstance();

        // Read the names table
        fm.seek(dirOffset);
        String[] names = null;
        if (nameLength != 0) {
          fm.skip(numFiles * 4);

          names = new String[numFiles];
          for (int i = 0; i < numFiles; i++) {
            // X - Name
            // 1 - null Name Terminator
            String name = fm.readNullString();
            names[i] = name;
          }
        }

        fm.seek(fileDataOffset);

        // Now go through the archive
        for (int i = 0; i < numFiles; i++) {
          if (mode == CODEC_VORBIS) {
            //
            // OGG
            //
            long offset = dataOffsets[i];
            fm.relativeSeek(offset);

            long length = 0;
            if (i == numFiles - 1) {
              //length = arcSize - offset;
              length = (startOffset + dataLength);// - offset; // 3.14 dataLength already contains the actual length of the data (github issue #9)
            }
            else {
              length = dataOffsets[i + 1] - offset;
            }
            long endOffset = offset + length;

            int maxChunks = Archive.getMaxFiles() * 2;
            long[] offsets = new long[maxChunks];
            long[] lengths = new long[maxChunks];

            /*
            int numChunks = 1; // we're going to use the first entry to be the ogg header from a source file
            
            // insert ogg header
            offsets[0] = 0;
            lengths[0] = 4303;
            */
            int numChunks = 0;

            while (fm.getOffset() < endOffset) {
              // 2 - Chunk Length
              int chunkLength = ShortConverter.unsign(fm.readShort());
              lengths[numChunks] = chunkLength;

              // X - Chunk Data
              offsets[numChunks] = fm.getOffset();
              fm.skip(chunkLength);

              numChunks++;
            }

            // resize the array
            if (numChunks < maxChunks) {
              long[] oldOffsets = offsets;
              long[] oldLengths = lengths;

              offsets = new long[numChunks];
              lengths = new long[numChunks];

              System.arraycopy(oldOffsets, 0, offsets, 0, numChunks);
              System.arraycopy(oldLengths, 0, lengths, 0, numChunks);
            }

            String filename;
            if (names != null) {
              filename = names[i] + ".ogg";
            }
            else {
              filename = Resource.generateFilename(realNumFiles) + ".ogg";
            }

            // Need a proper wrapper for this, which writes the ogg headers, and splits the ogg file data into chunks 
            Exporter_Custom_FSB5_OGG oggExporter = new Exporter_Custom_FSB5_OGG(offsets, lengths);

            //path,id,name,offset,length,decompLength,exporter
            Resource_FSB_Audio resource = new Resource_FSB_Audio(path, filename, offset, length, length, oggExporter);
            resource.setChannels((short) channels[i]);
            resource.setFrequency(frequencies[i]);
            resource.setSetupCRC(crcs[i]);
            resource.forceNotAdded(true); // because we can call this from an ASSETS plugin
            resources[realNumFiles] = resource;
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
          else {
            // 
            // MP3/WAV/etc.
            //
            //long offset = fm.getOffset();
            //long length = arcSize - offset;
            long offset = dataOffsets[i];
            long length = 0;
            if (i == numFiles - 1) {
              //length = arcSize - offset;
              length = (startOffset + dataLength) - offset;
            }
            else {
              length = dataOffsets[i + 1] - offset;
            }

            String filename;
            if (names != null) {
              filename = names[i];
            }
            else {
              filename = Resource.generateFilename(realNumFiles);
            }

            //path,id,name,offset,length,decompLength,exporter
            Resource_FSB_Audio resource = new Resource_FSB_Audio(path, filename, offset, length, length, exporterGeneric);
            resource.setCodec(mode);
            resource.setChannels((short) channels[i]);
            resource.setFrequency(frequencies[i]);
            resource.setSamplesLength(sampleLengths[i]);

            //boolean multiChannel = ((mode & 0x04000000) == 0x04000000);
            //resource.addProperty("MultiChannel", multiChannel);

            offset += length;

            if (mode == CODEC_PCM8) {
              resource.setBits((short) 8);
            }
            else if (mode == CODEC_PCM16) {
              resource.setBits((short) 16);
            }
            else if (mode == CODEC_PCM24) {
              resource.setBits((short) 24);
            }
            else if (mode == CODEC_PCM32) {
              resource.setBits((short) 32);
            }
            else {
              // work out the bitrate
              short bits = 16;
              if ((flags & 0x00000008) == 0x00000008) {
                bits = 8;
              }
              else if ((flags & 0x00000010) == 0x00000010) {
                bits = 16;
              }
              resource.setBits(bits);
            }

            resource.addExtensionForCodec();
            resource.forceNotAdded(true); // because we can call this from an ASSETS plugin

            if (mode == CODEC_MPEG) {
              // special exporter for MP3's, fixes the frames etc.
              resource.setExporter(exporterMP3);
            }

            resources[realNumFiles] = resource;
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
        }

        fm.seek(fileDataOffset + dataLength); // move to the start of the next file
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}