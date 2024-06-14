/* LZOConstants.java -- various constants (Original file)

   This file is part of the LZO real-time data compression library.

   Copyright (C) 1999 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1998 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1997 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1996 Markus Franz Xaver Johannes Oberhumer

   The LZO library is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of
   the License, or (at your option) any later version.

   The LZO library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with the LZO library; see the file COPYING.
   If not, write to the Free Software Foundation, Inc.,
   59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

   Markus F.X.J. Oberhumer
   <markus.oberhumer@jk.uni-linz.ac.at>
   http://wildsau.idv.uni-linz.ac.at/mfx/lzo.html
   

   Java Porting of minilzo.c (2.03) by
   Copyright (C) 2010 Mahadevan Gorti Surya Srinivasa <sgorti@gmail.com>
 */

package org.jvcompress.lzo;


import org.jvcompress.util.MInt;

/**  Java ported code of minilzo.c(2.03).
 *
 * All compress/decompress/decompress_safe were ported. Original java version of MiniLZO supported 
 * only decompression via Lzo1xDecompressor.java and Lzo1xDecompressor ported way back in 1999.
 *
 * This new MiniLZO.java based was taken from minilzo.c version 2.03. 
 * 
 *  @author mahadevan.gss
 */

public final class MiniLZO implements LZOConstants {

  public static final int c_top_loop = 1;

  public static final int c_first_literal_run = 2;

  public static final int c_match = 3;

  public static final int c_copy_match = 4;

  public static final int c_match_done = 5;

  public static final int c_match_next = 6;

  public static final int c_eof_found = 7;

  public static final int c_input_overrun = 8;

  public static final int c_output_overrun = 9;

  public static final int c_lookbehind_overrun = 10;

  public static final int c0_top = 1;

  public static final int c0_try_match = 2;

  public static final int c0_literal = 3;

  public static final int c0_match = 4;

  public static final int c0_m3_m4_len = 5;

  public static final int c0_m3_m4_offset = 6;

  public static final int c0_last = 7;

  private static final boolean debug = false;

  // Is there in original, later remove to reduce function call overhead
  @SuppressWarnings("unused")
  private static final int U(byte b) {
    return b & 0xff;
  }




