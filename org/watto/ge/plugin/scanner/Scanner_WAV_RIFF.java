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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ScannerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Scanner_WAV_RIFF extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_WAV_RIFF() {
    super("wav", "RIFF Format");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 82) {
        return null;
      }

      if (fm.readByte() != 73 || fm.readByte() != 70 || fm.readByte() != 70) {
        return null;
      }

      int length = fm.readInt() + 8;
      FieldValidator.checkLength(length - 1, fm.getRemainingLength() + 8);

      long offset = fm.getOffset() - 8;

      String type2 = fm.readString(4);
      String ext = ".riff";

      if (type2.equals("AVI ")) {
        ext = ".avi";
      }
      else if (type2.equals("WAVE") && fm.readString(4).equals("fmt ")) {
        ext = ".wav";
      }
      else if (type2.equals("MIDS") && fm.readString(4).equals("fmt ")) {
        ext = ".mid";
      }

      fm.seek(offset + length);

      if (length < 16) {
        return null;
      }

      //path,id,name,offset,length,compressed
      return new Resource(ext, offset, length);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return null;
  }

}