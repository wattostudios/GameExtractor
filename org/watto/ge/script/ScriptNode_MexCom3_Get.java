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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
 **********************************************************************************************
 * A MexCom3 ScriptNode for command Get
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_Get extends ScriptNode {

  String variable;

  String type;

  int fileNum;

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_Get(String variable, String type, String fileNum) {
    super("Get");
    this.variable = variable;
    this.type = type;
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

      if (type.equals("Long")) {
        long value = var.fm[fileNum].readInt();
        if (value < 0) {
          value = (4294967296L + (int) value);
        }
        var.set(variable, new Long(value));

        //System.out.println("Long: " + variable + " = " + value);
      }
      else if (type.equals("Int")) {
        int value = var.fm[fileNum].readShort();
        if (value < 0) {
          value = (65536 + (short) value);
        }
        var.set(variable, new Integer(value));
      }
      else if (type.equals("Byte")) {
        int value = ByteConverter.unsign(var.fm[fileNum].readByte());
        var.set(variable, new Integer(value));
      }
      else if (type.equals("String")) {
        String value = var.fm[fileNum].readNullString();
        var.set(variable, value);
      }
      else if (type.equals("ThreeByte")) {
        byte[] bytes = new byte[] { var.fm[fileNum].readByte(), var.fm[fileNum].readByte(), var.fm[fileNum].readByte(), 0 };
        int value = IntConverter.convertLittle(bytes);
        var.set(variable, new Integer(value));
      }
      else if (type.equals("ASize")) {
        //value = new Integer(var.fm[fileNum].readByteU());
        //var.set(variable,value);
        throw new ScriptException("The type " + type + " is not supported by Game Extractor.");
      }
      else {
        throw new ScriptException("The data type " + type + " is not valid.");
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
    if (type.equals("Long")) {
      return null;
    }
    else if (type.equals("Int")) {
      return null;
    }
    else if (type.equals("Byte")) {
      return null;
    }
    else if (type.equals("String")) {
      return null;
    }
    else if (type.equals("ThreeByte")) {
      return null;
    }
    else if (type.equals("ASize")) {
      return "The type ASize is not supported by Game Extractor.";
    }
    else {
      return "Invalid data type: " + type;
    }
  }

}