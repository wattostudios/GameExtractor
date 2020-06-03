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
 * A MexCom3 ScriptNode for command Do
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_Do extends ScriptNode {

  String variable;

  String function;

  String check;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_Do() {
    super("Do");
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

      boolean success = true;

      while (success) {
        if (!checkErrors()) {
          return;
        }

        success = false;

        Object checkVal = var.get(check);
        if (checkVal == null) {
          try {
            checkVal = new Long((String) checkVal);
          }
          catch (Throwable t) {
            checkVal = check;
          }
        }

        if (checkVal instanceof String) {
          // maybe it is actually a hardcoded number
          try {
            checkVal = new Long(Long.parseLong(check));
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
          else if (function.equals("<>")) {
            success = !(varString.equals(checkString));
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
      return "Invalid Do function: " + function;
    }
  }

  /**
   **********************************************************************************************
   * Sets the parameters of this DO command once the WHILE command has been reached (as the WHILE
   * command has the conditions in it to determine how long to repeat the loop)
   **********************************************************************************************
   **/
  public void setParams(String variable, String function, String check) {
    this.variable = variable;
    this.function = function;
    this.check = check;
  }

}