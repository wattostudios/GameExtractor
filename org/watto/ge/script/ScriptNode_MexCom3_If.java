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
 * A MexCom3 ScriptNode for command If
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_If extends ScriptNode {

  String variable;

  String function;

  String check;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_If(String variable, String function, String check) {
    super("If");
    this.variable = variable;
    this.function = function;
    this.check = check;
  }

  /**
   **********************************************************************************************
   * Gets the parameters of this IF statement. Used so that the associated ELSE statement can get
   * the params for comparison too
   * @returns the parameters of the IF statement, namely [0]=variable, [1]=function, [2]=check
   **********************************************************************************************
   **/
  public String[] getParams() {
    return new String[] { variable, function, check };
  }

  /**
   **********************************************************************************************
   * Runs the commandd
   **********************************************************************************************
   **/
  @Override
  @SuppressWarnings("static-access")
  public void run() {
    try {
      if (!checkErrors()) {
        return;
      }

      boolean success = false;

      Object checkVal = var.get(check);
      if (checkVal == null) {
        try {
          checkVal = new Long((String) checkVal);
        }
        catch (Throwable t) {
          checkVal = check;
        }
      }

      if (checkVal instanceof Long) {
        long checkLong = ((Long) checkVal).longValue();
        long varLong = var.getLong(variable);

        if (function.equals("=")) {
          success = (varLong == checkLong);
        }
        else if (function.equals("<")) {
          success = (varLong < checkLong);
        }
        else if (function.equals(">")) {
          success = (varLong > checkLong);
        }
        else if (function.equals("<=")) {
          success = (varLong <= checkLong);
        }
        else if (function.equals(">=")) {
          success = (varLong >= checkLong);
        }
        else if (function.equals("<>")) {
          success = (varLong != checkLong);
        }
        else {
          throw new ScriptException("The function " + function + " is not valid.");
        }

      }
      else if (checkVal instanceof String) {
        String checkString = (String) checkVal;
        String varString = var.getString(variable);

        if (function.equals("=")) {
          success = (varString.equals(checkString));
        }
        else {
          throw new ScriptException("The function " + function + " is not valid.");
        }

      }
      else {
        throw new ScriptException("The data type " + checkVal + " is not valid.");
      }

      if (success) {
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
    if (function.equals("=")) {
      return null;
    }
    else if (function.equals("<")) {
      return null;
    }
    else if (function.equals(">")) {
      return null;
    }
    else if (function.equals("<=")) {
      return null;
    }
    else if (function.equals(">=")) {
      return null;
    }
    else if (function.equals("<>")) {
      return null;
    }
    else {
      return "Invalid If function: " + function;
    }
  }

}