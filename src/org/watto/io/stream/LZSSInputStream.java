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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
**********************************************************************************************
LZSS compression routines

This compression algorithm is based on the ideas of Lempel and Ziv, with the modifications
suggested by Storer and Szymanski. The algorithm is based on the use of a ring buffer, which
initially contains zeros. We read several characters from the file into the buffer, and then
search the buffer for the longest string that matches the characters just read, and output the
length and position of the match in the buffer.

With a buffer size of 4096 bytes, the position can be encoded in 12 bits. If we represent the
match length in four bits, the <position,length> pair is two bytes long. If the longest match
is no more than two characters, then we send just one character without encoding, and restart
the process with the next letter. We must send one extra bit each time to tell the decoder
whether we are sending a <position,length> pair or an unencoded character, and these flags are
stored as an eight bit mask every eight items.

This implementation uses binary trees to speed up the search for the longest match.

Original code by Haruhiko Okumura. Modified for use in the Allegro filesystem by Shawn Hargreaves.

Use, distribute, and modify this code freely.
**********************************************************************************************
**/
public class LZSSInputStream extends FilterInputStream {

  /** 4k buffers for LZ compression */
  private static int N = 4096;
  /** upper limit for LZ match length */
  private static int F = 18;
  /** LZ encode string into pos and length if match size is greater than this */
  private static int THRESHOLD = 2;

  // for reading LZ files
  int i, j, k, r, c;
  int flags;
  /** where have we got to? */
  int state;
  /** ring buffer, with F-1 extra bytes for string comparison */
  byte text_buf[];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public LZSSInputStream(InputStream i) {
    super(i);
    text_buf = new byte[N + F - 1];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public LZSSInputStream(InputStream i, int inF) {
    super(i);
    F = inF;
    text_buf = new byte[N + F - 1];
  }

  // code stream
  // bits flag , bit 1-quoted byte 0-packed string
  // follows 8 codes, types determined by flagbits
  // 1st code type is in the lowest flag bit
  // packed string is encoded in 16 bits:
  // 12 bits buffer position, 4 bits size
  // 1st byte: low part of pos.
  //   2nd byte: high 4 bits = 4 high bits of pos.
  //             low  4 bits = size - 2

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int available() {
    return 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() throws IOException {
    text_buf = null;
    super.close();
    in = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void mark(int readlimit) {
  }

  // non supported interface methods

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean markSupported() {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() throws IOException {
    byte b[] = new byte[1];
    int rc = read(b, 0, 1);
    if (rc == -1) {
      return -1;
    }
    byte z = b[0];
    return z & 0xFF;
  }

  /**
  **********************************************************************************************
  Called by refill_buffer(). Unpacks into <i>buf</i>, until either EOF is reached or <i>s</i>
  bytes have been extracted.
  @return the number of bytes added to the buffer
  **********************************************************************************************
  **/
  @Override
  public final int read(byte buf[], int bufi, int s) throws IOException {
    int size = 0;
    if (state == 0) {
      r = N - F; // ringbuffer pos?
      flags = 0; // zero flags
    }
    while (s > 0) {
      if (state == 2) {
        // output encoded string from buffer
        if (k <= j) {
          c = text_buf[(i + k) & (N - 1)];
          text_buf[r++] = (byte) c;
          buf[bufi++] = (byte) c;
          r &= (N - 1);
          k++;
          --s;
          size++;
          continue;
        }
      }
      // test zda mame nacist dalsi flags byte
      if (((flags >>>= 1) & 256) == 0) {
        if ((c = in.read()) == -1) {
          break;
        }
        flags = c | 0xFF00; // uses higher byte to count eight
      }
      if ((flags & 1) == 1) {
        // quoted character
        if ((c = in.read()) == -1) {
          break;
        }
        text_buf[r++] = (byte) c;
        r &= N - 1;
        buf[bufi++] = (byte) c;
        ++size;
        --s;
        state = 1;
        continue;
      }
      // encoded string
      if ((i = in.read()) == -1) {
        break;
      }
      if ((j = in.read()) == -1) {
        break;
      }
      i |= ((j & 0xF0) << 4); // position
      j = (j & 0x0F) + THRESHOLD; // size
      k = 0; // output string pos
      state = 2;
    } /* while s>0 */

    if (size == 0) {
      return -1;
    }
    else {
      return size;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void reset() throws IOException {
    throw new IOException("Reset is not supported");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long skip(long n) {
    return 0;
  }

}
