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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IMG_AFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMG_AFS() {

    super("IMG_AFS", "IMG_AFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pro Evolution Soccer 2010",
        "Devil May Cry 3");
    setExtensions("img"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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

      // Header
      String headerString = fm.readString(3);
      int headerByte = fm.readByte();
      if (headerString.equals("AFS") && headerByte == 0) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Padding Multiple (2048)
      if (fm.readInt() == 2048) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      //ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("AFS" + null)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding Multiple (2048)
      // 4 - null
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize + 1);

        // 4 - Compressed File Length (including the file headers)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Now go through and look for compression...

      fm.getBuffer().setBufferSize(16); // small quick buffer

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long resourceOffset = resource.getOffset();

        fm.seek(resourceOffset);

        /*
        if (fm.readString(3).equals("PAC")) {
          // PAC blocks
        
          fm.skip(1);
        
          // 4 - Number of Blocks
          int numBlocks = fm.readInt();
          FieldValidator.checkNumFiles(numBlocks);
        
          long resourceLength = resource.getLength();
        
          if (numBlocks == 1) {
            // 4 - Block Offset
            long offset = fm.readInt();
        
            long length = resourceLength - offset;
            offset += resourceOffset;
        
            resource.setOffset(offset);
            resource.setLength(length);
            resource.setDecompressedLength(length);
          }
          else {
            // for each block
            //   4 - Block Offset
            long[] offsets = new long[numBlocks];
        
            int realNumBlocks = 0;
            for (int b = 0; b < numBlocks; b++) {
              long offset = fm.readInt();
              if (offset != 0) {
                FieldValidator.checkOffset(offset, resourceLength);
                offsets[realNumBlocks] = offset + resourceOffset;
                realNumBlocks++;
              }
            }
        
            if (realNumBlocks != numBlocks) {
              long[] tempOffsets = offsets;
              offsets = new long[realNumBlocks];
              System.arraycopy(tempOffsets, 0, offsets, 0, realNumBlocks);
              numBlocks = realNumBlocks;
            }
        
            long[] lengths = new long[numBlocks];
            for (int b = 0; b < numBlocks - 1; b++) {
              lengths[b] = offsets[b + 1] - offsets[b];
            }
            lengths[numBlocks - 1] = (resourceLength + resourceOffset) - offsets[numBlocks - 1];
        
            BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterDefault, offsets, lengths, lengths);
            resource.setExporter(blockExporter);
          }
        
        }
        else {
        */
        fm.skip(3);

        // 8 - Compression Header ((bytes)0,1,1 + "WESYS") // already read the first 3 bytes
        String fileHeader = fm.readString(5);
        if (fileHeader.equals("WESYS")) {
          // compressed

          // 4 - Compressed File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // X - File Data (ZLib Compression)
          long offset = fm.getOffset();

          resource.setLength(length);
          resource.setOffset(offset);

          if (decompLength == 0) {
            resource.setDecompressedLength(length);
          }
          else {
            resource.setDecompressedLength(decompLength);
            resource.setExporter(exporter);
          }
        }

        //}

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

    if (headerInt1 == 1144148816) {
      return "ps2d";
    }
    else if (headerInt1 == 1297436755) {
      return "spumapdt";
    }
    else if (headerInt1 == 1414745680) {
      return "pnst";
    }
    else if (headerInt1 == 1416847972) {
      return "dbst";
    }
    else if (headerInt1 == 1684104520) {
      return "head";
    }
    else if (headerInt1 == 4407632) {
      return "pac";
    }
    else if (headerInt1 == 541347661) {
      return "mod";
    }
    else if (headerInt1 == 5527109) {
      return "evt";
    }
    else if (headerInt1 == 843925844) {
      return "tm2";
    }

    return null;
  }

}
