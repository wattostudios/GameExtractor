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
import java.util.Enumeration;
import java.util.Hashtable;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
A singleton class that allows the sharing of information between ScriptNodes
**********************************************************************************************
**/
public class ScriptGlobals {

  /** The singleton instance **/
  static ScriptGlobals instance = new ScriptGlobals();

  /** The resources **/
  static Resource[] resources;
  /** The properties, each with a key (name) and value **/
  static Hashtable<String, Object> variables;
  /** The number of files in <i>resources</i> **/
  static int filenum;
  /** An array of files opened for read access **/
  public static FileManipulator[] fm = new FileManipulator[0];
  /** The main file, which is also in fm[0] **/
  static File file;
  /** Can these resources be replaced? **/
  static boolean replacable;
  /** Are these resources compressed? **/
  static boolean compressed;
  /** What exporter is used for these resources? **/
  static ExporterPlugin exporter;

  /**
  **********************************************************************************************
  Adds a resource into the <i>resources[]</i> array
  @param resource the resource to add
  **********************************************************************************************
  **/
  public static void addResource(Resource resource) {
    resources[filenum] = resource;
    filenum++;
    set("logEntries", new Long(filenum)); // MexCom3 Parameter
  }

  /**
  **********************************************************************************************
  Resets the global variables back to their default values
  **********************************************************************************************
  **/
  public static void clearVariables() {

    // Close any open files
    for (int i = 0; i < fm.length; i++) {
      try {
        fm[i].close();
      }
      catch (Throwable t) {
      }
    }

    resources = new Resource[Archive.getMaxFiles()];
    variables = new Hashtable<String, Object>();
    filenum = 0;
    fm = new FileManipulator[1];
    file = null;
    replacable = false;
    compressed = false;
    exporter = Exporter_Default.getInstance();
  }

  /**
  **********************************************************************************************
  Closes the open file pointers
  **********************************************************************************************
  **/
  public static void closeFilePointers() {

    // Close any open files
    for (int i = 0; i < fm.length; i++) {
      try {
        fm[i].close();
      }
      catch (Throwable t) {
      }
    }

  }

  /**
  **********************************************************************************************
  Gets the value of the property with key <i>key</i>
  @param key the property name
  @return the value of the property
  **********************************************************************************************
  **/
  public static Object get(String key) {
    // IF THIS VARIABLE IS NEVER USED, REMOVE IT FROM HERE (faster)
    if (key.equals("BytesRead")) {
      try {
        return new Long(fm[0].getOffset());
      }
      catch (Throwable t) {
        return new Long(-1);
      }
    }
    return variables.get(key);
  }

  /**
  **********************************************************************************************
  Gets the value of <i>exporter</i>
  @return the exporter
  **********************************************************************************************
  **/
  public static ExporterPlugin getExporter() {
    return exporter;
  }

  /**
  **********************************************************************************************
  Gets the <i>file</i>
  @return the file
  **********************************************************************************************
  **/
  public static File getFile() {
    return file;
  }

  /**
  **********************************************************************************************
  Gets the number of resources in <i>resources[]</i> (not the length of the array!)
  @return the number of resources
  **********************************************************************************************
  **/
  public static int getFileNum() {
    return filenum;
  }

  /**
  **********************************************************************************************
  Gets the singleton instance of this class
  @return the <i>instance</i>
  **********************************************************************************************
  **/
  public static ScriptGlobals getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  Gets the value of the property with key <i>key</i>, as a <i>long</i>
  @param key the property name
  @return the value of the property
  **********************************************************************************************
  **/
  public static long getLong(String key) {
    try {
      //return ((Long)variables.get(key)).longValue();
      Object object = variables.get(key);
      if (object instanceof Long) {
        return ((Long) object).longValue();
      }
      else if (object instanceof String) {
        return Long.parseLong((String) object);
      }
      else if (object instanceof Integer) {
        return ((Integer) object).intValue();
      }
      else {
        return -1;
      }
    }
    catch (Throwable t) {
      return -1;
    }
  }

  /**
  **********************************************************************************************
  Gets the <i>resources[]</i> array
  @return the resources
  **********************************************************************************************
  **/
  public static Resource[] getResources() {
    return resources;
  }

  /**
  **********************************************************************************************
  Gets the value of the property with key <i>key</i>, as a <i>String</i>
  @param key the property name
  @return the value of the property
  **********************************************************************************************
  **/
  public static String getString(String key) {
    try {
      return (String) variables.get(key);
    }
    catch (Throwable t) {
      return "";
    }
  }

  /**
  **********************************************************************************************
  Gets the value of the property with key <i>key</i>
  @param key the property name
  @return the value of the property
  **********************************************************************************************
  **/
  public static Object getVariable(String key) {
    return get(key);
  }

