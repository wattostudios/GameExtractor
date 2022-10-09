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

import java.util.zip.InflaterInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_AES_ZLib extends ExporterPlugin {

  static Exporter_AES_ZLib instance = new Exporter_AES_ZLib();

  Cipher cipher = null;

  byte[] buffer = new byte[0];

  int bufferLength = 0;

  int bufferPos = 0;

  byte[] key = new byte[0];

  static InflaterInputStream readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Decrypts using the AES algorithm, then decompresses using ZLib
  **********************************************************************************************
  **/
  public static Exporter_AES_ZLib getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_AES_ZLib() {
    setName("AES Decryption with ZLib Decompression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] getKey() {
    return key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setKey(byte[] key) {
    this.key = key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_AES_ZLib(byte[] key) {
    setName("AES Decryption with ZLib Decompression");
    this.key = key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength > 0 && readSource.available() > 0) {
        return true;
      }
      return false;
    }
    catch (Throwable t) {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readSource.close();
      readSource = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      int compLength = (int) source.getLength();

      cipher = Cipher.getInstance("AES/ECB/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec);

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      // Read in the full compressed+encrypted file
      byte[] decryptedBuffer = cipher.update(fm.readBytes(compLength + 30)); // +30 to make sure we go slightly beyond the compressed length, so the Inflater works correctly. 

      // Now wrap that into the readSource that'll get decompressed by ZLib
      fm.close();

      readSource = new InflaterInputStream(new ManipulatorInputStream(new FileManipulator(new ByteBuffer(decryptedBuffer))));
      readLength = source.getDecompressedLength();

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {

      cipher = Cipher.getInstance("AES/ECB/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec);

      // Read in the full compressed+encrypted file
      byte[] decryptedBuffer = cipher.update(fmIn.readBytes(compLengthIn + 30)); // +30 to make sure we go slightly beyond the compressed length, so the Inflater works correctly.

      // Now wrap that into the readSource that'll get decompressed by ZLib
      FileManipulator fm = new FileManipulator(new ByteBuffer(decryptedBuffer));

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
      readLength = decompLengthIn;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT DONE
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
      readLength--;
      //System.out.println(readLength);
      return readSource.read();
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}