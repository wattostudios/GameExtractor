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

public class Scanner_BMP_BM extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_BMP_BM() {
    super("bmp", "Bitmap Image");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 66) {
        return null;
      }

      if (fm.readByte() != 77) {
        return null;
      }

      long length = fm.readInt();
      int nullReserved = fm.readInt();

      FieldValidator.checkLength(length - 11, fm.getRemainingLength());
      FieldValidator.checkEquals(nullReserved, 0);

      long offset = fm.getOffset() - 10;

      fm.skip(4);

      int headSize = fm.readInt();
      FieldValidator.checkEquals(headSize, 40);

      fm.skip(8);

      int numPlanes = fm.readShort();
      FieldValidator.checkEquals(numPlanes, 1);

      int bits = fm.readShort();
      if (bits != 1 && bits != 2 && bits != 4 && bits != 8 && bits != 16 && bits != 24 && bits != 32) {
        return null;
      }

      int comp = fm.readInt();
      if (comp != 0 && comp != 1 && comp != 2) {
        return null;
      }

      if (length < 20) {
        return null;
      }

      fm.skip(length - 10);

      //path,id,name,offset,length,compressed
      return new Resource(".bmp", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}