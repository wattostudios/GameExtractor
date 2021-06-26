/* Constants.java -- various constants

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
 * Various constants.
 *
 * @author  Markus F.X.J. Oberhumer <markus.oberhumer@jk.uni-linz.ac.at>
 ***********************************************************************/

public interface Constants
{
    int LZO_E_OK                  =  0;
    int LZO_E_ERROR               = -1;
    int LZO_E_OUT_OF_MEMORY       = -2;
    int LZO_E_NOT_COMPRESSIBLE    = -3;
    int LZO_E_INPUT_OVERRUN       = -4;
    int LZO_E_OUTPUT_OVERRUN      = -5;
    int LZO_E_LOOKBEHIND_OVERRUN  = -6;
    int LZO_E_EOF_NOT_FOUND       = -7;
    int LZO_E_INPUT_NOT_CONSUMED  = -8;
}


// vi:ts=4:et

