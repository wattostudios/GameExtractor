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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
public class LZSSOutputStream extends FilterOutputStream {

  /** 4k buffers for LZ compression */
  final private static int N = 4096;
  /** upper limit for LZ match length */
  final private static int F = 18;
  /** LZ encode string into pos and length if match size is greater than this */
  final private static int THRESHOLD = 2;

  // stuff for doing LZ compression
  int i, len, r, s;
  byte c;
  int last_match_length, code_buf_ptr;
  byte mask;
  byte code_buf[];
  int match_position;
  int match_length;
  /** where have we got to in the pack? */
  int state;
  /** left children, */
  int lson[];
  /** right children, */
  int rson[];
  /** and parents, = binary search trees */
  int dad[];
  /** ring buffer, with F-1 extra bytes for string comparison */
  byte text_buf[];

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public LZSSOutputStream(OutputStream ou) {
    super(ou);
    code_buf = new byte[17];
    lson = new int[N + 1];
    rson = new int[N + 257];
    dad = new int[N + 1];
    text_buf = new byte[N + F - 1];
    state = 0;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void close() throws IOException {

    if (code_buf == null) {
      throw new IOException("LZSSOutputStream is already closed");
    }

    try {
      flush();
    }
    catch (IOException err) {
      throw err;
    }
    finally {
      out = null;
      code_buf = null;
      lson = null;
      rson = null;
      dad = null;
      text_buf = null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  protected final void finalize() throws Throwable {
    try {
      flush();
    }
    finally {
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void flush() throws IOException {
    pack_write(0, null, 0, true);
  }

  /**
  **********************************************************************************************
  For i = 0 to N-1, rson[i] and lson[i] will be the right and left children of node i. These nodes
  need not be initialized. Also, dad[i] is the parent of node i. These are initialized to N, which
  stands for 'not used.' For i = 0 to 255, rson[N+i+1] is the root of the tree for strings that
  begin with character i. These are initialized to N. Note there are 256 trees.
  **********************************************************************************************
  **/
  private final void inittree() {
    int i;

    for (i = N + 1; i <= N + 256; i++) {
      rson[i] = N;
    }

    for (i = 0; i < N; i++) {
      dad[i] = N;
    }
  }

  /**
  **********************************************************************************************
  Removes a node from a tree.
  **********************************************************************************************
  **/
  private final void pack_deletenode(int p) {
    int q;

    if (dad[p] == N) {
      // not in tree
      return;
    }

    if (rson[p] == N) {
      q = lson[p];
    }
    else if (lson[p] == N) {
      q = rson[p];
    }
    else {
      q = lson[p];

      if (rson[q] != N) {
        do {
          q = rson[q];
        }
        while (rson[q] != N);

        rson[dad[q]] = lson[q];
        dad[lson[q]] = dad[q];
        lson[q] = lson[p];
        dad[lson[p]] = q;
      }
      rson[q] = rson[p];
      dad[rson[p]] = q;
    }

    dad[q] = dad[p];
    if (rson[dad[p]] == p) {
      rson[dad[p]] = q;
    }
    else {
      lson[dad[p]] = q;
    }

    dad[p] = N;
  }

  /**
  **********************************************************************************************
  Inserts a string of length F, text_buf[r..r+F-1], into one of the trees (text_buf[r]'th tree)
  and returns the longest-match position and length via match_position and match_length. If
  match_length = F, then removes the old node in favor of the new one, because the old one will
  be deleted sooner. Note r plays double role, as tree node and position in the buffer.
  **********************************************************************************************
  **/
  private final void pack_insertnode(int r) {
    int i, p, cmp;

    cmp = 1;
    p = N + 1 + (text_buf[r] & 0xFF);
    rson[r] = lson[r] = N;
    match_length = 0;

    for (;;) {

      if (cmp >= 0) {
        if (rson[p] != N) {
          p = rson[p];
        }
        else {
          rson[p] = r;
          dad[r] = p;
          return;
        }
      }
      else {
        if (lson[p] != N) {
          p = lson[p];
        }
        else {
          lson[p] = r;
          dad[r] = p;
          return;
        }
      }

      for (i = 1; i < F; i++) {
        if ((cmp = (text_buf[r + i] & 0xff) - (text_buf[p + i]) & 0xFF) != 0) {
          break;
        }
      }

      if (i > match_length) {
        match_position = p;
        if ((match_length = i) >= F) {
          break;
        }
      }
    }

    dad[r] = dad[p];
    lson[r] = lson[p];
    rson[r] = rson[p];
    dad[lson[p]] = r;
    dad[rson[p]] = r;

    if (rson[dad[p]] == p) {
      rson[dad[p]] = r;
    }
    else {
      lson[dad[p]] = r;
    }

    // remove p
    dad[p] = N;
  }

  /**
  **********************************************************************************************
  Called by flush_buffer(). Packs size bytes from buf, using the pack information contained in dat.
  @return 0 on success
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  private final void pack_write(int size, byte buf[], int bufi, boolean last) throws IOException {

    boolean skipme = false;
    if (state == 0) {
      code_buf[0] = 0;

      // code_buf[1..16] saves eight units of code, and code_buf[0] works as eight flags,
      // "1" representing that the unit is an unencoded letter (1 byte), "0" a
      // position-and-length pair (2 bytes). Thus, eight units require at most 16 bytes of code.
      code_buf_ptr = mask = 1;

      s = 0;
      r = N - F;
      inittree();
      len = 0;
    }
    else if (state == 1) {
      len++;
    }

    if (state != 2) {
      while ((len < F) && (size > 0)) {
        text_buf[r + len] = buf[bufi++];
        if (--size == 0) {
          if (!last) {
            state = 1;
            return;
          }
        }
        pos1: len++;
      }

      if (len == 0) {
        return;
      }

      for (i = 1; i <= F; i++) {
        // Insert the F strings, each of which begins with one or more 'space' characters.
        // Note the order in which these strings are inserted. This way, degenerate trees
        // will be less likely to occur.
        pack_insertnode(r - i);
      }

      // Finally, insert the whole string just read. match_length and match_position are set.
      pack_insertnode(r);

    } /* state!=2 */
    else {
      skipme = true;
    }

    do {
      if (skipme == false) {
        if (match_length > len) {
          // match_length may be long near the end
          match_length = len;
        }

        if (match_length <= THRESHOLD) {
          // not long enough match: send one byte, 'send one byte' flag, send uncoded
          match_length = 1;
          code_buf[0] |= mask;
          code_buf[code_buf_ptr++] = text_buf[r];
        }
        else {
          // send position and length pair. Note match_length > THRESHOLD
          code_buf[code_buf_ptr++] = (byte) match_position;
          code_buf[code_buf_ptr++] = (byte) (((match_position >>> 4) & 0xF0) | (match_length - (THRESHOLD + 1)));
        }

        if ((mask <<= 1) == 0) {
          //send at most 8 units of code together
          out.write(code_buf, 0, code_buf_ptr);
          code_buf[0] = 0;
          code_buf_ptr = mask = 1;
        }

        last_match_length = match_length;
        i = 0;
      } /*skipme*/

      for (;;) {
        if (skipme == false) {
          if ((i >= last_match_length) || (size <= 0)) {
            break;
          }
          c = buf[bufi++];
          if (--size == 0) {
            if (!last) {
              state = 2;
              return;
            }
          }
        }
        else {
          skipme = false;
        }

        // delete old strings and read new bytes
        pos2: pack_deletenode(s);
        text_buf[s] = c;

        if (s < F - 1) {
          // if the position is near the end of buffer, extend buffer so string comparison is easier
          text_buf[s + N] = c;
        }

        // since this is a ring buffer, increment the position modulo N
        s = (s + 1) & (N - 1);
        r = (r + 1) & (N - 1);

        // register the string in text_buf[r..r+F-1]
        pack_insertnode(r);
        i++;
      }

      // after the end of text, no need to read, but buffer may not be empty
      while (i++ < last_match_length) {
        pack_deletenode(s);
        s = (s + 1) & (N - 1);
        r = (r + 1) & (N - 1);
        if (--len != 0) {
          pack_insertnode(r);
        }
      }

    }
    while (len > 0); // until length of string to be processed is zero

    if (code_buf_ptr > 1) {
      // send remaining code
      out.write(code_buf, 0, code_buf_ptr);
      return;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(byte zz[], int ofs, int sz) throws IOException {
    pack_write(sz, zz, ofs, false);
  }

}
