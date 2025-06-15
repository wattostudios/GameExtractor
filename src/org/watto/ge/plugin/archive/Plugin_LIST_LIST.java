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

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LIST_LIST extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LIST_LIST() {

    super("LIST_LIST", "LIST_LIST");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Aliens Versus Predator: Extinction");
    setExtensions("list"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // Header
      if (fm.readString(4).equals("LIST")) {
        rating += 25;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("XBOX")) {
        rating += 25;
      }

      // Header
      if (fm.readString(4).equals("TYPE")) {
        rating += 5;
      }

      // Type Length
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

      // 4 - Header (LIST)
      // 4 - Block Length (not including these 2 header fields)
      // 4 - Header (XBOX)
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      long[] offsets = null;

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long blockOffset = fm.getOffset();

        // 4 - Block Header
        String header = fm.readString(4);

        // 4 - Block Length (not including these 2 header fields)
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength, arcSize);

        if (header.equals("OFFS")) {
          // offsets

          // 4 - Number of Entries
          int numEntries = fm.readInt();

          offsets = new long[numEntries];

          for (int i = 0; i < numEntries; i++) {
            // 4 - File Offset (relative to the start of the File Data DATA block) (if the top byte is =32, this is in an external file or something)
            int offset = fm.readInt();
            if ((offset & 536870912) == 536870912) {
              // an external file or something
              offsets[i] = -1;
            }
            else {
              //FieldValidator.checkOffset(offset, arcSize);
              offsets[i] = offset;
            }
          }

        }
        else if (header.equals("DATA") || header.equals("DATX")) {
          // data
          if (header.equals("DATX") || offsets == null) {
            // no offsets, so store as a plain data file
            String filename = Resource.generateFilename(0) + "." + header;

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, blockOffset, blockLength + 8);
            realNumFiles++;

            fm.skip(blockLength);
          }
          else {
            // store all the separate files in this DATA

            int numEntries = offsets.length;

            long relativeOffset = fm.getOffset();

            // set the correct offsets, and work out the lengths
            long[] lengths = new long[numEntries];

            for (int i = 0; i < numEntries; i++) {
              long offset = offsets[i];
              if (offset == -1) {
                // external file or something - skip
                continue;
              }

              // find the next valid offset (uses the blockLength if no further valid files are found)
              long nextOffset = blockLength;
              for (int j = i + 1; j < numEntries; j++) {
                if (offsets[j] != -1) {
                  nextOffset = offsets[j];
                  break;
                }
              }

              // set the length
              long length = nextOffset - offset;
              if (length < 0) {
                length = 0;
              }
              lengths[i] = length;

              // update the offset to be relative to the data offset
              offset += relativeOffset;
              offsets[i] = offset;
            }

            // now store the files
            int numDataFiles = 0;
            for (int i = 0; i < numEntries; i++) {
              long offset = offsets[i];
              if (offset == -1) {
                // external file or something - skip
                continue;
              }

              String filename = Resource.generateFilename(numDataFiles) + "." + header;
              numDataFiles++;

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, lengths[i]);
              realNumFiles++;
            }

            fm.skip(blockLength);
          }
        }
        else {
          // another block type, just add it as a file
          String filename = Resource.generateFilename(0) + "." + header;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, blockOffset, blockLength + 8);
          realNumFiles++;

          fm.skip(blockLength);
        }

        TaskProgressManager.setValue(fm.getOffset());

      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      // now go through and see if we can find the audio files
      fm.getBuffer().setBufferSize(32);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.getExtension().equals("DATA")) {
          fm.seek(resource.getOffset());

          // for audio files...
          // 4 - Unknown
          // 4 - null
          // 2 - Unknown (1)
          // 2 - Unknown (1)
          // 4 - Frequency 1
          // 4 - Frequency 2
          // 2 - Channels? (2)
          // 2 - Bitrate? (16)
          // X - Audio Data

          fm.skip(4);
          if (fm.readInt() == 0 && fm.readInt() == 65537) {
            // maybe an audio file

            int frequency = fm.readInt();
            fm.skip(4); // want frequency1 which seems to be right

            short channels = fm.readShort();
            channels = 1; // overwrite - mono seems to work better

            short bitrate = fm.readShort();
            if (frequency > 0 && frequency < 45000 && (channels == 1 || channels == 2) && bitrate > 0 && bitrate < 50) {

              long offset = fm.getOffset();
              long length = resource.getLength() - 28;
              String filename = resource.getName() + ".wav";

              // likely an audio file
              Resource_WAV_RawAudio resourceWAV = new Resource_WAV_RawAudio(path, filename, offset, length);
              resourceWAV.setAudioProperties(frequency, bitrate, channels);
              resources[i] = resourceWAV;

            }
          }
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt2 == 0 && headerInt3 == 65537) {
      return "wav";
    }

    return null;
  }

}
