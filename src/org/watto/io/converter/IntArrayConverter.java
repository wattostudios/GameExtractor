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
Converts primitives and primitive arrays into a double array
***********************************************************************************************/
public class IntArrayConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public IntArrayConverter(){}


  /***********************************************************************************************
  Changes the format of a <code>int</code> array between Little Endian and Big Endian ordering
  @param in the <code>int</code> array to be changed
  @return the changed <code>int</code> array
  ***********************************************************************************************/
  public static int[] changeFormat(int[] in){
    int size = in.length;

    int[] out = new int[size];
    for (int i = 0,j = size - 1;i < size;i++,j--) {
      out[i] = in[j];
    }
    return out;
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(double[] in){
    return convertLittle(DoubleArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>int</code> array, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>int</code> array, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(boolean in){
    if (in) {
      return new int[]{1};
    }
    return new int[]{0};
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(byte in){
    return new int[]{IntConverter.convertLittle(in)};
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(byte[] in){
    int numInt = in.length / 4;

    int[] out;

    int remainder = in.length % 4;
    if (remainder != 0) {
      // uneven length
      out = new int[numInt + 1];
    }
    else {
      //even length
      out = new int[numInt];
    }

    for (int i = 0,j = 0;i < numInt;i++,j += 4) {
      out[i] = IntConverter.convertLittle(new byte[]{in[j],in[j + 1],in[j + 2],in[j + 3]});
    }

    if (remainder == 1) {
      out[numInt] = IntConverter.convertLittle(new byte[]{in[in.length - 1],0,0,0});
    }
    else if (remainder == 2) {
      out[numInt] = IntConverter.convertLittle(new byte[]{in[in.length - 2],in[in.length - 1],0,0});
    }
    else if (remainder == 3) {
      out[numInt] = IntConverter.convertLittle(new byte[]{in[in.length - 3],in[in.length - 2],in[in.length - 1],0});
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(double[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(float[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(long[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>int</code> array, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>int</code> array, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>int</code> array
  ***********************************************************************************************/
  public static int[] convertLittle(String in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a signed <code>int</code> array into an unsigned <code>long</code> array
  @param in the <code>int</code> array to be unsigned
  @return the <code>int</code> array of unsigned values
  ***********************************************************************************************/
  public static long[] unsign(int[] in){
    long[] out = new long[in.length];
    for (int i = 0;i < in.length;i++) {
      out[i] = IntConverter.unsign(in[i]);
    }
    return out;
  }
}