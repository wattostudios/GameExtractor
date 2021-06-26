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

public class Scanner_VAG_VAGP extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_VAG_VAGP() {
    super("vag", "Playstation VAG Audio");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 86) {
        return null;
      }

      //System.out.println("A");

      if (fm.readByte() != 65 || fm.readByte() != 71 || fm.readByte() != 112) {
        return null;
      }

      long offset = fm.getOffset() - 4;

      if (fm.readByte() != 0 || fm.readByte() != 0) {
        return null;
      }

      if (fm.readInt() != 768) {
        return null;
      }

      if (fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0) {
        return null;
      }

      fm.skip(8);

      if (fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0) {
        return null;
      }

      fm.skip(48);

      boolean finished = false;
      while (!finished) {
        //int decompBlock = ByteConverter.unsign(fm.readByte());
        fm.skip(1);
        int continueByte = fm.readByte();

        fm.skip(14);

        if (continueByte != 0/* && decompBlock == 0 */) {
          finished = true;
        }
      }

      long length = fm.getOffset() - offset;

      //path,id,name,offset,length,compressed
      return new Resource(".vag", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}