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

/**
 **********************************************************************************************
 * A MexCom3 ScriptNode for command GoTo
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_GoTo extends ScriptNode {

  String variable;

  int fileNum;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_GoTo(String variable, String fileNum) {
    super("GoTo");
    this.variable = variable;
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

      long value = 0;
      if (fileNum > 0 && variable.equals("EOF")) {
        value = var.fm[fileNum].getLength();
      }
      else {
        value = getLong(variable);
      }

      var.fm[fileNum].seek(value);

      if (value > var.fm[fileNum].getLength()) {
        throw new ScriptException("The offset " + value + " is larger than the length of the file.");
      }

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
    return null;
  }

}