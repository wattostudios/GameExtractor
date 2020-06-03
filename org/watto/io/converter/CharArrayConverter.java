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
Converts primitives and primitive arrays into a char array
***********************************************************************************************/
public class CharArrayConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public CharArrayConverter(){

  }


  /***********************************************************************************************
  Changes the format of a <code>double</code> array between Little Endian and Big Endian ordering
  @param in the <code>double</code> array to be changed
  @return the changed <code>double</code> array
  ***********************************************************************************************/
  public static char[] changeFormat(char[] in){
    int size = in.length;

    char[] out = new char[size];
    for (int i = 0,j = size - 1;i < size;i++,j--) {
      out[i] = in[j];
    }
    return out;
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(double[] in){
    return convertLittle(DoubleArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(int[] in){
    return convertLittle(IntArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>char</code> array, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>char</code> array, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(boolean in){
    if (in) {
      return new char[]{(char)1};
    }
    return new char[]{(char)0};
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(byte in){
    return new char[]{(char)in};
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(byte[] in){
    int numChar = in.length / 2;

    if (in.length % 2 == 1) {
      // uneven length
      char[] out = new char[numChar + 1];

      for (int i = 0,j = 0;i < numChar;i++,j += 2) {
        out[i] = CharConverter.convertLittle(new byte[]{in[j],in[j + 1]});
      }

      out[numChar] = CharConverter.convertLittle(new byte[]{in[in.length - 1],0});

      return out;
    }
    else {
      //even length
      char[] out = new char[numChar];

      for (int i = 0,j = 0;i < numChar;i++,j += 2) {
        out[i] = CharConverter.convertLittle(new byte[]{in[j],in[j + 1]});
      }

      return out;
    }
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(double[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(float[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(int[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(long[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>char</code> array, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>char</code> array, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>char</code> array
  ***********************************************************************************************/
  public static char[] convertLittle(String in){
    char[] chars = new char[in.length()];
    in.getChars(0,chars.length,chars,0);
    return chars;
  }
}