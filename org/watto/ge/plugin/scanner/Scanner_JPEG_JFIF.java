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
import org.watto.ge.plugin.ScannerPlugin;
import org.watto.io.FileManipulator;

public class Scanner_JPEG_JFIF extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_JPEG_JFIF() {
    super("jpg", "JPEG Image");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 74) {
        return null;
      }

      if (fm.readByte() != 70 || fm.readByte() != 73 || fm.readByte() != 70 || fm.readByte() != 0 || fm.readByte() != 1) {
        return null;
      }

      int version2 = fm.readByte();
      int units = fm.readByte();
      if ((version2 != 0 && version2 != 1 && version2 != 2) || (units != 0 && units != 1 && units != 2)) {
        return null;
      }

      long offset = fm.getOffset() - 14;

      b = fm.readByte();
      while (fm.getRemainingLength() >= 0) {
        // look for the end marker
        if (b == -1) {

          b = fm.readByte();
          if (b == -39) {
            // found the end marker

            long length = fm.getOffset() - offset;

            if (length < 16) {
              return null;
            }

            //path,id,name,offset,length,compressed
            return new Resource(".jpg", offset, length);

          }
        }
        else {
          b = fm.readByte();
        }
      }

      // did not find the end marker
      return null;

    }
    catch (Throwable t) {
    }
    return null;
  }

}