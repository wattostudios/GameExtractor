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

import org.watto.ErrorLogger;

/**
**********************************************************************************************
A MexCom3 ScriptNode for command Math
**********************************************************************************************
**/
public class ScriptNode_MexCom3_Math extends ScriptNode {

  String firstVariable;

  String secondVariable;

  String function;

  boolean isVariable = false;

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptNode_MexCom3_Math(String firstVariable, String function, String secondVariable) {
    super("Math");
    this.function = function;
    this.firstVariable = firstVariable;
    this.secondVariable = secondVariable;
  }

  /**
  **********************************************************************************************
  Runs the command
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

      if (function.equals("=")) {
        value = getLong(secondVariable);
      }
      else if (function.equals("+=")) {
        value = getLong(firstVariable) + getLong(secondVariable);
      }
      else if (function.equals("-=")) {
        value = getLong(firstVariable) - getLong(secondVariable);
      }
      else if (function.equals("*=")) {
        value = getLong(firstVariable) * getLong(secondVariable);
      }
      else if (function.equals("/=")) {
        value = getLong(firstVariable) / getLong(secondVariable);
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
    if (function.equals("=")) {
      return null;
    }
    else if (function.equals("+=")) {
      return null;
    }
    else if (function.equals("-=")) {
      return null;
    }
    else if (function.equals("*=")) {
      return null;
    }
    else if (function.equals("/=")) {
      return null;
    }
    else {
      return "Invalid Math function: " + function;
    }
  }

}