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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_9 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_9() {

    super("ARC_9", "ARC_9");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Just Cause");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("PC", "XBox 360");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      String filePath = fm.getFilePath();
      int filePathLength = filePath.length();
      if (filePathLength > 5) {
        Integer.parseInt(filePath.substring(filePathLength - 5, filePathLength - 4));
        rating += 5;

        if (new File(filePath.substring(0, filePathLength - 5) + ".tab").exists()) {
          rating += 25;
        }
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

      //ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      //File sourcePath = getDirectoryFile(path, "tab");

      // get the tab file
      String filePath = path.getAbsolutePath();
      int filePathLength = filePath.length();
      String basePath = filePath.substring(0, filePathLength - 5);
      File sourcePath = new File(basePath + ".tab");
      if (!sourcePath.exists()) {
        return null;
      }

      // now work out how many archives there are, and where the file offset boundaries lie
      int numArchives = 0;
      File[] archiveFiles = new File[10];
      long[] offsets = new long[10];
      long[] lengths = new long[10];
      long currentOffset = 0;
      for (int i = 0; i < 10; i++) {
        File archiveFile = new File(basePath + i + ".arc");
        if (!archiveFile.exists()) {
          break; // found the last file
        }
        archiveFiles[i] = archiveFile;
        offsets[i] = currentOffset;
        lengths[i] = archiveFile.length();
        currentOffset += lengths[i];
        numArchives++;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Block Size (2048)
      boolean changeEndian = false;
      int blockSize = fm.readInt();
      if (blockSize == 524288) {
        // XBox360 - BIG ENDIAN
        changeEndian = true;
        blockSize = IntConverter.changeFormat(blockSize);
      }
      FieldValidator.checkRange(blockSize, 32, 4096);

      // 4 - Unknown
      fm.skip(4);

      int numFiles = (int) ((fm.getLength() - 12) / 12);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Unknown
        fm.skip(4);
        //System.out.println(HexConverter.convertLittle(fm.readInt()));

        // 4 - File Offset [*BlockSize]
        //long offset = IntConverter.unsign(fm.readInt()) * blockSize;
        long offset = 0;
        if (!changeEndian) {
          // little
          offset = IntConverter.unsign(fm.readInt()) * blockSize;
        }
        else {
          // big
          offset = IntConverter.unsign(IntConverter.changeFormat(fm.readInt())) * blockSize;
        }

        // 4 - File Length
        int length = fm.readInt();
        if (changeEndian) {
          length = IntConverter.changeFormat(length);
        }

        // work out which archive it's in
        int archiveNumber = -1;
        for (int a = 0; a < numArchives; a++) {
          if (offset >= offsets[a]) {
            // keep going
            archiveNumber++;
          }
          else {
            // gone too far
            break;
          }
        }

        File archiveFile = archiveFiles[archiveNumber];
        offset -= offsets[archiveNumber];
        long thisArcSize = lengths[archiveNumber];

        FieldValidator.checkOffset(offset, thisArcSize);
        FieldValidator.checkLength(length, thisArcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(archiveFile, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      fm.close();

      /*
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
      
      fm.close();
      */

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

    if (headerInt2 == 1129464147) {
      return "sarc";
    }
    else if (headerInt2 == 1145913938) {
      return "rbmd";
    }

    else if (headerInt1 == 5785168) {
      return "pfx";
    }
    else if (headerInt1 == 1296649793) {
      return "anim";
    }
    else if (headerInt1 == 168636207 || headerShort1 == 12079) {
      return "txt";
    }
    else if (headerInt1 == 825127792) {
      return "ps";
    }
    else if (headerInt1 == 825127798) {
      return "vs";
    }
    else if (headerInt1 == 5000019 || headerInt1 == 21777235) {
      return "skl";
    }

    else if (headerBytes[0] == 60) {
      return "xml";
    }

    return null;
  }

}
