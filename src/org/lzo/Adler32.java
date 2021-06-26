/* Adler32.java -- compute an adler32 checksum

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
 */


package org.lzo;


/***********************************************************************
 * A class that can be used to compute the Adler-32 checksum of a data
 * stream. An Adler-32 checksum is almost as reliable as a CRC-32 but
 * can be computed faster.
 * <p>
 * Note: as of JDK 1.1 you should use <code>java.util.zip.Adler32</code>
 *       instead - it is fully equivalent and much faster than this
 *       pure Java implementation.
 *
 * @author  Markus F.X.J. Oberhumer <markus.oberhumer@jk.uni-linz.ac.at>
 * @see     java.util.zip.Adler32
 * @see     java.util.zip.Checksum
 ***********************************************************************/

public class Adler32
    // implements java.util.zip.Checksum    // @JDK@ 1.1
{
    private static final int BASE = 65521;
    private static final int NMAX = 2775;

    private int s1 = 1;
    private int s2 = 0;

    public void reset() {
        s1 = 1;
        s2 = 0;
    }

    public long getValue() {
        return ((long) s2 << 16) | s1;
    }

    public void update(int b) {
        s1 += b & 0xff;
        s2 += s1;
        s1 %= BASE;
        s2 %= BASE;
    }

    public void update(byte b[], int off, int len) {
        while (len > 0) {
            int k = len < NMAX ? len : NMAX;
            len -= k;
            while (k >= 16) {
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                s1 += b[off++] & 0xff; s2 += s1;
                k -= 16;
            }
            if (k != 0) do {
                s1 += b[off++] & 0xff; s2 += s1;
            } while (--k > 0);
            s1 %= BASE;
            s2 %= BASE;
        }
    }

    public void update(byte b[]) {
        update(b,0,b.length);
    }
}


// vi:ts=4:et

