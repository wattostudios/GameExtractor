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
import org.watto.io.StringHelper;

/**
 **********************************************************************************************
 * A MexCom3 ScriptNode for command GetCT
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_GetCT extends ScriptNode {

  String variable;

  String type;

  String terminator;

  int fileNum;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_GetCT(String variable, String type, String terminator, String fileNum) {
    super("GetCT");
    this.variable = variable;
    this.type = type;
    this.terminator = terminator;
    this.fileNum = Integer.parseInt(fileNum);
  }

  /**
   **********************************************************************************************
   * Runs the command
   **********************************************************************************************
   **/
  @Override
  @SuppressWarnings("static-access")
  public void run() {
    try {
      if (!checkErrors()) {
        return;
      }

      String value = "";

      if (type.equals("Byte")) {
        int term = Integer.parseInt(terminator);
        value = StringHelper.readTerminatedString(var.fm[fileNum].getBuffer(), (byte) term);
      }
      else if (type.equals("String")) {
        int term = terminator.getBytes()[0];
        value = StringHelper.readTerminatedString(var.fm[fileNum].getBuffer(), (byte) term);
      }
      else {
        throw new ScriptException("The data type " + type + " is not valid.");
      }

      var.set(variable, value);
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
    if (type.equals("Byte")) {
      return null;
    }
    else if (type.equals("String")) {
      return null;
    }
    else {
      return "Invalid data type: " + type;
    }
  }

}