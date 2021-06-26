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
import org.watto.io.converter.LongConverter;

/**
 **********************************************************************************************
 * A MexCom3 ScriptNode for command ReverseLong
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_ReverseLong extends ScriptNode {

  String variable;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_ReverseLong(String variable) {
    super("ReverseLong");
    this.variable = variable;
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

      int oldValue = (int) getLong(variable);
      long value = LongConverter.changeFormat(oldValue);
      if (value < 0) {
        value = (4294967296L + (int) value);
      }
      var.set(variable, new Long(value));
      var.set("REVERSE_" + variable, new Boolean(true));

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