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

package org.watto.ge.plugin.exporter;

import java.io.File;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.Plugin_FSB_FSB5;
import org.watto.ge.plugin.resource.Resource_FSB_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

public class Exporter_Custom_FSB5_ProcessWithinArchive extends ExporterPlugin {

  static Exporter_Custom_FSB5_ProcessWithinArchive instance = new Exporter_Custom_FSB5_ProcessWithinArchive();

  /**
  **********************************************************************************************
  This exporter will read an FSB file from within an existing archive, and if there is only 1
  audio file in it, it will forward it to one of the following exporters for handling...
    Exporter_Custom_FSB5_OGG
    Exporter_Custom_FSB_Audio
  The reading code is based on Plugin_FSB_FSB5, which reads data from a standalone(exported) file
  **********************************************************************************************
  **/
  public static Exporter_Custom_FSB5_ProcessWithinArchive getInstance() {
    return instance;
  }

  ExporterPlugin exporter = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FSB5_ProcessWithinArchive() {
    setName("FSB5 Audio Exporter");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (exporter == null) {
      return false;
    }
    return exporter.available();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    if (exporter == null) {
      return;
    }
    exporter.close();
    exporter = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter adds the appropriate audio header bytes when extracting from an FSB FMOD Soundbank archive\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public void open(Resource source) {
    try {
      if (exporter == null) {

        // a new file - process the FSB and work out what exporter it should really use

        File path = source.getSource();
        FileManipulator fm = new FileManipulator(path, false);

        long relOffset = source.getOffset();
        fm.seek(relOffset);

        // 4 - Header (FSB5)
        String header = fm.readString(4);
        if (header.equals("FSB5")) {
          // FSB5

          // 4 - Version (1)
          fm.skip(4);

          // 4 - Number Of Files
          int numFiles = fm.readInt();
          FieldValidator.checkNumFiles(numFiles);

          if (numFiles != 1) {
            // can only preview it if it contains a single audio file
            throw new Exception();
          }

          boolean debug = Settings.getBoolean("DebugMode");

          long arcSize = source.getLength();

          // 4 - Directory Length
          long dirOffset = relOffset + fm.readInt() + 60;
          FieldValidator.checkOffset(dirOffset, relOffset + arcSize);

          // 4 - Name Length
          // 4 - Data Length
          fm.skip(8);

          // 4 - Mode (Codec)
          int mode = fm.readInt();
          if (debug) {
            System.out.println("[Exporter_Custom_FSB5_ProcessWithinArchive] Codec/Mode is " + mode + " (" + Resource_FSB_Audio.getExtensionForCodec(mode) + ")");
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
          for (int i = 0; i < numFiles; i++) {
            //000000000100001101010011011110 0000000000000000000000000000 1 1000 1
            long sampleDetails = fm.readLong();

            int nextChunk = (int) ((sampleDetails & 1));
            int frequencyMap = (int) ((sampleDetails >> 1) & 15);
            channels[i] = (int) ((sampleDetails >> 5) & 1) + 1;
            int dataOffset = (int) ((sampleDetails >> 6) & 268435455) * 16;
            int samples = (int) ((sampleDetails >> 34) & 1073741823);

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

              if (chunkType == Plugin_FSB_FSB5.CHUNK_VORBIS) {
                // 4 - CRC
                int crc = fm.readInt();
                crcs[i] = crc;

                if (!new File(Settings.getString("OGG_Headers_Path") + File.separator + "setupTable_" + IntConverter.unsign(crc) + ".ogg").exists()) {
                  System.out.println("[Exporter_Custom_FSB5_ProcessWithinArchive] Missing CRC " + IntConverter.unsign(crc) + " found in file " + source.getName());
                }

                // X - Unknown
                fm.skip(chunkSize - 4);
              }
              else if (chunkType == Plugin_FSB_FSB5.CHUNK_CHANNELS) {
                // 1 - Number of Channels
                fm.skip(1);
              }
              else if (chunkType == Plugin_FSB_FSB5.CHUNK_FREQUENCY) {
                // 4 - Frequency
                fm.skip(4);
              }
              else if (chunkType == Plugin_FSB_FSB5.CHUNK_LOOP) {
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

          fm.seek(dirOffset); // This will skip over the name table, under the assumption that there are no names in the file

          // Now go through the archive
          //for (int i = 0; i < numFiles; i++) {
          if (mode == Plugin_FSB_FSB5.CODEC_VORBIS) {
            //
            // OGG
            //
            int maxChunks = Archive.getMaxFiles();
            long[] offsets = new long[maxChunks];
            long[] lengths = new long[maxChunks];

            /*
            int numChunks = 1; // we're going to use the first entry to be the ogg header from a source file
            
            // insert ogg header
            offsets[0] = 0;
            lengths[0] = 4303;
            */
            int numChunks = 0;

            long endOfFile = arcSize + relOffset;
            while (fm.getOffset() < endOfFile) {
              // 2 - Chunk Length
              int length = ShortConverter.unsign(fm.readShort());
              lengths[numChunks] = length;

              // X - Chunk Data
              offsets[numChunks] = fm.getOffset();
              fm.skip(length);

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

            // Need a proper wrapper for this, which writes the ogg headers, and splits the ogg file data into chunks 
            int approxLength = (int) (arcSize);

            //path,id,name,offset,length,decompLength,exporter
            Resource_FSB_Audio resource = new Resource_FSB_Audio(path, "", dirOffset, approxLength);
            resource.setChannels((short) channels[0]);
            resource.setFrequency(frequencies[0]);
            resource.setSetupCRC(crcs[0]);

            // use the new Source for reading the file. see exporter.open() down further.
            exporter = new Exporter_Custom_FSB5_OGG(offsets, lengths);
            source = resource;
          }
          else {
            // 
            // MP3/WAV/etc.
            //
            long offset = fm.getOffset();
            long length = (relOffset + arcSize) - offset;

            //path,id,name,offset,length,decompLength,exporter
            Resource_FSB_Audio resource = new Resource_FSB_Audio(path, "", offset, length);
            resource.setCodec(mode);
            resource.setChannels((short) channels[0]);
            resource.setFrequency(frequencies[0]);

            if (mode == Plugin_FSB_FSB5.CODEC_PCM8) {
              resource.setBits((short) 8);
            }
            else if (mode == Plugin_FSB_FSB5.CODEC_PCM16) {
              resource.setBits((short) 16);
            }
            else if (mode == Plugin_FSB_FSB5.CODEC_PCM24) {
              resource.setBits((short) 24);
            }
            else if (mode == Plugin_FSB_FSB5.CODEC_PCM32) {
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

            // use the new Source for reading the file. see exporter.open() down further.
            exporter = Exporter_Custom_FSB_Audio.getInstance();
            source = resource;

          }
          //}

          fm.close();

        }
        else {
          // Unsupported FSB type
          throw new Exception();
        }

      }
    }
    catch (Throwable t) {
    }

    if (exporter == null) { // just in case
      // Default Exporter in all failed cases
      exporter = Exporter_Default.getInstance();
    }
    exporter.open(source);

  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    if (exporter == null) {
      return;
    }
    exporter.pack(source, destination);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    if (exporter == null) {
      return 0;
    }
    return exporter.read();
  }

}