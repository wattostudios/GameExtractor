/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PACK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PACK_3() {

    super("PACK_3", "PACK_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Disney Speedstorm");
    setExtensions("pack"); // MUST BE LOWER CASE
    setPlatforms("PC");

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
      if (fm.readInt() == 1263534080) {
        rating += 50;
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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.relativeSeek(arcSize - 6);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      if (dirOffset == -1) {
        // 64-bit dir offset, go back further
        fm.relativeSeek(arcSize - 34);

        dirOffset = fm.readLong();
        FieldValidator.checkOffset(dirOffset, arcSize);

        fm.relativeSeek(dirOffset + 48);
        dirOffset = fm.readLong();
      }
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.relativeSeek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 24) { // actually only need -22

        // 2 - Header (PK)
        fm.skip(2);

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        if (entryType == 513 || entryType == 1311233) {
          // Directory Entry

          // 2 - null
          // 2 - Unknown (2048)
          fm.skip(4);

          // 2 - Compression Method? (0/8)
          short compType = fm.readShort();

          // 4 - null
          // 12 - CRC?
          fm.skip(16);

          // 2 - Filename Length
          short filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // 4 - Unknown (32)
          // 4 - Unknown (65535)
          // 4 - null
          // 4 - Unknown (-1)
          fm.skip(16);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // 2 - Unknown (1)
          // 2 - Unknown (28)
          fm.skip(4);

          // 8 - Compressed File Length
          long length = fm.readLong();
          FieldValidator.checkLength(length, arcSize);

          // 8 - Decompressed File Length
          long decompLength = fm.readLong();
          FieldValidator.checkLength(decompLength);

          // 8 - File Offset (pointer to a PK entry)
          long offset = fm.readLong() + 66 + filenameLength; // +66 +filenameLength to skip the PK header
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - null
          fm.skip(4);

          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          /*
          if (compType == 0) {
            // uncompressed
          
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          else {
            // compressed - probably Deflate
          
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          */
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 2885126) {
          // 64bit Dir Details
          fm.skip(50);
        }
        else if (entryType == 1541 || entryType == 1798) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else {
          ErrorLogger.log("[PACK_3]: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6));
        }

      }

      resources = resizeResources(resources, realNumFiles);

      fm.getBuffer().setBufferSize(28);

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];

        long offset = resource.getOffset();

        fm.relativeSeek(offset);
        TaskProgressManager.setValue(offset);

        if (fm.readByte() == 120) {
          fm.skip(15);

          try {

            // 4 - Compressed Length
            int length = fm.readInt();
            if (length == 0) {
              continue;
            }
            FieldValidator.checkLength(length, resource.getLength());

            // 4 - Decompressed Length
            int decompLength = fm.readInt();
            FieldValidator.checkLength(decompLength);

            // 8 - CRC?
            fm.skip(8);

            resource.setOffset(fm.getOffset());
            resource.setLength(length);
            resource.setDecompressedLength(decompLength);

          }
          catch (Throwable t) {
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
