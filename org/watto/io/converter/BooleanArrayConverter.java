////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.io.converter;

import org.watto.io.Hex;


/***********************************************************************************************
Converts primitives and primitive arrays into a boolean array
***********************************************************************************************/
public class BooleanArrayConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public BooleanArrayConverter(){}


  /***********************************************************************************************
  Changes the format of a <code>boolean</code> array between Little Endian and Big Endian ordering
  @param in the <code>boolean</code> array to be changed
  @return the changed <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] changeFormat(boolean[] in){
    int size = in.length;

    boolean[] out = new boolean[size];
    for (int i = 0,j = size - 1;i < size;i++,j--) {
      out[i] = in[j];
    }
    return out;
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(double[] in){
    return convertLittle(DoubleArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(int[] in){
    return convertLittle(IntArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>boolean</code> array, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>boolean</code> array, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(boolean in){
    return new boolean[]{in};
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(byte in){
    boolean[] bits = new boolean[8];

    if ((in & 128) == 128) {
      bits[0] = true;
    }
    else {
      bits[0] = false;
    }
    if ((in & 64) == 64) {
      bits[1] = true;
    }
    else {
      bits[1] = false;
    }
    if ((in & 32) == 32) {
      bits[2] = true;
    }
    else {
      bits[2] = false;
    }
    if ((in & 16) == 16) {
      bits[3] = true;
    }
    else {
      bits[3] = false;
    }
    if ((in & 8) == 8) {
      bits[4] = true;
    }
    else {
      bits[4] = false;
    }
    if ((in & 4) == 4) {
      bits[5] = true;
    }
    else {
      bits[5] = false;
    }
    if ((in & 2) == 2) {
      bits[6] = true;
    }
    else {
      bits[6] = false;
    }
    if ((in & 1) == 1) {
      bits[7] = true;
    }
    else {
      bits[7] = false;
    }

    return bits;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(byte[] in){
    boolean[] bits = new boolean[in.length * 8];
    for (int i = 0,j = 0;i < in.length;i++,j += 8) {
      boolean[] byteBits = convertLittle(in[i]);
      System.arraycopy(byteBits,0,bits,j,8);
    }
    return bits;
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(double[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(float[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(int[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(long[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>boolean</code> array, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>boolean</code> array, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>boolean</code> array
  ***********************************************************************************************/
  public static boolean[] convertLittle(String in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }

}