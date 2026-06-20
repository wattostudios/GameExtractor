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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.Hex;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RDB_DRK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RDB_DRK() {

    super("RDB_DRK", "RDB_DRK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("God of War: Ascension");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PS3");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      if (fm.getFilePath().endsWith(".rdb.bin")) {
        rating += 25;
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = path.length();

      String dirFilePath = path.getAbsolutePath();
      int dotPos = dirFilePath.lastIndexOf(".bin");
      if (dotPos <= 0) {
        return null;
      }

      File sourcePath = new File(dirFilePath.substring(0, dotPos));
      if (!sourcePath.exists() || !sourcePath.isFile()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 8 - Header (_DRK0000)
      // 4 - Unknown (32)
      // 4 - Unknown (10)
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      // X - Directory Name ("data/")
      // 1 - null Directory Name Terminator
      fm.readNullString();

      // 0-3 - null Padding to a multiple of 4 bytes
      fm.skip(calculatePadding(fm.getOffset(), 4));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        long nextOffset = fm.getOffset();

        // 8 - Header (IDRK0000)
        fm.skip(8);

        // 8 - File Entry Length (not including padding)
        int entryLength = fm.readInt();
        fm.skip(4);

        entryLength += calculatePadding(entryLength, 4);

        nextOffset += entryLength;

        // 8 - Unknown (14/15)
        // 4 - Unknown
        // 4 - null
        fm.skip(16);

        // 4 - Extra Data Length
        int extraLength = fm.readInt();

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(20);

        if (extraLength == 1) {
          // 13 - Extra Data
          fm.skip(13);
        }
        else {
          // X - Extra Data (optional) (length = ExtraData*4)
          fm.skip(extraLength * 4);
        }

        // if not the last file
        // X - File Offset and Length String (2 Hex Strings with "@" between them)
        // 1 - null Hex String Terminator
        String hexString = fm.readNullString();
        int atPos = hexString.indexOf('@');
        //System.out.println(hexString + "\t" + fm.getOffset());
        if (atPos > 0) {
          // 4 - File Offset
          String offsetString = hexString.substring(0, atPos);
          while (offsetString.length() < 8) {
            offsetString = "0" + offsetString;
          }
          Hex offsetHex = new Hex(offsetString);
          long offset = IntConverter.unsign(IntConverter.convertBig(offsetHex));
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          String lengthString = hexString.substring(atPos + 1);
          while (lengthString.length() < 8) {
            lengthString = "0" + lengthString;
          }
          Hex lengthHex = new Hex(lengthString);
          long length = IntConverter.unsign(IntConverter.convertBig(lengthHex));
          FieldValidator.checkLength(length, arcSize);

          // X - Filename (null)
          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(i);

        }

        //   0-3 - null Padding to a multiple of 4 bytes
        fm.relativeSeek(nextOffset);

      }

      fm.close();

      if (numFiles != realNumFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

      numFiles = realNumFiles;

      // Now go through the actual data file and work out the compression
      fm = new FileManipulator(path, false, 16); // small quick reads

      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        Resource resource = resources[i];

        fm.seek(resource.getOffset() + 16);

        // 8 - Compressed File Length (File Data only)
        long compLength = fm.readLong();
        FieldValidator.checkLength(compLength, arcSize);

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        long startOffset = resource.getOffset() + resource.getLength() - compLength;
        fm.seek(startOffset);

        //System.out.println(resource.getOffset() + "\t" + compLength + "\t" + decompLength + "\t" + startOffset);

        int blockSize = 16384;

        int numBlocks = (int) (decompLength / blockSize);
        int lastBlock = (int) (decompLength % blockSize);
        if (lastBlock != 0) {
          numBlocks++;
        }

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Compressed Block Length
          int blockCompLength = fm.readInt();
          FieldValidator.checkLength(blockCompLength, arcSize);

          // X - Compressed Data Block (ZLib Compression)
          blockOffsets[b] = fm.getOffset();
          blockLengths[b] = blockCompLength;
          blockDecompLengths[b] = blockSize;
          if (b == numBlocks - 1) {
            if (lastBlock != 0) {
              blockDecompLengths[b] = lastBlock;
            }
          }

          fm.skip(blockCompLength);
        }

        resource.setLength(compLength);
        resource.setDecompressedLength(decompLength);

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
        resource.setExporter(blockExporter);

      }

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

    if (headerInt1 == -2142024115) {
      return "snm";
    }
    else if (headerInt1 == 1194410335) {
      return "g1a";
    }
    else if (headerInt1 == 1194412127) {
      return "g1h";
    }
    else if (headerInt1 == 1194412361) {
      return "g1i";
    }
    else if (headerInt1 == 1194413407) {
      return "g1m";
    }
    else if (headerInt1 == 1194415175) {
      return "g1t";
    }
    else if (headerInt1 == 1194475871) {
      return "g2a";
    }
    else if (headerInt1 == 1194480479) {
      return "g2s";
    }
    else if (headerInt1 == 1262965343) {
      return "kgr";
    }
    else if (headerInt1 == 1262965855) {
      return "kgt";
    }
    else if (headerInt1 == 1263486047) {
      return "kod";
    }
    else if (headerInt1 == 1398162527) {
      return "svd";
    }
    else if (headerInt1 == 1363629907) {
      return "swgq";
    }
    else if (headerInt1 == 1363629907) {
      return "bgir";
    }

    return null;
  }

}