  private final static int _lzo1x_1_do_compress(final byte[] in, final int in_len, final byte[] out, MInt out_len, int[] dict) {
    int ip = 0;
    int in_base = 0;
    int out_base = 0;
    int op;
    int in_end = in_base + in_len;
    int ip_end = in_base + in_len - 8 - 5;
    int ii = 0;
    int state = c0_top;
    op = out_base;
    ip = in_base;
    ii = ip;

    ip += 4;
    int m_pos = in_base;
    int m_off = in_base;
    int m_len = 0;
    int dindex = 0;

    loop0: for (;;) {
      switch (state) {
        case c0_top:
          dindex = (((((int) ((0x21) * ((((((((int) ((in[ip + 1 + 2 + 1] & 0xff) << (6)) ^ (in[ip + 1 + 1 + 1] & 0xff) << (5)) ^ (in[ip + 1 + 0] & 0xff) << (5)) ^ (in[ip + 0] & 0xff))))) >> 5) & (((1 << (14)) - 1) >> (0))) << (0)))));
          m_pos = dict[dindex];
          m_pos = ip - (ip - m_pos);
          if ((m_pos <= in_base) ||
              ((m_off = (ip - m_pos)) <= 0) ||
              (m_off > 0xbfff)) {//49151
            if (debug)
              System.out.println("1:m_pos:" + m_pos + ", ip:" + ip + ", m_off:" + m_off);
            state = c0_literal;
            continue loop0;//goto literal;
          }
          //System.out.println("2:m_pos:"+m_pos+", ip:"+ip+", m_off:"+m_off);
          if (m_off <= 0x0800 || in[m_pos + 3] == in[ip + 3]) {
            state = c0_try_match;
            continue loop0;//goto try_match;
          }
          dindex = (dindex & (((1 << (14)) - 1) & 0x7ff)) ^ (((((1 << (14)) - 1) >> 1) + 1) | 0x1f);

          m_pos = dict[dindex];
          if ((m_pos < in_base) || (m_off = (ip - m_pos)) <= 0 || m_off > 0xbfff) {
            state = c0_literal;
            continue loop0;//goto literal;
          }
          if (m_off <= 0x0800 || in[m_pos + 3] == in[ip + 3]) {
            state = c0_try_match;
            continue loop0;//goto try_match;
          }
          state = c0_literal;
          continue loop0;//goto literal;

        case c0_try_match:
          //if ((* (const unsigned short *) m_pos != * (const unsigned short *) ip) { }
          if (in[m_pos] != in[ip] || in[m_pos + 1] != in[ip + 1]) {
          }
          else {
            if (in[m_pos + 2] == in[ip + 2]) {
              state = c0_match;
              continue loop0;//goto match;
            }// else { }
          }

        case c0_literal:
          dict[dindex] = (ip);
          ip++;
          if (ip >= ip_end)
            break loop0;
          state = c0_top;
          continue loop0;

        case c0_match:
          dict[dindex] = (ip);
          if ((ip - ii) > 0) {
            int t = ip - ii;

            if (t <= 3) {
              out[op - 2] |= (byte) t;
            }
            else if (t <= 18) {
              out[op++] = (byte) (t - 3);
            }
            else {
              int tt = t - 18;

              out[op++] = 0;
              while (tt > 255) {
                tt -= 255;
                out[op++] = 0;
              }
              out[op++] = (byte) tt;
            }
            do
              out[op++] = in[ii++];
            while (--t > 0);
          }

          ip += 3;
          if (in[m_pos + 3] != in[ip++] || in[m_pos + 4] != in[ip++] || in[m_pos + 5] != in[ip++] ||
              in[m_pos + 6] != in[ip++] || in[m_pos + 7] != in[ip++] || in[m_pos + 8] != in[ip++]) {
            --ip;
            m_len = ip - ii;

            if (m_off <= 0x0800) {
              m_off -= 1;

              out[op++] = (byte) (((m_len - 1) << 5) | ((m_off & 7) << 2));
              out[op++] = (byte) (m_off >> 3);
            }
            else if (m_off <= 0x4000) {
              m_off -= 1;
              out[op++] = (byte) (32 | (m_len - 2));
              state = c0_m3_m4_offset;
              continue loop0;//goto m3_m4_offset;
            }
            else {
              m_off -= 0x4000;
              out[op++] = (byte) (16 | ((m_off & 0x4000) >> 11) | (m_len - 2));
              state = c0_m3_m4_offset;
              continue loop0;// goto m3_m4_offset;
            }
            state = c0_last;
            continue loop0;
          }
          else {
            int end = in_end;
            int m = m_pos + 8 + 1;
            while (ip < end && in[m] == in[ip]) {
              m++;
              ip++;
            }
            m_len = ip - ii;

            if (m_off <= 0x4000) {
              m_off -= 1;
              if (m_len <= 33) {
                out[op++] = (byte) (32 | (m_len - 2));
              }
              else {
                m_len -= 33;
                out[op++] = 32 | 0;
                state = c0_m3_m4_len;
                continue loop0;// goto m3_m4_len;
              }
            }
            else {
              m_off -= 0x4000;
              if (m_len <= 9) {
                out[op++] = (byte) (16 | ((m_off & 0x4000) >> 11) | (m_len - 2));
              }
              else {
                m_len -= 9;
                out[op++] = (byte) (16 | ((m_off & 0x4000) >> 11));
                //m3_m4_len:
                while (m_len > 255) {
                  m_len -= 255;
                  out[op++] = 0;
                }
                out[op++] = (byte) m_len;
              }
            }
            //m3_m4_offset:            
            out[op++] = (byte) ((m_off & 63) << 2);
            out[op++] = (byte) (m_off >> 6);

            state = c0_last;
            continue loop0;
          }
          // These two case statements were duplicated, as I am  not able to break m3_m4_len & m3_m4_offset labels
          // from nested if/else blocks :)
        case c0_m3_m4_len:
          while (m_len > 255) {
            m_len -= 255;
            out[op++] = 0;
          }
          out[op++] = (byte) m_len;

        case c0_m3_m4_offset:
          out[op++] = (byte) ((m_off & 63) << 2);
          out[op++] = (byte) (m_off >> 6);
        case c0_last:
          ii = ip;
          if (ip >= ip_end)
            break loop0;
          state = c0_top;
      }
    }

    out_len.v = op - out_base;
    return (in_end - ii);
  }

