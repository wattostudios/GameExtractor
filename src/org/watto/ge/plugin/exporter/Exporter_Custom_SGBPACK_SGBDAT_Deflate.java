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

import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.FileBuffer;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.buffer.ROTRepeatingKeyBufferWrapper;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_Custom_SGBPACK_SGBDAT_Deflate extends ExporterPlugin {

  static Exporter_Custom_SGBPACK_SGBDAT_Deflate instance = new Exporter_Custom_SGBPACK_SGBDAT_Deflate();

  static InflaterInputStream readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Deflate operating on ROT'd data
  **********************************************************************************************
  **/
  public static Exporter_Custom_SGBPACK_SGBDAT_Deflate getInstance() {
    return instance;
  }

  int[] rotKey = new int[0];

  int currentKeyPos = 0;

  int keyLength = 0;

  int[] postRotKey = new int[0];

  int currentPostKeyPos = 0;

  int postKeyLength = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_SGBPACK_SGBDAT_Deflate() {
    setName("ROT'd data, followed by Deflate, followed by ROT'd again");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_SGBPACK_SGBDAT_Deflate(int[] rotKey, int keyPos, int[] postRotKey) {
    setName("ROT'd data, followed by Deflate, followed by ROT'd again");
    this.rotKey = rotKey;
    this.keyLength = rotKey.length;
    this.currentKeyPos = keyPos;
    openAtCurrentKeyPos = true;
    this.currentPostKeyPos = 0;
    this.postRotKey = postRotKey;
    this.postKeyLength = postRotKey.length;
  }

  public int getCurrentKeyPos() {
    return currentKeyPos;
  }

  public void setCurrentKeyPos(int currentKeyPos) {
    this.currentKeyPos = currentKeyPos;
  }

  /** false will reset the currentKeyPos in open(), true will retain it **/
  boolean openAtCurrentKeyPos = false;

  public void startAtCurrentKeyPos(boolean openAtCurrentKeyPos) {
    this.openAtCurrentKeyPos = openAtCurrentKeyPos;
  }

  boolean finished = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (!finished && readSource.available() > 0) {
        if (readLength > 0) {
          return true;
        }
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
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    finished = false;

    if (!openAtCurrentKeyPos) {
      currentKeyPos = 0;
    }

    fm = fmIn;

    // Set the ROT
    fm.setBuffer(new ROTRepeatingKeyBufferWrapper(fm.getBuffer(), rotKey, currentKeyPos));

    readSource = new InflaterInputStream(new ManipulatorInputStream(fm), new Inflater(true));
    readLength = decompLengthIn;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    finished = false;

    try {
      if (!openAtCurrentKeyPos) {
        currentKeyPos = 0;
      }

      // set the ROT
      ManipulatorBuffer buffer = new ROTRepeatingKeyBufferWrapper(new FileBuffer(source.getSource(), false), rotKey, currentKeyPos);

      fm = new FileManipulator(buffer);
      fm.seek(source.getOffset());

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm), new Inflater(true));
      readLength = source.getDecompressedLength();
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
    DeflaterOutputStream outputStream = null;
    ManipulatorBuffer existingBuffer = fm.getBuffer();
    try {

      // Set the ROT
      fm.setBuffer(new ROTRepeatingKeyBufferWrapper(existingBuffer, rotKey));

      outputStream = new DeflaterOutputStream(new ManipulatorOutputStream(destination), new Deflater(Deflater.DEFAULT_COMPRESSION, true));

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        outputStream.write(exporter.read());
      }

      exporter.close();

      outputStream.finish();

    }
    catch (Throwable t) {
      logError(t);
      fm.setBuffer(existingBuffer);
      if (outputStream != null) {
        try {
          outputStream.finish();
        }
        catch (IOException e) {
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;

      int byteValue = readSource.read();
      byteValue += postRotKey[currentPostKeyPos++];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      else if (byteValue >= 256) {
        byteValue -= 256;
      }

      if (currentPostKeyPos >= postKeyLength) {
        currentPostKeyPos = 0;
      }
      return byteValue;

    }
    catch (Throwable t) {
      // END OF STREAM
      //t.printStackTrace();
      readLength = 0;
      finished = true;
      return 0;
    }
  }

}