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
import org.watto.io.FileManipulator;

/**
 **********************************************************************************************
 * Class for parsing MexCom3 scripts (MultiEX Commander *.BMS scripts), converting them into a
 * ScriptNode tree for handling by GameExtractor
 **********************************************************************************************
 **/
public class ScriptManager_MexCom3 extends ScriptManager {

  static FileManipulator fm;

  /** any errors while reading the script in loadFile() will be added here **/
  static String[] scriptErrors = new String[20];

  static int errorCount = 0;

  /** the line number, when reading from a file **/
  static int linePos = 0;

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public static void addError(String error) {
    if (errorCount >= 20) {
      return; // too many errors
    }

    scriptErrors[errorCount] = "Line " + linePos + ": " + error;
    errorCount++;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public static String[] getScriptErrors() {
    if (errorCount == 0) {
      return new String[0];
    }
    else if (errorCount == 20) {
      return scriptErrors;
    }
    else {
      String[] shortErrors = new String[errorCount];
      System.arraycopy(scriptErrors, 0, shortErrors, 0, errorCount);
      return shortErrors;
    }
  }

  /**
   **********************************************************************************************
   * Builds a branch of the command tree (root, a loop, if/else, etc.)
   * @param root the node on which the branch is being built
   **********************************************************************************************
   **/
  public static void buildCommandBranch(ScriptNode root) {
    try {
      ScriptNode node;

      String line = fm.readLine();
      linePos++;

      while (line != null) {
        String[] params = line.split(" ");
        int numParams = params.length;
        if (numParams < 1) {
          // skip blank lines
          line = fm.readLine();
          linePos++;
          continue;
        }

        // lower case for consistency
        String tag = params[0].toLowerCase();

        // construct the node based on the tag name
        if (tag.equals("set")) {
          node = new ScriptNode_MexCom3_Set(params[1], params[2], params[3]);
          root.addChild(node);
        }
        else if (tag.equals("get")) {
          node = new ScriptNode_MexCom3_Get(params[1], params[2], params[3]);
          root.addChild(node);
        }
        else if (tag.equals("open")) {
          node = new ScriptNode_MexCom3_Open(params[1], params[2], params[3]);
          root.addChild(node);
        }
        else if (tag.equals("for")) {
          node = new ScriptNode_MexCom3_For(params[1], params[3], params[5]);
          root.addChild(node);
          buildCommandBranch(node);
        }
        else if (tag.equals("next")) {
          return;
        }
        else if (tag.equals("getdstring")) {
          node = new ScriptNode_MexCom3_GetDString(params[1], params[2], params[3]);
          root.addChild(node);
        }
        else if (tag.equals("idstring")) {
          node = new ScriptNode_MexCom3_IDString(params[1], params[2]);
          root.addChild(node);
        }
        else if (tag.equals("goto")) {
          node = new ScriptNode_MexCom3_GoTo(params[1], params[2]);
          root.addChild(node);
        }
        else if (tag.equals("math")) {
          node = new ScriptNode_MexCom3_Math(params[1], params[2], params[3]);
          root.addChild(node);
        }
        else if (tag.equals("log")) {
          node = new ScriptNode_MexCom3_Log(params[1], params[2], params[3], params[4], params[5]);
          root.addChild(node);
        }
        else if (tag.equals("savepos")) {
          node = new ScriptNode_MexCom3_SavePos(params[1], params[2]);
          root.addChild(node);
        }
        else if (tag.equals("imptype")) {
          node = new ScriptNode_MexCom3_ImpType(params[1]);
          root.addChild(node);
        }
        else if (tag.equals("getct")) {
          node = new ScriptNode_MexCom3_GetCT(params[1], params[2], params[3], params[4]);
          root.addChild(node);
        }
        else if (tag.equals("comtype")) {
          node = new ScriptNode_MexCom3_ComType(params[1]);
          root.addChild(node);
        }
        else if (tag.equals("clog")) {
          node = new ScriptNode_MexCom3_CLog(params[1], params[2], params[3], params[4], params[5], params[6], params[7]);
          root.addChild(node);
        }
        else if (tag.equals("findloc")) {
          node = new ScriptNode_MexCom3_FindLoc(params[1], params[2], params[3], params[4]);
          root.addChild(node);
        }
        else if (tag.equals("reverselong")) {
          node = new ScriptNode_MexCom3_ReverseLong(params[1]);
          root.addChild(node);
        }
        else if (tag.equals("if")) {
          node = new ScriptNode_MexCom3_If(params[1], params[2], params[3]);
          root.addChild(node);
          buildCommandBranch(node);
        }
        else if (tag.equals("else")) {
          String[] ifParams = ((ScriptNode_MexCom3_If) root).getParams();
          node = new ScriptNode_MexCom3_Else(ifParams[0], ifParams[1], ifParams[2]);
          ((ScriptNode) root.getParent()).addChild(node);
          buildCommandBranch(node);
          return; // so it stops the IF branch
        }
        else if (tag.equals("endif")) {
          return;
        }
        else if (tag.equals("do")) {
          node = new ScriptNode_MexCom3_Do();
          root.addChild(node);
          buildCommandBranch(node);
        }
        else if (tag.equals("while")) {
          //ScriptNode_MexCom3_Do doNode = (ScriptNode_MexCom3_Do)root.getLastChild();
          ScriptNode_MexCom3_Do doNode = (ScriptNode_MexCom3_Do) root;
          doNode.setParams(params[1], params[2], params[3]);
          return;
        }
        else if (tag.equals("cleanexit")) {
          //fm.seek(fm.getLength());
          //return;
        }
        else if (tag.equals("string")) {
          node = new ScriptNode_MexCom3_String(params[1], params[2], params[3]);
          root.addChild(node);
        }
        else if (tag.equals("#")) {
          // comment
        }
        else if (tag.equals("<bms")) {
          // XML wrapper (opening tag)
          node = new ScriptNode_MexCom3_XMLWrapper(line);
          root.addChild(node);
          buildCommandBranch(node);
        }
        else if (tag.equals("</bms>")) {
          // XML wrapper (closing tag)
          return;
        }
        else {
          // bad command
          addError("Unknown command: " + tag);
          return;
        }

        // add the node to its parent
        // now done in each individual step because some don't want to add the child normally
        //root.addChild(node);

        line = fm.readLine();
        linePos++;
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   * Loads the script <i>file</i> into a <i>ScriptNode</i> tree.
   * Any errors found while loading the script will be added to <i>scriptErrors</i>
   * @param file the script file to load
   * @return the tree of commands
   **********************************************************************************************
   **/
  public static ScriptNode loadFile(File file) {
    try {
      fm = new FileManipulator(file, false);

      errorCount = 0; // reset the error count
      linePos = 0; // as we're reading from the start of a file

      ScriptNode tree = new ScriptNode();
      buildCommandBranch(tree);

      //tree.printTree();

      fm.close();

      return tree;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptManager_MexCom3() {
  }

}