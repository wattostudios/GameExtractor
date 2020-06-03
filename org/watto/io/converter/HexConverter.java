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
Converts primitives and primitive arrays into a Hex
***********************************************************************************************/
public class HexConverter implements Converter {

  //public static final char[] hexTable = new char[]{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public HexConverter(){}


  /***********************************************************************************************
  Changes the format of a <code>boolean</code> array between Little Endian and Big Endian ordering
  @param in the <code>boolean</code> array to be changed
  @return the changed <code>boolean</code> array
  ***********************************************************************************************/
  public static Hex changeFormat(Hex in){
    char[] chars = in.toCharArray();
    int size = chars.length;

    char[] out = new char[size];

    for (int i = 0,j = size - 2;i < size;i += 2,j -= 2) {
      out[i] = chars[j];
      out[i + 1] = chars[j + 1];
    }

    return new Hex(new String(out));
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(double[] in){
    return convertLittle(DoubleArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(int[] in){
    return convertLittle(IntArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>Hex</code>, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>Hex</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(boolean in){
    if (in) {
      return new Hex("01");
    }
    return new Hex("00");
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(byte in){
    int b = in;
    if (in < 0) {
      b = 256 + in;
    }
    int upperVal = b / 16;
    int lowerVal = b % 16;
    return new Hex(hexTable[upperVal] + "" + hexTable[lowerVal]);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(byte[] in){
    String hex = "";
    for (int i = 0;i < in.length;i++) {
      hex += convertLittle(in[i]).toString();
    }
    return new Hex(hex);
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(double[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(float[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(int[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(long[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>Hex</code>, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>Hex</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>Hex</code>
  ***********************************************************************************************/
  public static Hex convertLittle(String in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }

}