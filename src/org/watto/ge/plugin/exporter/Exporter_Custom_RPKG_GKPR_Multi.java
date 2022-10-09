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

package org.watto.ge.plugin.exporter;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.FileBuffer;
import org.watto.io.buffer.XORRepeatingKeyBufferWrapper;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class Exporter_Custom_RPKG_GKPR_Multi extends ExporterPlugin {

  static Exporter_Custom_RPKG_GKPR_Multi instance = new Exporter_Custom_RPKG_GKPR_Multi();

  byte[] buffer = null;

  int bufferPos = 0;

  int bufferLength = 0;

  /**
  **********************************************************************************************
  Does XOR decryption with a repeating key, then feeds that into an LZ4 decompressor
  **********************************************************************************************
  **/
  public static Exporter_Custom_RPKG_GKPR_Multi getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_RPKG_GKPR_Multi() {
    setName("XOR Repeating Key Decryption + LZ4 Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      return (bufferPos < bufferLength);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    buffer = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      // reset now, so if there's any errors, it'll return nothing
      bufferPos = 0;
      bufferLength = 0;

      int compLength = (int) source.getLength();
      int decompLength = (int) source.getDecompressedLength();

      // Read in the bytes (and decrypt it)
      FileManipulator fm = new FileManipulator(new XORRepeatingKeyBufferWrapper(new FileBuffer(source.getSource(), false), new int[] { 220, 69, 166, 156, 211, 114, 76, 171 }));
      fm.seek(source.getOffset());
      byte[] compBytes = fm.readBytes(compLength);
      fm.close();

      // Pass it into the LZ4 Decompressor
      LZ4Factory factory = LZ4Factory.fastestInstance();
      LZ4FastDecompressor decompressor = factory.fastDecompressor();
      buffer = new byte[decompLength];
      decompressor.decompress(compBytes, 0, buffer, 0, decompLength);

      // ready to read from the buffer
      bufferPos = 0;
      bufferLength = decompLength;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int currentByte = buffer[bufferPos];
      bufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      bufferPos = bufferLength;
      return 0;
    }
  }

}