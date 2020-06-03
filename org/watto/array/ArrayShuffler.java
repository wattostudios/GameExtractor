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
Utilities for shuffling primitive <code>Array</code>s
***********************************************************************************************/
public class ArrayShuffler {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ArrayShuffler(){}


  /***********************************************************************************************
  Shuffles the contents of a <code>boolean</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static boolean[] shuffle(boolean[] source){
    int numItems = source.length;
    boolean[] shuffled = new boolean[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Boolean</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Boolean[] shuffle(Boolean[] source){
    int numItems = source.length;
    Boolean[] shuffled = new Boolean[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>byte</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static byte[] shuffle(byte[] source){
    int numItems = source.length;
    byte[] shuffled = new byte[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Byte</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Byte[] shuffle(Byte[] source){
    int numItems = source.length;
    Byte[] shuffled = new Byte[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>char</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static char[] shuffle(char[] source){
    int numItems = source.length;
    char[] shuffled = new char[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Character</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Character[] shuffle(Character[] source){
    int numItems = source.length;
    Character[] shuffled = new Character[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>double</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static double[] shuffle(double[] source){
    int numItems = source.length;
    double[] shuffled = new double[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Double</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Double[] shuffle(Double[] source){
    int numItems = source.length;
    Double[] shuffled = new Double[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>float</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static float[] shuffle(float[] source){
    int numItems = source.length;
    float[] shuffled = new float[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Float</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Float[] shuffle(Float[] source){
    int numItems = source.length;
    Float[] shuffled = new Float[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>int</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static int[] shuffle(int[] source){
    int numItems = source.length;
    int[] shuffled = new int[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Integer</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Integer[] shuffle(Integer[] source){
    int numItems = source.length;
    Integer[] shuffled = new Integer[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>long</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static long[] shuffle(long[] source){
    int numItems = source.length;
    long[] shuffled = new long[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Long</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Long[] shuffle(Long[] source){
    int numItems = source.length;
    Long[] shuffled = new Long[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Object</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Object[] shuffle(Object[] source){
    int numItems = source.length;
    Object[] shuffled = new Object[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>short</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static short[] shuffle(short[] source){
    int numItems = source.length;
    short[] shuffled = new short[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>Short</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static Short[] shuffle(Short[] source){
    int numItems = source.length;
    Short[] shuffled = new Short[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }


  /***********************************************************************************************
  Shuffles the contents of a <code>String</code> array
  @param source the source array
  @return the shuffled array
  ***********************************************************************************************/
  public static String[] shuffle(String[] source){
    int numItems = source.length;
    String[] shuffled = new String[numItems];

    int numToShuffle = numItems;
    for (int i = 0;i < numItems;i++) {
      int random = (int)(Math.random() * numToShuffle);
      shuffled[i] = source[random];
      numToShuffle--;
      source[random] = source[numToShuffle]; // move after -- so that we get the last item
    }

    return shuffled;
  }
}