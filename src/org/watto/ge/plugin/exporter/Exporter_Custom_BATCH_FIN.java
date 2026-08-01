/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.exporter;

import java.util.Arrays;

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Exporter_Custom_BATCH_FIN extends ExporterPlugin {

  static Exporter_Custom_BATCH_FIN instance = new Exporter_Custom_BATCH_FIN();

  byte[] buffer = null;
  int bufferLength = 0;
  int bufferPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_BATCH_FIN getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_BATCH_FIN() {
    setName("BATCH Encrypted File with FIN compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return bufferPos < bufferLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    buffer = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      int compLength = (int) source.getLength();
      int decompLength = (int) source.getDecompressedLength();

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) compLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      FileManipulator readSource = new FileManipulator(source.getSource(), false, bufferSize);
      readSource.seek(source.getOffset());

      // read in the source, and do the decryption
      int CL = 0;
      byte[] readBuffer = new byte[compLength];
      for (int i = 0; i < compLength; i++) {
        int currentByte = readSource.readByte();
        int originalByte = currentByte;

        currentByte ^= 0x02;
        currentByte -= CL;
        CL = originalByte;

        readBuffer[i] = (byte) currentByte;
      }

      readSource.close();

      // now do the decompression
      byte[] decompBuffer = new byte[decompLength];

      unfin(readBuffer, compLength, decompBuffer, decompLength);

      buffer = decompBuffer;
      bufferLength = decompLength;
      bufferPos = 0;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // NOT IMPLEMENTED
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int currentByte = ByteConverter.unsign(buffer[bufferPos]);
      bufferPos++;
      return currentByte;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static int FIN_INDEX(int p1, int p2) {
    return ((p1 & 0xFF) << 7) ^ (p2 & 0xFF);
  }

  /**
  **********************************************************************************************
  Finish submission to the Dr Dobbs contest written by Jussi Puttonen, Timo Raita and Jukka Teuhola.
  **********************************************************************************************
  **/
  int unfin(byte[] in, int insz, byte[] out, int outsz) {

    int[] pcTable = new int[32768];
    int ci, co; // characters (in and out)
    int p1 = 0, p2 = 0; // previous 2 characters
    int ctr = 8; // number of characters processed for this mask
    int mask = 0; // mask to mark successful predictions

    int i = 0;
    int o = 0;

    Arrays.fill(pcTable, 32);// space (ASCII 32) is the most used char

    for (;;) {
      if (i >= insz)
        break;
      ci = ByteConverter.unsign(in[i++]);
      // get mask (for 8 characters)
      mask = ci;

      // for each bit in the mask
      for (ctr = 0; ctr < 8; ctr++) {
        if ((mask & (1 << ctr)) != 0) {
          // predicted character
          co = pcTable[FIN_INDEX(p1, p2)];
        }
        else {
          // not predicted character
          if (i >= insz)
            break;
          co = ByteConverter.unsign(in[i++]);
          pcTable[FIN_INDEX(p1, p2)] = co;
        }
        if (o >= outsz) {
          break; // overflow
        }
        out[o++] = (byte) co;
        p1 = p2;
        p2 = co;
      }

      if (o >= outsz) {
        break; // overflow
      }
    }
    return o;
  }

}