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
 * A MexCom3 ScriptNode for command String
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_String extends ScriptNode {

  String firstVariable;

  String secondVariable;

  String function;

  boolean isVariable = false;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_String(String firstVariable, String function, String secondVariable) {
    super("String");
    this.function = function;
    this.firstVariable = firstVariable;
    this.secondVariable = secondVariable;
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

      String firstString = var.getString(firstVariable);
      String secondString = var.getString(secondVariable);

      if (secondString.equals("")) {
        // maybe we're trying to add a number to the string, so check for this
        long secondLong = var.getLong(secondVariable);
        if (secondLong != -1) {
          secondString = "" + secondLong;
        }
      }

      String value = firstString;

      if (function.equals("+=")) {
        value = firstString + secondString;
      }
      else if (function.equals("-=")) {
        value = firstString.replaceAll(secondString, "");
      }
      else {
        throw new ScriptException("The function " + function + " is not valid.");
      }

      var.set(firstVariable, value);

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
    if (function.equals("+=")) {
      return null;
    }
    else if (function.equals("-=")) {
      return null;
    }
    else {
      return "Invalid String function: " + function;
    }
  }

}