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
import org.watto.datatype.jna.JNA_UINT8;
import org.watto.ge.helper.JNAHelper;
import org.watto.ge.helper.NativeOodleDecomp32;
import org.watto.ge.helper.NativeOodleDecomp64;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import com.sun.jna.Memory;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Exporter_Oodle extends ExporterPlugin {

  static Exporter_Oodle instance = new Exporter_Oodle();

  static byte[] decompBuffer = null;

  static int decompPos = 0;

  static int decompLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Oodle getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Oodle() {
    setName("Oodle Compressed File");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return decompPos < decompLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    decompBuffer = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      int compLength = (int) source.getLength();

      int bufferSize = (int) compLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      // Read in the full file, then decompress it into a buffer
      FileManipulator fm = new FileManipulator(source.getSource(), false, bufferSize);
      fm.seek(source.getOffset());
      byte[] compBytes = fm.readBytes(compLength);
      fm.close();

      try {
        decompBuffer = oodleDecompress(compBytes, (int) source.getDecompressedLength());
      }
      catch (Throwable t) {
        // Hopefully just something simple like, can't find the DLL files
        ErrorLogger.log(t);
      }

      if (decompBuffer == null) {
        // if there was any error, just return the compressed data instead
        decompBuffer = compBytes;
      }

      decompPos = 0;
      decompLength = decompBuffer.length;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT DONE - DEFAULT
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

      //destination.forceWrite();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      return (decompBuffer[decompPos++]);
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /** 32-bit library **/
  private static NativeOodleDecomp32 lib32 = null;

  /** 64-bit library **/
  private static NativeOodleDecomp64 lib64 = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static byte[] oodleDecompress(byte[] compBytes, int decompLength) {
    try {
      loadLibrary();

      int compLength = compBytes.length;
      byte[] decompBytes = new byte[decompLength];

      Memory sourcePointer = new Memory(compLength);
      sourcePointer.write(0, compBytes, 0, compLength);
      Memory resultPointer = new Memory(decompLength);
      resultPointer.write(0, decompBytes, 0, decompLength);

      int result = -1;
      if (System.getProperty("os.arch").equals("x86")) {
        result = lib32.OodleLZ_Decompress(new JNA_UINT8(sourcePointer), compLength, new JNA_UINT8(resultPointer), decompLength, 0, 0, 0, new JNA_UINT8(), 0, com.sun.jna.Pointer.createConstant(0), com.sun.jna.Pointer.createConstant(0), com.sun.jna.Pointer.createConstant(0), 0, 0);
      }
      else {
        result = lib64.OodleLZ_Decompress(new JNA_UINT8(sourcePointer), compLength, new JNA_UINT8(resultPointer), decompLength, 0, 0, 0, new JNA_UINT8(), 0, com.sun.jna.Pointer.createConstant(0), com.sun.jna.Pointer.createConstant(0), com.sun.jna.Pointer.createConstant(0), 0, 0);
      }

      if (result <= 0) {
        ErrorLogger.log("Oodle Decompression failed: " + result);
        return null;
      }

      byte[] resultantBytes = resultPointer.getByteArray(0, result);
      return resultantBytes;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void loadLibrary() {
    try {
      if (System.getProperty("os.arch").equals("x86")) {
        if (lib32 == null) {
          JNAHelper.setLibraryPaths();
          lib32 = NativeOodleDecomp32.INSTANCE;
        }
      }
      else {
        if (lib64 == null) {
          JNAHelper.setLibraryPaths();
          lib64 = NativeOodleDecomp64.INSTANCE;
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}