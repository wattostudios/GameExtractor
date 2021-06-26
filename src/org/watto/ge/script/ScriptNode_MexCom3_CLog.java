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
import javax.script.ScriptException;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.ReplaceDetails;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ExporterPlugin;

/**
 **********************************************************************************************
 * A MexCom3 ScriptNode for command CLog
 **********************************************************************************************
 **/
public class ScriptNode_MexCom3_CLog extends ScriptNode {

  String v_offset = "0";

  String v_length = "0";

  String v_decompLength = "0";

  String v_offsetOffset = "0";

  String v_lengthOffset = "0";

  String v_decompLengthOffset = "0";

  String v_name = "";

  /**
   **********************************************************************************************
   Checks the syntax of this command for any errors.
   return <b>null</b> if no errors, otherwise returns the error message
   **********************************************************************************************
   **/
  public String checkSyntax() {
    return null;
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode_MexCom3_CLog(String name, String offset, String length, String offsetOffset, String lengthOffset, String decompLength, String decompLengthOffset) {
    super("CLog");
    this.v_name = name;
    this.v_offset = offset;
    this.v_length = length;
    this.v_offsetOffset = offsetOffset;
    this.v_lengthOffset = lengthOffset;
    this.v_decompLength = decompLength;
    this.v_decompLengthOffset = decompLengthOffset;
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

      File file = var.getFile();
      ExporterPlugin exporter = var.getExporter();

      String name = getString(v_name);
      if (name.equals("0") || name.equals("\"\"") || name.equals(v_name)) {
        name = Resource.generateFilename(var.getFileNum());
      }

      long offset = getLong(v_offset);
      long length = getLong(v_length);
      long offsetOffset = getLong(v_offsetOffset);
      long lengthOffset = getLong(v_lengthOffset);
      long decompLength = getLong(v_decompLength);
      long decompLengthOffset = getLong(v_decompLengthOffset);

      // getting the endian order for this field value
      Object offsetEndianObject = ScriptGlobals.getVariable("REVERSE_" + v_offset);
      boolean offsetEndian = true;
      if (offsetEndianObject != null && ((Boolean) offsetEndianObject).booleanValue()) {
        offsetEndian = false; // has been reversed, therefore is BigEndian, and needs to be set to false
      }

      // getting the endian order for this field value
      Object lengthEndianObject = ScriptGlobals.getVariable("REVERSE_" + v_length);
      boolean lengthEndian = true;
      if (lengthEndianObject != null && ((Boolean) lengthEndianObject).booleanValue()) {
        lengthEndian = false; // has been reversed, therefore is BigEndian, and needs to be set to false
      }

      // getting the endian order for this field value
      Object decompEndianObject = ScriptGlobals.getVariable("REVERSE_" + v_decompLength);
      boolean decompEndian = true;
      if (decompEndianObject != null && ((Boolean) decompEndianObject).booleanValue()) {
        decompEndian = false; // has been reversed, therefore is BigEndian, and needs to be set to false
      }

      if (Settings.getBoolean("CheckScriptResourceValues")) {
        // checking for invalid entries (negatives, etc.)
        long arcSize = var.fm[0].getLength();
        try {
          FieldValidator.checkOffset(offset, arcSize);
        }
        catch (Throwable t) {
          ErrorLogger.log("SCRIPT", new ScriptException("The resource " + name + " has an invalid offset."));
          errorCount++;
        }
        try {
          FieldValidator.checkOffset(offsetOffset, arcSize);
        }
        catch (Throwable t) {
          ErrorLogger.log("SCRIPT", new ScriptException("The resource " + name + " has an invalid offset offset."));
          errorCount++;
        }
        try {
          FieldValidator.checkOffset(lengthOffset, arcSize);
        }
        catch (Throwable t) {
          ErrorLogger.log("SCRIPT", new ScriptException("The resource " + name + " has an invalid compressed length offset."));
          errorCount++;
        }
        try {
          FieldValidator.checkLength(length, arcSize);
        }
        catch (Throwable t) {
          ErrorLogger.log("SCRIPT", new ScriptException("The resource " + name + " has an invalid compressed length"));
          errorCount++;
        }
        try {
          FieldValidator.checkOffset(decompLengthOffset, arcSize);
        }
        catch (Throwable t) {
          ErrorLogger.log("SCRIPT", new ScriptException("The resource " + name + " has an invalid decompressed length offset."));
          errorCount++;
        }
        try {
          FieldValidator.checkLength(decompLength);
        }
        catch (Throwable t) {
          ErrorLogger.log("SCRIPT", new ScriptException("The resource " + name + " has an invalid decompressed length."));
          errorCount++;
        }
      }

      if (var.isReplacable()) {
        int offsetLength = 4;
        int lengthLength = 4;
        int decompLengthLength = 4;

        if (getObject(v_offset) instanceof Integer) {
          offsetLength = 2;
        }
        if (getObject(v_length) instanceof Integer) {
          lengthLength = 2;
        }
        if (getObject(v_decompLength) instanceof Integer) {
          decompLengthLength = 2;
        }

        //path,name,offset,length,decompLength,exporter
        ReplaceDetails replaceOffset = new ReplaceDetails("Offset", offsetOffset, offsetLength, offset, offsetEndian);
        ReplaceDetails replaceLength = new ReplaceDetails("Length", lengthOffset, lengthLength, length, lengthEndian);
        ReplaceDetails replaceDecomp = new ReplaceDetails("Decompressed", decompLengthOffset, decompLengthLength, decompLength, decompEndian);
        var.addResource(new ReplacableResource(file, name, exporter, replaceOffset, replaceLength, replaceDecomp));
      }
      else {
        //path,name,offset,length,decompLength,exporter
        var.addResource(new Resource(file, name, offset, length, decompLength, exporter));
      }

    }
    catch (Throwable t) {
      ErrorLogger.log("SCRIPT", t);
      errorCount++;
    }
  }

}