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
 * A MexCom3 ScriptNode for command For
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_For extends ScriptNode {

  String variable;

  String startVal;

  String endVal;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_For(String variable, String startVal, String endVal) {
    super("For");
    this.variable = variable;
    this.startVal = startVal;
    this.endVal = endVal;
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

      int start = (int) getLong(startVal);
      int end = (int) getLong(endVal);

      if (start > end) {
        throw new ScriptException("The start value " + start + " is larger than the end value " + end + ".");
      }

      for (int i = start; i <= end; i++) {
        if (!checkErrors()) {
          return;
        }
        var.set(variable, new Long(i));
        runChildren();
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