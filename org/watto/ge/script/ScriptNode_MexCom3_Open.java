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

import java.io.File;
import org.watto.ErrorLogger;

/**
**********************************************************************************************
A MexCom3 ScriptNode for command Open
**********************************************************************************************
**/
public class ScriptNode_MexCom3_Open extends ScriptNode {

  String path;

  int fileNum;

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  @SuppressWarnings("static-access")
  public ScriptNode_MexCom3_Open(String directory, String filename, String fileNum) {
    super("Open");
    if (directory.equals("FileDir")) {
      this.path = var.getString(directory) + File.separator + filename;
    }
    else if (directory.equals("FDDE")) {
      this.path = var.getString(directory);
      int dotPos = path.lastIndexOf(".");
      if (dotPos > 0) {
        path = path.substring(0, dotPos + 1) + filename;
      }
    }
    else {
      this.path = directory + File.separator + filename;
    }

    try {
      this.fileNum = Integer.parseInt(fileNum);
    }
    catch (Throwable t) {
      this.fileNum = -1;
    }
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

      var.loadAdditionalFile(new File(path));

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
    if (fileNum == -1) {
      return "Invalid File Number";
    }
    return null;
  }

}