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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.JNAHelper;
import org.watto.ge.helper.NativeQuickBMS;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

import com.sun.jna.Memory;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Exporter_QuickBMS_DLL extends ExporterPlugin {

  static Exporter_QuickBMS_DLL instance = new Exporter_QuickBMS_DLL();

  static byte[] decompBuffer = null;

  static int decompPos = 0;

  static int decompLength = 0;

  String compressionAlgorithm = "";

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_QuickBMS_DLL getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_QuickBMS_DLL() {
    setName("Compressed File (handled by QuickBMS)");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_QuickBMS_DLL(String compType) {
    compressionAlgorithm = compType;
    setName("QuickBMS Compressed File (" + compressionAlgorithm + ")");
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
        decompBuffer = quickbmsDecompress(compBytes, (int) source.getDecompressedLength());
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

  /** quickbms library **/
  private static NativeQuickBMS quickbmsLib = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] quickbmsDecompress(byte[] compBytes, int decompLength) {
    try {
      loadLibrary();

      int compLength = compBytes.length;
      byte[] decompBytes = new byte[decompLength];

      Memory sourcePointer = new Memory(compLength);
      sourcePointer.write(0, compBytes, 0, compLength);

      Memory resultPointer = new Memory(decompLength);
      resultPointer.write(0, decompBytes, 0, decompLength);

      //String algo, Pointer in, int zsize, Pointer out, int size
      int result = quickbmsLib.quickbms_compression(compressionAlgorithm, sourcePointer, compLength, resultPointer, decompLength);

      if (result <= 0) {
        ErrorLogger.log("QuickBMS Decompression failed: " + result);
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
      if (quickbmsLib == null) {
        JNAHelper.setLibraryPaths();
        quickbmsLib = NativeQuickBMS.INSTANCE;
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}