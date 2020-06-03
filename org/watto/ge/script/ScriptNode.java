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

/**
**********************************************************************************************
The ScriptNode class is an object that allows a group of commands to be run sequentially, and
heirachically. Simply, ScriptNode is a treeable object (it can have children), where each
ScriptNode in the tree has a function associated with it.
<br><br>
The ScriptNode is designed to be extended for every unique function that you wish to perform,
simply by overwriting the run() method to contain the actual function code.
<br><br>
To run the tree, you simply run the first ScriptNode, and all its children are also run. If you
set up the functions of a particular ScriptNode correctly, you can also introduce iterations
that repeat over the children multiple times, and you can easily set up a singleton object for
sharing information between each node in the tree.
<br><br>
The ScriptNode has an XMLNode as its superclass, so it contains all the same processing and
functionality as a regular XMLNode, such as attributes. You can also quickly and easily
adjust the XMLReader and XMLWriter classes to construct a ScriptNode tree from an XML file.
**********************************************************************************************
**/

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.ge.helper.FieldValidator;
import org.watto.xml.XMLNode;

public class ScriptNode extends XMLNode {

  /** A singleton class for sharing information between the other ScriptNodes **/
  public static ScriptGlobals var = ScriptGlobals.getInstance();

  /** the number of errors that occur when running the script **/
  public static int errorCount = 0;

  public static int maxAllowedErrors = 10;

  /** quick access to the field validator **/
  static FieldValidator check = new FieldValidator();

  /**
   **********************************************************************************************
   * Checks the number of errors
   * @return true if the script is OK, false if there are too many errors
   **********************************************************************************************
   **/
  public static boolean checkErrors() {
    return (errorCount < maxAllowedErrors);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void clearErrorCount() {
    errorCount = 0;
    maxAllowedErrors = Settings.getInt("MaxAllowedScriptErrors");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static int getErrorCount() {
    return errorCount;
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode() {
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public ScriptNode(String tag) {
    super(tag);
  }

  /**
   **********************************************************************************************
   * Gets a <i>long</i> value from the singleton <i>var</i>
   * @param variable the name of the long to retrieve
   * @return the long
   **********************************************************************************************
   **/
  @SuppressWarnings("static-access")
  public long getLong(String variable) {
    long value = var.getLong(variable);
    //System.out.println("Looking for " + variable);
    if (value == -1) {
      try {
        return Long.parseLong(variable);
      }
      catch (Throwable t) {
        //var.printVariableList();
        ErrorLogger.log("SCRIPT", t);
        errorCount++;
      }
    }
    return value;
  }

  /**
   **********************************************************************************************
   * Gets an <i>Object</i> value from the singleton <i>var</i>
   * @param variable the name of the Object to retrieve
   * @return the Object
   **********************************************************************************************
   **/
  @SuppressWarnings("static-access")
  public Object getObject(String variable) {
    Object value = var.get(variable);
    if (value == null) {
      return variable;
    }
    return value;
  }

  /**
   **********************************************************************************************
   * Gets a <i>String</i> value from the singleton <i>var</i>
   * @param variable the name of the String to retrieve
   * @return the String
   **********************************************************************************************
   **/
  @SuppressWarnings("static-access")
  public String getString(String variable) {
    String value = var.getString(variable);
    if (value == null || value.equals("")) {
      return variable;
    }
    return value;
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

  /**
   **********************************************************************************************
   * Runs the function of this ScriptNode
   **********************************************************************************************
   **/
  public void run() {
    if (!checkErrors()) {
      return;
    }

    runChildren();
  }

  /**
   **********************************************************************************************
   * Calls the run() method of each child ScriptNode
   **********************************************************************************************
   **/
  public void runChildren() {
    try {
      int numChildren = getChildCount();

      for (int i = 0; i < numChildren; i++) {
        //System.out.println(getChild(i).getTag());
        ((ScriptNode) getChild(i)).run();
      }

    }
    catch (Throwable t) {
      t.printStackTrace();
    }

  }

}