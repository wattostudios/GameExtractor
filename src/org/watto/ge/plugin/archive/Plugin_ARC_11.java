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
import java.util.HashMap;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Oodle;
import org.watto.ge.plugin.exporter.Exporter_ZStd;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.HexConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_11 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_11() {

    super("ARC_11", "ARC_11");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("RAGE 2");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      getDirectoryFile(fm.getFile(), "tab");
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

      ExporterPlugin exporterOodle = Exporter_Oodle.getInstance();
      ExporterPlugin exporterZStd = Exporter_ZStd.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      // first, see if there's a filename file we can parse
      setCanScanForFileTypes(true);
      String archiveNumber = FilenameSplitter.getFilename(path);
      if (archiveNumber.length() > 4) {
        archiveNumber = archiveNumber.substring(4);
      }

      HashMap<String, String> filenameMap = null;
      File filenameDirFile = new File(FilenameSplitter.getDirectory(path) + File.separatorChar + "game_hash_names" + archiveNumber + ".txt");
      if (filenameDirFile.exists()) {
        // found a filename file, open it and read it
        filenameMap = new HashMap<String, String>();

        FileManipulator nameFM = new FileManipulator(filenameDirFile, false);

        // skip the first 2 lines, which are the headers
        nameFM.readLine();
        nameFM.readLine();

        String line = nameFM.readLine();
        while (line != null && line.length() > 0) {
          // line has this format (example):
          // 
          // HASH               BYTEOFFSET SIZE       DCOMP SIZE  NAME            
          // 0x3D304A7D32FF05EF 0x00000000 0x000A31F3 0x001FAAEC  sarc.0.gtoc

          if (line.length() < 54) {
            // not long enough
            continue;
          }

          String hash = line.substring(2, 18);
          String filename = line.substring(53);

          filenameMap.put(hash, filename);

          // read the next line
          line = nameFM.readLine();
        }

        nameFM.close();
        setCanScanForFileTypes(false); // don't scan for filenames if we already have the filenames from a mapping file
      }

      File sourcePath = getDirectoryFile(path, "tab");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header ("TAB" + null)
      // 2 - Version Major? (3)
      // 2 - Version Minor? (1)
      // 4 - Padding Multiple (4096)
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Directory 1 Entries
      int numDir1Entries = fm.readInt();
      FieldValidator.checkNumFiles(numDir1Entries);

      // 4 - null
      fm.skip(4);

      // DIRECTORY 1
      fm.skip(numDir1Entries * 8);

      // 4 - End of Directory Marker? (-1)
      // 4 - End of Directory Marker? (-1)
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Hash
        byte[] hexBytes = fm.readBytes(8);
        hexBytes = new byte[] { hexBytes[7], hexBytes[6], hexBytes[5], hexBytes[4], hexBytes[3], hexBytes[2], hexBytes[1], hexBytes[0] }; // swap the order
        String hash = HexConverter.convertLittle(hexBytes).toString();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 2 - Unknown
        fm.skip(2);

        // 1 - Compression Flag? (0=uncompressed)
        int compressionType = fm.readByte();

        // 1 - Unknown
        fm.skip(1);

        String filename = Resource.generateFilename(i);
        if (filenameMap != null) {
          String mappedFilename = filenameMap.get(hash);
          if (mappedFilename != null) {
            filename = mappedFilename;
          }
        }

        if (filename.endsWith("wavc")) { // FSB5 with a 16-byte header
          offset += 16;
          length -= 16;
          decompLength -= 16;
          filename += ".fsb";
        }

        if (compressionType == 0 || length == decompLength) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else if (compressionType == 3) {
          // ZSTD Compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterZStd);
        }
        else {
          // Oodle Compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterOodle);
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 1129337938) {
      return "rtpc";
    }
    else if (headerInt1 == 809976148) {
      return "tag0";
    }
    else if (headerInt1 == 1094993408 || headerInt1 == 1094993440) {
      return "adf";
    }
    else if (headerInt1 == 1481922113) {
      return "avtx";
    }
    else if (headerInt1 == 1731347019) {
      return "kb2g";
    }
    else if (headerInt1 == 1766541634) {
      return "bik";
    }

    else if (headerBytes[0] == 67 && headerBytes[1] == 70 && headerBytes[2] == 88) {
      return "cfx";
    }
    else if (headerInt1 == 1853190003) {
      return "txt";
    }

    else if (headerInt2 == 1129337938) {
      return "rtpc";
    }
    else if (headerInt2 == 809976148) {
      return "tag0";
    }
    else if (headerInt2 == 1094993408 || headerInt2 == 1094993440) {
      return "adf";
    }
    else if (headerInt2 == 1481922113) {
      return "avtx";
    }

    else if (headerInt3 == 1129337938) {
      return "rtpc";
    }
    else if (headerInt3 == 809976148) {
      return "tag0";
    }
    else if (headerInt3 == 1094993408 || headerInt3 == 1094993440) {
      return "adf";
    }
    else if (headerInt3 == 1481922113) {
      return "avtx";
    }

    return null;
  }

}
