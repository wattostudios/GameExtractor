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

package org.watto.io.stream;

import java.io.File;
import org.watto.ge.plugin.archive.Plugin_CAB_MSCF;
import org.watto.io.FileManipulator;
import org.watto.io.Manipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;
import org.watto.io.stream.datatype.ZLibXTree;

/***********************************************************************************************
MSZIP decompression
Ported from C-code at https://bitbucket.org/jibsen/tinf and modified to suit ZLibX format
***********************************************************************************************/
public class MSZIPInputStream extends ZLibXInputStream {

  /***********************************************************************************************
  Creates an <code>InputStream</code> for the <code>file</code>
  @param file the <code>File</code> to read from
  ***********************************************************************************************/
  public MSZIPInputStream(File file, long decompLength, long blockOffset, int bytesToDiscard) {
    super(file, decompLength);

    discardBytesToFileStart(blockOffset, bytesToDiscard);
  }

  /***********************************************************************************************
  Creates an <code>InputStream</code> for the <code>Manipulator</code>
  @param manipulator the <code>Manipulator</code> to read from
  ***********************************************************************************************/
  public MSZIPInputStream(Manipulator manipulator, long decompLength, long blockOffset, int bytesToDiscard) {
    super(manipulator, decompLength);

    discardBytesToFileStart(blockOffset, bytesToDiscard);
  }

  /***********************************************************************************************
  Gets the number of bytes left to read from the <code>Manipulator</code> source
  @return the remaining length
  ***********************************************************************************************/
  @Override
  public int available() {

    if ((int) destManipulator.getRemainingLength() <= 0) {
      if (decompLength > 0) {
        // we've run out of data in this block, need to read the next compressed block

        // move the buffer along to only retain the last 1 block of history
        ByteBuffer byteBuffer = (ByteBuffer) destManipulator.getBuffer();
        int decompBlockLength = byteBuffer.getBufferSize() / 2;
        byteBuffer.seek(decompBlockLength);
        byte[] bufferHistory = byteBuffer.getBuffer(decompBlockLength);
        byteBuffer.seek(0);
        byteBuffer.write(bufferHistory); // this moves the pointer along too

        //System.out.println(">>> STARTING. Read offset = " + sourceManipulator.getOffset());

        // 4 - Checksum
        sourceManipulator.skip(4);

        // 2 - Compressed Data Length
        long blockLength = ShortConverter.unsign(sourceManipulator.readShort());

        // 2 - Uncompressed Data Length
        decompBlockLength = ShortConverter.unsign(sourceManipulator.readShort());

        // X - Reserve Data (length = FileReserveSize)
        sourceManipulator.skip(Plugin_CAB_MSCF.getFileReserveSize());

        // X - Compressed Data

        long nextBlockOffset = sourceManipulator.getOffset() + blockLength;
        //System.out.println(">>> Block length = " + blockLength + ", next block should start at " + nextBlockOffset);

        // MSZIP Header (67, 75);
        sourceManipulator.skip(2);

        // discard the dynamic trees from previous blocks
        dynamicLengthTree = new ZLibXTree();
        dynamicDistanceTree = new ZLibXTree();

        // Uncompress the block
        long startDestOffset = destManipulator.getOffset();
        boolean resetBitCounter = true;
        while (sourceManipulator.getOffset() < nextBlockOffset) {
          //System.out.println(">>> Decompressing at offset " + sourceManipulator.getOffset());
          int returnCode = tinf_uncompress(resetBitCounter);
          if (returnCode != 0) {
            // error somewhere - terminate the decompression here!
            decompLength = 0; // pretend the file has been fully decompressed
            return 0;
          }

          resetBitCounter = false; // so remaining iterations through the while() will continue from the existing bit, not reset it
        }

        // Now that we've uncompressed all the data, we need to return the buffer to the beginning,
        // ready for the read() by the extract() method when trying to export this file to disk
        destManipulator.seek(startDestOffset);

        decompLength -= decompBlockLength;
      }
    }

    return (int) destManipulator.getRemainingLength();
  }