  /** Compress the data. Error codes would be returned (@see LZOConstants). 
   * Compressed length is returned via out_len. Pass the integer array for 
   * dictionary(so that user can reuse the same over multiple calls. Ensure 
   * to zero out the dict contents).
   *
   * @param in Input byte array to be compressed
   * @param in_len Input length 
   * @param out  compressed output byte array. Ensure out_len =  (in_len + in_len / 16 + 64 + 3)
   * @param out_len Compressed data length
   * @param dict Dictionary array. Zero out before reuse.
   */
  public final static int lzo1x_1_compress(final byte[] in, final int in_len, final byte[] out, MInt out_len, int[] dict) {
    int in_base = 0;
    int out_base = 0;
    int op = 0;//out;
    int t = 0;

    if ((in_len <= 8 + 5)) {
      t = in_len;
    }
    else {
      t = _lzo1x_1_do_compress(in, in_len, out, out_len, dict);
      op += out_len.v;
    }

    if (t > 0) {
      int ii = in_base + in_len - t;

      if (op == out_base && t <= 238)
        out[op++] = (byte) (17 + t);
      else if (t <= 3)
        out[op - 2] |= (byte) t;
      else if (t <= 18)
        out[op++] = (byte) (t - 3);
      else {
        int tt = t - 18;
        out[op++] = 0;
        while (tt > 255) {
          tt -= 255;
          out[op++] = 0;
        }
        out[op++] = (byte) tt;
      }
      do
        out[op++] = in[ii++];
      while (--t > 0);
    }

    out[op++] = (byte) (16 | 1);
    out[op++] = 0;
    out[op++] = 0;

    out_len.v = (op - out_base);
    return 0;
  }

  /** Decompress the data. Error codes would be returned (@see LZOConstants). 
   * Decompressed length is returned via out_len. 
   *
   * @param in Input byte array to be decompressed
   * @param in_len Input length 
   * @param out  decompressed output byte array. Ensure that out array has sufficient length 
   * @param out_len decompressed data length
   */

  public final static int lzo1x_decompress(final byte[] in, final int in_len, final byte[] out, MInt out_len) {
    int op = 0;
    return lzo1x_decompress(in, in_len, out, op, out_len);
  }

