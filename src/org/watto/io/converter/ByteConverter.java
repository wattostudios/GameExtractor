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
Converts primitives and primitive arrays into a byte
***********************************************************************************************/
public class ByteConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ByteConverter(){}


  /***********************************************************************************************
  Changes the format of a <code>boolean</code> array between Little Endian and Big Endian ordering
  @param in the <code>boolean</code> array to be changed
  @return the changed <code>boolean</code> array
  ***********************************************************************************************/
  public static byte changeFormat(byte in){
    boolean[] bits = BooleanArrayConverter.convertLittle(in);
    bits = BooleanArrayConverter.changeFormat(bits);
    return convertLittle(bits);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }



  /***********************************************************************************************
  Converts a <code>double</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>byte</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(boolean in){
    if (in) {
      return (byte)1;
    }
    return (byte)0;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(boolean[] in){
    return ByteArrayConverter.convertLittle(in)[0];
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(char in){
    return (byte)in;
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(double in){
    return (byte)in;
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(float in){
    return (byte)in;
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(Hex in){
    return ByteArrayConverter.convertLittle(in)[0];
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(int in){
    return (byte)in;
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(long in){
    return (byte)in;
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(short in){
    return (byte)in;
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>byte</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>byte</code>
  ***********************************************************************************************/
  public static byte convertLittle(String in){
    return ByteArrayConverter.convertLittle(in)[0];
  }


  /***********************************************************************************************
  Converts a signed <code>byte</code> into an unsigned <code>short</code>
  @param in the <code>byte</code> to be unsigned
  @return the unsigned <code>short</code>
  ***********************************************************************************************/
  public static short unsign(byte in){
    if (in < 0) {
      return (short)(256 + (short)in);
    }
    return (short)in;
  }

}