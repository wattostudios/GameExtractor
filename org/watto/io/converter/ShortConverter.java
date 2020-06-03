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
Converts primitives and primitive arrays into a short
***********************************************************************************************/
public class ShortConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ShortConverter(){

  }


  /***********************************************************************************************
  Changes the format of a <code>boolean</code> array between Little Endian and Big Endian ordering
  @param in the <code>boolean</code> array to be changed
  @return the changed <code>boolean</code> array
  ***********************************************************************************************/
  public static short changeFormat(short in){
    byte[] bytes = ByteArrayConverter.convertLittle(in);
    bytes = ByteArrayConverter.changeFormat(bytes);
    return convertLittle(bytes);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>short</code>, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>short</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>short</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>short</code>, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>short</code>, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>short</code>, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>short</code>, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>short</code>, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>short</code>, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>short</code>, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>short</code>, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>short</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>short</code>, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(boolean in){
    if (in) {
      return (short)1;
    }
    return (short)0;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>short</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>short</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(byte in){
    if (in < 0) {
      return (short)(256 + in);
    }
    return (short)in;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>short</code>, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(byte[] in){
    java.nio.ByteBuffer bBuffer = java.nio.ByteBuffer.wrap(in);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    ShortBuffer lBuffer = bBuffer.asShortBuffer();
    return lBuffer.get();
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>short</code>, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>short</code>, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>short</code>, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>short</code>, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>short</code>, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>short</code>, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>short</code>, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>short</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>short</code>
  ***********************************************************************************************/
  public static short convertLittle(String in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a signed <code>byte</code> into an unsigned <code>short</code>
  @param in the <code>byte</code> to be unsigned
  @return the unsigned <code>short</code>
  ***********************************************************************************************/
  public static int unsign(short in){
    if (in < 0) {
      return (int)(65536 + (int)in);
    }
    return (int)in;
  }

}