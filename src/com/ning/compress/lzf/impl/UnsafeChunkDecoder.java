
package com.ning.compress.lzf.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import com.ning.compress.lzf.ChunkDecoder;
import com.ning.compress.lzf.LZFChunk;
import com.ning.compress.lzf.LZFException;
import sun.misc.Unsafe;

/**
 * Highly optimized {@link ChunkDecoder} implementation that uses
 * Sun JDK's Unsafe class (which may be included by other JDK's as well;
 * IBM's apparently does).
 *<p>
 * Credits for the idea go to Dain Sundstrom, who kindly suggested this use,
 * and is all-around great source for optimization tips and tricks.
 * Big thanks also to LZ4-java developers, whose stellar performance made
 * me go back and see what more I can do to optimize this code!
 */
public class UnsafeChunkDecoder extends ChunkDecoder {

  private static final Unsafe unsafe;
  static {
    try {
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      unsafe = (Unsafe) theUnsafe.get(null);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final long BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
  //    private static final long SHORT_ARRAY_OFFSET = unsafe.arrayBaseOffset(short[].class);
  //    private static final long SHORT_ARRAY_STRIDE = unsafe.arrayIndexScale(short[].class);

  public UnsafeChunkDecoder() {
  }

  @Override
  public final int decodeChunk(final InputStream is, final byte[] inputBuffer, final byte[] outputBuffer)
      throws IOException {
    /* note: we do NOT read more than 5 bytes because otherwise might need to shuffle bytes
     * for output buffer (could perhaps optimize in future?)
     */

    int bytesRead = readHeader(is, inputBuffer);
    if ((bytesRead < HEADER_BYTES)
        || inputBuffer[0] != LZFChunk.BYTE_Z || inputBuffer[1] != LZFChunk.BYTE_V) {
      if (bytesRead == 0) { // probably fine, clean EOF
        return -1;
      }
      _reportCorruptHeader();
    }
    int type = inputBuffer[2];
    int compLen = uint16(inputBuffer, 3);
    if (type == LZFChunk.BLOCK_TYPE_NON_COMPRESSED) { // uncompressed
      readFully(is, false, outputBuffer, 0, compLen);
      return compLen;
    }
    // compressed
    readFully(is, true, inputBuffer, 0, 2 + compLen); // first 2 bytes are uncompressed length
    int uncompLen = uint16(inputBuffer, 0);
    decodeChunk(inputBuffer, 2, outputBuffer, 0, uncompLen);
    return uncompLen;

  }

  @Override
  public final void decodeChunk(byte[] in, int inPos, byte[] out, int outPos, int outEnd)
      throws LZFException {
    // We need to take care of end condition, leave last 32 bytes out
    final int outputEnd8 = outEnd - 8;
    final int outputEnd32 = outEnd - 32;

    main_loop: do {
      int ctrl = in[inPos++] & 255;
      while (ctrl < LZFChunk.MAX_LITERAL) { // literal run(s)
        if (outPos > outputEnd32) {
          System.arraycopy(in, inPos, out, outPos, ctrl + 1);
        }
        else {
          copyUpTo32(in, inPos, out, outPos, ctrl);
        }
        ++ctrl;
        inPos += ctrl;
        outPos += ctrl;
        if (outPos >= outEnd) {
          break main_loop;
        }
        ctrl = in[inPos++] & 255;
      }
      // back reference
      int len = ctrl >> 5;
      ctrl = -((ctrl & 0x1f) << 8) - 1;
      // short back reference? 2 bytes; run lengths of 2 - 8 bytes
      if (len < 7) {
        ctrl -= in[inPos++] & 255;
        if (ctrl < -7 && outPos < outputEnd8) { // non-overlapping? can use efficient bulk copy
          final long rawOffset = BYTE_ARRAY_OFFSET + outPos;
          unsafe.putLong(out, rawOffset, unsafe.getLong(out, rawOffset + ctrl));
          //                    moveLong(out, outPos, outEnd, ctrl);
          outPos += len + 2;
          continue;
        }
        // otherwise, byte-by-byte
        outPos = copyOverlappingShort(out, outPos, ctrl, len);
        continue;
      }
      // long back reference: 3 bytes, length of up to 264 bytes
      len = (in[inPos++] & 255) + 9;
      ctrl -= in[inPos++] & 255;
      // First: ovelapping case can't use default handling, off line.
      if ((ctrl > -9) || (outPos > outputEnd32)) {
        outPos = copyOverlappingLong(out, outPos, ctrl, len - 9);
        continue;
      }
      // but non-overlapping is simple
      if (len <= 32) {
        copyUpTo32(out, outPos + ctrl, outPos, len - 1);
        outPos += len;
        continue;
      }
      copyLong(out, outPos + ctrl, outPos, len, outputEnd32);
      outPos += len;
    }
    while (outPos < outEnd);

    // sanity check to guard against corrupt data:
    if (outPos != outEnd) {
      throw new LZFException("Corrupt data: overrun in decompress, input offset " + inPos + ", output offset " + outPos);
    }

  }

  /**
  **********************************************************************************************
  GE: copy of decodeChunk, modified to return the inPos
  **********************************************************************************************
  **/
  public final int decodeChunkGE(byte[] in, int inPos, byte[] out, int outPos, int outEnd)
      throws LZFException {
    // We need to take care of end condition, leave last 32 bytes out
    final int outputEnd8 = outEnd - 8;
    final int outputEnd32 = outEnd - 32;

    main_loop: do {
      int ctrl = in[inPos++] & 255;
      while (ctrl < LZFChunk.MAX_LITERAL) { // literal run(s)
        if (outPos > outputEnd32) {
          System.arraycopy(in, inPos, out, outPos, ctrl + 1);
        }
        else {
          copyUpTo32(in, inPos, out, outPos, ctrl);
        }
        ++ctrl;
        inPos += ctrl;
        outPos += ctrl;
        if (outPos >= outEnd) {
          break main_loop;
        }
        ctrl = in[inPos++] & 255;
      }
      // back reference
      int len = ctrl >> 5;
      ctrl = -((ctrl & 0x1f) << 8) - 1;
      // short back reference? 2 bytes; run lengths of 2 - 8 bytes
      if (len < 7) {
        ctrl -= in[inPos++] & 255;
        if (ctrl < -7 && outPos < outputEnd8) { // non-overlapping? can use efficient bulk copy
          final long rawOffset = BYTE_ARRAY_OFFSET + outPos;
          unsafe.putLong(out, rawOffset, unsafe.getLong(out, rawOffset + ctrl));
          //                    moveLong(out, outPos, outEnd, ctrl);
          outPos += len + 2;
          continue;
        }
        // otherwise, byte-by-byte
        outPos = copyOverlappingShort(out, outPos, ctrl, len);
        continue;
      }
      // long back reference: 3 bytes, length of up to 264 bytes
      len = (in[inPos++] & 255) + 9;
      ctrl -= in[inPos++] & 255;
      // First: ovelapping case can't use default handling, off line.
      if ((ctrl > -9) || (outPos > outputEnd32)) {
        outPos = copyOverlappingLong(out, outPos, ctrl, len - 9);
        continue;
      }
      // but non-overlapping is simple
      if (len <= 32) {
        copyUpTo32(out, outPos + ctrl, outPos, len - 1);
        outPos += len;
        continue;
      }
      copyLong(out, outPos + ctrl, outPos, len, outputEnd32);
      outPos += len;
    }
    while (outPos < outEnd);

    // sanity check to guard against corrupt data:
    if (outPos != outEnd) {
      throw new LZFException("Corrupt data: overrun in decompress, input offset " + inPos + ", output offset " + outPos);
    }

    return inPos;
  }

  @Override
  public int skipOrDecodeChunk(final InputStream is, final byte[] inputBuffer, final byte[] outputBuffer, final long maxToSkip)
      throws IOException {
    int bytesRead = readHeader(is, inputBuffer);
    if ((bytesRead < HEADER_BYTES)
        || inputBuffer[0] != LZFChunk.BYTE_Z || inputBuffer[1] != LZFChunk.BYTE_V) {
      if (bytesRead == 0) { // probably fine, clean EOF
        return -1;
      }
      _reportCorruptHeader();
    }
    int type = inputBuffer[2];
    int compLen = uint16(inputBuffer, 3);
    if (type == LZFChunk.BLOCK_TYPE_NON_COMPRESSED) { // uncompressed, simple
      if (compLen <= maxToSkip) {
        skipFully(is, compLen);
        return compLen;
      }
      readFully(is, false, outputBuffer, 0, compLen);
      return -(compLen + 1);
    }
    // compressed: need 2 more bytes to know uncompressed length...
    readFully(is, true, inputBuffer, 0, 2);
    int uncompLen = uint16(inputBuffer, 0);
    // can we just skip it wholesale?
    if (uncompLen <= maxToSkip) { // awesome: skip N physical compressed bytes, which mean M logical (uncomp) bytes
      skipFully(is, compLen);
      return uncompLen;
    }
    // otherwise, read and uncompress the chunk normally
    readFully(is, true, inputBuffer, 2, compLen); // first 2 bytes are uncompressed length
    decodeChunk(inputBuffer, 2, outputBuffer, 0, uncompLen);
    return -(uncompLen + 1);
  }

  /*
  ///////////////////////////////////////////////////////////////////////
  // Internal methods
  ///////////////////////////////////////////////////////////////////////
   */

  private final int copyOverlappingShort(final byte[] out, int outPos, final int offset, int len) {
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    switch (len) {
      case 6:
        out[outPos] = out[outPos++ + offset];
      case 5:
        out[outPos] = out[outPos++ + offset];
      case 4:
        out[outPos] = out[outPos++ + offset];
      case 3:
        out[outPos] = out[outPos++ + offset];
      case 2:
        out[outPos] = out[outPos++ + offset];
      case 1:
        out[outPos] = out[outPos++ + offset];
    }
    return outPos;
  }

  private final static int copyOverlappingLong(final byte[] out, int outPos, final int offset, int len) {
    // otherwise manual copy: so first just copy 9 bytes we know are needed
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];
    out[outPos] = out[outPos++ + offset];

    // then loop
    // Odd: after extensive profiling, looks like magic number
    // for unrolling is 4: with 8 performance is worse (even
    // bit less than with no unrolling).
    len += outPos;
    final int end = len - 3;
    while (outPos < end) {
      out[outPos] = out[outPos++ + offset];
      out[outPos] = out[outPos++ + offset];
      out[outPos] = out[outPos++ + offset];
      out[outPos] = out[outPos++ + offset];
    }
    switch (len - outPos) {
      case 3:
        out[outPos] = out[outPos++ + offset];
      case 2:
        out[outPos] = out[outPos++ + offset];
      case 1:
        out[outPos] = out[outPos++ + offset];
    }
    return outPos;
  }

  private final static void copyUpTo32(byte[] buffer, int inputIndex, int outputIndex, int lengthMinusOne) {
    long inPtr = BYTE_ARRAY_OFFSET + inputIndex;
    long outPtr = BYTE_ARRAY_OFFSET + outputIndex;

    unsafe.putLong(buffer, outPtr, unsafe.getLong(buffer, inPtr));
    if (lengthMinusOne > 7) {
      inPtr += 8;
      outPtr += 8;
      unsafe.putLong(buffer, outPtr, unsafe.getLong(buffer, inPtr));
      if (lengthMinusOne > 15) {
        inPtr += 8;
        outPtr += 8;
        unsafe.putLong(buffer, outPtr, unsafe.getLong(buffer, inPtr));
        if (lengthMinusOne > 23) {
          inPtr += 8;
          outPtr += 8;
          unsafe.putLong(buffer, outPtr, unsafe.getLong(buffer, inPtr));
        }
      }
    }
  }

  private final static void copyUpTo32(byte[] in, int inputIndex, byte[] out, int outputIndex, int lengthMinusOne) {
    long inPtr = BYTE_ARRAY_OFFSET + inputIndex;
    long outPtr = BYTE_ARRAY_OFFSET + outputIndex;

    unsafe.putLong(out, outPtr, unsafe.getLong(in, inPtr));
    if (lengthMinusOne > 7) {
      inPtr += 8;
      outPtr += 8;
      unsafe.putLong(out, outPtr, unsafe.getLong(in, inPtr));
      if (lengthMinusOne > 15) {
        inPtr += 8;
        outPtr += 8;
        unsafe.putLong(out, outPtr, unsafe.getLong(in, inPtr));
        if (lengthMinusOne > 23) {
          inPtr += 8;
          outPtr += 8;
          unsafe.putLong(out, outPtr, unsafe.getLong(in, inPtr));
        }
      }
    }
  }

  private final static void copyLong(byte[] buffer, int inputIndex, int outputIndex, int length, int outputEnd8) {
    if ((outputIndex + length) > outputEnd8) {
      copyLongTail(buffer, inputIndex, outputIndex, length);
      return;
    }
    long inPtr = BYTE_ARRAY_OFFSET + inputIndex;
    long outPtr = BYTE_ARRAY_OFFSET + outputIndex;

    while (length >= 8) {
      unsafe.putLong(buffer, outPtr, unsafe.getLong(buffer, inPtr));
      inPtr += 8;
      outPtr += 8;
      length -= 8;
    }
    if (length > 4) {
      unsafe.putLong(buffer, outPtr, unsafe.getLong(buffer, inPtr));
    }
    else if (length > 0) {
      unsafe.putInt(buffer, outPtr, unsafe.getInt(buffer, inPtr));
    }
  }

  private final static void copyLongTail(byte[] buffer, int inputIndex, int outputIndex, int length) {
    for (final int inEnd = inputIndex + length; inputIndex < inEnd;) {
      buffer[outputIndex++] = buffer[inputIndex++];
    }
  }
}
