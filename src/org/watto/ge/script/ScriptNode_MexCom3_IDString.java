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
 * A MexCom3 ScriptNode for command IDString
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_IDString extends ScriptNode {

  String header;

  int fileNum;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_IDString(String fileNum, String header) {
    super("IDString");
    this.header = header;
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

      String value = var.fm[fileNum].readString(header.length());
      if (!(header.equals(value))) {
        throw new ScriptException("The header " + value + " is not the same as the expected value " + header + ".");
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