  /** Decompress the data. Error codes would be returned (@see LZOConstants). 
   * Decompressed length is returned via out_len. 
   *
   * @param in Input byte array to be decompressed
   * @param in_len Input length 
   * @param out  decompressed output byte array. Ensure that out array has sufficient length
   * @param op OUT POSITION IN THE ARRAY 
   * @param out_len decompressed data length
   */
  public final static int lzo1x_decompress(final byte[] in, final int in_len, final byte[] out, int op, MInt out_len) {
    try {
      int ip = 0;
      int t;
      int state = c_top_loop;
      int max = 0, diff = 0, min = 0;
      int m_pos = 0;

      out_len.v = 0;

      t = (in[ip] & 0xff);
      if (t > 17) {
        ip++;
        t -= 17;
        if (t < 4) {
          state = c_match_next; //goto match_next;
        }
        else {
          do
            out[op++] = in[ip++];
          while (--t > 0);
          state = c_first_literal_run;//goto first_literal_run;
        }
      }
      top_loop_ori: do {
        boolean if_block = false;
        switch (state) {
          //while (true)  top_loop_ori
          case c_top_loop:
            t = (in[ip++] & 0xff);
            if (t >= 16) {
              state = c_match;
              continue top_loop_ori; //goto match;
            }
            if (t == 0) {
              while (in[ip] == 0) {
                t += 255;
                ip++;
              }
              t += 15 + (in[ip++] & 0xff);
            }

            //s=3; do out[op++] = in[ip++]; while(--s > 0);//* (lzo_uint32 *)(op) = * (const lzo_uint32 *)(ip);op += 4; ip += 4;
            out[op] = in[ip];
            out[op + 1] = in[ip + 1];
            out[op + 2] = in[ip + 2];
            out[op + 3] = in[ip + 3];
            op += 4;
            ip += 4;
            //op++; ip++; //GSSM ?? for the forth byte

            if (--t > 0) {
              if (t >= 4) {
                do {
                  //* (lzo_uint32 *)(op) = * (const lzo_uint32 *)(ip);
                  //op += 4; ip += 4; t -= 4;
                  out[op] = in[ip];
                  out[op + 1] = in[ip + 1];
                  out[op + 2] = in[ip + 2];
                  out[op + 3] = in[ip + 3];
                  op += 4;
                  ip += 4;
                  t -= 4;
                }
                while (t >= 4);
                if (t > 0)
                  do
                    out[op++] = in[ip++];
                  while (--t > 0);
              }
              else
                do
                  out[op++] = in[ip++];
                while (--t > 0);
            }
          case c_first_literal_run: /*first_literal_run: */
            t = (in[ip++] & 0xff);
            if (t >= 16) {
              state = c_match;
              continue top_loop_ori;  //goto match;
            }
            //m_pos = op - (1 + 0x0800);
            //m_pos -= t >> 2;
            //m_pos -= U(in[ip++]) << 2;
            m_pos = op - 0x801 - (t >> 2) - ((in[ip++] & 0xff) << 2);
            diff = Math.abs(m_pos - op);
            if (diff > max)
              max = diff;
            diff = (m_pos - op);
            if (diff < min)
              min = diff;
            //*op++ = *m_pos++; *op++ = *m_pos++; *op++ = *m_pos;
            out[op++] = out[m_pos++];
            out[op++] = out[m_pos++];
            out[op++] = out[m_pos];

            state = c_match_done;
            continue top_loop_ori;//goto match_done;
          case c_match:
            //do {
            //match:
            if (t >= 64) {
              m_pos = op - 1;
              m_pos -= (t >> 2) & 7;
              m_pos -= (in[ip++] & 0xff) << 3;
              diff = Math.abs(m_pos - op);
              if (diff > max)
                max = diff;
              diff = (m_pos - op);
              if (diff < min)
                min = diff;
              t = (t >> 5) - 1;
              state = c_copy_match;
              continue top_loop_ori;//goto copy_match;

            }
            else if (t >= 32) {
              t &= 31;
              if (t == 0) {
                while (in[ip] == 0) {
                  t += 255;
                  ip++;
                }
                t += 31 + (in[ip++] & 0xff);
              }
              m_pos = op - 1;
              m_pos -= (((in[ip] & 0xff) + ((in[ip + 1] & 0xff) << 8)) >> 2);//m_pos -= (* (const unsigned short *) ip) >> 2;
              diff = Math.abs(m_pos - op);
              if (diff > max)
                max = diff;
              diff = (m_pos - op);
              if (diff < min)
                min = diff;

              ip += 2;
            }
            else if (t >= 16) {
              m_pos = op;
              m_pos -= (t & 8) << 11;
              diff = Math.abs(m_pos - op);
              if (diff > max)
                max = diff;
              diff = (m_pos - op);
              if (diff < min)
                min = diff;

              t &= 7;
              if (t == 0) {
                while (in[ip] == 0) {
                  t += 255;
                  ip++;
                }
                t += 7 + (in[ip++] & 0xff);
              }
              m_pos -= (((in[ip] & 0xff) + ((in[ip + 1] & 0xff) << 8)) >> 2);//m_pos -= (* (const unsigned short *) ip) >> 2;
              diff = Math.abs(m_pos - op);
              if (diff > max)
                max = diff;
              diff = (m_pos - op);
              if (diff < min)
                min = diff;
              ip += 2;
              if (m_pos == op) {
                break top_loop_ori;//goto eof_found;
              }
              m_pos -= 0x4000;
            }
            else {
              m_pos = op - 1;
              m_pos -= t >> 2;
              m_pos -= (in[ip++] & 0xff) << 2;
              diff = Math.abs(m_pos - op);
              if (diff > max)
                max = diff;
              diff = (m_pos - op);
              if (diff < min)
                min = diff;

              out[op++] = out[m_pos++];
              out[op++] = out[m_pos];//*op++ = *m_pos++; *op++ = *m_pos;
              state = c_match_done;
              continue top_loop_ori;//goto match_done;
            }
            if (t >= 2 * 4 - (3 - 1) && (op - m_pos) >= 4) {
              if_block = true;
              //* (lzo_uint32 *)(op) = * (const lzo_uint32 *)(m_pos);
              out[op] = out[m_pos];
              out[op + 1] = out[m_pos + 1];
              out[op + 2] = out[m_pos + 2];
              out[op + 3] = out[m_pos + 3];
              op += 4;
              m_pos += 4;
              t -= 2;
              do {
                /// * (lzo_uint32 *)(op) = * (const lzo_uint32 *)(m_pos);
                out[op] = out[m_pos];
                out[op + 1] = out[m_pos + 1];
                out[op + 2] = out[m_pos + 2];
                out[op + 3] = out[m_pos + 3];
                op += 4;
                m_pos += 4;
                t -= 4;
              }
              while (t >= 4);
              if (t > 0)
                do
                  out[op++] = out[m_pos++];
                while (--t > 0);
            }// else 
          case c_copy_match:
            if (!if_block) {
              //*op++ = *m_pos++; *op++ = *m_pos++;
              out[op++] = out[m_pos++];
              out[op++] = out[m_pos++];
              //do *op++ = *m_pos++; while (--t > 0);
              do
                out[op++] = out[m_pos++];
              while (--t > 0);
            }
          case c_match_done:
            t = (in[ip - 2] & 0xff) & 3;
            if (t == 0) {
              state = c_top_loop;
              continue top_loop_ori; //break;
            }
          case c_match_next:
            //*op++ = *ip++;
            out[op++] = in[ip++];
            //if (t > 1) { *op++ = *ip++; if (t > 2) { *op++ = *ip++; } }
            if (t > 1) {
              out[op++] = in[ip++];
              if (t > 2) {
                out[op++] = in[ip++];
              }
            }
            t = (in[ip++] & 0xff);
            state = c_match;
            continue top_loop_ori;
          //}// while (1);
          //// state=c_top_loop; continue top_loop_ori;
        }
      }
      while (true);

      //eof_found:
      //out_len = ((lzo_uint) ((op)-(out)));
      out_len.v = op;
      if (debug)
        System.err.println("\n@@@@@@@@@@@@ diff:" + max + ": min:" + min + "\n");
      //return (ip == ip_end ? 0 : (ip < ip_end ? (-8) : (-4)));
      return (ip == in.length ? 0 : (ip < in.length ? (-8) : (-4)));
    }
    catch (Throwable t) {
      // quiet failure
      return 0;
    }
  }

