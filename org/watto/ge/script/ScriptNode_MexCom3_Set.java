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
A MexCom3 ScriptNode for command Set
**********************************************************************************************
**/
public class ScriptNode_MexCom3_Set extends ScriptNode {

  String variable;

  String type;

  String value;

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptNode_MexCom3_Set(String variable, String type, String value) {
    super("Set");
    this.variable = variable;
    this.type = type;
    this.value = value;
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

      Object object = getObject(value);
      if (object == value && type.equals("String")) {
        if (value.length() > 0) {
          if (value.charAt(0) == '\"') {
            value = value.substring(1);
          }
          if (value.charAt(value.length() - 1) == '\"') {
            value = value.substring(0, value.length() - 1);
          }
          object = value;
        }
      }

      var.set(variable, object);

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