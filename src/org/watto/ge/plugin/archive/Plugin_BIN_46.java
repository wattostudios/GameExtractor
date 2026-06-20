/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_RNC2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_46 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_46() {

    super("BIN_46", "BIN_46");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shadow Man: 2econd Coming");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      getDirectoryFile(fm.getFile(), "DIR");
      rating += 25;

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

      ExporterPlugin exporter = Exporter_RNC2.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "DIR");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown
      fm.skip(4);

      // Read the folders until we reach the end
      int numFolders = Archive.getMaxFiles();
      int realNumFolders = 0;
      int expectedNumber = 0;

      int[] filesInFolders = new int[numFolders];

      long dirFileLength = sourcePath.length();
      while (fm.getOffset() < dirFileLength) {
        // 4 - First File in this Folder
        int firstFile = fm.readInt();
        if (firstFile != expectedNumber) {
          fm.relativeSeek(fm.getOffset() - 4);
          break;
        }
        FieldValidator.checkNumFiles(firstFile + 1); // +1 because the root is entry 0

        // 4 - Number of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder);

        filesInFolders[realNumFolders] = numFilesInFolder;
        realNumFolders++;

        expectedNumber = firstFile + numFilesInFolder;
      }

      int numFiles = expectedNumber;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int dirPos = 0;
      int dirRemaining = filesInFolders[0];
      String folderName = "Folder " + (dirPos + 1) + "\\";
      int[] decompLengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        if (dirRemaining == 0) {
          dirPos++;
          dirRemaining = filesInFolders[dirPos];
          folderName = "Folder " + (dirPos + 1) + "\\";
        }

        // 4 - Hash
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 3 - Compressed File Length
        // 1 - Compression Flag (128 = compressed, 0=uncompressed)
        byte[] lengthBytes = fm.readBytes(4);
        boolean compressed = ((ByteConverter.unsign(lengthBytes[3]) >> 7) == 1);
        lengthBytes[3] &= 127;
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        if (length == 0) {
          length = decompLength;
        }

        String filename = folderName + Resource.generateFilename(i);

        dirRemaining--;

        decompLengths[i] = 0;

        if (compressed) {

          if (decompLength <= 32768) {
            offset += 22;
            length -= 22;

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
            decompLengths[i] = decompLength;

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // For each file that is compressed in blocks, we need to read through and get the decompressed block sizes.
      // Need to close the DIR file and open the BIN file (and open it for small and quick reads)
      fm.close();
      fm = new FileManipulator(path, false, 24);

      for (int i = 0; i < numFiles; i++) {
        int decompLength = decompLengths[i];
        if (decompLength == 0) {
          continue; // either a single compressed block, or an uncompressed file
        }

        int numBlocks = decompLength / 32768;
        int lastBlockLength = decompLength % 32768;
        if (lastBlockLength != 0) {
          numBlocks++;
        }

        Resource resource = resources[i];
        long offset = resource.getOffset();
        fm.seek(offset);

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Unknown (0=last chunk, #=more chunks after this one)
          // 3 - RNC Header ("RNC")
          // 1 - Version (2)
          fm.skip(8);

          // 4 - Decompressed Block Length (BIG ENDIAN)
          int blockDecompLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(blockDecompLength, decompLength);
          blockDecompLengths[b] = blockDecompLength;

          // 4 - Compressed Block Length (BIG ENDIAN)
          int blockLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(blockLength, decompLength);
          blockLengths[b] = blockLength;

          // 2 - Decompressed CRC
          // 2 - Compressed CRC
          // 1 - Leeway
          // 1 - Number of Chunks
          fm.skip(6);

          // X - Block of Compressed Data
          blockOffsets[b] = fm.getOffset();

          // X - null Padding to a multiple of 4 bytes
          blockLength += calculatePadding((blockLength + 22), 4); //+22 because of the compression header 
          fm.skip(blockLength);
        }

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
        resource.setExporter(blockExporter);

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

    if (headerShort1 == 20531) {
      return "3p";
    }
    else if (headerShort1 == 20530) {
      return "2p";
    }
    else if (headerInt1 == 1094988358) {
      return "ad2f"; // F2DA
    }
    else if (headerInt1 == 1295003952) {
      return "m010"; // 010M
    }
    else if (headerInt1 == 1296126539) {
      return "mark"; // KRAM
    }
    else if (headerInt1 == 1347834929) {
      return "pvt"; // 1TVP
    }
    else if (headerInt1 == 829969488) {
      return "ptx"; // PTx1
    }
    else if (headerInt1 == 843600976) {
      return "pth"; // PTH2
    }
    else if (headerInt1 == 1952669987) {
      return "act";
    }
    else if (headerInt1 == 1297239878) {
      if (headerInt3 == 541934160) {
        return "pbm";
      }
      else if (headerInt3 == 1296190537) {
        return "lbm";
      }
    }
    else if (headerInt3 == 1293960528) {
      return "pe_map";
    }
    else if (headerInt3 == 1415065650) {
      return "20xt";
    }
    else if ((headerInt3 == 22050 || headerInt3 == 44100) && (headerInt2 == 1 || headerInt2 == 2)) {
      /*
      long offset = resource.getOffset();
      offset += 2048;
      
      long length = resource.getDecompressedLength();
      length -= 2048;
      
      if (resource.getExporter() instanceof Exporter_Default) {
        // can't do this, if we're already exporting with a RNC2 decompressor
        resource.setOffset(offset);
        resource.setLength(length);
        resource.setDecompressedLength(length);
        resource.setExporter(Exporter_Custom_VAG_Audio.getInstance());
      
        resource.addProperty("Frequency", headerInt3);
        resource.addProperty("Bitrate", 16);
        resource.addProperty("Channels", headerInt2);
        //resource.addProperty("AudioSigned", false);
      }
      */

      return "vag_audio_interleaved";
    }
    else if ((headerInt2 == 22050 || headerInt2 == 44100 || headerInt2 == 16000)) {
      long offset = resource.getOffset();
      offset += 16;

      long length = resource.getDecompressedLength();
      length -= 16;

      if (resource.getExporter() instanceof Exporter_Default) {
        // can't do this, if we're already exporting with a RNC2 decompressor
        resource.setOffset(offset);
        resource.setLength(length);
        resource.setDecompressedLength(length);
        resource.setExporter(Exporter_Custom_VAG_Audio.getInstance());

        resource.addProperty("Frequency", headerInt2);
        resource.addProperty("Bitrate", 16);
        resource.addProperty("Channels", 1);
        //resource.addProperty("AudioSigned", false);
      }

      return "vag";
    }

    return null;
  }

}
