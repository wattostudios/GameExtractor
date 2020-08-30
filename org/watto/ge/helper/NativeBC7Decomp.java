/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

/**
**********************************************************************************************
BC7 Decoder (JNI Wrapper)
https://github.com/richgel999/bc7enc/blob/master/bc7decomp.cpp
**********************************************************************************************
**/
public class NativeBC7Decomp {

  static {
    JNIHelper.loadLibrary("bc7decomp");
  }

  private native boolean unpack_bc7(byte[] pBlock, int[] pPixels);

  public int[] unpackBC7Block(byte[] pBlock) {
    int[] pPixels = new int[pBlock.length];
    unpack_bc7(pBlock, pPixels);
    return pPixels;
  }

  // TESTING ONLY
  public static void main(String[] args) {
    NativeBC7Decomp app = new NativeBC7Decomp();

    /*
    byte[] input = new byte[] {1,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
    int[] output = new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    
    System.out.println(app.unpack_bc7(input,output));
    
    System.out.println(output[0] + "," + output[1] + "," + output[2] + "," + output[3]);
    */

    byte[] input = new byte[] { 1, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255 };
    int[] output = app.unpackBC7Block(input);

    System.out.println(output[0] + "," + output[1] + "," + output[2] + "," + output[3]);

  }

}