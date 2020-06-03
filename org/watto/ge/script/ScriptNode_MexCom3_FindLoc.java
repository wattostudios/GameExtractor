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

import javax.script.ScriptException;
import org.watto.ErrorLogger;
import org.watto.io.PatternFinder;

/**
 **********************************************************************************************
 * A MexCom3 ScriptNode for command FindLoc
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_FindLoc extends ScriptNode {

  String variable;

  String type;

  String search;

  int fileNum;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_FindLoc(String variable, String type, String search, String fileNum) {
    super("FindLoc");
    this.variable = variable;
    this.type = type;
    this.search = search;
    this.fileNum = Integer.parseInt(fileNum);
  }

  /**
   **********************************************************************************************
   * Runs the command
   **********************************************************************************************
   **/
  @SuppressWarnings("static-access")
  @Override
  public void run() {
    try {
      if (!checkErrors()) {
        return;
      }

      long value = 0;

      if (type.equals("String")) {
        value = new PatternFinder(var.fm[fileNum]).find(search);
      }
      else {
        throw new ScriptException("The search type " + type + " is not valid.");
      }

      var.set(variable, new Long(value));

    }
    catch (Throwable t) {
      ErrorLogger.log("SCRIPT", t);
      errorCount++;
    }
  }

  /**
   **********************************************************************************************
   Checks the syntax of this command for any errors.
   return <b>null</b> if no errors, otherwise returns the error message
   **********************************************************************************************
   **/
  public String checkSyntax() {
    if (type.equals("String")) {
      return null;
    }
    else {
      return "Invalid search type: " + type;
    }
  }

}