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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.PluginGroup_U;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;

public class Exporter_Custom_U_Sound_Generic extends ExporterPlugin {

  static Exporter_Custom_U_Sound_Generic instance = new Exporter_Custom_U_Sound_Generic();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_U_Sound_Generic getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_U_Sound_Generic() {
    setName("Unreal Sound File");
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
    return "This exporter extracts the Sound Files from Unreal Engine files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override

  public void open(Resource source) {
    try {

      PluginGroup_U readPlugin = (PluginGroup_U) Archive.getReadPlugin();

      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      if (readSource.readInt() == 3 && readSource.readInt() == 0 && readSource.readInt() == 1) {
        // skip these fields (SWAT4 Archives)
      }
      else {
        // these fields don't exist, so return to the correct spot
        readSource.seek(source.getOffset());
      }

      // X - Properties
      if (source instanceof Resource_Unreal) {
        Resource_Unreal resource = (Resource_Unreal) source;
        resource.setUnrealProperties(readPlugin.readProperties(readSource));
      }
      else {
        readPlugin.skipProperties(readSource);
      }

      // 1-5 - Number of Files? (1)
      PluginGroup_U.readIndex(readSource);

      // 1-5 - File Length
      PluginGroup_U.readIndex(readSource);
      /*
      int length = (int) PluginGroup_U.readIndex(readSource);
      if (length == 0) { // Redneck Kentucky
        readSource.skip(7);
        PluginGroup_U.readIndex(readSource);
      }
      */

      // Find the RIFF header (some files have other fields in here)
      // Want to do this because this is the Generic plugin, so want to suit as many cases as possible
      long offset = readSource.getOffset();

      byte byte1 = readSource.readByte();
      byte byte2 = readSource.readByte();
      byte byte3 = readSource.readByte();
      byte byte4 = readSource.readByte();

      for (int i = 0; i < 30; i++) {
        if (byte1 == 82 && byte2 == 73 && byte3 == 70 && byte4 == 70) {
          // found RIFF
          break;
        }
        else {
          // move the bytes along 1, and try again
          offset++;

          byte1 = byte2;
          byte2 = byte3;
          byte3 = byte4;
          byte4 = readSource.readByte();
        }
      }

      readSource.relativeSeek(offset);

      // Now, for some reason, some games like WarPath wrap an OGG audio file in a RIFF header, so we need to check for this.
      readSource.skip(44);
      if (readSource.readString(4).equals("OggS")) {
        offset += 44;
      }

      readSource.relativeSeek(offset);

      readLength = source.getLength() - (readSource.getOffset() - source.getOffset());

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      //for (int i=0;i<decompLength;i++){
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
      readLength--;
      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}