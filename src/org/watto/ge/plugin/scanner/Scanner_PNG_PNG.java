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
import org.watto.io.converter.IntConverter;

public class Scanner_PNG_PNG extends ScannerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Scanner_PNG_PNG() {
    super("png", "PNG Image");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != -119) {
        return null;
      }

      if (fm.readByte() != 80 || fm.readByte() != 78 || fm.readByte() != 71 || fm.readByte() != 13 || fm.readByte() != 10 || fm.readByte() != 26 || fm.readByte() != 10 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 13 || fm.readByte() != 73 || fm.readByte() != 72 || fm.readByte() != 68 || fm.readByte() != 82) {
        return null;
      }

      long offset = (int) fm.getOffset() - 16;

      // IHDR data
      fm.skip(17);

      int dataSize = IntConverter.changeFormat(fm.readInt()) + 4;
      FieldValidator.checkLength(dataSize - 1, fm.getRemainingLength());

      String typeCode = fm.readString(4);
      while (!typeCode.equals("IEND")) {
        fm.skip(dataSize);

        dataSize = IntConverter.changeFormat(fm.readInt()) + 4;
        FieldValidator.checkLength(dataSize - 1, fm.getRemainingLength());
        typeCode = fm.readString(4);
      }

      // CRC for the end of the file
      fm.skip(4);

      long length = fm.getOffset() - offset;

      if (length < 20) {
        return null;
      }

      //path,id,name,offset,length,compressed
      return new Resource(".png", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}