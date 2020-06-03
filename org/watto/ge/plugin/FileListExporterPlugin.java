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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.WSObjectPlugin;
import org.watto.component.WSTableColumn;
import org.watto.io.FileManipulator;

public abstract class FileListExporterPlugin extends WSObjectPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListExporterPlugin(String code, String name) {
    setCode(code);
    setName(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {

    String description = toString() + "\n\n" + Language.get("Description_FileListExporterPlugin");

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
  
  **********************************************************************************************
  **/
  public void write(WSTableColumn[] columns, File destination) {
    try {
      FileManipulator fm = new FileManipulator(destination, true);
      write(columns, fm);
      fm.close();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void write(WSTableColumn[] columns, FileManipulator fm);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void write(WSTableColumn[] columns, String destination) {
    write(columns, new File(destination + "." + getCode()));
  }

}