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

package org.watto.array;

/***********************************************************************************************
Utilities for resizing primitive <code>Array</code>s
***********************************************************************************************/
public class ArrayResizer {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ArrayResizer(){}


  /***********************************************************************************************
  Resizes a <code>boolean</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static boolean[] resize(boolean[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    boolean[] dest = new boolean[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Boolean</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Boolean[] resize(Boolean[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Boolean[] dest = new Boolean[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>byte</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static byte[] resize(byte[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    byte[] dest = new byte[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Byte</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Byte[] resize(Byte[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Byte[] dest = new Byte[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
   Resizes a <code>char</code> array
   @param source the source array
   @param newSize the size of the output array
   @return the resized array
  ***********************************************************************************************/
  public static char[] resize(char[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    char[] dest = new char[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Character</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Character[] resize(Character[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Character[] dest = new Character[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>double</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static double[] resize(double[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    double[] dest = new double[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Double</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Double[] resize(Double[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Double[] dest = new Double[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>float</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static float[] resize(float[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    float[] dest = new float[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Float</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Float[] resize(Float[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Float[] dest = new Float[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>int</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static int[] resize(int[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    int[] dest = new int[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Integer</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Integer[] resize(Integer[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Integer[] dest = new Integer[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>long</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static long[] resize(long[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    long[] dest = new long[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Long</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Long[] resize(Long[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Long[] dest = new Long[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Object</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Object[] resize(Object[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Object[] dest = new Object[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>short</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static short[] resize(short[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    short[] dest = new short[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>Short</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static Short[] resize(Short[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Short[] dest = new Short[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }


  /***********************************************************************************************
  Resizes a <code>String</code> array
  @param source the source array
  @param newSize the size of the output array
  @return the resized array
  ***********************************************************************************************/
  public static String[] resize(String[] source,int newSize){
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    String[] dest = new String[newSize];
    System.arraycopy(source,0,dest,0,copySize);

    return dest;
  }

}