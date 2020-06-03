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
Converts primitives and primitive arrays into an int
***********************************************************************************************/
public class IntConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public IntConverter(){}


  /***********************************************************************************************
  Changes the format of an <code>int</code> between Little Endian and Big Endian ordering
  @param in the <code>int</code> to be changed
  @return the changed <code>int</code>
  ***********************************************************************************************/
  public static int changeFormat(int in){
    byte[] bytes = ByteArrayConverter.convertLittle(in);
    bytes = ByteArrayConverter.changeFormat(bytes);
    return convertLittle(bytes);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into an <code>int</code>, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into an <code>int</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into an <code>int</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into an <code>int</code>, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into an <code>int</code>, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into an <code>int</code>, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into an <code>int</code>, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into an <code>int</code>, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into an <code>int</code>, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into an <code>int</code>, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into an <code>int</code>, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into an <code>int</code>, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into an <code>int</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into an <code>int</code>, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(boolean in){
    if (in) {
      return 1;
    }
    return 0;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into an <code>int</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into an <code>int</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(byte in){
    if (in < 0) {
      return (int)(256 + in);
    }
    return (int)in;
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into an <code>int</code>, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(byte[] in){
    ByteBuffer bBuffer = ByteBuffer.wrap(in);
    bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    IntBuffer lBuffer = bBuffer.asIntBuffer();
    return lBuffer.get();
  }


  /***********************************************************************************************
  Converts a <code>char</code> into an <code>int</code>, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into an <code>int</code>, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into an <code>int</code>, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into an <code>int</code>, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into an <code>int</code>, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into an <code>int</code>, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into an <code>int</code>, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into an <code>int</code>, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into an <code>int</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>int</code>
  ***********************************************************************************************/
  public static int convertLittle(String in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a signed <code>byte</code> into an unsigned <code>short</code>
  @param in the <code>byte</code> to be unsigned
  @return the unsigned <code>short</code>
  ***********************************************************************************************/
  public static long unsign(int in){
    if (in < 0) {
      return (long)(4294967296L + (long)in);
    }
    return (long)in;
  }
}