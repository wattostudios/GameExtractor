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

public class Scanner_HTML_HTML extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_HTML_HTML() {
    super("html", "HTML Webpage");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 60) {
        return null;
      }

      int byte2 = fm.readByte();
      // LOOKING FOR <HTML or <html
      if (byte2 == 72 && (fm.readByte() != 84 || fm.readByte() != 77 || fm.readByte() != 76)) {
        return null;
      }
      else if (byte2 == 104 && (fm.readByte() != 116 || fm.readByte() != 109 || fm.readByte() != 108)) {
        return null;
      }
      else if (byte2 != 72 && byte2 != 104) {
        return null;
      }

      long offset = fm.getOffset() - 5;

      b = fm.readByte();
      while (fm.getRemainingLength() >= 0) {
        // LOOKING FOR </HTML> or </html>
        if (b == 60) {

          b = fm.readByte();
          if (b == 47) {
            // found an end tag

            b = fm.readByte();
            if ((b == 72 && fm.readByte() == 84 && fm.readByte() == 77 && fm.readByte() == 76 && fm.readByte() == 62) || (b == 104 && fm.readByte() == 116 && fm.readByte() == 109 && fm.readByte() == 108 && fm.readByte() == 62)) {
              // found it
              long length = fm.getOffset() - offset;

              //path,id,name,offset,length,compressed
              return new Resource(".html", offset, length);
            }

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