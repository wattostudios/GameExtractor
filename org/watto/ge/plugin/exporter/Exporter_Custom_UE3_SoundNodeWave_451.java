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

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.PluginGroup_UE3;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.io.FileManipulator;

public class Exporter_Custom_UE3_SoundNodeWave_451 extends ExporterPlugin {

  static Exporter_Custom_UE3_SoundNodeWave_451 instance = new Exporter_Custom_UE3_SoundNodeWave_451();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  Adds a WAV audio header to the front of raw audio data
  **********************************************************************************************
  **/
  public static Exporter_Custom_UE3_SoundNodeWave_451 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_UE3_SoundNodeWave_451() {
    setName("Unreal Engine 3 Version 451 Audio Extractor");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return readLength > 0;
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
  public String getDescription() {
    return "This exporter exports audio from an Unreal Engine archive\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public void open(Resource source) {
    try {
      long offset = source.getOffset();

      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(offset);

      readLength = source.getLength();

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (!(readPlugin instanceof PluginGroup_UE3)) {
        readLength = 0; // force quit
        return;
      }

      PluginGroup_UE3 ue3Plugin = (PluginGroup_UE3) readPlugin;

      // 4 - File ID Number? (incremental from 0) (thie is NOT an ID into the Names Directory!)
      readSource.skip(4);

      // for each property (until Property Name ID = "None")
      //   8 - Property Name ID
      //   8 - Property Type ID
      //   8 - Property Length
      //   X - Property Data
      UnrealProperty[] properties = ue3Plugin.readProperties(readSource);

      // 8 - null
      // 8 - null
      // 4 - null
      // 4 - Unknown
      // 4 - Audio Format? (1=ogg, 2=mp3)
      // 4 - null
      // 4 - Audio Length (Compressed Length?)
      // 4 - Audio Length (Decompressed Length?)
      // 4 - Unknown
      readSource.skip(44);

      // X - Audio Data
      long headerLength = readSource.getOffset() - offset;

      readLength -= headerLength;

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * NOT DONE
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
      readLength--;
      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}