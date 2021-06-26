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

package org.watto.ge.plugin.exporter;

import java.io.IOException;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.Plugin_CAB_MSCF;
import org.watto.ge.plugin.resource.Resource_CAB_MSCF;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.io.stream.ManipulatorOutputStream;
import com.dorkbox.cabParser.decompress.lzx.DecompressLzx;
import com.dorkbox.cabParser.structure.CabConstants;

public class Exporter_LZX extends ExporterPlugin {

  static Exporter_LZX instance = new Exporter_LZX();

  static long readLength = 0;
  static int currentByte = 0;

  /**
  **********************************************************************************************
  LZX Compression used in Microsoft CAB archives
  Ref: https://git.dorkbox.com/dorkbox/CabParser

  // DOESN'T REALLY WORK!
  **********************************************************************************************
  **/
  public static Exporter_LZX getInstance() {
    return instance;
  }

  DecompressLzx decompressor;
  byte[] decompBuffer = new byte[0];
  int bufferPos = 0;
  int bufferLength = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZX() {
    setName("LZX Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (readLength > 0) {
        if (bufferPos >= bufferLength) {
          // need to decompress the next block

          // 4 - Checksum
          fm.skip(4);

          // 2 - Compressed Data Length
          int blockLength = ShortConverter.unsign(fm.readShort());

          // 2 - Uncompressed Data Length
          int decompBlockLength = ShortConverter.unsign(fm.readShort());

          // X - Reserve Data (length = FileReserveSize)
          fm.skip(Plugin_CAB_MSCF.getFileReserveSize());

          // X - Compressed Data
          //long nextBlockOffset = fm.getOffset() + blockLength;

          byte[] sourceBuffer = fm.readBytes(blockLength);
          decompBuffer = new byte[decompBlockLength];

          // do the decompression
          decompressor.decompress(sourceBuffer, decompBuffer, blockLength, decompBlockLength);

          bufferLength = decompBlockLength;
          bufferPos = 0;

        }

        currentByte = decompBuffer[bufferPos];
        readLength--;

        if (currentByte >= 0) {
          return true;
        }
      }

      return false;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);

      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /***********************************************************************************************
  We're at the offset to a compressed block, but the file doesn't start until part-way through
  this block. We need to discard the first X decompressed bytes from this block to get to the
  correct start of the file.
  ***********************************************************************************************/
  public void discardBytesToFileStart(long blockOffset, int bytesToDiscard) {

    // should already be here, but just in case
    fm.seek(blockOffset);

    // Decompress a block (and fill the decompBuffer)
    available();

    // get to the right spot
    while (bytesToDiscard > bufferLength) {
      // decompress and discard the whole block

      // and reduce the size of bytesToDiscard
      bytesToDiscard -= bufferLength;

      // read/decompress the next block
      available();
    }

    // now, this block is the block we want - it contains the start of the file.

    // find the start of the file in this block
    bufferPos = bytesToDiscard;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readLength = source.getDecompressedLength();

      int compressionMethod = 5379;

      //int type = compressionMethod & 0xF;
      int windowBits = (compressionMethod & 0x1F00) >>> 8;

      decompressor = new DecompressLzx();
      decompressor.reset(windowBits);

      decompBuffer = new byte[CabConstants.CAB_BLOCK_SIZE + this.decompressor.getMaxGrowth()];
      this.decompressor.init(windowBits);

      if (source instanceof Resource_CAB_MSCF) {
        Resource_CAB_MSCF resource = (Resource_CAB_MSCF) source;
        long blockOffset = resource.getBlockOffset();
        int bytesToDiscard = (int) resource.getBlockDiscardBytes();
        discardBytesToFileStart(blockOffset, bytesToDiscard);
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    LZMACompressorOutputStream outputStream = null;
    try {
      outputStream = new LZMACompressorOutputStream(new ManipulatorOutputStream(destination));

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        outputStream.write(exporter.read());
      }

      exporter.close();

      outputStream.finish();

    }
    catch (Throwable t) {
      logError(t);
      if (outputStream != null) {
        try {
          outputStream.finish();
        }
        catch (IOException e) {
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      // NOTE: The actual reading of the byte is done in available()
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}