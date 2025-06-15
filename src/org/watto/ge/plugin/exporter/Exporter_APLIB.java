/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

public class Exporter_APLIB extends ExporterPlugin {

  static Exporter_APLIB instance = new Exporter_APLIB();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://ibsensoftware.com/products_aPLib.html
  **********************************************************************************************
  **/
  public static Exporter_APLIB getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_APLIB() {
    setName("APLIB Compression");
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
    return "This exporter decompresses APLIB files when exporting\n\n" + super.getDescription();
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

      decompBuffer = new byte[(int) decompLength];

      decompressFile(fm, (int) source.getLength());

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
  public void decompressFile(FileManipulator fm, int compLength) {
    try {

      // TODO convert to fm reads

      srclen = compLength;
      dstlen = decompLength;

      int returnValue = aP_depack_safe(fm);

      if (returnValue != 1) {
        ErrorLogger.log("[Exporter_APLIB] Error in decompression");
      }

      // return to the start of the buffer, ready for reading
      decompPos = 0;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  //byte[] source; //const unsigned char *source; 
  int srclen; //unsigned int srclen;
  //byte[] destination; //unsigned char *destination;
  int dstlen; //unsigned int dstlen;
  int tag; //unsigned int tag;
  int bitcount;//unsigned int bitcount;

  boolean returncode = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int aP_getbit_safe(FileManipulator fm) {

    //unsigned int bit;
    int bit;

    /* check if tag is empty */
    if (bitcount-- == 0) {
      if (srclen-- == 0) {
        returncode = false;
        return 0;
      }

      /* load next tag */
      tag = ByteConverter.unsign(fm.readByte());//source++;
      bitcount = 7;
    }

    /* shift bit out of tag */
    bit = (tag >> 7) & 0x01;
    tag <<= 1;

    int result = bit;

    returncode = true;

    return result;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int aP_getgamma_safe(FileManipulator fm) {
    int bit; //unsigned int bit;
    int v = 1;//unsigned int v = 1;

    int result;

    /* input gamma2-encoded bits */
    do {
      //if (!aP_getbit_safe(bit)) {
      bit = aP_getbit_safe(fm);
      if (!returncode) {
        returncode = false;
        return 0;
      }

      if ((v & 0x80000000) == 0x80000000) {
        returncode = false;
        return 0;
      }

      v = (v << 1) + bit;

      //if (!aP_getbit_safe(bit)) {
      bit = aP_getbit_safe(fm);
      if (!returncode) {
        returncode = false;
        return 0;
      }
    }
    while (bit != 0);

    result = v;

    returncode = true;
    return result;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int aP_depack_safe(FileManipulator fm) {

    int offs, len, R0, LWM, bit; //unsigned int offs, len, R0, LWM, bit;
    int i;

    bitcount = 0;

    R0 = -1; //(unsigned int) -1;
    LWM = 0;
    boolean done = false;

    /* first byte verbatim */
    if (srclen-- == 0 || dstlen-- == 0) {
      return -1; // APLIB_ERROR
    }
    decompBuffer[decompPos] = fm.readByte();
    decompPos++;

    /* main decompression loop */
    while (!done) {
      //if (!aP_getbit_safe(bit)) {
      bit = aP_getbit_safe(fm);
      if (!returncode) {
        return -1; // APLIB_ERROR
      }

      if (bit != 0) {
        //if (!aP_getbit_safe(bit)) {
        bit = aP_getbit_safe(fm);
        if (!returncode) {
          return -1; // APLIB_ERROR
        }

        if (bit != 0) {
          //if (!aP_getbit_safe(bit)) {
          bit = aP_getbit_safe(fm);
          if (!returncode) {
            return -1; // APLIB_ERROR
          }

          if (bit != 0) {
            offs = 0;

            for (i = 4; i > 0; i--) {
              // if (!aP_getbit_safe(bit)) {
              bit = aP_getbit_safe(fm);
              if (!returncode) {
                return -1; // APLIB_ERROR
              }
              offs = (offs << 1) + bit;
            }

            if (offs != 0) {
              //if (offs > (dstlen - ud.dstlen)) {
              if (offs > decompPos) {
                return -1; // APLIB_ERROR
              }

              if (dstlen-- == 0) {
                return -1; // APLIB_ERROR
              }

              decompBuffer[decompPos] = decompBuffer[decompPos - offs]; // *ud.destination = *(ud.destination - offs);
              decompPos++; // ud.destination++;
            }
            else {
              if (dstlen-- == 0) {
                return -1; // APLIB_ERROR
              }

              decompBuffer[decompPos] = 0; // *ud.destination++ = 0x00;
              decompPos++;
            }

            LWM = 0;
          }
          else {
            if (srclen-- == 0) {
              return -1; // APLIB_ERROR
            }

            offs = ByteConverter.unsign(fm.readByte());//*ud.source++;

            len = 2 + (offs & 0x0001);

            offs >>= 1;

            if (offs != 0) {
              //if (offs > (dstlen - ud.dstlen)) {
              if (offs > decompPos) {
                return -1; // APLIB_ERROR
              }

              //if (len > ud.dstlen) {
              if (len > decompLength - decompPos) {
                return -1; // APLIB_ERROR
              }

              dstlen -= len;

              for (; len > 0; len--) {
                decompBuffer[decompPos] = decompBuffer[decompPos - offs]; // *ud.destination = *(ud.destination - offs);
                decompPos++; // ud.destination++;
              }
            }
            else {
              done = true;
            }

            R0 = offs;
            LWM = 1;
          }
        }
        else {
          //if (!aP_getgamma_safe(offs)) {
          offs = aP_getgamma_safe(fm);
          if (!returncode) {
            return -1; // APLIB_ERROR
          }

          if ((LWM == 0) && (offs == 2)) {
            offs = R0;

            //if (!aP_getgamma_safe(len)) {
            len = aP_getgamma_safe(fm);
            if (!returncode) {
              return -1; // APLIB_ERROR
            }

            //if (offs > (dstlen - ud.dstlen)) {
            if (offs > decompPos) {
              return -1; // APLIB_ERROR
            }

            //if (len > ud.dstlen) {
            if (len > decompLength - decompPos) {
              return -1; // APLIB_ERROR
            }

            dstlen -= len;

            for (; len > 0; len--) {
              decompBuffer[decompPos] = decompBuffer[decompPos - offs]; // *ud.destination = *(ud.destination - offs);
              decompPos++; // ud.destination++;
            }
          }
          else {
            if (LWM == 0) {
              offs -= 3;
            }
            else {
              offs -= 2;
            }

            if (offs > 0x00fffffe) {
              return -1; // APLIB_ERROR
            }

            if (srclen-- == 0) {
              return -1; // APLIB_ERROR
            }

            offs <<= 8;
            //offs += *ud.source++;
            offs += ByteConverter.unsign(fm.readByte());//*ud.source++;

            //if (!aP_getgamma_safe(len)) {
            len = aP_getgamma_safe(fm);
            if (!returncode) {
              return -1; // APLIB_ERROR
            }

            if (offs >= 32000) {
              len++;
            }
            if (offs >= 1280) {
              len++;
            }
            if (offs < 128) {
              len += 2;
            }

            //if (offs > (dstlen - ud.dstlen)) {
            if (offs > decompPos) {
              return -1; // APLIB_ERROR
            }

            //if (len > ud.dstlen) {
            if (len > decompLength - decompPos) {
              return -1; // APLIB_ERROR
            }

            dstlen -= len;

            for (; len > 0; len--) {
              decompBuffer[decompPos] = decompBuffer[decompPos - offs]; // *ud.destination = *(ud.destination - offs);
              decompPos++; // ud.destination++;
            }

            R0 = offs;
          }

          LWM = 1;
        }
      }
      else {
        if (srclen-- == 0 || dstlen-- == 0) {
          return -1; // APLIB_ERROR
        }
        decompBuffer[decompPos] = fm.readByte(); //*ud.destination++ = *ud.source++;
        decompPos++;

        LWM = 0;
      }
    }

    //return (unsigned int) (ud.destination - (unsigned char *) destination);
    return 1;
  }

}