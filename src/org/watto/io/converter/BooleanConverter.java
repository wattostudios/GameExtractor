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
Converts primitives and primitive arrays into a boolean
***********************************************************************************************/
public class BooleanConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public BooleanConverter(){}



  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>boolean</code>, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }




  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(byte in){
    if (in == 0) {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(char in){
    if (in == '0' || in == 'f' || in == 'F' || in == 'N' || in == 'n') {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(double in){
    if (in == 0) {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(float in){
    if (in == 0) {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(Hex in){
    return BooleanArrayConverter.convertLittle(in)[0];
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(int in){
    if (in == 0) {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(long in){
    if (in == 0) {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(short in){
    if (in == 0) {
      return false;
    }
    return true;
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>boolean</code>, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>boolean</code>
  ***********************************************************************************************/
  public static boolean convertLittle(String in){
    if (in.equalsIgnoreCase("true") || in.equalsIgnoreCase("t") || in.equalsIgnoreCase("y") || in.equals("1")) {
      return true;
    }
    else if (in.equalsIgnoreCase("false") || in.equalsIgnoreCase("f") || in.equalsIgnoreCase("n") || in.equals("0")) {
      return false;
    }
    else {
      return BooleanArrayConverter.convertLittle(in)[0];
    }
  }
}