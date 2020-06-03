/*
 * Copyright 2012 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dorkbox.cabParser.decompress.lzx;

import com.dorkbox.cabParser.structure.CabException;
import com.dorkbox.cabParser.structure.CorruptCabException;

public final class DecompressLzx implements Decompressor, LZXConstants {

  private int[] extraBits = new int[51];
  private int[] positionBase = new int[51];

  private DecompressLzxTree mainTree;
  private DecompressLzxTree lengthTree;
  private DecompressLzxTree alignedTree;
  private DecompressLzxTree preTree;

  private int wndSize;
  private int windowMask;
  private int mainElements;
  private int blocksRemaining;
  private int blockType;
  private int blockRemaining;

  private int blockLength;
  private int windowPosition;
  private int R0;
  private int R1;
  private int R2;

  private byte[] localWindow;

  private int windowSize;
  private boolean readHeader;

  private int outputPosition;
  private int index;
  private int length;

  private byte[] inputBytes;

  private boolean abort;

  int bitsLeft;

  private int blockAlignOffset;
  private int intelFileSize;
  private int intelCursorPos;

  private byte[] savedBytes = new byte[6];

  private boolean intelStarted;
  private int framesRead;

  public DecompressLzx() {
    int i = 4;
    int j = 1;
    do {
      this.extraBits[i] = j;
      this.extraBits[i + 1] = j;
      i += 2;
      j++;
    }
    while (j <= 16);

    do {
      this.extraBits[i++] = 17;
    }
    while (i < 51);

    i = -2;
    for (j = 0; j < this.extraBits.length; j++) {
      this.positionBase[j] = i;
      i += 1 << this.extraBits[j];
    }

    this.windowSize = -1;
  }

  private void alignedAlgo(int this_run) throws CorruptCabException {
    int windowPos = this.windowPosition;
    int mask = this.windowMask;
    byte[] window = this.localWindow;
    int r0 = this.R0;
    int r1 = this.R1;
    int r2 = this.R2;

    while (this_run > 0) {
      int mainElement = this.mainTree.decodeElement();

      if (mainElement < NUM_CHARS) {
        window[windowPos] = (byte) mainElement;
        windowPos = windowPos + 1 & mask;
        this_run--;
      }
      /* is a match */
      else {
        mainElement -= NUM_CHARS;

        int matchLength = mainElement & NUM_PRIMARY_LENGTHS;
        if (matchLength == NUM_PRIMARY_LENGTHS) {
          matchLength += this.lengthTree.decodeElement();
        }

        int match_offset = mainElement >>> 3;

        if (match_offset > 2) {
          // not repeated offset
          int extra = this.extraBits[match_offset];
          match_offset = this.positionBase[match_offset];

          if (extra > 3) {
            // verbatim and aligned bits
            match_offset += (readBits(extra - 3) << 3) + this.alignedTree.decodeElement();
          }
          else if (extra == 3) {
            // aligned bits only
            match_offset += this.alignedTree.decodeElement();
          }
          else if (extra > 0) {
            // verbatim bits only
            match_offset += readBits(extra);
          }
          else {
            match_offset = 1;
          }

          // update repeated offset LRU queue
          r2 = r1;
          r1 = r0;
          r0 = match_offset;
        }
        else if (match_offset == 0) {
          match_offset = r0;
        }
        else if (match_offset == 1) {
          match_offset = r1;
          r1 = r0;
          r0 = match_offset;
        }
        else {
          match_offset = r2;
          r2 = r0;
          r0 = match_offset;
        }

        matchLength += MIN_MATCH;
        this_run -= matchLength;

        while (matchLength > 0) {
          window[windowPos] = window[windowPos - match_offset & mask];
          windowPos = windowPos + 1 & mask;
          matchLength--;
        }
      }
    }

    if (this_run != 0) {
      throw new CorruptCabException();
    }

    this.R0 = r0;
    this.R1 = r1;
    this.R2 = r2;
    this.windowPosition = windowPos;
  }

  private void decodeIntelBlock(byte[] bytes, int outLength) {
    if (outLength <= 6 || !this.intelStarted) {
      this.intelCursorPos += outLength;
      return;
    }

    int cursorPos = this.intelCursorPos;
    int fileSize = this.intelFileSize;
    int abs_off = 0;

    int adjustedOutLength = outLength - 6;

    // save bytes
    while (abs_off < 6) {
      this.savedBytes[abs_off] = bytes[adjustedOutLength + abs_off];
      bytes[adjustedOutLength + abs_off] = (byte) -24;
      abs_off++;
    }

    int dataIndex = 0;
    int cursor_pos = cursorPos + adjustedOutLength;

    while (cursorPos < cursor_pos) {
      while (bytes[dataIndex++] == -24) {
        if (cursorPos >= cursor_pos) {
          break;
        }

        abs_off = bytes[dataIndex] & 0xFF |
            (bytes[dataIndex + 1] & 0xFF) << 8 |
            (bytes[dataIndex + 2] & 0xFF) << 16 |
            (bytes[dataIndex + 3] & 0xFF) << 24;

        if (abs_off >= 0) {
          if (abs_off < fileSize) {
            int rel_off = abs_off - cursorPos;

            bytes[dataIndex] = (byte) (rel_off & 0xFF);
            bytes[dataIndex + 1] = (byte) (rel_off >>> 8 & 0xFF);
            bytes[dataIndex + 2] = (byte) (rel_off >>> 16 & 0xFF);
            bytes[dataIndex + 3] = (byte) (rel_off >>> 24);
          }
        }
        else if (abs_off >= -cursorPos) {
          int rel_off = abs_off + this.intelFileSize;

          bytes[dataIndex] = (byte) (rel_off & 0xFF);
          bytes[dataIndex + 1] = (byte) (rel_off >>> 8 & 0xFF);
          bytes[dataIndex + 2] = (byte) (rel_off >>> 16 & 0xFF);
          bytes[dataIndex + 3] = (byte) (rel_off >>> 24);
        }

        dataIndex += 4;
        cursorPos += 5;
      }
      cursorPos++;
    }

    this.intelCursorPos = cursor_pos + 6;

    // restore saved bytes
    abs_off = 0;
    while (abs_off < 6) {
      bytes[adjustedOutLength + abs_off] = this.savedBytes[abs_off];
      abs_off++;
    }
  }

  @Override
  public void decompress(byte[] inputBytes, byte[] outputBytes, int inputLength, int outputLength) throws CabException {
    this.abort = false;
    this.index = 0;
    this.inputBytes = inputBytes;
    this.length = inputBytes.length;

    initBitStream();

    int decompressedOutputLength = decompressLoop(outputLength);
    System.arraycopy(this.localWindow, this.outputPosition, outputBytes, 0, decompressedOutputLength);

    if (this.framesRead++ < E8_DISABLE_THRESHOLD && this.intelFileSize != 0) {
      decodeIntelBlock(outputBytes, decompressedOutputLength);
    }
  }

  private void decompressBlockActions(int bytesToRead) throws CabException {
    this.windowPosition &= this.windowMask;

    if (this.windowPosition + bytesToRead > this.wndSize) {
      throw new CabException();
    }

    switch (this.blockType) {
      case BLOCKTYPE_UNCOMPRESSED:
        uncompressedAlgo(bytesToRead);
        return;
      case BLOCKTYPE_ALIGNED:
        alignedAlgo(bytesToRead);
        return;
      case BLOCKTYPE_VERBATIM:
        verbatimAlgo(bytesToRead);
        return;
      default:
        throw new CorruptCabException();
    }
  }

  private int decompressLoop(int bytesToRead) throws CabException {
    int i = bytesToRead;
    int lastWindowPosition;
    int k;
    int m;

    if (this.readHeader) {
      // read header
      if (readBits(1) == 1) {
        k = readBits(16);
        m = readBits(16);
        this.intelFileSize = k << 16 | m;
      }
      else {
        this.intelFileSize = 0;
      }
      this.readHeader = false;
    }

    lastWindowPosition = 0;
    while (bytesToRead > 0) {
      if (this.blocksRemaining == 0) {
        if (this.blockType == BLOCKTYPE_UNCOMPRESSED) {
          if ((this.blockLength & 0x1) != 0 && /* realign bitstream to word */
              this.index < this.length) {
            this.index++;
          }
          this.blockType = BLOCKTYPE_INVALID;
          initBitStream();
        }

        this.blockType = readBits(3);
        k = readBits(8);
        m = readBits(8);
        int n = readBits(8);

        if (this.abort) {
          break;
        }

        this.blockRemaining = this.blockLength = (k << 16) + (m << 8) + n;

        if (this.blockType == BLOCKTYPE_ALIGNED) {
          this.alignedTree.readLengths();
          this.alignedTree.buildTable();
          // rest of aligned header is same as verbatim
        }

        if (this.blockType == BLOCKTYPE_ALIGNED || this.blockType == BLOCKTYPE_VERBATIM) {
          this.mainTree.read();
          this.lengthTree.read();

          this.mainTree.readLengths(0, NUM_CHARS);
          this.mainTree.readLengths(NUM_CHARS, NUM_CHARS + this.mainElements * ALIGNED_NUM_ELEMENTS);
          this.mainTree.buildTable();

          if (this.mainTree.LENS[0xE8] != 0) {
            this.intelStarted = true;
          }

          this.lengthTree.readLengths(0, SECONDARY_NUM_ELEMENTS);
          this.lengthTree.buildTable();
        }
        else if (this.blockType == BLOCKTYPE_UNCOMPRESSED) {
          // because we can't assume otherwise
          this.intelStarted = true;
          this.index -= 2; // align the bitstream

          if (this.index < 0 || this.index + 12 >= this.length) {
            throw new CorruptCabException();
          }

          this.R0 = readInt();
          this.R1 = readInt();
          this.R2 = readInt();
        }
        else {
          throw new CorruptCabException();
        }
      }
      this.blocksRemaining = 1;

      while (this.blockRemaining > 0 && bytesToRead > 0) {
        if (this.blockRemaining < bytesToRead) {
          k = this.blockRemaining;
        }
        else {
          k = bytesToRead;
        }
        decompressBlockActions(k);

        this.blockRemaining -= k;
        bytesToRead -= k;
        lastWindowPosition += k;
      }

      if (this.blockRemaining == 0) {
        this.blocksRemaining = 0;
      }

      if (bytesToRead == 0 && this.blockAlignOffset != 16) {
        readNumberBits(this.blockAlignOffset);
      }
    }

    if (lastWindowPosition != i) {
      throw new CorruptCabException();
    }

    if (this.windowPosition == 0) {
      this.outputPosition = this.wndSize - lastWindowPosition;
    }
    else {
      this.outputPosition = this.windowPosition - lastWindowPosition;
    }

    return lastWindowPosition;
  }

  @Override
  public int getMaxGrowth() {
    return MAX_GROWTH;
  }

  @Override
  public void init(int windowBits) throws CabException {
    this.wndSize = 1 << windowBits;
    this.windowMask = this.wndSize - 1;
    reset(windowBits);
  }

  private void initBitStream() {
    if (this.blockType != BLOCKTYPE_UNCOMPRESSED) {
      this.bitsLeft = readShort() << 16 | readShort();
      this.blockAlignOffset = 16;
    }
  }

  private void maybeReset() {
    this.mainElements = 4;
    int i = 4;
    do {
      i += 1 << this.extraBits[this.mainElements];
      this.mainElements++;
    }
    while (i < this.wndSize);
  }

  int readBits(int numBitsToRead) {
    int i = this.bitsLeft >>> 32 - numBitsToRead;
    readNumberBits(numBitsToRead);
    return i;
  }

  private int readInt() {
    int i = this.inputBytes[this.index] & 0xFF |
        (this.inputBytes[this.index + 1] & 0xFF) << 8 |
        (this.inputBytes[this.index + 2] & 0xFF) << 16 |
        (this.inputBytes[this.index + 3] & 0xFF) << 24;
    this.index += 4;

    return i;
  }

  void readNumberBits(int numBits) {
    this.bitsLeft <<= numBits;
    this.blockAlignOffset -= numBits;

    if (this.blockAlignOffset <= 0) {
      this.bitsLeft |= readShort() << -this.blockAlignOffset;
      this.blockAlignOffset += 16;
    }
  }

  private int readShort() {
    if (this.index < this.length) {
      int i = this.inputBytes[this.index] & 0xFF | (this.inputBytes[this.index + 1] & 0xFF) << 8;
      this.index += 2;
      return i;
    }

    this.abort = true;
    this.index = 0;
    return 0;
  }

  @Override
  public void reset(int windowBits) throws CabException {
    if (this.windowSize == windowBits) {
      this.mainTree.reset();
      this.lengthTree.reset();
      this.alignedTree.reset();
    }
    else {
      maybeReset();
      int i = NUM_CHARS + this.mainElements * ALIGNED_NUM_ELEMENTS;
      this.localWindow = new byte[this.wndSize + 261];

      this.preTree = new DecompressLzxTree(PRETREE_NUM_ELEMENTS, ALIGNED_NUM_ELEMENTS, this, null);
      this.mainTree = new DecompressLzxTree(i, 9, this, this.preTree);
      this.lengthTree = new DecompressLzxTree(SECONDARY_NUM_ELEMENTS, 6, this, this.preTree);
      this.alignedTree = new DecompressLzxTree(ALIGNED_NUM_ELEMENTS, NUM_PRIMARY_LENGTHS, this, this.preTree);
    }

    this.windowSize = windowBits;
    this.R0 = this.R1 = this.R2 = 1;

    this.blocksRemaining = 0;
    this.windowPosition = 0;
    this.intelCursorPos = 0;

    this.readHeader = true;
    this.intelStarted = false;
    this.framesRead = 0;
    this.blockType = BLOCKTYPE_INVALID;
  }

  private void uncompressedAlgo(int length) throws CorruptCabException {
    if (this.index + length > this.length || this.windowPosition + length > this.wndSize) {
      throw new CorruptCabException();
    }

    this.intelStarted = true;
    System.arraycopy(this.inputBytes, this.index, this.localWindow, this.windowPosition, length);
    this.index += length;
    this.windowPosition += length;
  }

  private void verbatimAlgo(int this_run) throws CorruptCabException {
    int i = this.windowPosition;
    int mask = this.windowMask;
    byte[] windowPosition = this.localWindow;
    int r0 = this.R0;
    int r1 = this.R1;
    int r2 = this.R2;

    int[] arrayOfInt1 = this.extraBits;
    int[] arrayOfInt2 = this.positionBase;

    DecompressLzxTree mainTree = this.mainTree;
    DecompressLzxTree lengthTree = this.lengthTree;

    while (this_run > 0) {
      int main_element = mainTree.decodeElement();
      if (main_element < NUM_CHARS) {
        windowPosition[i++] = (byte) main_element;
        this_run--;
      }
      /* is a match */
      else {
        main_element -= NUM_CHARS;

        int match_length = main_element & NUM_PRIMARY_LENGTHS;
        if (match_length == NUM_PRIMARY_LENGTHS) {
          match_length += lengthTree.decodeElement();
        }

        int matchOffset = main_element >>> 3;

        /* check for repeated offsets (positions 0,1,2) */
        if (matchOffset == 0) {
          matchOffset = r0;
        }
        else if (matchOffset == 1) {
          matchOffset = r1;
          r1 = r0;
          r0 = matchOffset;
        }
        else if (matchOffset > 2) {
          // not repeated offset
          if (matchOffset > 3) {
            matchOffset = verbatimAlgo2(arrayOfInt1[matchOffset]) + arrayOfInt2[matchOffset];
          }
          else {
            matchOffset = 1;
          }

          r2 = r1;
          r1 = r0;
          r0 = matchOffset;
        }
        else {
          matchOffset = r2;
          r2 = r0;
          r0 = matchOffset;
        }
        match_length += MIN_MATCH;

        this_run -= match_length;

        while (match_length > 0) {
          windowPosition[i] = windowPosition[i - matchOffset & mask];
          i++;
          match_length--;
        }
      }
    }

    if (this_run != 0) {
      throw new CorruptCabException();
    }

    this.R0 = r0;
    this.R1 = r1;
    this.R2 = r2;
    this.windowPosition = i;
  }

  private int verbatimAlgo2(int position) {
    int i = this.bitsLeft >>> 32 - position;

    this.bitsLeft <<= position;
    this.blockAlignOffset -= position;

    if (this.blockAlignOffset <= 0) {
      this.bitsLeft |= readShort() << -this.blockAlignOffset;
      this.blockAlignOffset += 16;

      if (this.blockAlignOffset <= 0) {
        this.bitsLeft |= readShort() << -this.blockAlignOffset;
        this.blockAlignOffset += 16;
      }
    }

    return i;
  }
}
