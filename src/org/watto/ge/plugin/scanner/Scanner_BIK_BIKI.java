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

public class Scanner_BIK_BIKI extends ScannerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Scanner_BIK_BIKI() {
    super("bik", "Bink Video");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 66) {
        return null;
      }

      if (fm.readByte() != 73 || fm.readByte() != 75) {
        return null;
      }

      int BINKbyte2 = fm.readByte();
      if (BINKbyte2 != 105 && BINKbyte2 != 102) {
        return null;
      }

      long length = fm.readInt();
      FieldValidator.checkLength(length - 1, fm.getRemainingLength());

      length += 8;

      long offset = fm.getOffset() - 8;

      fm.skip(length - 8);

      //path,id,name,offset,length,compressed
      return new Resource(".bik", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}