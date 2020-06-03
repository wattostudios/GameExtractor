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

package org.watto.ge.script;

import org.watto.Language;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************
A plugin that can read an archive from a script file
**********************************************************************************************
**/
public abstract class ScriptArchivePlugin extends ArchivePlugin {

  /**
  **********************************************************************************************
  Constructor
  @param name the name of the plugin
  **********************************************************************************************
  **/
  public ScriptArchivePlugin(String name) {
    super(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getScript() {
    return null;
  }

  /**
  **********************************************************************************************
  Gets the plugin description
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    String description = toString() + "\n\n" + Language.get("Description_ScriptArchivePlugin");

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
  Checks the script syntax for errors
  return <b>null</b> if there are no errors, otherwise a list of errors.  
  **********************************************************************************************
  **/
  public String checkScript() {
    return null;
  }

}