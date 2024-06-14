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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BF_ABE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BF_ABE() {

    super("BF_ABE", "BF_ABE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Michael Jackson: The Experience");
    setExtensions("bf"); // MUST BE LOWER CASE
    setPlatforms("Wii");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("fcg", "h", "fct", "var"); // LOWER CASE

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
      if (fm.readInt() == 4538945) { // "ABE" + null
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
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      Exporter_LZO_MiniLZO exporter = Exporter_LZO_MiniLZO.getInstance();
      exporter.setForceDecompress(true);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("ABE" + null)
      // 4 - Unknown (5)
      // 4 - Unknown (1389)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - null
      fm.skip(24);

      // 4 - Files Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - Folder Directory Offset
      // 4 - Number of Files (including the last empty entry)
      // 4 - Number of Folders? (1)
      // 4 - Number of Files
      // 4 - Unknown (1)
      // 12 - Unknown (-1)
      // 4 - null
      // 4 - Unknown
      // 5112 - null Padding
      // 152 - Description (null terminated, filled with nulls)
      fm.seek(dirOffset);

      // 4 - Number of Files (including the last empty entry)
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (all 255)
        // 4 - Unknown (all 170)
        fm.skip(8);

        // 64 - Filename (null terminated, filled with (byte)170)
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);

        // 1 - null
        // 15 - Unknown (all 170)
        // 4 - Parent Folder ID?
        // 4 - Unknown ID (incremental from -1)
        fm.skip(24);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - null
        fm.skip(16);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Flags (131072/65536/...)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 1 - null
        // 55 - Unknown (all 170)
        fm.skip(72);

        /*
        // Change the offsets/lengths to remove the compression header
        offset += 32;
        length -= 32;
        
        if (length < 2000 && decompLength > length * 20) {
          // not really compressed - it's actually a pointer file to somewhere else.
        
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // normal compressed file
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        */

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(80); // small quick reads

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 4 - Compressed Data Length
        int realCompLength = fm.readInt();

        // 4 - Decompressed Data Length
        int realDecompLength = fm.readInt();

        // 4 - Unknown (0)
        // 4 - Unknown (0/2/3/4/...)
        // 16 - Unknown (all 170)
        fm.skip(24);

        long realOffset = fm.getOffset();

        if (realCompLength == realDecompLength) {
          // an uncompressed file
          resource.setOffset(realOffset);
          resource.setLength(realCompLength);
          resource.setDecompressedLength(realDecompLength);
          resource.setExporter(exporterDefault);
          continue;
        }
        else if (realDecompLength > realCompLength * 10) {
          // not a real file to decompress - just a pointer to another file somewhere
          resource.setOffset(realOffset);
          resource.setLength(realCompLength);
          resource.setDecompressedLength(realCompLength);
          resource.setExporter(exporterDefault);
          continue;
        }

        // 4 - Compression Chunk Flag (1)
        if (fm.readInt() == 1) {
          // the file is compressed in chunks

          // 4 - Number of Chunks
          int numChunks = fm.readInt();
          try {
            FieldValidator.checkRange(numChunks, 1, 50);//guess
          }
          catch (Throwable t) {
            continue;
          }

          long[] blockOffsets = new long[numChunks];
          long[] blockLengths = new long[numChunks];
          long[] blockDecompLengths = new long[numChunks];

          long offset = fm.getOffset() + (numChunks * 4);
          long totalDecompLength = resource.getDecompressedLength();
          for (int b = 0; b < numChunks; b++) {
            // 4 - Compressed Block Length
            int compBlockLength = fm.readInt();
            FieldValidator.checkLength(compBlockLength, arcSize);

            blockOffsets[b] = offset;
            blockLengths[b] = compBlockLength;

            if (b == numChunks - 1) {
              blockDecompLengths[b] = totalDecompLength - (262140 * (numChunks - 1));
            }
            else {
              blockDecompLengths[b] = 262140;
            }

            offset += compBlockLength;
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }
        else {
          resource.setOffset(realOffset);
          resource.setLength(realCompLength);
          resource.setDecompressedLength(realDecompLength);
          continue;
        }
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}
