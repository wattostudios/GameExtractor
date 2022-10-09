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

package org.watto.ge.plugin.exporter;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Exporter_LBALZSS1 extends ExporterPlugin {

  static Exporter_LBALZSS1 instance = new Exporter_LBALZSS1();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: QuickBMS
  **********************************************************************************************
  **/
  public static Exporter_LBALZSS1 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LBALZSS1() {
    setName("LBALZSS1 Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (decompPos < decompLength) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    decompBuffer = null;
    decompPos = 0;
    decompLength = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses LBALZSS1 files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      decompPos = 0;
      decompLength = (int) source.getDecompressedLength();

      //decompBuffer = new byte[(int) decompLength];

      decompressFile(fm, (int) source.getLength(), decompLength);

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      byte currentByte = decompBuffer[decompPos];
      decompPos++;
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
  public void decompressFile(FileManipulator fm, int compLength, int decompLength) {
    try {

      //byte[] compBytes = fm.readBytes(compLength);
      decompBuffer = new byte[(int) decompLength];

      //int lbalzss(unsigned char *in, int insz, unsigned char *out, int outsz, int add, int use_neg_slide) {
      int add = 1; // LBALZSS1 = 1, LBALZSS2 = 2
      //boolean use_neg_slide = false;

      int inPos = 0;
      int outPos = 0;

      int i;
      int length;
      int slide;
      int c; // u16
      int cnt; // u8
      int flag; // u8
      int s; //*s // u8

      while ((inPos < compLength) && (outPos < decompLength)) {
        flag = ByteConverter.unsign(fm.readByte());//*in++;
        inPos++;

        for (cnt = 0; cnt < 8; cnt++) {
          if ((flag & 1) == 1) {
            if (inPos >= compLength || outPos >= decompLength) {
              return;
            }
            //*o++ = *in++;
            decompBuffer[outPos] = fm.readByte();
            inPos++;
            outPos++;

          }
          else {
            if ((inPos + 2) > compLength) {
              return;
            }
            //c = in[0] | (in[1] << 8);
            c = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);
            inPos += 2;

            length = (c & 0xf) + add;
            slide = (c ^ -1) >> 4;
            s = outPos + slide;

            /*
            if (use_neg_slide && (slide == -1)) {
              //c = *s;
              c = s;
              if (((outPos - out) & 1) == 1) {
                length++;
              }
              for (i = 0; i <= length; i++) {
                if (outPos >= decompLength) {
                  return;
                }
                //*o++ = c;
                decompBuffer[outPos] = decompBuffer[c];
                outPos++;
              }
            }
            */
            //else {
            for (i = 0; i <= length; i++) {
              if (outPos >= decompLength) {
                return;
              }
              //*o++ = *s++;
              decompBuffer[outPos] = decompBuffer[s++];
              outPos++;
            }
            //}
          }
          flag >>= 1;
        }
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}