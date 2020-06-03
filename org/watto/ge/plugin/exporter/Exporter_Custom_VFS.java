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

import java.io.File;
import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.datatype.SplitChunkResource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_Custom_VFS extends ExporterPlugin {

  static Exporter_Custom_VFS instance = new Exporter_Custom_VFS();

  static FileManipulator fm;
  static InflaterInputStream readSource;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_VFS getInstance() {
    return instance;
  }

  int nextByte = 0;

  int nextOffset = 0;
  //int numWritten = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_VFS() {
    setName("UFO Aftershock ZLib Compressed Chunks");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      nextByte = readSource.read();
      if (readSource.available() <= 0) {
        readSource.close();

        //System.out.println("going to " + nextOffset);
        //System.out.println("numWritten " + numWritten);
        fm.seek(nextOffset);

        int sizeOfNext = fm.readInt();
        if (sizeOfNext == 8) {
          fm.skip(8);
          sizeOfNext = fm.readInt();
          nextOffset += 12;
        }

        nextOffset += sizeOfNext + 4;

        if (fm.getOffset() >= fm.getLength()) {
          return false;
        }

        readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
        nextByte = readSource.read();
      }
    }
    catch (Throwable t) {
      nextByte = 0;
    }

    return true;
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

      fm.close();
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
  public String getDescription() {
    return "This exporter decompresses the files in UFO Aftershock archives when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource src) {
    try {
      SplitChunkResource source = (SplitChunkResource) src;

      fm = new FileManipulator(source.getSource(), false);

      long[] readLengths = source.getLengths();
      long[] readOffsets = source.getOffsets();

      //numWritten = 0;

      FileManipulator temp = new FileManipulator(new File("temp" + File.separator + "vfs-compressed-temp.txt"), true);
      for (int i = 0; i < readOffsets.length; i++) {
        fm.seek(readOffsets[i]);
        temp.writeBytes(fm.readBytes((int) readLengths[i]));
      }
      File usedFile = temp.getFile();
      temp.close();

      fm = new FileManipulator(usedFile, false);

      nextOffset = fm.readInt() + 4;

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm));

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // This can't be done using this method, so we will run the default pack() method instead
    Exporter_Default.getInstance().pack(source, destination);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      //numWritten++;
      return nextByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}