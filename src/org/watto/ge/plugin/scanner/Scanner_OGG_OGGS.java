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
import org.watto.io.converter.BooleanArrayConverter;
import org.watto.io.converter.ByteConverter;

public class Scanner_OGG_OGGS extends ScannerPlugin {

  long offset = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Scanner_OGG_OGGS() {
    super("ogg", "Ogg Vorbis Audio");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 79) {
        return null;
      }

      if (fm.readByte() != 103 || fm.readByte() != 103 || fm.readByte() != 83 || fm.readByte() != 0) {
        return null;
      }

      boolean[] packetType = BooleanArrayConverter.changeFormat(fm.readBits());

      if (packetType[1]) {
        // start packet
        offset = (int) fm.getOffset() - 6;
      }
      else if (!packetType[0]) {
        // middle packet(s) or end packet
      }
      else {
        // not an OGG stream
        return null;
      }

      // determine the length of the page, then skip it
      fm.skip(20);

      int numSegments = ByteConverter.unsign(fm.readByte());
      int pageLength = 0;

      for (int i = 0; i < numSegments; i++) {
        int segmentLength = ByteConverter.unsign(fm.readByte());
        pageLength += segmentLength;
      }
      fm.skip(pageLength);

      if (packetType[2]) {
        // end packet

        long length = fm.getOffset() - offset;

        //path,id,name,offset,length,compressed
        return new Resource(".ogg", offset, length);
      }
      else {
        // call this method again, to read the middle segments until the end packet is found
        return scan(fm.readByte(), fm);
      }

    }
    catch (Throwable t) {
    }
    return null;
  }

}