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

package org.watto.component;

import java.io.File;
import java.io.FileFilter;
import org.watto.Language;
import org.watto.datatype.Resource;

public abstract class DirectoryListPanel extends WSPanelPlugin {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public DirectoryListPanel() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public DirectoryListPanel(String name) {
    super();
    setCode(name);
    setType("DirectoryList");
  }

  /**
   **********************************************************************************************
   * Checks the current directory when it is being repainted incase any files are added/removed
   **********************************************************************************************
   **/
  public abstract void checkFilesExist();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract void constructInterface(File directory);

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void dropFiles(Resource[] resources) {
    // Overwritten by the specific panel
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract File[] getAllSelectedFiles();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract File getCurrentDirectory();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getDescription() {

    String description = toString() + "\n\n" + Language.get("Description_DirectoryListPanel");

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
  public abstract DirectoryListPanel getNew();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract File getSelectedFile();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract void reload();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract void scrollToSelected();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract void setMatchFilter(FileFilter filter);

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public abstract void setMultipleSelection(boolean multi);

  /**
   **********************************************************************************************
   * Gets the name of the plugin
   * @return the name
   **********************************************************************************************
   **/
  @Override
  public String toString() {
    String nameCode = "DirectoryListPanel_" + code + "_Name";
    if (Language.has(nameCode)) {
      return Language.get(nameCode);
    }
    return code;
  }

}