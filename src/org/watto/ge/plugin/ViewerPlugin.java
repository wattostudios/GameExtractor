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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.component.WSObjectPlugin;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.io.FileManipulator;

public abstract class ViewerPlugin extends WSObjectPlugin {

  /** quick access to the field validator **/
  static FieldValidator check = new FieldValidator();

  /**
   **********************************************************************************************
   * Records the error/exception stack trace in the log file. If debug is enabled, it will also
   * write the error to the <i>System.out</i> command prompt
   * @param t the <i>Throwable</i> error/exception
   **********************************************************************************************
   **/
  public static void logError(Throwable t) {
    ErrorLogger.log(t);
  }

  /** The default extension of archives complying to this format **/
  protected String[] extensions = new String[] { "" };

  /** The games that use this archive format **/
  protected String[] games = new String[] { "" };

  /** The platforms that this archive exists on (such as "PC", "XBox", or "PS2") **/
  protected String[] platforms = new String[] { "" };

  /** The name of this plugin **/
  String name = "Viewer Plugin";

  /** true if the format read by this plugin is a standard or commonly-used format, false if proprietary **/
  boolean standardFileFormat = false;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ViewerPlugin() {
    setCode("ViewerPlugin");
    setName("Viewer Plugin");
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ViewerPlugin(String code) {
    setCode(code);
    setName(code);
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ViewerPlugin(String code, String name) {
    setCode(code);
    setName(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract boolean canWrite(PreviewPanel panel);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {

    String description = toString() + "\n\n" + Language.get("Description_ViewerPlugin");

    if (games.length <= 0 || (games.length == 1 && games[0].equals(""))) {
      description += "\n\n" + Language.get("Description_NoDefaultGames");
    }
    else {
      description += "\n\n" + Language.get("Description_DefaultGames");

      for (int i = 0; i < games.length; i++) {
        description += "\n -" + games[i];
      }

    }

    if (platforms.length <= 0 || (platforms.length == 1 && platforms[0].equals(""))) {
      description += "\n\n" + Language.get("Description_NoDefaultPlatforms");
    }
    else {
      description += "\n\n" + Language.get("Description_DefaultPlatforms");

      for (int i = 0; i < platforms.length; i++) {
        description += "\n -" + platforms[i];
      }

    }

    if (extensions.length <= 0) {
      description += "\n\n" + Language.get("Description_NoDefaultExtensions");
    }
    else {
      description += "\n\n" + Language.get("Description_DefaultExtensions") + "\n";

      for (int i = 0; i < extensions.length; i++) {
        if (i > 0) {
          description += " *." + extensions[i];
        }
        else {
          description += "*." + extensions[i];
        }
      }

    }

    description += "\n\n" + Language.get("Description_SupportedOperations");
    if (canWrite(new PreviewPanel_Image())) { // Only supports telling people about ImageWriters
      description += "\n - " + Language.get("Description_WriteOperation");
    }

    if (isStandardFileFormat()) {
      description += "\n\n" + Language.get("Description_StandardFileFormat");
    }
    else {
      description += "\n\n" + Language.get("Description_NonStandardFileFormat");
    }

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
  public String getExtension(int num) {
    if (num < extensions.length) {
      return extensions[num];
    }
    else {
      return "unk";
    }
  }

  /**
  **********************************************************************************************
  Gets all the extensions
  @return the extensions
  **********************************************************************************************
  **/
  public String[] getExtensions() {
    return extensions;
  }

  /**
  **********************************************************************************************
  Gets a list of the extensions
  @return the list
  **********************************************************************************************
  **/
  public String getExtensionsList() {
    String list = "";

    for (int i = 0; i < extensions.length; i++) {
      if (i > 0) {
        list += ", ";
      }
      list += "*." + extensions[i];
    }

    return list;
  }

  /**
  **********************************************************************************************
  Gets all the games
  @return the games
  **********************************************************************************************
  **/
  public String[] getGames() {
    return games;
  }

  /**
  **********************************************************************************************
  Gets a list of the games
  @return the list
  **********************************************************************************************
  **/
  public String getGamesList() {
    String list = "";

    for (int i = 0; i < games.length; i++) {
      if (i > 0) {
        list += ", ";
      }
      list += games[i];
    }

    return list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getMatchRating(File file) {
    try {
      FileManipulator fm = new FileManipulator(file, false);
      int rating = getMatchRating(fm);
      fm.close();
      return rating;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract int getMatchRating(FileManipulator fm);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String[] getPlatforms() {
    return platforms;
  }

  /**
  **********************************************************************************************
  Gets a list of the platforms
  @return the list
  **********************************************************************************************
  **/
  public String getPlatformsList() {
    String list = "";

    for (int i = 0; i < platforms.length; i++) {
      if (i > 0) {
        list += ", ";
      }
      list += platforms[i];
    }

    return list;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public boolean isStandardFileFormat() {
    return standardFileFormat;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel read(File path) {
    try {
      FileManipulator fm = new FileManipulator(path, false);
      PreviewPanel panel = read(fm);
      fm.close();
      return panel;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  public abstract PreviewPanel read(FileManipulator source);

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  public ImageResource readThumbnail(FileManipulator source) {
    return null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setExtensions(String... extensions) {
    this.extensions = extensions;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setGames(String... games) {
    this.games = games;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setPlatforms(String... platforms) {
    this.platforms = platforms;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public void setStandardFileFormat(boolean standardFileFormat) {
    this.standardFileFormat = standardFileFormat;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void write(PreviewPanel panel, File destination) {
    try {
      FileManipulator fm = new FileManipulator(destination, true);
      write(panel, fm);
      fm.close();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      ;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void write(PreviewPanel panel, FileManipulator destination);

}