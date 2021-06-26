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
public class DoubleArrayConverter implements Converter {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public DoubleArrayConverter() {
  }

  /***********************************************************************************************
  Changes the format of a <code>double</code> array between Little Endian and Big Endian ordering
  @param in the <code>double</code> array to be changed
  @return the changed <code>double</code> array
  ***********************************************************************************************/
  public static double[] changeFormat(double[] in){
    int size = in.length;

    double[] out = new double[size];
    for (int i=0,j=size-1;i<size;i++,j--){
      out[i] = in[j];
      }
    return out;
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(boolean in){
    // no changeFormat() for a boolean
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(boolean[] in){
    return convertLittle(BooleanArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(byte in){
    return convertLittle(ByteConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>char</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(char in){
    return convertLittle(CharConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(char[] in){
    return convertLittle(CharArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>double</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(double in){
    return convertLittle(DoubleConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(byte[] in){
    return convertLittle(ByteArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>float</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(float in){
    return convertLittle(FloatConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(float[] in){
    return convertLittle(FloatArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(Hex in){
    return convertLittle(HexConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>int</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(int in){
    return convertLittle(IntConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>int</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(int[] in){
    return convertLittle(IntArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>long</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(long in){
    return convertLittle(LongConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(long[] in){
    return convertLittle(LongArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>short</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(short in){
    return convertLittle(ShortConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>double</code> array, in Big Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(short[] in){
    return convertLittle(ShortArrayConverter.changeFormat(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>double</code> array, in Big Endian order
  @param in the <code>String</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertBig(String in){
    // no changeFormat() for a String
    return convertLittle(in);
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>boolean</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(boolean in){
    if (in){
      return new double[]{(double)1};
      }
    return new double[]{(double)0};
  }


  /***********************************************************************************************
  Converts a <code>boolean</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>boolean</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(boolean[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>byte</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>byte</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(byte in){
    return new double[]{DoubleConverter.convertLittle(in)};
  }

  /***********************************************************************************************
  Converts a <code>byte</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>byte</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(byte[] in){
    int numDouble = in.length/8;

    double[] out;

    int remainder = in.length%8;
    if (remainder != 0){
      // uneven length
      out = new double[numDouble+1];
      }
    else {
      //even length
      out = new double[numDouble];
      }


    for (int i=0,j=0;i<numDouble;i++,j+=8){
      out[i] = DoubleConverter.convertLittle(new byte[]{in[j],in[j+1],in[j+2],in[j+3],in[j+4],in[j+5],in[j+6],in[j+7]});
      }

    if (remainder == 1){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-1],0,0,0,0,0,0,0});
      }
    else if (remainder == 2){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-2],in[in.length-1],0,0,0,0,0,0});
      }
    else if (remainder == 3){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-3],in[in.length-2],in[in.length-1],0,0,0,0,0});
      }
    else if (remainder == 4){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-4],in[in.length-3],in[in.length-2],in[in.length-1],0,0,0,0});
      }
    else if (remainder == 5){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-5],in[in.length-4],in[in.length-3],in[in.length-2],in[in.length-1],0,0,0});
      }
    else if (remainder == 6){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-6],in[in.length-5],in[in.length-4],in[in.length-3],in[in.length-2],in[in.length-1],0,0});
      }
    else if (remainder == 7){
      out[numDouble] = DoubleConverter.convertLittle(new byte[]{in[in.length-7],in[in.length-6],in[in.length-5],in[in.length-4],in[in.length-3],in[in.length-2],in[in.length-1],0});
      }

    return out;
  }
  

  /***********************************************************************************************
  Converts a <code>char</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>char</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(char in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>char</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>char</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(char[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>double</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>double</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(double in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }





  /***********************************************************************************************
  Converts a <code>float</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>float</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(float in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>float</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>float</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(float[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>Hex</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>Hex</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(Hex in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>int</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(int in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts an <code>int</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>int</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(int[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>long</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(long in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>long</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>long</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(long[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>short</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(short in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>short</code> array into a <code>double</code> array, in Little Endian order
  @param in the <code>short</code> array to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(short[] in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }


  /***********************************************************************************************
  Converts a <code>String</code> into a <code>double</code> array, in Little Endian order
  @param in the <code>String</code> to be changed
  @return the <code>double</code> array
  ***********************************************************************************************/
  public static double[] convertLittle(String in){
    return convertLittle(ByteArrayConverter.convertLittle(in));
  }
}