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

import java.io.BufferedInputStream;
import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.component.WSPluginGroup;
import org.watto.component.WSPluginManager;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.StringBuffer;

/**
**********************************************************************************************
A class that handles the construction and processing of a script file
**********************************************************************************************
**/
public abstract class ScriptManager {

  /**
  **********************************************************************************************
  Loads the scripts
  **********************************************************************************************
  **/
  public static void loadScripts() {
    // get/create the group for these plugins
    WSPluginGroup group = WSPluginManager.getGroup("Script");
    if (group == null) {
      //WSPluginManager.addPrefix("Script_","Script");
      WSPluginManager.addGroup("Script");
      group = WSPluginManager.getGroup("Script");
      if (group == null) {
        // could not create the group for some reason
        return;
      }
    }

    loadScripts(new File(Settings.get("ScriptsDirectory")), group);
  }

  /**
  **********************************************************************************************
  Loads scripts from a directory
  **********************************************************************************************
  **/
  public static void loadScripts(File directory, WSPluginGroup group) {
    // load the scripts into plugins, and into the group
    File[] scriptFiles = directory.listFiles();

    for (int i = 0; i < scriptFiles.length; i++) {
      if (scriptFiles[i].isDirectory()) {
        loadScripts(scriptFiles[i], group);
        continue;
      }

      String name = scriptFiles[i].getName();
      if (name.indexOf(".bms") > 0) {
        name = name.substring(0, name.length() - 4);

        analyseBMS(scriptFiles[i], name, group);
      }
      else if (name.indexOf(".zip") > 0) {
        loadScriptsFromZip(scriptFiles[i], group);
      }

    }

  }

  public static int SCRIPT_MEXCOM3 = 1;

  public static int SCRIPT_QUICKBMS = 2;

  public static int SCRIPT_UNKNOWN = -1;

  /**
  **********************************************************************************************
  Determines whether a BMS script is MexCom3 or QuickBMS
  **********************************************************************************************
  **/
  public static int analyseBMS(String scriptData) {
    StringBuffer buffer = new StringBuffer(scriptData);
    FileManipulator fm = new FileManipulator(buffer);

    int type = analyseBMS(fm);

    fm.close();

    return type;

  }

  /**
  **********************************************************************************************
  Determines whether a BMS script is MexCom3 or QuickBMS
  **********************************************************************************************
  **/
  public static int analyseBMS(File filePath) {
    FileManipulator fm = new FileManipulator(filePath, false);

    int type = analyseBMS(fm);

    fm.close();

    return type;

  }

  /**
  **********************************************************************************************
  Determines whether a BMS script is MexCom3 or QuickBMS, and then adds the relevant plugin to 
  the group.
  **********************************************************************************************
  **/
  public static void analyseBMS(File scriptFile, String name, WSPluginGroup group) {

    FileManipulator fm = new FileManipulator(scriptFile, false);
    int type = analyseBMS(fm);
    fm.close();

    if (type == SCRIPT_MEXCOM3 || type == SCRIPT_UNKNOWN) {
      addMexCom3(scriptFile, name, group);
    }
    else if (type == SCRIPT_QUICKBMS) {
      addQuickBMS(scriptFile, name, group);
    }

  }

