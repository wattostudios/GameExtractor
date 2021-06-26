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

public class Scanner_DDS_DDS extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_DDS_DDS() {
    super("dds", "DirectX DDS Image");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 68) {
        return null;
      }

      if (fm.readByte() != 68 || fm.readByte() != 83 || fm.readByte() != 32 || fm.readByte() != 124 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0) {
        return null;
      }

      long offset = fm.getOffset() - 8;

      fm.skip(4);

      int height = fm.readInt();
      int width = fm.readInt();

      fm.skip(4);

      int depth = fm.readInt();
      int numMipmaps = fm.readInt();
      if (numMipmaps == 0) {
        numMipmaps = 1;
      }
      else if (numMipmaps < 0) {
        return null;
      }

      fm.skip(44);

      if (fm.readInt() != 32) {
        return null;
      }

      fm.skip(4);

      String format = fm.readString(4);

      fm.skip(40);

      int blockSize = 16;
      if (format.equals("DXT1")) {
        blockSize = 8;
      }

      if (format.equals("DXT1") || format.equals("DXT3") || format.equals("DXT4") || format.equals("DXT5")) {
        // ok
      }
      else {
        return null;
      }

      long length = 0;
      for (int n = 0; n < numMipmaps; n++) {
        if (depth != 0) {
          length += ((width + 3) / 4) * ((height + 3) / 4) * ((depth + 3) / 4) * blockSize;
        }
        else {
          length += ((width + 3) / 4) * ((height + 3) / 4) * blockSize;
        }

        width >>= 1;
        height >>= 1;

        if (width == 0) {
          width = 1;
        }
        if (height == 0) {
          height = 1;
        }

      }

      FieldValidator.checkLength(length - 1, fm.getRemainingLength());

      if (length <= 0) {
        return null;
      }

      fm.skip(length);

      length += 128;

      //path,id,name,offset,length,compressed
      return new Resource(".dds", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}