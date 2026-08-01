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

public class Exporter_ANCO extends ExporterPlugin {

  static Exporter_ANCO instance = new Exporter_ANCO();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://github.com/LittleBigBug/QuickBMS/blob/5315ffe664b88dc09ae783ad17d9dfd252b1c927/src/included/unanco.c#L317
  **********************************************************************************************
  **/
  public static Exporter_ANCO getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ANCO() {
    setName("Anco Compression");
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
    return "This exporter decompresses Anco files when exporting\n\n" + super.getDescription();
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

      int compLength = (int) source.getLength();
      byte[] compBytes = fm.readBytes(compLength);

      decompPos = 0;

      decompLength = (int) source.getDecompressedLength();
      decompBuffer = new byte[decompLength];

      anco_unpack(compBytes, compLength, decompBuffer, decompLength);

      decompPos = 0;

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
  int anco_unpack(byte[] in, int insz, byte[] out, int outsz) {
    if (insz < 0x10) {
      return -1;
    }

    int inPos = 0;

    if (in[inPos++] != 'C') {
      return -2;
    }
    if (in[inPos++] != 'M') {
      return -2;
    }
    if (in[inPos++] != 'P') {
      return -2;
    }
    if (in[inPos++] != '0') {
      return -2;
    }

    int algo = in[inPos] | (in[inPos + 1] << 8) | (in[inPos + 2] << 16) | (in[inPos + 3] << 24);
    inPos += 4;
    System.out.println(algo);

    int zsize = ByteConverter.unsign(in[inPos]) | (ByteConverter.unsign(in[inPos + 1]) << 8) | (ByteConverter.unsign(in[inPos + 2]) << 16) | (ByteConverter.unsign(in[inPos + 3]) << 24);
    inPos += 4;

    int size = ByteConverter.unsign(in[inPos]) | (ByteConverter.unsign(in[inPos + 1]) << 8) | (ByteConverter.unsign(in[inPos + 2]) << 16) | (ByteConverter.unsign(in[inPos + 3]) << 24);
    inPos += 4;

    insz -= 0x10;
    if ((zsize < 0) || (zsize > insz)) {
      return -3;
    }
    insz = zsize;

    if ((size < 0) || (size > outsz)) {
      return -4;
    }
    outsz = size;

    int ret;
    switch (algo) {
    case 0:
      ret = anco_unpack0(in, insz, inPos, out, outsz);
      break;
    case 1:
      ret = anco_unpack1(in, insz, inPos, out, outsz, 0);
      break;
    case 2:
      ret = anco_unpack2(in, insz, inPos, out, outsz, 0);
      break;
    case 3:
      ret = anco_unpack3(in, insz, inPos, out, outsz);
      break;
    case 4:
      ret = anco_unpack4(in, insz, inPos, out, outsz);
      break;
    case 5:
      ret = anco_unpack5(in, insz, inPos, out, outsz);
      break;
    default:
      ret = -5;
      break;
    }

    return ret;
  }

  /**
  **********************************************************************************************
  Tested OK
  **********************************************************************************************
  **/
  int anco_unpack0(byte[] in, int insz, int inPos, byte[] out, int outsz) {
    int inl = in.length;
    int ol = out.length;

    int outPos = 0;

    int[] dict = new int[0x10000];

    int b;

    Arrays.fill(dict, 0);

    int d = 0;
    for (;;) {

      if (inPos >= inl) {
        break;
      }
      int flags = ByteConverter.unsign(in[inPos++]);

      for (int x = 0; x < 8; x++) {

        if ((flags & (1 << x)) == (1 << x)) {

          b = dict[d & 0xffff];

        }
        else {

          if (inPos >= inl) {
            break;
          }
          b = ByteConverter.unsign(in[inPos++]);
          dict[d & 0xffff] = b;

        }

        d = (d << 8) | b;

        if (outPos >= ol) {
          return -1;
        }
        out[outPos++] = (byte) b;
      }
    }

    return outPos;
  }

  /**
  **********************************************************************************************
  Tested OK (Direct, and via anco_unpack2)
  **********************************************************************************************
  **/
  int anco_unpack1(byte[] in, int insz, int inPos, byte[] out, int outsz, int skip_outsz2) {
    int inl = in.length;
    int ol = out.length;

    int[] dict = new int[256];
    int ax;
    int outsz2;
    int dictsz;
    int x;
    int b;

    int outPos = 0;

    outsz2 = outsz;
    if (skip_outsz2 == 0) { // redundant field
      if ((inPos + 4) > inl) {
        return -1;
      }
      outsz2 = ByteConverter.unsign(in[inPos + 0]) | (ByteConverter.unsign(in[inPos + 1]) << 8) | (ByteConverter.unsign(in[inPos + 2]) << 16) | (ByteConverter.unsign(in[inPos + 3]) << 24);
      inPos += 4;
    }

    if ((inPos + 4) > inl) {
      return -2;
    }
    dictsz = ByteConverter.unsign(in[inPos + 0]) | (ByteConverter.unsign(in[inPos + 1]) << 8) | (ByteConverter.unsign(in[inPos + 2]) << 16) | (ByteConverter.unsign(in[inPos + 3]) << 24);
    inPos += 4;

    if (dictsz > 256) {
      return -3;
    }

    for (x = 0; x < dictsz; x++) {
      if ((inPos + 4) > inl) {
        return -4;
      }
      dict[x] = ByteConverter.unsign(in[inPos + 0]) | (ByteConverter.unsign(in[inPos + 1]) << 8) | (ByteConverter.unsign(in[inPos + 2]) << 16) | (ByteConverter.unsign(in[inPos + 3]) << 24);
      inPos += 4;
    }

    ax = 0;
    for (;;) {
      if (inPos >= inl) {
        break;
      }
      b = ByteConverter.unsign(in[inPos++]);

      for (x = 0; x < 8; x++) {
        ax = dict[ax];
        //ax = (b & (1 << x)) ? (ax >> 16) : (ax & 0xffff);
        if ((b & (1 << x)) == (1 << x)) {
          ax = (ax >> 16);
        }
        else {
          ax = (ax & 0xffff);
        }
        if (ax >= 256) {
          if (outsz2 > 0) { // useless
            outsz2--;
            if (outPos >= ol) {
              return -5;
            }
            out[outPos++] = (byte) ax;
          }
          ax = 0;
        }
      }

    }

    return outPos;
  }

  /**
  **********************************************************************************************
  Tested OK
  **********************************************************************************************
  **/
  int anco_unpack2(byte[] in, int insz, int inPos, byte[] out, int outsz, int skip_outsz2) {
    int[] dict = new int[256];
    int i;
    int x;
    int b;
    int c;

    outsz = anco_unpack1(in, insz, inPos, out, outsz, skip_outsz2);
    if (outsz < 0) {
      return -1;
    }

    for (x = 0; x < 256; x++) {
      dict[x] = x + 1;
    }

    c = 0;
    for (i = 0; i < outsz; i++) {
      b = ByteConverter.unsign(out[i]);

      if (b != 0) {
        int td;
        int tc;

        td = c;
        x = 0;
        do {
          x++;
          tc = td;
          td = dict[td];
        }
        while (x != b);

        dict[tc] = dict[td];
        dict[td] = c;
        c = td;
      }

      out[i] = (byte) c;
    }

    return i;
  }

  /**
  **********************************************************************************************
  Tested OK
  **********************************************************************************************
  **/
  int anco_unpack3(byte[] in, int insz, int inPos, byte[] out, int outsz) {
    int inl = in.length;
    int ol = out.length;

    int outPos = 0;

    int cmp;
    int b;

    if (inPos >= inl) {
      return -1;
    }
    cmp = ByteConverter.unsign(in[inPos++]);

    for (;;) {
      if (inPos >= inl) {
        break;
      }
      b = ByteConverter.unsign(in[inPos++]);

      if (b == cmp) {

        if (inPos >= inl) {
          break;
        }
        b = ByteConverter.unsign(in[inPos++]);

        if (b != 0) {

          int len = (b >> 2);
          int pos = (b & 3) << 8;

          if (inPos >= inl) {
            break;
          }
          pos += ByteConverter.unsign(in[inPos++]);

          if ((outPos - pos) < 0) {
            return -2;
          }
          if ((outPos + len) > ol) {
            return -3;
          }
          while (len-- != 0) {
            out[outPos] = out[outPos - pos];
            outPos++;
          }

        }
        else {

          if (outPos >= ol) {
            return -4;
          }
          out[outPos++] = (byte) cmp;

        }

      }
      else {

        if (outPos >= ol) {
          return -5;
        }
        out[outPos++] = (byte) b;

      }
    }

    return outPos;
  }

  /**
  **********************************************************************************************
  Untested (no samples)
  **********************************************************************************************
  **/
  int anco_unpack4(byte[] in, int insz, int inPos, byte[] out, int outsz) {
    int inl = in.length;
    int ol = out.length;

    int outPos = 0;

    int len;
    int abool;
    int b;

    for (;;) {

      if (inPos >= inl) {
        break;
      }
      abool = ByteConverter.unsign(in[inPos++]);

      if (inPos >= inl) {
        break;
      }
      b = ByteConverter.unsign(in[inPos++]);

      if (abool != 0) {

        out[outPos++] = (byte) b;

      }
      else {

        if (b != 0) {

          len = b;

        }
        else {

          if ((inPos + 2) > inl) {
            break;
          }
          len = (ByteConverter.unsign(in[inPos + 0]) << 8) | ByteConverter.unsign(in[inPos + 1]);
          inPos += 2;

        }

        if ((outPos + len) > ol) {
          return -1;
        }
        while (len-- != 0) {
          out[outPos++] = 0;
        }

      }
    }

    return outPos;
  }

  /**
  **********************************************************************************************
  Untested (no samples)
  **********************************************************************************************
  **/
  int anco_unpack5(byte[] in, int insz, int inPos, byte[] out, int outsz) {
    int inl = in.length;
    int ol = out.length;

    int outPos = 0;

    int cmp;
    int b;

    if (inPos >= inl) {
      return -1;
    }
    cmp = ByteConverter.unsign(in[inPos++]);

    for (;;) {
      if (inPos >= inl) {
        break;
      }
      b = ByteConverter.unsign(in[inPos++]);

      if (b == cmp) {

        if (inPos >= inl) {
          break;
        }
        b = ByteConverter.unsign(in[inPos++]);

        if (b != 0) {

          if (inPos >= inl) {
            break;
          }

          int len = ByteConverter.unsign(in[inPos++]);

          if (len == 0) {
            if ((inPos + 2) > inl) {
              break;
            }
            len = (ByteConverter.unsign(in[inPos + 0]) << 8) | ByteConverter.unsign(in[inPos + 1]);
            inPos += 2;
          }

          if ((outPos + len) > ol) {
            return -2;
          }
          while (len-- != 0) {
            out[outPos++] = (byte) b;
          }

        }
        else {

          if (outPos >= ol) {
            return -3;
          }
          out[outPos++] = (byte) cmp;

        }

      }
      else {

        if (outPos >= ol) {
          return -4;
        }
        out[outPos++] = (byte) b;

      }
    }

    return outPos;
  }

}
