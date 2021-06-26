/* Util.java -- utility functions

   This file is part of the LZO real-time data compression library.

   Copyright (C) 1999 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1998 Markus Franz Xaver Johannes Oberhumer

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
import java.io.*;


/***********************************************************************
 * Utility functions.
 *
 * @author  Markus F.X.J. Oberhumer <markus.oberhumer@jk.uni-linz.ac.at>
 ***********************************************************************/

public class Util
    implements /* imports */ Constants
{
    private Util() { }


//
// byte
//

    public static int memcmp(byte a[], int a_off, byte b[], int b_off, int len)
    {
        for ( ; len-- > 0; a_off++, b_off++)
            if (a[a_off] != b[b_off])
                return (a[a_off] & 0xff) - (b[b_off] & 0xff);
        return 0;
    }

    public static int memcmp(byte a[], byte b[], int len) {
        return memcmp(a,0,b,0,len);
    }


//
// IO
//

    public static int xread(InputStream f, byte buf[], int off, int len, boolean allow_eof)
        throws IOException
    {
        int l = 0;
        int bytes_to_read = len;

        while (bytes_to_read > 0) {
            int k = f.read(buf,off+l,bytes_to_read);
            if (k == -1)
                break;
            if (k < 0)
                throw new IOException();
            l += k;
            bytes_to_read -= k;
        }
        if (l != len && !allow_eof)
            throw new EOFException("read error - premature end of file");
        return l;
    }

    public static int xread(InputStream f, byte buf[], int off, int len)
        throws IOException
    {
        return xread(f,buf,off,len,true);
    }

    public static int xwrite(OutputStream f, byte buf[], int off, int len)
        throws IOException
    {
        if (f != null)
            f.write(buf,off,len);
        return len;
    }

    public static int xread32(InputStream f)
        throws IOException
    {
        byte b[] = new byte[4];
        int v;
        xread(f,b,0,4,false);
        v  = (b[3] & 0xff) <<  0;
        v |= (b[2] & 0xff) <<  8;
        v |= (b[1] & 0xff) << 16;
        v |= (b[0] & 0xff) << 24;
        return v;
    }

    public static void xwrite32(OutputStream f, int v)
        throws IOException
    {
        byte b[] = new byte[4];
        b[3] = (byte) (v >>>  0);
        b[2] = (byte) (v >>>  8);
        b[1] = (byte) (v >>> 16);
        b[0] = (byte) (v >>> 24);
        xwrite(f,b,0,4);
    }

    public static int xgetc(InputStream f)
        throws IOException
    {
        byte b[] = new byte[1];
        xread(f,b,0,1,false);
        return b[0] & 0xff;
    }

    public static void xputc(OutputStream f, int v)
        throws IOException
    {
        byte b[] = new byte[1];
        b[0] = (byte) (v & 0xff);
        xwrite(f,b,0,1);
    }
}


// vi:ts=4:et

