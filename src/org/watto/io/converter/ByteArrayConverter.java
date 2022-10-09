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

import java.nio.*;
import org.watto.io.Hex;


/***********************************************************************************************
Converts primitives and primitive arrays into a byte array
***********************************************************************************************/
public class ByteArrayConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ByteArrayConverter(){}


  /***********************************************************************************************
  Changes the format of a <code>byte</code> array between Little Endian and Big Endian ordering
  @param in the <code>byte</code> array to be changed
  @return the changed <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] changeFormat(byte[] in){
    int size = in.length;

    byte[] out = new byte[size];
    for (int i = 0,j = size - 1;i < size;i++,j--) {
      out[i] = in[j];
    }
    return out;
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(double[] in){
    return convertLittle(DoubleArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(int[] in){
    return convertLittle(IntArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>byte</code> array, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>byte</code> array, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(boolean in){
    if (in) {
      return new byte[]{(byte)1};
    }
    return new byte[]{(byte)0};
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(boolean[] in){
    int numBytes = in.length / 8;
    if (in.length % 8 != 0) {
      numBytes++;
    }

    byte[] bytes = new byte[numBytes];

    for (int b = in.length - 1,p = 0;b >= 0;b -= 8,p++) {
      int value = 0;

      for (int i = b,j = 0;i > b - 8 && i >= 0;i--,j++) {
        if (in[i]) {
          value += Math.pow(2,j);
        }
      }

      bytes[p] = (byte)value;

    }

    return bytes;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(byte in){
    return new byte[]{in};
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(char in){
    byte[] bArray = new byte[2];
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(bArray);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    CharBuffer lBuffer = bBuffer.asCharBuffer();
    lBuffer.put(0,in);
    return bArray;
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(char[] in){
    byte[] out = new byte[in.length * 2];

    for (int i = 0,j = 0;i < in.length;i++,j += 2) {
      byte[] charBytes = convertLittle(in[i]);
      System.arraycopy(charBytes,0,out,j,2);
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(double in){
    byte[] bArray = new byte[8];
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(bArray);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    DoubleBuffer lBuffer = bBuffer.asDoubleBuffer();
    lBuffer.put(0,in);
    return bArray;
  }


  /***********************************************************************************************
  Converts a <code>double</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>double</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(double[] in){
    byte[] out = new byte[in.length * 8];

    for (int i = 0,j = 0;i < in.length;i++,j += 8) {
      byte[] doubleBytes = convertLittle(in[i]);
      System.arraycopy(doubleBytes,0,out,j,8);
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(float in){
    byte[] bArray = new byte[4];
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(bArray);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    FloatBuffer lBuffer = bBuffer.asFloatBuffer();
    lBuffer.put(0,in);
    return bArray;
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(float[] in){
    byte[] out = new byte[in.length * 4];

    for (int i = 0,j = 0;i < in.length;i++,j += 4) {
      byte[] floatBytes = convertLittle(in[i]);
      System.arraycopy(floatBytes,0,out,j,4);
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(Hex in){
    byte[] bArray = new byte[in.length() / 2];

    String text = in.toString();
    int textLength = text.length();
    if (textLength%2 == 1) {
      // odd length for some reason - fix it by adding "0" to the start of the last value
      text = text.substring(0,textLength-1) + "0" + text.substring(textLength-1);
      textLength = text.length();
    }

    int bPos = 0;
    for (int i = 0;i < textLength;i += 2) {
      byte b = 0;

      char high = text.charAt(i);
      char low = text.charAt(i + 1);

      for (int h = 0;h < hexTable.length;h++) {
        if (hexTable[h] == high) {
          b += (h * 16);
        }
        if (hexTable[h] == low) {
          b += (h);
        }
      }

      bArray[bPos] = b;
      bPos++;
    }

    return bArray;
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(int in){
    byte[] bArray = new byte[4];
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(bArray);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    IntBuffer lBuffer = bBuffer.asIntBuffer();
    lBuffer.put(0,in);
    return bArray;
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(int[] in){
    byte[] out = new byte[in.length * 4];

    for (int i = 0,j = 0;i < in.length;i++,j += 4) {
      byte[] intBytes = convertLittle(in[i]);
      System.arraycopy(intBytes,0,out,j,4);
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(long in){
    byte[] bArray = new byte[8];
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(bArray);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    LongBuffer lBuffer = bBuffer.asLongBuffer();
    lBuffer.put(0,in);
    return bArray;
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(long[] in){
    byte[] out = new byte[in.length * 8];

    for (int i = 0,j = 0;i < in.length;i++,j += 8) {
      byte[] longBytes = convertLittle(in[i]);
      System.arraycopy(longBytes,0,out,j,8);
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(short in){
    byte[] bArray = new byte[2];
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(bArray);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    ShortBuffer lBuffer = bBuffer.asShortBuffer();
    lBuffer.put(0,in);
    return bArray;
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>byte</code> array, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(short[] in){
    byte[] out = new byte[in.length * 2];

    for (int i = 0,j = 0;i < in.length;i++,j += 2) {
      byte[] shortBytes = convertLittle(in[i]);
      System.arraycopy(shortBytes,0,out,j,2);
    }

    return out;
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>byte</code> array, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>byte</code> array
  ***********************************************************************************************/
  public static byte[] convertLittle(String in){
    return in.getBytes();
  }


  /***********************************************************************************************
  Converts a signed <code>byte</code> array into an unsigned <code>short</code> array
  @param in the <code>byte</code> array to be unsigned
  @return the <code>short</code> array of unsigned values
  ***********************************************************************************************/
  public static short[] unsign(byte[] in){
    short[] out = new short[in.length];
    for (int i = 0;i < in.length;i++) {
      out[i] = ByteConverter.unsign(in[i]);
    }
    return out;
  }

}