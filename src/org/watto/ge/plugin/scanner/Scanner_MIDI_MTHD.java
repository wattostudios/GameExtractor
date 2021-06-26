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
import org.watto.io.converter.ShortConverter;

public class Scanner_MIDI_MTHD extends ScannerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Scanner_MIDI_MTHD() {
    super("mid", "Midi Audio");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 77) {
        return null;
      }

      if (fm.readByte() != 84 || fm.readByte() != 104 || fm.readByte() != 100 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 0 || fm.readByte() != 6) {
        return null;
      }

      int format = ShortConverter.changeFormat(fm.readShort());
      if (format != 0 && format != 1 && format != 2) {
        return null;
      }

      int numTracks = ShortConverter.changeFormat(fm.readShort());
      if ((numTracks == 1 && format == 0) || (numTracks > 1 && format == 1) || (numTracks > 0 && format == 2)) {
        // ok
      }
      else {
        return null;
      }

      fm.skip(2);

      long offset = fm.getOffset() - 14;

      boolean success = true;
      for (int i = 0; i < numTracks; i++) {
        // read each track
        if (!fm.readString(4).equals("MTrk")) {
          success = false;
          i = numTracks;
        }

        int trackLength = IntConverter.changeFormat(fm.readInt());
        try {
          FieldValidator.checkLength(trackLength - 1, fm.getRemainingLength());
          fm.skip(trackLength);
        }
        catch (Throwable t) {
          success = false;
          i = numTracks;
        }
      }

      if (!success) {
        return null;
      }

      long length = fm.getOffset() - offset;

      //path,id,name,offset,length,compressed
      return new Resource(".mid", offset, length);

    }
    catch (Throwable t) {
    }
    return null;
  }

}