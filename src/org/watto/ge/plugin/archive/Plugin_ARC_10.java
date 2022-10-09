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
import java.util.HashMap;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.HexConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_10 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_10() {

    super("ARC_10", "ARC_10");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Just Cause 3",
        "theHunter: Call Of The Wild");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("fsb", "FSB Audio File", FileType.TYPE_AUDIO),
        new FileType("bmpc", "Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("ddsc", "DDS Image", FileType.TYPE_IMAGE));

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

      //ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

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
          // HASH       BYTEOFFSET SIZE       DCOMP SIZE CONTENT DIGEST (SHA1)                     NAME      
          // 0xBE3743E7 0x00000000 0x05555680 0x05555680 d7855925e42542444dcd6b4dde1c5563300d887f  climate/hp_patagonia/common/tree_diffuse_atlas_0.ddsc

          if (line.length() < 86) {
            // not long enough
            continue;
          }

          String hash = line.substring(2, 10);
          String filename = line.substring(86);

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
      // 2 - Version Major? (2)
      // 2 - Version Minor? (1)
      // 4 - Padding Multiple (2048)

      fm.skip(12);

      int numFiles = (int) ((fm.getLength() - 12) / 12);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash?
        byte[] hexBytes = fm.readBytes(4);
        hexBytes = new byte[] { hexBytes[3], hexBytes[2], hexBytes[1], hexBytes[0] }; // swap the order
        String hash = HexConverter.convertLittle(hexBytes).toString();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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
          filename += ".fsb";
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      /*
      fm.close();
      
      // Now go through the ARC file and work out if the file is compressed or not
      fm = new FileManipulator(path, false, 4);
      //fm.getBuffer().setBufferSize(4);
      
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();
      
        fm.seek(offset);
      
        if (fm.readString(1).equals("x")) {
          // compressed
          resource.setExporter(exporter);
        }
      
      
        TaskProgressManager.setValue(i);
      }
      */

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

    if (headerInt1 == 1094993440) {
      return "fda";
    }
    else if (headerInt1 == 1129337938) {
      return "rtpc";
    }
    else if (headerInt1 == 1481922113) {
      return "avtx";
    }
    else if (headerInt1 == 207111747 || headerInt1 == 257443395) {
      return "cfx";
    }
    else if (headerInt1 == 1731347019) {
      return "kb2g";
    }
    else if (headerInt1 == 4604225) {
      return "aaf";
    }
    else if (headerInt1 == 1474355287) {
      return "waaw";
    }

    else if (headerInt2 == 1129464147) {
      return "sarc";
    }
    else if (headerInt2 == 1145913938) {
      return "rbmd";
    }

    return null;
  }

}
