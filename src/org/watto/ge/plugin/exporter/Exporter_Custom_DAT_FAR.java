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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.BooleanArrayConverter;
import org.watto.io.converter.ByteConverter;

public class Exporter_Custom_DAT_FAR extends ExporterPlugin {

  static Exporter_Custom_DAT_FAR instance = new Exporter_Custom_DAT_FAR();

  static FileManipulator readSource;
  static byte[] readBuffer = new byte[200000];
  static int readBufferPos = 0;
  static long readLength = 0;
  static int readBufferLevel = 0;

  static int decompressionType = 0;
  static boolean askFormat = true;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_DAT_FAR getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_DAT_FAR() {
    setName("Maxis DBPF (original) Compression");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength <= 0) {
        return false;
      }

      if (readBufferPos >= readBufferLevel) {
        fillBuffer();
      }
      return true;

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
      readBuffer = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void copyBytes(int numCopy, int copyOffset) {
    try {

      if (readBufferLevel + numCopy >= readBuffer.length) {
        moveBuffer(numCopy);
      }

      copyOffset = readBufferLevel - copyOffset;
      //System.out.println(readBufferLevel);

      for (int c = 0; c < numCopy; c++) {
        readBuffer[readBufferLevel] = readBuffer[copyOffset];
        readBufferLevel++;
        copyOffset++;
      }

    }
    catch (Throwable t) {
      System.out.println("Decryption copy error - " + t);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void fillBuffer() {
    try {

      int codeByte1 = readSource.readByte();
      boolean[] bit = BooleanArrayConverter.convertLittle((byte) codeByte1);
      if (codeByte1 < 0) {
        codeByte1 = 256 + codeByte1;
      }

      if (bit[0] && bit[1] && bit[2]) {
        //System.out.println("Plain Text");

        int numPlain = ((codeByte1 & 0x1F) << 2) + 4;
        //System.out.println("numPlain:" + numPlain);
        insertBytes(readSource.readBytes(numPlain));

      }
      else if (bit[0] && bit[1] && !bit[2]) {
        //System.out.println("Copy Type 1");
        int codeByte2 = ByteConverter.unsign(readSource.readByte());
        int codeByte3 = ByteConverter.unsign(readSource.readByte());
        int codeByte4 = ByteConverter.unsign(readSource.readByte());

        int numPlain = codeByte1 & 0x03;
        //System.out.println("numPlain:" + numPlain);
        insertBytes(readSource.readBytes(numPlain));

        int numCopy = 0;
        int copyOffset = 0;
        if (decompressionType == 0) {
          // TS2
          numCopy = ((codeByte1 & 0x0C) << 6) + codeByte4 + 5;
          copyOffset = ((codeByte1 & 0x10) << 12) + (codeByte2 << 8) + codeByte3 + 1;
        }
        else {
          // SC4, TSO?
          numCopy = ((codeByte1 & 0x1C) << 6) + codeByte4 + 5;
          copyOffset = (codeByte2 << 8) + codeByte3;
        }

        //System.out.println("numCopy:" + numCopy);
        //System.out.println("copyOffset:" + copyOffset);
        copyBytes(numCopy, copyOffset);

      }
      else if (bit[0] && !bit[1]) {
        //System.out.println("Copy Type 2");
        int codeByte2 = ByteConverter.unsign(readSource.readByte());
        int codeByte3 = ByteConverter.unsign(readSource.readByte());

        int numPlain = (codeByte2 >> 6) & 0x03;
        //System.out.println("numPlain:" + numPlain);
        insertBytes(readSource.readBytes(numPlain));

        int numCopy = (codeByte1 & 0x3F) + 4;
        int copyOffset = ((codeByte2 & 0x3F) << 8) + codeByte3 + 1;
        //System.out.println("numCopy:" + numCopy);
        //System.out.println("copyOffset:" + copyOffset);
        copyBytes(numCopy, copyOffset);

      }
      else if (!bit[0]) {
        //System.out.println("Copy Type 3");
        int codeByte2 = ByteConverter.unsign(readSource.readByte());

        int numPlain = codeByte1 & 0x03;
        //System.out.println("numPlain:" + numPlain);
        insertBytes(readSource.readBytes(numPlain));

        int numCopy = ((codeByte1 & 0x1C) >> 2) + 3;
        int copyOffset = ((codeByte1 & 0x60) << 3) + codeByte2 + 1;
        //System.out.println("numCopy:" + numCopy);
        //System.out.println("copyOffset:" + copyOffset);
        copyBytes(numCopy, copyOffset);

      }
      else { // this should be impossible!
        return;
      }

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
  public String getDescription() {
    return "This exporter decompresses Maxis DBPF-compressed files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void insertBytes(byte[] bytes) {

    int numBytesToInsert = bytes.length;
    if (readBufferLevel + numBytesToInsert >= readBuffer.length) {
      moveBuffer(numBytesToInsert);
    }

    System.arraycopy(bytes, 0, readBuffer, readBufferLevel, numBytesToInsert);
    readBufferLevel += numBytesToInsert;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void moveBuffer(int numBytesToInsert) {

    System.arraycopy(readBuffer, numBytesToInsert, readBuffer, 0, readBufferLevel - numBytesToInsert);
    readBufferLevel -= numBytesToInsert;
    readBufferPos -= numBytesToInsert;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset() + 9);

      readBuffer = new byte[200000];
      readBufferPos = 0;
      readBufferLevel = 0;
      readLength = source.getDecompressedLength();

      // Find out if the user is using The Sims Online, Simcity 4, or The Sims 2 Archives
      if (askFormat) {
        String ext = source.getExtension();
        if (ext.equals("package")) {
          decompressionType = 0;
        }
        else if (ext.equals("sc4lot")) {
          decompressionType = 1;
        }
        else if (ext.equals("sc4")) {
          decompressionType = 1;
        }
        else {
          String[] possibleValues = { "The Sims 2", "The Sims Online", "Simcity 4" };
          String selectedValue = (String) javax.swing.JOptionPane.showInputDialog(null, "What game does this archive belong to?", "Game", javax.swing.JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
          if (selectedValue.equals("The Sims 2")) {
            decompressionType = 0;
          }
          else if (selectedValue.equals("Simcity 4")) {
            decompressionType = 1;
          }
          else if (selectedValue.equals("The Sims Online")) {
            decompressionType = 2;
          }
        }
        askFormat = false;
      }

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

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
      int readByte = readBuffer[readBufferPos];
      readBufferPos++;
      readLength--;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}