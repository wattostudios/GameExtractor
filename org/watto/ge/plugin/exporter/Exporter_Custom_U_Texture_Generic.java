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
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;

public class Exporter_Custom_U_Texture_Generic extends ExporterPlugin {

  static Exporter_Custom_U_Texture_Generic instance = new Exporter_Custom_U_Texture_Generic();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_U_Texture_Generic getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_U_Texture_Generic() {
    setName("Unreal Texture Image");
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
    return "This exporter extracts the Texture Images (and corresponding Palette) from Unreal Engine files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("static-access")
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

        UnrealProperty property = resource.getUnrealProperty("Palette");
        if (property != null) {
          // found the palette property - so lets extract the palette too
          try {
            long resourceNumber = ((Long) property.getValue()).longValue();
            if (resourceNumber > 0) {
              resourceNumber--;
              Resource paletteResource = Archive.getResource((int) resourceNumber);
              paletteResource.extract(readPlugin.getDirectoryFile(exportDestination.getFile(), "Palette", false));
            }
          }
          catch (Throwable t) {
            //t.printStackTrace();
          }

        }

      }
      else {
        readPlugin.skipProperties(readSource);
      }

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