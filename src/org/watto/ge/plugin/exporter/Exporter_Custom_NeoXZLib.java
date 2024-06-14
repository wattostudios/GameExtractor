/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.exporter;

import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.stream.ManipulatorUnclosableInputStream;

public class Exporter_Custom_NeoXZLib extends ExporterPlugin {

  static Exporter_Custom_NeoXZLib instance = new Exporter_Custom_NeoXZLib();

  InputStream readSource;
  long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_NeoXZLib getInstance() {
    return instance;
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_NeoXZLib() {
    setName("NeoX Encryption + ZLib Compression");
  }

  int compLength = 0;
  int decompLength = 0;
  int decompCRC = 0;
  boolean compressed = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_NeoXZLib(int compLength, int decompLength, int decompCRC, boolean compressed) {
    setName("NeoX Encryption + ZLib Compression");
    this.compLength = compLength;
    this.decompLength = decompLength;
    this.decompCRC = decompCRC;
    this.compressed = compressed;
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
      fm.close();
      readSource.close();
      readSource = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************
  Closes and Re-opens the resource from the beginning. Here in case we want to keep decompressed
  buffers for the next run instead of deleting them and re-decompressing every time, for example.
  Used mainly in ExporterByteBuffer to roll back to the beginning of the file.
  **********************************************************************************************
  **/
  public void closeAndReopen(Resource source) {
    fm.seek(0);
    try {
      if (compressed) {
        readSource.close();
        readSource = new InflaterInputStream(new ManipulatorUnclosableInputStream(fm));
      }
      else {
        readSource.close();
        readSource = new ManipulatorUnclosableInputStream(fm);
      }
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
      fm = fmIn;

      // Read in the compressed bytes
      byte[] compBytes = fm.readBytes(compLengthIn);

      // Work out the encryption details
      // Ref (partial): https://github.com/zhouhang95/neox_tools/blob/master/extractor.py
      int b = decompCRC ^ decompLength;
      int start = 0;
      int size = compLength;
      if (size > 128) {
        start = (decompCRC >> 1) % (compLength - 128);
        size = ((2 * decompLength) % 96) + 32;
      }

      int[] key = new int[256];
      for (int k = 0; k < 256; k++) {
        key[k] = (b + k) & 0xFF;
      }

      // Decrypt the data
      for (int e = 0; e < size; e++) {
        compBytes[start + e] ^= key[(e % 256)];
      }

      // Prepare the data for decompression (or raw reading)
      FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));
      if (compressed) {
        readSource = new InflaterInputStream(new ManipulatorUnclosableInputStream(compFM));
      }
      else {
        readSource = new ManipulatorUnclosableInputStream(compFM);
      }

      fm = compFM;

      readLength = decompLengthIn;
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      //System.out.println(source.getName());

      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      int compLengthIn = (int) source.getLength();

      boolean fileTypeScanning = false;
      int originalCompLengthIn = compLengthIn;
      if (compLengthIn < compLength) {
        // we're just scanning for file types, not reading the full file, but we need a bigger buffer so it doesn't die
        fileTypeScanning = true;
        compLengthIn = 256;
      }

      // Read in the compressed bytes
      byte[] compBytes = fm.readBytes(compLengthIn);

      // Work out the encryption details
      // Ref (partial): https://github.com/zhouhang95/neox_tools/blob/master/extractor.py
      long b = IntConverter.unsign(decompCRC) ^ decompLength;
      int start = 0;
      int size = compLength;
      if (size > 128) {
        start = ((int) (IntConverter.unsign(decompCRC) >> 1)) % (compLength - 128);
        size = ((2 * decompLength) % 96) + 32;
      }

      int[] key = new int[256];
      for (int k = 0; k < 256; k++) {
        key[k] = (int) ((b + k) & 0xFF);
      }

      if (fileTypeScanning) {
        // we're just scanning for file types, not reading the full file. So we need to ensure the key changes
        // are only being applied if they're in the smaller array we're reading
        if (start >= compLengthIn) {
          size = 0; // skip it entirely
        }
        else {
          if (start + size >= compLengthIn) {
            //System.out.println(source.getName());
            size = compLengthIn - start;
          }
        }
      }

      // Decrypt the data
      for (int e = 0; e < size; e++) {
        compBytes[start + e] ^= key[(e % 256)];
      }

      // Prepare the data for decompression (or raw reading)
      FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));
      if (compressed) {
        readSource = new InflaterInputStream(new ManipulatorUnclosableInputStream(compFM));
      }
      else {
        readSource = new ManipulatorUnclosableInputStream(compFM);
      }

      fm = compFM;

      if (fileTypeScanning) {
        // we're just scanning for file types, not reading the full file, so just return the actual size we want for file type scanning
        readLength = originalCompLengthIn;
      }
      else {
        readLength = source.getDecompressedLength();
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // not supported
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;
      return readSource.read();
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}