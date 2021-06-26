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

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
A wrapper for a MexCom3 script that allows an archive to be read and integrated with the normal
Game Extractor functionality.
**********************************************************************************************
**/
public class ScriptArchivePlugin_MexCom3 extends ScriptArchivePlugin {

  ScriptNode commandTree = null;

  File scriptFile = null;

  String author = "";

  String version = "1.0";

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptArchivePlugin_MexCom3() {
    super("MexCom3");
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptArchivePlugin_MexCom3(File scriptFile, String name) {
    super(name);
    if (scriptFile != null) {
      this.scriptFile = scriptFile;

      String extension = scriptFile.getName();
      int dotPos = extension.lastIndexOf(".");
      if (dotPos > 0) {
        extension = extension.substring(0, dotPos);
      }

      setExtensions(extension);

      loadWrapper(scriptFile);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getAuthor() {
    return author;
  }

  /**
  **********************************************************************************************
  Checks the script syntax for errors
  return <b>null</b> if there are no errors, otherwise a list of errors.  
  **********************************************************************************************
  **/
  public String checkScript() {
    // TODO
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

    if (extensions.length <= 0 || (extensions.length == 1 && extensions[0].equals(""))) {
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
    if (canRead) {
      description += "\n - " + Language.get("Description_ReadOperation");
    }
    if (canWrite) {
      description += "\n - " + Language.get("Description_WriteOperation");
    }
    if (canRename) {
      description += "\n - " + Language.get("Description_RenameOperation");
    }
    if (canReplace && !allowImplicitReplacing) {
      description += "\n - " + Language.get("Description_ReplaceOperation");
    }
    if (allowImplicitReplacing) {
      description += "\n - " + Language.get("Description_ImplicitReplaceOperation");
    }

    if (version != null && version.length() > 0) {
      description += "\n\n" + Language.get("Description_Version") + version;
    }

    if (author != null && author.length() > 0) {
      description += "\n\n" + Language.get("Description_Author") + author;
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
  Gets the percentage chance that this plugin can read the file <i>fm</i>
  @param fm the file to analyse
  @return the percentage (0-100) chance
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    //if (FilenameSplitter.getExtension(fm.getFile()) == extensions[0]){
    //  return 25;
    //  }
    return 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File getScriptFile() {
    return scriptFile;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getScript() {
    if (scriptFile != null) {
      FileManipulator fm = new FileManipulator(scriptFile, false);
      String scriptData = fm.readString((int) fm.getLength());
      fm.close();
      return scriptData;
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getVersion() {
    return version;
  }

  /**
  **********************************************************************************************
  Loads the <i>scriptFile</i>into the <i>commandTree</i>
  **********************************************************************************************
  **/
  public void loadScript(File file) {
    commandTree = ScriptManager_MexCom3.loadFile(file);

    // Now look for an XML wrapper.
    // If it exists, load the properties for this plugin. (games, extensions, etc);
    try {
      ScriptNode child = (ScriptNode) commandTree.getChild(0);
      if (child instanceof ScriptNode_MexCom3_XMLWrapper) {
        ScriptNode_MexCom3_XMLWrapper wrapper = (ScriptNode_MexCom3_XMLWrapper) child;

        setExtensions(wrapper.getExtensions());
        setGames(wrapper.getGames());
        setPlatforms(wrapper.getPlatforms());
        setVersion(wrapper.getVersion());
        setAuthor(wrapper.getAuthor());

      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadWrapper() {
    loadWrapper(scriptFile);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadWrapper(File file) {

    FileManipulator fm = new FileManipulator(file, false, 100);
    String line = fm.readLine();
    fm.close();

    if (line.length() > 0 && line.charAt(0) == '<') {
      try {
        ScriptNode_MexCom3_XMLWrapper wrapper = new ScriptNode_MexCom3_XMLWrapper(line);

        setExtensions(wrapper.getExtensions());
        setGames(wrapper.getGames());
        setPlatforms(wrapper.getPlatforms());
        setVersion(wrapper.getVersion());
        setAuthor(wrapper.getAuthor());
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
    else {
      setGames("");
      setPlatforms("");
      setVersion("");
      setAuthor("");
    }

  }

  /**
  **********************************************************************************************
  Reads the archive <i>source</i>
  @param source the archive file
  @return the resources in the archive
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("static-access")
  public Resource[] read(File source) {
    // the script isn't loaded
    //if (commandTree == null){
    loadScript(scriptFile); // always reload, in case the script changed since last run (such as the temp/test script)
    //  }

    // the script failed during loading
    if (commandTree == null) {
      return null;
    }

    ScriptNode.clearErrorCount();

    try {
      // run the script on the archive
      ScriptGlobals var = ScriptGlobals.getInstance();

      var.clearVariables();
      var.loadFile(source);

      commandTree.run();

      var.closeFilePointers();

      if (!ScriptNode.checkErrors()) {
        return null;
      }

      setCanImplicitReplace(var.isReplacable());

      var.resizeResources();
      return var.getResources();
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setAuthor(String author) {
    this.author = author;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setVersion(String version) {
    this.version = version;
  }

}