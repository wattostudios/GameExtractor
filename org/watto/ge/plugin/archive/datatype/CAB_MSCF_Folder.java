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

package org.watto.ge.plugin.archive.datatype;

import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZX;
import org.watto.ge.plugin.exporter.Exporter_MSZIP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class CAB_MSCF_Folder {

  int offset = 0;
  int numBlocks = 0;
  int compression = 0;
  int reserveSize = 0;

  int[] blockOffsets = new int[0];
  int[] blockCompLengths = new int[0];
  int[] blockDecompLengths = new int[0];
  int[] blockDecompLengthsBefore = new int[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public CAB_MSCF_Folder(int offset, int numBlocks, int compression, int reserveSize) {
    this.offset = offset;
    this.numBlocks = numBlocks;
    this.compression = compression;
    this.reserveSize = reserveSize;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void calculateBlocks(FileManipulator fm) {
    blockOffsets = new int[numBlocks];
    blockCompLengths = new int[numBlocks];
    blockDecompLengths = new int[numBlocks];
    blockDecompLengthsBefore = new int[numBlocks];

    fm.seek(offset);
    int totalDecompLengthsBefore = 0;
    for (int i = 0; i < numBlocks; i++) {
      blockOffsets[i] = (int) fm.getOffset();

      // 4 - Checksum
      fm.skip(4);

      // 2 - Compressed Data Length
      int compLength = ShortConverter.unsign(fm.readShort());
      blockCompLengths[i] = compLength;

      // 2 - Uncompressed Data Length
      int decompLength = ShortConverter.unsign(fm.readShort());
      blockDecompLengths[i] = decompLength;

      // X - Reserve Data (length = FileReserveSize)
      fm.skip(reserveSize);

      // X - Compressed Data
      fm.skip(compLength);

      blockDecompLengthsBefore[i] = totalDecompLengthsBefore;
      totalDecompLengthsBefore += decompLength;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getBlockDecompLength(int blockNumber) {
    return blockDecompLengths[blockNumber];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getBlockDecompLengthBefore(int blockNumber) {
    return blockDecompLengthsBefore[blockNumber];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getBlockForOffset(int offset) {
    for (int i = 0; i < numBlocks; i++) {
      int totalDecompLengthsBefore = blockDecompLengthsBefore[i];
      if (offset == totalDecompLengthsBefore) {
        return i;
      }
      else if (offset < totalDecompLengthsBefore) {
        // we've gone too far - the previous block contains the start of this file
        if (i == 0) {
          return 0;
        }
        else {
          return i - 1;
        }
      }
      // haven't found the block yet - do the next iteration of the loop
    }
    // if we haven't found it yet, the file must be in the very last block
    return numBlocks - 1;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getBlockOffset(int blockNumber) {
    return blockOffsets[blockNumber];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ExporterPlugin getExporter() {
    if (compression == 1) { // MSZIP
      return Exporter_MSZIP.getInstance();
    }
    else if (compression == 5379) { // bytes (3,21) = LZX compression (3) compression ratio (21)
      return Exporter_LZX.getInstance();
    }

    return Exporter_Default.getInstance();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getOffset() {
    return offset;
  }

}
