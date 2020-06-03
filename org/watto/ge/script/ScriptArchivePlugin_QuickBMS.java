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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_QuickBMSWrapper;
import org.watto.io.FileManipulator;
import org.watto.io.Hex;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
A wrapper for a MexCom3 script that allows an archive to be read and integrated with the normal
Game Extractor functionality.
**********************************************************************************************
**/
public class ScriptArchivePlugin_QuickBMS extends ScriptArchivePlugin {

  File scriptFile = null;

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptArchivePlugin_QuickBMS() {
    super("QuickBMS");
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptArchivePlugin_QuickBMS(File scriptFile, String name) {
    super(name);
    if (scriptFile != null) {
      this.scriptFile = scriptFile;

      // read the first few lines of the script, try to determine what games this script is used for
      loadWrapper(scriptFile);
    }
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

    /*
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
    */

    description += "\n\n" + Language.get("Description_SupportedOperations");
    if (canRead) {
      description += "\n - " + Language.get("Description_ReadOperation");
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
  read the first few lines of the script, try to determine what games this script is used for
  **********************************************************************************************
  **/
  public void loadWrapper(File file) {

    FileManipulator fm = new FileManipulator(scriptFile, false, 100);

    long arcSize = fm.getLength();
    while (fm.getOffset() < arcSize) {
      String scriptLine = fm.readLine();
      if (scriptLine == null || scriptLine.length() <= 10) { // minimum 10 chars needed for game name
        continue;
      }

      if (scriptLine.startsWith("#")) {
        // a comment
        //System.out.println(getName() + "\t" + scriptLine);
        if (scriptLine.contains("QuickBMS")) {
          // QuickBMS comment line - ignore it

          // *Actually*, want to stop here, the games are usually listed before this line
          break;
        }
        else {
          // strip off the comment and the space, then assume the rest is a game name
          if (scriptLine.length() > 2 && scriptLine.charAt(1) == ' ') {
            char char2 = scriptLine.charAt(2);
            if (char2 == ' ' || char2 == '?') {
              continue;
            }

            scriptLine = scriptLine.substring(2);

            if (scriptLine.startsWith("http")) {
              continue;
            }

            try {
              scriptLine = scriptLine.split("[/(?|_]", 2)[0]; // strip out funny characters etc
            }
            catch (Throwable t) {
            }

            setGames(scriptLine);
            //System.out.println(getName() + "\t" + scriptLine);
            break; // only grab the first matching line

          }
        }
      }
      else {
        // only want to scan through the comments at the top of the file - if we go past the comments, we're in to the actual script
        break;
      }

    }

    fm.close();

  }

  /**
  **********************************************************************************************
  Reads the archive <i>source</i>
  @param source the archive file
  @return the resources in the archive
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File source) {
    try {

      // get the QuickBMS Executable
      String quickbmsPath = QuickBMSHelper.checkAndShowPopup();
      if (quickbmsPath == null) {
        return null;
      }

      String scriptPath = scriptFile.getAbsolutePath();

      ProcessBuilder pb = new ProcessBuilder(quickbmsPath, "-l", scriptPath, source.getAbsolutePath());

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_ReadingArchive"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      Process convertProcess = pb.start();
      //int returnCode = convertProcess.waitFor(); // wait for QuickBMS to finish
      int returnCode = 0;
      BufferedReader outputReader = new BufferedReader(new InputStreamReader(convertProcess.getInputStream()));
      BufferedReader errorReader = new BufferedReader(new InputStreamReader(convertProcess.getErrorStream()));

      ExporterPlugin exporter = new Exporter_QuickBMSWrapper(scriptPath);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];

      if (returnCode == 0) {
        String outputLine = outputReader.readLine();

        while (outputLine != null) {
          // the line is structured like this...
          //   offset     length     name
          // ... need to split it up and convert it into Resources
          String[] splitLine = outputLine.trim().split("\\s+", 3);
          if (splitLine != null && splitLine.length == 3) {
            String offsetHex = splitLine[0];
            int offset = IntConverter.convertBig(new Hex(offsetHex));

            int length = 0;
            try {
              length = Integer.parseInt(splitLine[1]);
            }
            catch (Throwable t) {
            }

            String filename = splitLine[2];

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(source, filename, offset, length, length, exporter);
            realNumFiles++;
          }

          outputLine = outputReader.readLine();
        }

        // finished - return the resources
        TaskProgressManager.stopTask();

        if (realNumFiles == 0) {
          // there was probably a QuickBMS error - check it and report it
          String bmsError = "";

          outputLine = errorReader.readLine();
          while (outputLine != null) {
            bmsError += outputLine + "\n";
            outputLine = errorReader.readLine();
          }

          ErrorLogger.log(bmsError);

        }

        resources = resizeResources(resources, realNumFiles);
        return resources;

      }

      // Stop the task
      TaskProgressManager.stopTask();

      return null;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}