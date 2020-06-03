/* Int.java -- wraps the primitive type int

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

/***********************************************************************
 * The Int class wraps a value of the primititve type <code>int</code>
 * in an object. As opposed to <code>java.lang.Integer</code>
 * the value can be modified after the object has been created.
 *
 * @author  Markus F.X.J. Oberhumer <markus.oberhumer@jk.uni-linz.ac.at>
 * @see     java.lang.Integer
 ***********************************************************************/

public class Int
    extends java.lang.Number
// implements java.io.Serializable      // @JDK@ 1.1
{

  /** serialVersionUID */
  private static final long serialVersionUID = -446439695317461933L;
  private int value = 0;

  public Int() {
    this(0);
  }

  public Int(int v) {
    value = v;
  }

  public void add(int v) {
    value += v;
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return value;
  }

  public void setValue(int v) {
    value = v;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }

  public void sub(int v) {
    value -= v;
  }
}

// vi:ts=4:et
