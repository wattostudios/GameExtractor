/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.scanner;

import java.util.zip.InflaterInputStream;

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ScannerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.stream.ManipulatorUnclosableInputStream;

public class Scanner_ZLib extends ScannerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Scanner_ZLib() {
    super("zlib", "ZLib Compressed Data");
  }

  @Override
  public Resource scan(int b, FileManipulator fm) {
    try {

      if (b != 120) {
        return null;
      }

      int secondParam = ByteConverter.unsign(fm.readByte());
      if (secondParam == 1 || secondParam == 94 || secondParam == 156 || secondParam == 218) {
        // valid compression value
      }
      else {
        return null;
      }

      long offset = fm.getOffset() - 2;

      // Now see if we can decompress the data
      fm.relativeSeek(offset);

      InflaterInputStream readSource = null;
      try {
        readSource = new InflaterInputStream(new ManipulatorUnclosableInputStream(fm));

        long decompLength = 0;
        while (readSource.available() > 0) {
          readSource.read();
          decompLength++;
        }

        // the ZLib exporter reads 1 byte too many, so need to go back 1 byte
        decompLength--;

        readSource.close();

        long endOffset = fm.getOffset();
        long length = endOffset - offset;

        //path,id,name,offset,length,compressed
        Resource resource = new Resource(".zlib", offset, length);
        resource.setDecompressedLength(decompLength);
        resource.setExporter(Exporter_ZLib.getInstance());
        return resource;

      }
      catch (Throwable t) {
        // not a ZLib stream

        try {
          if (readSource != null) {
            readSource.close();
          }
        }
        catch (Throwable t2) {
        }

      }

      return null;

    }
    catch (Throwable t) {
    }
    return null;
  }

}