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

public class Scanner_TIM_TIM2 extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_TIM_TIM2() {
    super("tim", "Playstation TIM2 Image");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 84) {
        return null;
      }

      //System.out.println("A");

      if (fm.readByte() != 73 || fm.readByte() != 77 || fm.readByte() != 50) {
        return null;
      }

      //System.out.println("B");

      fm.skip(2);

      if (fm.readByte() != 1 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0) {
        return null;
      }

      //System.out.println("C");

      long length = fm.readInt();
      FieldValidator.checkLength(length - 1, fm.getRemainingLength());
      fm.skip(length);

      //System.out.println("D");

      length += 16;

      long offset = fm.getOffset() - 20;

      //path,id,name,offset,length,compressed
      return new Resource(".tim", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}