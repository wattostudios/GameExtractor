/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XXX_RU extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XXX_RU() {

    super("XXX_RU", "XXX_RU");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Medal Of Honor: Airborne");
    setExtensions("xxx");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (fm.readString(4).equals("Ru" + (byte) 189 + (byte) 161)) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // first file ID (0)
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
      //ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      //ExporterPlugin exporterLZO = Exporter_LZO_SingleBlock.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("Ru" + (bytes)189,161)
      // 4 - null
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File ID (incremental from 0)
        // 4 - Lengths Directory Entry Offset
        fm.skip(8);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset == 0) {
          // end of directory
          break;
        }

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      /*
      // Now go through each file and find the chunks in it
      fm.getBuffer().setBufferSize(8);
      
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();
      
        TaskProgressManager.setValue(offset);
      
        fm.seek(offset);
      
        int remainingLength = (int) resource.getLength();
        int totalDecompLength = 0;
        int totalCompLength = 0;
      
        int numBlocks = remainingLength / 1984; // 1984 is the block size
        int finalBlock = remainingLength % 1984;
        if (finalBlock != 0) {
          numBlocks++;
        }
      
        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];
      
        int b = 0;
        while (remainingLength > 0) {
          //System.out.println(fm.getOffset());
          // 4 - Block Length (including these 2 headers) (remove the top-bit which means the last block in the file)
          byte[] blockLengthBytes = fm.readBytes(4);
          boolean lastBlock = (blockLengthBytes[0] < 0);
          blockLengthBytes[0] &= 127;
          int blockLength = IntConverter.convertBig(blockLengthBytes);
          FieldValidator.checkLength(blockLength, arcSize);
      
          // 4 - Block Decomp Length
          int blockDecompLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(blockDecompLength);
      
          totalCompLength += blockLength;
          totalDecompLength += blockDecompLength;
      
          remainingLength -= blockLength;
      
          blockLength -= 8;
          long blockOffset = fm.getOffset();
          fm.skip(blockLength);
      
          blockOffsets[b] = blockOffset;
          blockLengths[b] = blockLength;
          blockDecompLengths[b] = blockDecompLength;
          b++;
      
      
      
          if (lastBlock) {
            remainingLength = 0;
            if (b < numBlocks) {
              //System.out.println("Early finish " + b + " vs " + numBlocks);
              // shrink the arrays
              long[] temp = blockOffsets;
              blockOffsets = new long[b];
              System.arraycopy(temp, 0, blockOffsets, 0, b);
      
              temp = blockLengths;
              blockLengths = new long[b];
              System.arraycopy(temp, 0, blockLengths, 0, b);
      
              temp = blockDecompLengths;
              blockDecompLengths = new long[b];
              System.arraycopy(temp, 0, blockDecompLengths, 0, b);
            }
          }
        }
      
        
        resource.setLength(totalCompLength);
        resource.setDecompressedLength(totalDecompLength);
        
        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterLZO, blockOffsets, blockLengths, blockDecompLengths);
        resource.setExporter(blockExporter);
        
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

}
