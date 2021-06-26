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

import org.watto.datatype.Resource;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

public class Exporter_JNI_UNLZWX extends Exporter_Default {

  /** the type of compression **/
  String compressionType = "";

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_JNI_UNLZWX(String compressionType) {
    setName("UNLZWX Decompression [JNI]");
    this.compressionType = compressionType;
  }

  /**
  **********************************************************************************************
  JNI to run the decompression routine
  **********************************************************************************************
  **/
  private native int unlzwx(byte[] outbuff, int maxsize, byte[] in, int insize);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] runJNI(Resource resource, byte[] source, int decompLength) {
    try {

      System.loadLibrary("jni_exporter_jni_unlzwx");

      byte[] dest = new byte[decompLength];

      unlzwx(dest, decompLength, source, source.length);

      return dest;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      //
      // Read the full source bytes into an array
      //
      readLength = source.getLength();
      if (readLength < 0) {
        readLength = 0; // just in case, so we don't have a never-ending "while" loop down below
      }

      readSource = new FileManipulator(source.getSource(), false, 1); // 1, so that it doesn't read the whole file in first
      readSource.seek(source.getOffset());

      readSource.getBuffer().setBufferSize((int) readLength); // now resize the buffer to the file size
      byte[] sourceBytes = readSource.readBytes((int) readLength);

      readSource.close();
      readSource = null;

      //
      // Now that the source data is buffered, run the JNI decompression
      //
      byte[] decompBytes = runJNI(source, sourceBytes, (int) source.getDecompressedLength());

      //
      // Now open the byte buffer as a manipulator, ready for reading normally
      //
      readSource = new FileManipulator(new ByteBuffer(decompBytes));
      readSource.seek(0); // just in case, restart at the beginning of the decompressed file

      /*
      //
      // Now write it out to a file
      //
      String realFilePath = new File(Settings.get("TempDirectory")).getAbsolutePath();
      realFilePath += File.separator + source.getFilenameWithExtension();
      File realFile = new File(realFilePath);
      realFile = FilenameChecker.correctFilename(realFile); // removes funny characters etc.
      
      //
      // Now open this decompressed file for reading normally.
      //
      if (!realFile.exists()) {
        // some kind of problem running the decompression - a QuickBMS issue
        readLength = 0; // force the file to "not exist"
        return;
      }
      
      readLength = realFile.length();
      
      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }
      
      readSource = new FileManipulator(realFile, false, bufferSize);
      readSource.seek(0); // just in case, restart at the beginning of the decompressed file
      */

    }
    catch (Throwable t) {
    }
  }

}