  /**
  **********************************************************************************************
  Gets the value of <i>compressed</i>
  @return true if this is compressed, false otherwise
  **********************************************************************************************
  **/
  public static boolean isCompressed() {
    return compressed;
  }

  /**
  **********************************************************************************************
  Gets the value of <i>replacable</i>
  @return true if this is replacable, false otherwise
  **********************************************************************************************
  **/
  public static boolean isReplacable() {
    return replacable;
  }

  /**
  **********************************************************************************************
  Loads an additional file into the <i>fm[]</i> array
  @param file_In the file to load
  **********************************************************************************************
  **/
  public static void loadAdditionalFile(File file_In) {
    FileManipulator[] temp = fm;
    fm = new FileManipulator[temp.length + 1];
    System.arraycopy(temp, 0, fm, 0, temp.length);

    file = file_In;
    fm[temp.length] = new FileManipulator(file, false);
  }

  /**
  **********************************************************************************************
  Loads a script file, effectively setting some default values and initialising the <i>fm[]</i>
  @param file_In the file to load
  **********************************************************************************************
  **/
  public static void loadFile(File file_In) {
    clearVariables();
    file = file_In;
    fm[0] = new FileManipulator(file, false);

    set("EOF", new Long(fm[0].getLength()));
    set("SOF", new Long(0));
    set("FileDir", file.getParent());
    set("FDDE", file.getAbsolutePath());

  }

  /**
  **********************************************************************************************
  Dumps out a list of all loaded variables and their values
  **********************************************************************************************
  **/
  public static void printVariableList() {
    Enumeration<String> keys = variables.keys();
    System.out.println("== VARIABLE LIST DUMP ==");
    while (keys.hasMoreElements()) {
      Object element = keys.nextElement();
      System.out.println(element + ":\t" + variables.get(element));
    }
    System.out.println("== END ==");
  }

  /**
  **********************************************************************************************
  Resizes the <i>resources[]</i> array to length <i>filenum</i>
  **********************************************************************************************
  **/
  public static void resizeResources() {
    Resource[] temp = resources;
    resources = new Resource[filenum];

    System.arraycopy(temp, 0, resources, 0, filenum);
  }

  /**
  **********************************************************************************************
  Sets the property with name <i>key</i> to have the value <i>value</i>
  @param key the name of the property
  @param value the value of the property
  **********************************************************************************************
  **/
  public static void set(String key, long value) {
    set(key, new Long(value));
  }

  /**
  **********************************************************************************************
  Sets the property with name <i>key</i> to have the value <i>value</i>
  @param key the name of the property
  @param value the value of the property
  **********************************************************************************************
  **/
  public static void set(String key, Object value) {
    variables.put(key, value);
  }

  /**
  **********************************************************************************************
  Sets the property with name <i>key</i> to have the value <i>value</i>
  @param key the name of the property
  @param value the value of the property
  **********************************************************************************************
  **/
  public static void set(String key, String value) {
    set(key, (Object) value);
  }

  /**
  **********************************************************************************************
  Sets the value of <i>compressed</i>
  @param isCompressed true if this is compressed, false otherwise
  **********************************************************************************************
  **/
  public static void setCompressed(boolean isCompressed) {
    compressed = isCompressed;
  }

  /**
  **********************************************************************************************
  Sets the value of <i>exporter</i>
  @param exp the exporter
  **********************************************************************************************
  **/
  public static void setExporter(ExporterPlugin exp) {
    exporter = exp;
  }

  /**
  **********************************************************************************************
  Sets the property with name <i>key</i> to have the value <i>value</i>
  @param key the name of the property
  @param value the value of the property
  **********************************************************************************************
  **/
  public static void setLong(String key, long value) {
    set(key, new Long(value));
  }

  /**
  **********************************************************************************************
  Sets the value of <i>replacable</i>
  @param canReplace true if you can perform replacing operations, false otherwise.
  **********************************************************************************************
  **/
  public static void setReplacable(boolean canReplace) {
    replacable = canReplace;
  }

  /**
  **********************************************************************************************
  Sets the property with name <i>key</i> to have the value <i>value</i>
  @param key the name of the property
  @param value the value of the property
  **********************************************************************************************
  **/
  public static void setString(String key, String value) {
    set(key, value);
  }

  /**
  **********************************************************************************************
  Sets the property with name <i>key</i> to have the value <i>value</i>
  @param key the name of the property
  @param value the value of the property
  **********************************************************************************************
  **/
  public static void setVariable(String key, Object value) {
    set(key, value);
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptGlobals() {
    //clearVariables();
  }

}