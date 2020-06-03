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
//
//
//
//
//
//
//
// REPLACE WITH DSDECMP JAVA PACKAGE (https://github.com/Barubary/dsdecmp)
//
//
//
//
//
//
//

package org.watto.ge.plugin.exporter;

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.Manipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

public class Exporter_LZ77WII_Old extends ExporterPlugin {

  static Exporter_LZ77WII_Old instance = new Exporter_LZ77WII_Old();

  static FileManipulator readSource;

  static byte[] readBuffer = new byte[0];

  static int readBufferPos = 0;

  static long readLength = 0;

  static int readBufferLevel = 0;

  int WII_NONE_TAG = 0x00;

  int WII_LZ77_TAG = 0x10;

  int WII_LZSS_TAG = 0x11;

  int WII_HUFF_TAG = 0x20;

  int WII_RLE_TAG = 0x30;

  int WII_040_TAG = 0x40;

  /**
  **********************************************************************************************
  Nintendo LZ77WII Compression
  **********************************************************************************************
  **/
  public static Exporter_LZ77WII_Old getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  int unlz77wii_raw10(unsigned char *in, int insz, unsigned char *out, int outsz) {
  **********************************************************************************************
  **/
  int unlz77wii_raw10(Manipulator in, int insz, Manipulator out, int outsz) {
    int i;
    int j;
    int flags;
    int info;
    int num;

    int p;

    int outOffset = (int) out.getOffset();

    Manipulator o = out;
    int outl = (int) out.getOffset() + outsz;
    int inl = (int) in.getOffset() + insz;

    for (;;) {
      if (in.getOffset() >= inl) {
        break;
      }

      flags = ByteConverter.unsign(in.readByte());

      for (i = 0; i < 8; i++) {
        if (o.getOffset() >= outl) {
          break;    // needed
        }
        if ((flags & 0x80) == 0x80) {
          if ((in.getOffset() + 2) > inl) {
            break;
          }
          info = (ByteConverter.unsign(in.readByte()) << 8) | ByteConverter.unsign(in.readByte());

          num = 3 + ((info >> 12) & 0xF);
          p = (int) (o.getOffset() - (info & 0xfff)) - 1;
          if (p < outOffset) {
            return (-1);
          }
          if ((o.getOffset() + num) > outl) {
            return (-1);
          }

          int startWriteOffset = (int) o.getOffset();
          int startReadOffset = p;
          for (j = 0; j < num; j++) {
            // copy bytes from out(p.offset) to out(o.offset)
            o.relativeSeek(startReadOffset);
            byte copyByte = o.readByte();
            o.relativeSeek(startWriteOffset);
            o.writeByte(copyByte);

            startWriteOffset++;
            startReadOffset++;
          }
        }
        else {
          if (in.getOffset() >= inl) {
            break;
          }
          if (o.getOffset() >= outl) {
            return (-1);
          }
          o.writeByte(in.readByte());

        }
        flags <<= 1;
      }
    }

    int decompBlockSize = (int) (o.getOffset() - outOffset);
    return decompBlockSize;
  }

  /**
  **********************************************************************************************
  int unlz77wii_raw11(unsigned char *in, int insz, unsigned char *outdata, int decomp_size) {
  **********************************************************************************************
  **/
  int unlz77wii_raw11(Manipulator in, int insz, Manipulator outdata, int decomp_size) {

    int i, j, disp = 0, len = 0, cdest;
    boolean flag;

    int b1, bt, b2, b3, flags;//unsigned char

    int threshold = 1;

    int inl = (int) in.getOffset() + insz;
    int curr_size = 0;

    while (curr_size < decomp_size) {
      if (in.getOffset() >= inl) {
        break;
      }
      flags = ByteConverter.unsign(in.readByte());

      for (i = 0; i < 8 && curr_size < decomp_size; i++) {
        flag = (flags & (0x80 >> i)) > 0;
        if (flag) {
          if (in.getOffset() >= inl) {
            break;
          }
          b1 = ByteConverter.unsign(in.readByte());

          switch (b1 >> 4) {
            //#region case 0
            case 0: {
              // ab cd ef
              // =>
              // len = abc + 0x11 = bc + 0x11
              // disp = def

              len = b1 << 4;
              if (in.getOffset() >= inl) {
                break;
              }
              bt = ByteConverter.unsign(in.readByte());
              ;
              len |= bt >> 4;
              len += 0x11;

              disp = (bt & 0x0F) << 8;
              if (in.getOffset() >= inl) {
                break;
              }
              b2 = ByteConverter.unsign(in.readByte());
              ;
              disp |= b2;
              break;
            }
            //#endregion

            //#region case 1
            case 1: {
              // ab cd ef gh
              // => 
              // len = bcde + 0x111
              // disp = fgh
              // 10 04 92 3F => disp = 0x23F, len = 0x149 + 0x11 = 0x15A

              if ((in.getOffset() + 3) > inl) {
                break;
              }
              bt = ByteConverter.unsign(in.readByte());
              ;
              b2 = ByteConverter.unsign(in.readByte());
              ;
              b3 = ByteConverter.unsign(in.readByte());
              ;

              len = (b1 & 0xF) << 12; // len = b000
              len |= bt << 4; // len = bcd0
              len |= (b2 >> 4); // len = bcde
              len += 0x111; // len = bcde + 0x111
              disp = (b2 & 0x0F) << 8; // disp = f
              disp |= b3; // disp = fgh
              break;
            }
            //#endregion

            //#region other
            default: {
              // ab cd
              // =>
              // len = a + threshold = a + 1
              // disp = bcd

              len = (b1 >> 4) + threshold;

              disp = (b1 & 0x0F) << 8;
              if (in.getOffset() >= inl) {
                break;
              }
              b2 = ByteConverter.unsign(in.readByte());
              ;
              disp |= b2;
              break;
            }
            //#endregion
          }

          if (disp > curr_size)
            return (-1);

          cdest = curr_size;

          for (j = 0; j < len && curr_size < decomp_size; j++)
            //outdata[curr_size++] = outdata[cdest - disp - 1 + j];
            outdata.relativeSeek(cdest - disp - 1 + j);
          byte copyByte = outdata.readByte();
          outdata.relativeSeek(curr_size++);
          outdata.writeByte(copyByte);

          if (curr_size > decomp_size) {
            //throw new Exception(String.Format("File {0:s} is not a valid LZ77 file; actual output size > output size in header", filein));
            //Console.WriteLine(String.Format("File {0:s} is not a valid LZ77 file; actual output size > output size in header; {1:x} > {2:x}.", filein, curr_size, decomp_size));
            break;
          }
        }
        else {
          if (in.getOffset() >= inl) {
            break;
          }
          //outdata[curr_size++] = ByteConverter.unsign(in.readByte());
          outdata.relativeSeek(curr_size++);
          outdata.writeByte(in.readByte());

          if (curr_size > decomp_size) {
            //throw new Exception(String.Format("File {0:s} is not a valid LZ77 file; actual output size > output size in header", filein));
            //Console.WriteLine(String.Format("File {0:s} is not a valid LZ77 file; actual output size > output size in header; {1:x} > {2:x}", filein, curr_size, decomp_size));
            break;
          }
        }
      }
    }
    return (curr_size);
  }

  /**
  **********************************************************************************************
  int unlz77wii_raw30(unsigned char *in, int insz, unsigned char *outdata, int decomp_size) {
  **********************************************************************************************
  **/
  int unlz77wii_raw30(Manipulator in, int insz, Manipulator outdata, int decomp_size) {

    int i, rl;
    int flag;// unsigned char
    int b; // unsigned char
    boolean compressed;

    int inl = (int) in.getOffset() + insz;
    int curr_size = 0;

    while (true) {
      // get tag
      if (in.getOffset() >= inl) {
        break;
      }
      flag = ByteConverter.unsign(in.readByte());
      compressed = (flag & 0x80) > 0;
      rl = flag & 0x7F;
      if (compressed)
        rl += 3;
      else
        rl += 1;
      //curr_size += rl;
      if (compressed) {
        if (in.getOffset() >= inl) {
          break;
        }
        b = ByteConverter.unsign(in.readByte());
        outdata.relativeSeek(curr_size);
        for (i = 0; i < rl; i++)
          //outdata[curr_size++] = b;
          outdata.writeByte((byte) b);
        curr_size++;
      }
      else
        for (i = 0; i < rl; i++) {
          if (in.getOffset() >= inl) {
            break;
          }
          //outdata[curr_size++] = ByteConverter.unsign(in.readByte());
          outdata.relativeSeek(curr_size++);
          outdata.writeByte(in.readByte());
        }

      if (curr_size > decomp_size) {
        //Console.WriteLine("curr_size > decomp_size; {0:x}>{1:x}", curr_size, decomp_size);
        break;// throw new Exception(String.Format("File {0:s} is not a valid LZSS file; actual output size > output size in header", filein));
      }
      if (curr_size == decomp_size)
        break;
    }
    return (curr_size);
  }

  /**
  **********************************************************************************************
  int unlz77wii_raw20(unsigned char *in, int zsize, unsigned char *out, int size) {
  **********************************************************************************************
  **/
  int unlz77wii_raw20(Manipulator in, int zsize, Manipulator out, int size) {
    nintendo_ds_set_inout(in, zsize, out, size);
    //TODO HUF_Decode("");
    size = nintendo_ds_set_inout(null, 0, null, 0);
    return size;
  }

  Manipulator nintento_ds_in = null;

  int nintento_ds_insz = 0;

  Manipulator nintento_ds_out = null;

  int nintento_ds_outsz = 0;

  /**
  **********************************************************************************************
   int nintendo_ds_set_inout(unsigned char *in, int insz, unsigned char *out, int outsz) {
  **********************************************************************************************
  **/
  int nintendo_ds_set_inout(Manipulator in, int insz, Manipulator out, int outsz) {
    int ret = nintento_ds_outsz;

    byte[] inBytes = in.readBytes(insz);

    nintento_ds_in = new FileManipulator(new ByteBuffer(inBytes));

    nintento_ds_insz = insz;
    nintento_ds_out = out;
    nintento_ds_outsz = outsz;
    return ret;
  }

  /**
  **********************************************************************************************
  int unlz77wii_raw00(unsigned char *in, int insz, unsigned char *out, int outsz)
  **********************************************************************************************
  **/
  int unlz77wii_raw00(Manipulator in, int insz, Manipulator out, int outsz) {
    if (insz > outsz) {
      return (-1);
    }

    //memcpy(out, in, outsz);
    out.writeBytes(in.readBytes(outsz));

    return (outsz);
  }

  /**
  **********************************************************************************************
  int unlz77wii(unsigned char *in, int insz, u8 **ret_out, int *full_outsz) {
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  int unlz77wii(Manipulator in, int insz, Manipulator out, int full_outsz) {
    int outsz,
        tag;

    if (insz < 4) {
      return (-1);
    }

    // We don't check for the 4-byte header
    /*
    if(!memcmp(in, "LZ77", 4) | !memcmp(in, "CMPR", 4)) {
        in   += 4;
        insz -= 4;
    }
    */

    //tag = (in[0] >> 4) & 0xf;
    tag = ByteConverter.unsign(in.readByte());

    outsz = ByteConverter.unsign(in.readByte()) | (ByteConverter.unsign(in.readByte()) << 8) | (ByteConverter.unsign(in.readByte()) << 16);

    insz -= 4;
    //out = *ret_out;
    //myalloc(&out, outsz, full_outsz);
    //*ret_out = out;

    /* TODO
    switch (tag >> 4) {
      case (WII_LZ77_TAG >> 4): {
        if (tag == WII_LZ77_TAG)
          outsz = unlz77wii_raw10(in, insz, out, outsz);
        else if (tag == WII_LZSS_TAG)
          outsz = unlz77wii_raw11(in, insz, out, outsz);
        else
          outsz = -1;
        break;
      }
      case (WII_RLE_TAG >> 4):
        outsz = unlz77wii_raw30(in, insz, out, outsz);
        break;
      case (WII_HUFF_TAG >> 4):
        outsz = unlz77wii_raw20(in, insz, out, outsz);
        break;
      case (WII_NONE_TAG >> 4):
        outsz = unlz77wii_raw00(in, insz, out, outsz);
        break;
      case (WII_040_TAG >> 4):
        outsz = ntcompress_40(in, insz, out);
        break;
      default:
        outsz = -1;
        // modify COMP_NTCOMPRESS too
    }*/
    return (outsz);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZ77WII_Old() {
    setName("Nintendo LZ77 Wii Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength <= 0) {
        return false;
      }

      if (readBufferPos >= readBufferLevel) {
        //TODO fillBuffer();
      }
      return true;

    }
    catch (Throwable t) {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readSource.close();
      readSource = null;
      readBuffer = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses RefPack-compressed files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      readSource = fmIn;

      readBufferPos = 0;
      readBufferLevel = 0;

      readLength = decompLengthIn;

      int readBufferSize = 200000;
      if (readLength < readBufferSize) {
        readBufferSize = (int) readLength;
      }

      readBuffer = new byte[readBufferSize];

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      readBufferPos = 0;
      readBufferLevel = 0;
      readLength = source.getDecompressedLength();

      int readBufferSize = 200000;
      if (readLength < readBufferSize) {
        readBufferSize = (int) readLength;
      }

      readBuffer = new byte[readBufferSize];

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int readByte = readBuffer[readBufferPos];
      readBufferPos++;
      readLength--;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}