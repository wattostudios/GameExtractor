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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LINK_LINK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LINK_LINK() {

    super("LINK_LINK", "LINK_LINK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shadow Of Rome");
    setExtensions("link");
    setPlatforms("PS2");

    setCanScanForFileTypes(true);

    setFileTypes(new FileType("oim", "OIM Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("msg1", "MSG1 Language Files", FileType.TYPE_OTHER));

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
      if (fm.readString(4).equals("LINK")) {
        rating += 50;
      }

      fm.skip(8);

      // Length Of Link Header (48)
      if (fm.readInt() == 48) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Length
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      ExporterPlugin exporterLZSS0 = Exporter_LZSS.getInstance();
      ExporterPlugin exporterPUYOLZ01 = new Exporter_QuickBMS_Decompression("PUYO_LZ01");

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = (int) fm.getLength();

      // 4 - Link Header (LINK)
      fm.skip(4);

      // 1 - Number of Sub-files
      int numFiles = ByteConverter.unsign(fm.readByte());

      // 1 - Unknown
      // 2 - null
      fm.skip(3);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int blockSize = 0;
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (blockSize == 0) {
          // use the first offset to calculate the block size (either 12 or 16)
          blockSize = (int) ((offset - 16) / numFiles);
          if (blockSize == 17) {
            blockSize = 16;
          }
          else if (blockSize == 13) {
            blockSize = 12;
          }
        }

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (blockSize == 16) {
          // 4 - null
          fm.skip(4);
        }

        if (length == 0) {
          continue;
        }

        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      // now work out whether the files are compressed or not
      fm.getBuffer().setBufferSize(16);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();

        fm.seek(offset);

        // 4 - Compression Header
        if (fm.readString(4).equals("CPK0")) {
          // compressed

          // 4 - Compressed Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compression Algorithm
          int compression = fm.readInt();

          resource.setOffset(offset + 16);
          resource.setLength(length);
          resource.setDecompressedLength(decompLength);

          if (compression == 0) {
            resource.setExporter(exporterLZSS0);
          }
          else {
            resource.setExporter(exporterPUYOLZ01);
          }

        }
        else {
          continue;
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

    if (headerInt1 == 1480938561) {
      return "atex";
    }
    else if (headerInt1 == 5523788) {
      return "lit";
    }
    else if (headerInt1 == 826757965) {
      return "msg1";
    }
    else if (headerInt1 == 860703055) {
      return "oim";
    }
    else if (headerInt3 == resource.getDecompressedLength() - 184) {
      return "arc"; // some kind of archive?
    }
    else if (headerInt1 == 826757965) {
      return "msg1";
    }
    else if (headerInt1 == 4412229) {
      return "esc";
    }

    return null;
  }

}