  /** Decompress the data safely with more error checks. Error codes would be returned (@see LZOConstants). 
   * Decompressed length is returned via out_len. 
   *
   * @param in Input byte array to be decompressed
   * @param in_len Input length 
   * @param out  decompressed output byte array. Ensure that out array has sufficient length 
   * @param out_len decompressed data length
   */

  public final static int lzo1x_decompress_safe(final byte[] in, int in_len, byte[] out, MInt out_len) {
    int op = 0;
    int ip = 0;
    int t = 0;
    int m_pos = 0;
    int state = c_top_loop;

    out_len.v = 0;
    t = (in[ip] & 0xff);
    if (t > 17) {
      ip++;
      t -= 17;
      if (t < 4) {
        state = c_match_next;//goto match_next;
      }
      else if (out.length < t) {
        state = c_output_overrun;//goto output_overrun;
      }
      else if (in.length < (t + 1)) {
        state = c_input_overrun;//goto input_overrun;
      }
      else {
        do
          out[op++] = in[ip++];
        while (--t > 0);
        state = c_first_literal_run;//goto first_literal_run;
      }
    }
    final int out_base = 0;
    final int ip_end = in.length;
    final int op_end = out.length;
    top_loop_ori: while (ip < ip_end) {
      boolean if_block = false;
      switch (state) {
        default:
          break top_loop_ori;
        case c_top_loop:
          t = (in[ip++] & 0xff);
          if (t >= 16) {
            state = c_match;
            continue top_loop_ori;//goto match;
          }
          if (t == 0) {
            if ((ip_end - ip) < 1) {
              state = c_input_overrun;
              continue top_loop_ori;//goto input_overrun;
            }
            while (in[ip] == 0) {
              t += 255;
              ip++;
              if ((ip_end - ip) < 1) {
                state = c_input_overrun;
                continue top_loop_ori;//goto input_overrun;
              }
            }
            t += 15 + (in[ip++] & 0xff);
          }
          if ((op_end - op) < (t + 3)) {
            state = c_output_overrun;
            continue top_loop_ori;// goto output_overrun; 
          }
          if ((ip_end - ip) < (t + 4)) {
            state = c_input_overrun;
            continue top_loop_ori;//goto input_overrun;
          }
          // * (lzo_uint32 *)(op) = * (const lzo_uint32 *)(ip);
          out[op] = in[ip];
          out[op + 1] = in[ip + 1];
          out[op + 2] = in[ip + 2];
          out[op + 3] = in[ip + 3];
          op += 4;
          ip += 4;

          if (--t > 0) {
            if (t >= 4) {
              do {
                //* (lzo_uint32 *)(op) = * (const lzo_uint32 *)(ip);
                out[op] = in[ip];
                out[op + 1] = in[ip + 1];
                out[op + 2] = in[ip + 2];
                out[op + 3] = in[ip + 3];
                op += 4;
                ip += 4;
              }
              while (t >= 4);
              if (t > 0)
                do
                  out[op++] = in[ip++];
                while (--t > 0);
            }
            else {
              do
                out[op++] = in[ip++];
              while (--t > 0);
            }
          }
        case c_first_literal_run:
          t = (in[ip++] & 0xff);
          if (t >= 16) {
            state = c_match;
            continue top_loop_ori;  //goto match;
          }
          //m_pos = op - (1 + 0x0800);
          //m_pos -= t >> 2;
          //m_pos -= *ip++ << 2;
          m_pos = op - 0x801 - (t >> 2) - ((in[ip++] & 0xff) << 2);

          if (m_pos < out_base || m_pos >= op) {
            state = c_lookbehind_overrun;
            continue top_loop_ori;// goto lookbehind_overrun;
          }
          if ((op_end - op) < (3)) {
            state = c_output_overrun;
            continue top_loop_ori;// goto output_overrun; 
          }
          //*op++ = *m_pos++; *op++ = *m_pos++; *op++ = *m_pos;
          out[op++] = out[m_pos++];
          out[op++] = out[m_pos++];
          out[op++] = out[m_pos];

          state = c_match_done;
          continue top_loop_ori;//goto match_done;

        case c_match:
          //do {
          if (t >= 64) {
            m_pos = op - 1;
            m_pos -= (t >> 2) & 7;
            m_pos -= (in[ip++] & 0xff) << 3;
            t = (t >> 5) - 1;
            if (m_pos < out_base || m_pos >= op) {
              state = c_lookbehind_overrun;
              continue top_loop_ori;// goto lookbehind_overrun;
            }
            if ((op_end - op) < (t + 3 - 1)) {
              state = c_output_overrun;
              continue top_loop_ori;// goto output_overrun; 
            }

            state = c_match_done;
            continue top_loop_ori;//goto match_done;
          }
          else if (t >= 32) {
            t &= 31;
            if (t == 0) {
              if ((ip_end - ip) < 1) {
                state = c_input_overrun;
                continue top_loop_ori;//goto input_overrun;
              }
              while (in[ip] == 0) {
                t += 255;
                ip++;
                if ((ip_end - ip) < 1) {
                  state = c_input_overrun;
                  continue top_loop_ori;//goto input_overrun;
                }
              }
              t += 31 + (in[ip++] & 0xff);
            }
            m_pos = op - 1;
            m_pos -= (((in[ip] & 0xff) + ((in[ip + 1] & 0xff) << 8)) >> 2);//m_pos -= (* (const unsigned short *) ip) >> 2;
            ip += 2;
          }
          else if (t >= 16) {
            m_pos = op;
            m_pos -= (t & 8) << 11;
            t &= 7;
            if (t == 0) {
              if ((ip_end - ip) < 1) {
                state = c_input_overrun;
                continue top_loop_ori;//goto input_overrun;
              }
              while (in[ip] == 0) {
                t += 255;
                ip++;
                if ((ip_end - ip) < 1) {
                  state = c_input_overrun;
                  continue top_loop_ori;//goto input_overrun;
                }
              }
              t += 7 + (in[ip++] & 0xff);
            }
            m_pos -= (((in[ip] & 0xff) + ((in[ip + 1] & 0xff) << 8)) >> 2);//m_pos -= (* (const unsigned short *) ip) >> 2;
            ip += 2;
            if (m_pos == op) {
              state = c_eof_found;
              break top_loop_ori;//goto eof_found;
            }
            m_pos -= 0x4000;
          }
          else {
            m_pos = op - 1;
            m_pos -= t >> 2;
            m_pos -= (in[ip++] & 0xff) << 2;

            if (m_pos < out_base || m_pos >= op) {
              state = c_lookbehind_overrun;
              continue top_loop_ori;// goto lookbehind_overrun;
            }
            if ((op_end - op) < 2) {
              state = c_output_overrun;
              continue top_loop_ori;// goto output_overrun; 
            }

            out[op++] = out[m_pos++];
            out[op++] = out[m_pos];//*op++ = *m_pos++; *op++ = *m_pos;
            state = c_match_done;
            continue top_loop_ori;//goto match_done;
          }
          if (m_pos < out_base || m_pos >= op) {
            state = c_lookbehind_overrun;
            continue top_loop_ori;// goto lookbehind_overrun;
          }
          if ((op_end - op) < (t + 3 - 1)) {
            state = c_output_overrun;
            continue top_loop_ori;// goto output_overrun; 
          }

          if (t >= 2 * 4 - (3 - 1) && (op - m_pos) >= 4) {
            if_block = true;
            //* (lzo_uint32 *)(op) = * (const lzo_uint32 *)(m_pos);
            out[op] = out[m_pos];
            out[op + 1] = out[m_pos + 1];
            out[op + 2] = out[m_pos + 2];
            out[op + 3] = out[m_pos + 3];
            op += 4;
            m_pos += 4;
            t -= 4 - (3 - 1);
            do {
              //* (lzo_uint32 *)(op) = * (const lzo_uint32 *)(m_pos);
              out[op] = out[m_pos];
              out[op + 1] = out[m_pos + 1];
              out[op + 2] = out[m_pos + 2];
              out[op + 3] = out[m_pos + 3];
              op += 4;
              m_pos += 4;
              t -= 4;
            }
            while (t >= 4);
            if (t > 0)
              do
                out[op++] = out[m_pos++];
              while (--t > 0);
          }// else {
        case c_copy_match:
          if (!if_block) {
            out[op++] = out[m_pos++];
            out[op++] = out[m_pos++];
            do
              out[op++] = out[m_pos++];
            while (--t > 0);
          }
        case c_match_done:
          t = in[ip - 2] & 3;

          if (t == 0) {
            state = c_top_loop;
            continue top_loop_ori; //break;
          }
        case c_match_next:
          if ((op_end - op) < t) {
            state = c_output_overrun;
            continue top_loop_ori;// goto output_overrun; 
          }
          if ((ip_end - ip) < (t + 1)) {
            state = c_input_overrun;
            continue top_loop_ori;//goto input_overrun;
          }
          out[op++] = in[ip++];
          //if (t > 1) { *op++ = *ip++; if (t > 2) { *op++ = *ip++; } }
          if (t > 1) {
            out[op++] = in[ip++];
            if (t > 2) {
              out[op++] = in[ip++];
            }
          }
          t = (in[ip++] & 0xff);
          //} while ((ip < ip_end) && 1);
          if (ip < ip_end) {
            state = c_match;
            continue top_loop_ori;
          }
          else {
            state = c_top_loop;
            continue top_loop_ori;
          }
      }
    }

    out_len.v = op - out_base;
    switch (state) {
      //case c_eof_found: return (ip == ip_end ? 0 : (ip < ip_end ? (-8) : (-4)));
      case c_eof_found:
        return (ip == ip_end ? LZO_E_OK : (ip < ip_end ? LZO_E_INPUT_NOT_CONSUMED : LZO_E_INPUT_OVERRUN));
      case c_input_overrun:
        return LZO_E_INPUT_OVERRUN;//(-4);
      case c_output_overrun:
        return LZO_E_OUTPUT_OVERRUN;// (-5);
      case c_lookbehind_overrun:
        return LZO_E_LOOKBEHIND_OVERRUN;//(-6);
      default:
        return LZO_E_EOF_NOT_FOUND;
    }
  }
  
  
  
  
  

}