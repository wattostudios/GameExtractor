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

package org.watto.ge.plugin;

import java.io.File;
import java.io.OutputStream;
import org.watto.Language;
import org.watto.component.WSObjectPlugin;
import org.watto.datatype.Resource;
import org.watto.io.FileManipulator;

public abstract class ExporterPlugin extends WSObjectPlugin {

  protected FileManipulator exportDestination;

  /**
   **********************************************************************************************
   * false if the file has been read fully, true if there is more to read.
   **********************************************************************************************
   **/
  public abstract boolean available();

  /**
   **********************************************************************************************
   * Closes the file
   **********************************************************************************************
   **/
  public abstract void close();
  
  /**
  **********************************************************************************************
  Closes and Re-opens the resource from the beginning. Here in case we want to keep decompressed
  buffers for the next run instead of deleting them and re-decompressing every time, for example.
  Used mainly in ExporterByteBuffer to roll back to the beginning of the file.
  **********************************************************************************************
  **/
  public void closeAndReopen(Resource source) {
    close();
    open(source);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void extract(Resource source, File destination) {
    extract(source, new FileManipulator(destination, true));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void extract(Resource source, FileManipulator destination) {
    try {
      this.exportDestination = destination;

      open(source);
      while (available()) {
        destination.writeByte(read());
      }
      close();

      this.exportDestination = null;
    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Used for ZIP compression/decompression!
   **********************************************************************************************
   **/
  public void extract(Resource source, OutputStream destination) {
    try {
      open(source);
      while (available()) {
        destination.write(read());
      }
      close();
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

    String description = toString() + "\n\n" + Language.get("Description_ExporterPlugin");

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;

  }

  /**
   **********************************************************************************************
   * Opens the file for extracting
   **********************************************************************************************
   **/
  public abstract void open(Resource source);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void pack(Resource source, FileManipulator destination);

  /**
   **********************************************************************************************
   * Reads the next byte of data
   **********************************************************************************************
   **/
  public abstract int read();

}