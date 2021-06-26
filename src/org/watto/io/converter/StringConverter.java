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
Converts primitives and primitive arrays into a String
***********************************************************************************************/
public class StringConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public StringConverter(){

  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>String</code>, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>String</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>String</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>String</code>, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>String</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(double[] in){
    return convertLittle(DoubleArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>String</code>, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>String</code>, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>String</code>, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(int[] in){
    return convertLittle(IntArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>String</code>, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>String</code>, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>String</code>, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>String</code>, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(boolean in){
    if (in) {
      return convertLittle(new byte[]{1});
    }
    return convertLittle(new byte[]{0});
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>String</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>String</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(byte in){
    return convertLittle(new byte[]{in});
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(byte[] in){
    return new String(in);
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>String</code>, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>String</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(double[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>String</code>, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(float[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>String</code>, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>String</code>, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(int[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>String</code>, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(long[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>String</code>, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>String</code>, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>String</code>
  ***********************************************************************************************/
  public static String convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Reverses the order of the <code>char</code>acters in the <code>String</code>
  @param in the original <code>String</code>
  @return the reversed <code>String</code> 
  ***********************************************************************************************/
  public static String reverse(String in){
    char[] inArray = in.toCharArray();

    int inLength = inArray.length;
    for (int i = 0,j = inLength - 1;i < j;i++,j--) {
      char tempFirst = inArray[i];
      inArray[i] = inArray[j];
      inArray[j] = tempFirst;
    }

    return new String(inArray);
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into ASCII format <code>String</code>
  @param in the <code>char</code> array
  @return the ASCII <code>String</code>
  ***********************************************************************************************/
  public static String toAscii(char[] in){
    byte[] bytes = new byte[in.length / 2];

    for (int i = 0;i < in.length;i++) {
      bytes[i] = (byte)in[i];
    }

    return new String(bytes);
  }


  /***********************************************************************************************
  Converts a Unicode <code>String</code> into ASCII format
  @param in the Unicode <code>String</code>
  @return the ASCII <code>String</code>
  ***********************************************************************************************/
  public static String toAscii(String in){
    return toAscii(in.toCharArray());
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a Unicode format <code>String</code>
  @param in the <code>byte</code> array
  @return the Unicode <code>String</code>
  ***********************************************************************************************/
  public static String toUnicode(byte[] in){
    try {
      return new String(in,"UTF-16LE");
    }
    catch (Throwable t) {
      return new String(in);
    }
  }


  /***********************************************************************************************
  Converts an ASCII <code>String</code> into Unicode format
  @param in the ASCII <code>String</code>
  @return the Unicode <code>String</code>
  ***********************************************************************************************/
  public static String toUnicode(String in){
    return toUnicode(in.getBytes());
  }
}