  /**
  **********************************************************************************************
  Determines whether a BMS script is MexCom3 or QuickBMS
  @return SCRIPT_QUICKBMS or SCRIPT_MEXCOM3
  **********************************************************************************************
  **/
  private static int analyseBMS(FileManipulator fm) {

    long arcSize = fm.getLength();

    if (arcSize > 100000) {// too large
      ErrorLogger.log("[ScriptManager] Skipping script file " + fm.getFile().getName() + " becasue it's too large");
      return SCRIPT_UNKNOWN;
    }

    while (fm.getOffset() < arcSize) {
      String scriptLine = fm.readLine();
      if (scriptLine == null || scriptLine.length() <= 0) {
        continue;
      }

      if (scriptLine.startsWith("<bms")) {
        // MexCom3
        return SCRIPT_MEXCOM3;
      }
      else if (scriptLine.startsWith("#")) {
        // a comment
        if (scriptLine.contains("QuickBMS")) {
          // Comment indicates it's a QuickBMS script
          return SCRIPT_QUICKBMS;
        }
      }
      else {
        // if this is a real script line, process it and look for things that are QuickBMS-specific, or things that are't supported by MexCom3
        scriptLine = scriptLine.trim();

        String[] tagSplit = scriptLine.split("\\s+", 2);
        String tag = tagSplit[0].toLowerCase();

        if (tag.equals("quickbmsver") || tag.equals("append") || tag.equals("break") || tag.equals("calldll") || tag.equals("callfunction") || tag.equals("codepage") || tag.equals("continue") || tag.equals("debug") || tag.equals("encryption") || tag.equals("endfunction") || tag.equals("endian") || tag.equals("filecrypt") || tag.equals("filerot") || tag.equals("filexor") || tag.equals("getarray") || tag.equals("getbits") || tag.equals("getvarchr") || tag.equals("include") || tag.equals("label") || tag.equals("namecrc") || tag.equals("padding") || tag.equals("print") || tag.equals("put") || tag.equals("putarray") || tag.equals("putbits") || tag.equals("putct") || tag.equals("putdstring") || tag.equals("putvarchr") || tag.equals("reverselonglong") || tag.equals("reverseshort") || tag.equals("slog") || tag.equals("scandir") || tag.equals("sortarray") || tag.equals("startfunction") || tag.equals("strlen") || tag.equals("xmath") || tag.equals("elif")) {
          // QuickBMS-specific tags
          return SCRIPT_QUICKBMS;
        }
        else if (tag.equals("imptype")) {
          // MexCom3-specific tags
          return SCRIPT_MEXCOM3;
        }
        else {
          // the tag is supported for both script types - check if there's something in the variables that make it only for QuickBMS (eg a compression type other than ZLib)
          if (tagSplit.length == 2) {
            String variableLine = tagSplit[1].toLowerCase();

            if (tag.equals("comtype")) {
              // comtype ALGO [DICT] [DICT_SIZE]
              // MexCom3 only supports the algorithm "Zlib1"
              if (!variableLine.contains("zlib1")) {
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("get")) {
              // get VAR TYPE [FILENUM]
              // MexCom3 only supports types Long, Int, Byte, ThreeByte, String
              if (variableLine.contains("long") || variableLine.contains("int") || variableLine.contains("byte") || variableLine.contains("threebyte") || variableLine.contains("string")) {
                // Could be either
              }
              else {
                // a type that MexCom3 doesn't understand
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("math")) {
              // math VAR OP VAR
              // MexCom3 only supports operators = += *= /= -=
              if (variableLine.contains("=")) {
                // Could be either
              }
              else {
                // an operator that MexCom3 doesn't understand
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("findloc")) {
              // findloc VAR TYPE STRING [FILENUM] [ERR_VALUE] [END_OFF]
              // MexCom3 only supports the TYPE as being STRING
              if (!variableLine.contains("string")) {
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("goto")) {
              // goto OFFSET [FILENUM] [TYPE]
              // QuickBMS has an optional [TYPE] at the end
              if (variableLine.endsWith("seek_set") || variableLine.endsWith("seek_cur") || variableLine.endsWith("seek_end")) {
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("set")) {
              // set VAR [TYPE] VAR
              // QuickBMS has some optional special types
              if (variableLine.contains("unicode") || variableLine.contains("to_unicode") || variableLine.contains("binary") || variableLine.contains("alloc") || variableLine.contains("filename") || variableLine.contains("basename") || variableLine.contains("extension") || variableLine.contains("unknown") || variableLine.contains("strlen")) {
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("string")) {
              // string VAR OP VAR
              // MexCom3 only supports += and -= operations
              if (variableLine.contains("=")) {
                // Could be either
              }
              else {
                // an operator that MexCom3 doesn't understand
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("if") || tag.equals("while")) {
              // if VAR COND VAR [...]
              // while VAR COND VAR
              // MexCom3 only supports = < > <= >= <> operations
              if (variableLine.contains("=") || variableLine.contains("<") || variableLine.contains(">")) {
                // Could be either
              }
              else {
                // an operator that MexCom3 doesn't understand
                return SCRIPT_QUICKBMS;
              }
            }
            else if (tag.equals("for")) {
              // for [VAR] [OP] [VALUE] [COND] [VAR]
              // MexCom3 basically only supports = (for the OP) and TO (for the COND), QuickBMS supports heaps more in both cases
              if (variableLine.contains("TO")) {
                // Could be either (eg, "TO" could be the operator in MexCom3, or in the variable of QuickBMS).
                // However, it is REQUIRED in MexCom3
              }
              else {
                // an operator that MexCom3 doesn't understand
                return SCRIPT_QUICKBMS;
              }
            }
            /*
            // These are basically identical in both, so can't be used for comparison 
            cleanexit
            do
            else
            endif
            idstring [FILENUM] STRING
            reverselong VAR
            savepos VAR [FILENUM]
            
            // these we'll just ignore - should hopefully have an answer before this point
            clog NAME OFFSET ZSIZE SIZE [FILENUM] [XSIZE]
            log NAME OFFSET SIZE [FILENUM] [XSIZE]
            next [VAR] [OP] [VALUE]
            open FOLDER NAME [FILENUM] [EXISTS]
            getct VAR TYPE CHAR [FILENUM]
            getdstring VAR LENGTH [FILENUM]
            */
          }

        }
      }
    }

    // MEXCOM3 fallback
    return SCRIPT_UNKNOWN;
  }

  /**
  **********************************************************************************************
  Adds a MexCom3 script to the group
  **********************************************************************************************
  **/
  public static void addMexCom3(File scriptFile, String name, WSPluginGroup group) {
    group.addPlugin(new ScriptArchivePlugin_MexCom3(scriptFile, name));
    System.out.println("Added MexCom3 Script: " + name);
  }

  /**
  **********************************************************************************************
  Adds a QuickBMS script to the group
  **********************************************************************************************
  **/
  public static void addQuickBMS(File scriptFile, String name, WSPluginGroup group) {
    group.addPlugin(new ScriptArchivePlugin_QuickBMS(scriptFile, name));
    //System.out.println("Added QuickBMS Script: " + name);
  }

  /**
  **********************************************************************************************
  Loads scripts from a zip file
  **********************************************************************************************
  **/
  public static void loadScriptsFromZip(File zip, WSPluginGroup group) {
    try {
      ZipFile zipFile = new ZipFile(zip);
      Enumeration<? extends ZipEntry> files = zipFile.entries();

      String tempDir = Settings.get("TempDirectory");

      while (files.hasMoreElements()) {
        ZipEntry entry = files.nextElement();

        String name = entry.getName();
        if (name.indexOf(".bms") > 0) {
          name = name.substring(0, name.length() - 4);

          BufferedInputStream source = new BufferedInputStream(zipFile.getInputStream(entry));
          long length = entry.getSize();

          File outputPath = new File(tempDir + File.separator + "scripts" + File.separator + entry.getName());

          FileManipulator destination = new FileManipulator(outputPath, true);
          outputPath = destination.getFile();
          for (int i = 0; i < length; i++) {
            destination.writeByte(source.read());
          }
          destination.close();

          //group.addPlugin(new ScriptArchivePlugin_MexCom3(outputPath, name));
          //System.out.println("Analyzing Script " + name);
          analyseBMS(outputPath, name, group);
        }
      }

      zipFile.close();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptManager() {
  }

}