  /***********************************************************************************************
  We're at the offset to a compressed block, but the file doesn't start until part-way through
  this block. We need to discard the first X decompressed bytes from this block to get to the
  correct start of the file.
  ***********************************************************************************************/
  public void discardBytesToFileStart(long blockOffset, int bytesToDiscard) {

    sourceManipulator.seek(blockOffset);

    // 4 - Checksum
    sourceManipulator.skip(4);

    // 2 - Compressed Data Length
    long blockLength = ShortConverter.unsign(sourceManipulator.readShort());

    // 2 - Uncompressed Data Length
    int decompBlockLength = ShortConverter.unsign(sourceManipulator.readShort());

    ByteBuffer byteBuffer = new ByteBuffer(decompBlockLength * 2);
    destManipulator = new FileManipulator(byteBuffer);
    destManipulator.seek(decompBlockLength); // start half-way through the buffer, so we have "history" from the previous block

    // X - Reserve Data (length = FileReserveSize)
    sourceManipulator.skip(Plugin_CAB_MSCF.getFileReserveSize());

    // X - Compressed Data
    while (bytesToDiscard > decompBlockLength) {
      // decompress and discard the whole block
      //System.out.println(">>> DISCARDING BLOCK AT Read offset = " + sourceManipulator.getOffset());

      long nextBlockOffset = sourceManipulator.getOffset() + blockLength;
      //System.out.println(">>> Block length = " + blockLength + ", next block should start at " + nextBlockOffset);

      // MSZIP Header (67, 75);
      sourceManipulator.skip(2);

      // discard the dynamic trees from previous blocks
      dynamicLengthTree = new ZLibXTree();
      dynamicDistanceTree = new ZLibXTree();

      // Uncompress the block
      boolean resetBitCounter = true;
      while (sourceManipulator.getOffset() < nextBlockOffset) {
        //System.out.println(">>> Decompressing at offset " + sourceManipulator.getOffset());
        int returnCode = tinf_uncompress(resetBitCounter);
        if (returnCode != 0) {
          // error somewhere - terminate the decompression here!
          return;
        }

        resetBitCounter = false; // so remaining iterations through the while() will continue from the existing bit, not reset it
      }

      // now that we have read the block, move the buffer along to only retain the last 1 block of history
      byteBuffer.seek(decompBlockLength);
      byte[] bufferHistory = byteBuffer.getBuffer(decompBlockLength);
      byteBuffer.seek(0);
      byteBuffer.write(bufferHistory); // this moves the pointer along too

      // and reduce the size of bytesToDiscard
      bytesToDiscard -= decompBlockLength;

      // Now read the header details for the next block

      // 4 - Checksum
      sourceManipulator.skip(4);

      // 2 - Compressed Data Length
      blockLength = ShortConverter.unsign(sourceManipulator.readShort());

      // 2 - Uncompressed Data Length
      decompBlockLength = ShortConverter.unsign(sourceManipulator.readShort());

      // X - Reserve Data (length = FileReserveSize)
      sourceManipulator.skip(Plugin_CAB_MSCF.getFileReserveSize());
    }

    // now, this block is the block we want - it contains the start of the file.

    //System.out.println(">>> FOUND THE STARTING BLOCK. Read offset = " + sourceManipulator.getOffset());

    long nextBlockOffset = sourceManipulator.getOffset() + blockLength;
    //System.out.println(">>> Block length = " + blockLength + ", next block should start at " + nextBlockOffset);

    // MSZIP Header (67, 75);
    sourceManipulator.skip(2);

    // discard the dynamic trees from previous blocks
    dynamicLengthTree = new ZLibXTree();
    dynamicDistanceTree = new ZLibXTree();

    // Uncompress the block
    long startDestOffset = destManipulator.getOffset();
    boolean resetBitCounter = true;
    while (sourceManipulator.getOffset() < nextBlockOffset) {
      //System.out.println(">>> Decompressing at offset " + sourceManipulator.getOffset());
      int returnCode = tinf_uncompress(resetBitCounter);
      if (returnCode != 0) {
        // error somewhere - terminate the decompression here!
        return;
      }

      resetBitCounter = false; // so remaining iterations through the while() will continue from the existing bit, not reset it
    }

    // Now that we've uncompressed all the data, we need to return the buffer to the beginning,
    // then discard the bytesToDiscard to get us to the start of this file data
    destManipulator.seek(startDestOffset + bytesToDiscard);
    //System.out.println(">>> Discarding bytes: " + bytesToDiscard);

  }

  /***********************************************************************************************
  build extra bits and base tables
  bits = unsigned char
  base = unsigned short
  ***********************************************************************************************/
  @Override
  public void tinf_build_bits_base(int[] bits, long[] base, int delta, int first) {
    // build bits table
    for (int i = 0; i < delta; ++i) {
      bits[i] = 0;
    }

    for (int i = 0; i < 30 - delta; ++i) { // USE DEFAULT SIZE 30
      bits[i + delta] = (i / delta);
    }

    // build base table
    long sum = first;
    for (int i = 0; i < 30; ++i) { // USE DEFAULT SIZE 30
      base[i] = sum;
      sum += 1l << bits[i];
    }
  }

  /***********************************************************************************************
  initialize global (static) data
  ***********************************************************************************************/
  @Override
  public void tinf_init() {
    /** extra bits and base tables for length codes **/
    length_bits = new int[30]; // USE DEFAULT SIZE 30
    length_base = new long[30]; // USE DEFAULT SIZE 30

    /** extra bits and base tables for distance codes **/
    dist_bits = new int[30]; // USE DEFAULT SIZE 30
    dist_base = new long[30]; // USE DEFAULT SIZE 30

    super.tinf_init();
  }

}