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

package org.watto.ge.plugin.scanner;

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ScannerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Scanner_GIF_GIF extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_GIF_GIF() {
    super("gif", "GIF Image");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 71) {
        return null;
      }

      if (fm.readByte() != 73 || fm.readByte() != 70 || fm.readByte() != 56) {
        return null;
      }

      long offset = fm.getOffset() - 4;

      fm.skip(2); // 7a or 9a

      // 2 - width
      int width = fm.readShort();
      FieldValidator.checkLength(width); // check negatives
      // 2 - height
      int height = fm.readShort();
      FieldValidator.checkLength(height); // check negatives

      int colorTableSize = 0;
      // Image Details
      boolean[] bitFlags = fm.readBits();
      if (bitFlags[0]) {
        int bitsPerPixel = 1;
        if (bitFlags[7]) {
          bitsPerPixel += 1;
        }
        if (bitFlags[6]) {
          bitsPerPixel += 2;
        }
        if (bitFlags[5]) {
          bitsPerPixel += 4;
        }

        colorTableSize = (int) (Math.pow(2, bitsPerPixel) * 3);
      }

      // 1 - BGColor Number
      fm.skip(1);

      // 1 - Padding (null)
      int padding = fm.readByte();
      if (padding == 0) {

        FieldValidator.checkLength(colorTableSize, fm.getRemainingLength());
        fm.skip(colorTableSize);

        int tByte = fm.readByte();
        while (tByte == 44 || tByte == 33) {

          if (tByte == 44) {
            // Image Descriptor ","

            // 2 - LeftPos
            // 2 - TopPos
            // 2 - Width
            // 2 - Height
            fm.skip(8);

            // 1 - Flags
            boolean[] localBitFlags = fm.readBits();
            if (localBitFlags[0]) {
              int localBitsPerPixel = 1;
              if (localBitFlags[7]) {
                localBitsPerPixel += 1;
              }
              if (localBitFlags[6]) {
                localBitsPerPixel += 2;
              }
              if (localBitFlags[5]) {
                localBitsPerPixel += 4;
              }

              int localColorTableSize = (int) (Math.pow(2, localBitsPerPixel) * 3);
              FieldValidator.checkLength(localColorTableSize, fm.getRemainingLength());
              fm.skip(localColorTableSize);
            }

            // 1 byte of something here - not documented though?!
            fm.skip(1);

            int imageCodeSize = ByteConverter.unsign(fm.readByte());
            while (imageCodeSize != 0) {
              fm.skip(imageCodeSize);
              imageCodeSize = ByteConverter.unsign(fm.readByte());
            }

          }

          else if (tByte == 33) {
            // Extension Block "!"

            // 1 - Function Code
            fm.skip(1);

            int extCodeSize = ByteConverter.unsign(fm.readByte());
            while (extCodeSize != 0) {
              fm.skip(extCodeSize);
              extCodeSize = ByteConverter.unsign(fm.readByte());
            }

          }

          tByte = fm.readByte();
        }

        if (tByte != 59) {
          return null;
        }
      }

      // GIF Terminator Character ";"

      long length = fm.getOffset() - offset;

      //path,id,name,offset,length,compressed
      return new Resource(".gif", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}