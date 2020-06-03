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

public class Scanner_PCX extends ScannerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Scanner_PCX() {
    super("pcx", "PCX Image");
  }

  @Override
  @SuppressWarnings("unused")
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 10) {
        return null;
      }

      int version = fm.readByte();

      if ((version != 0 && version != 2 && version != 3 && version != 5) || fm.readByte() != 1) {
        return null;
      }

      long offset = fm.getOffset() - 3;

      int bitsPerPixel = fm.readByte();

      if (fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0) {
        return null;
      }

      int width = fm.readShort();
      FieldValidator.checkLength(width); // check negatives
      int height = fm.readShort();
      FieldValidator.checkLength(height); // check negatives

      fm.skip(4);

      // 48 - 16-color Color Palette
      fm.skip(48);

      if (fm.readByte() != 0) {
        return null;
      }

      int numPlanes = fm.readByte();
      int bytesPerLine = fm.readShort();

      fm.skip(2);

      // 58 - Padding
      if (fm.readLong() != 0 || fm.readLong() != 0 || fm.readLong() != 0 || fm.readLong() != 0 || fm.readLong() != 0 || fm.readLong() != 0 || fm.readLong() != 0 || fm.readShort() != 0) {
        return null;
      }

      // Move through image
      int totalSize = numPlanes * bytesPerLine;

      for (int h = 0; h < height; h++) {
        int numPixels = 0;

        while (numPixels < totalSize) {
          boolean[] counter = fm.readBits();
          if (counter[0] && counter[1]) {
            int countSize = 0;

            if (counter[2]) {
              countSize += 32;
            }
            if (counter[3]) {
              countSize += 16;
            }
            if (counter[4]) {
              countSize += 8;
            }
            if (counter[5]) {
              countSize += 4;
            }
            if (counter[6]) {
              countSize += 2;
            }
            if (counter[7]) {
              countSize += 1;
            }

            // 1 - color to repeat
            fm.skip(1);

            numPixels += countSize;
          }
          else {
            numPixels++;
          }

        }

      }

      int limitCount = 0;

      if (version == 5) {
        boolean keepTrying = true;
        while (keepTrying) {
          if (fm.readByte() != 12) {
            if (limitCount > 50) {
              keepTrying = false;
            }
            else {
              limitCount++;
            }
          }
          else {
            keepTrying = false;
          }
        }

        fm.skip(768);
      }

      if (limitCount > 50) {
        return null;
      }

      long length = (int) fm.getOffset() - offset;

      //path,id,name,offset,length,compressed
      return new Resource(".pcx", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}