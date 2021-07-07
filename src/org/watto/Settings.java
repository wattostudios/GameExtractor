////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;
import org.watto.xml.XMLWriter;

/***********************************************************************************************
 * A singleton class that provides changable and rememberable settings for use by any class in
 * the JVM. Has the ability to load and save settings to a file.
 ***********************************************************************************************/
public class Settings {

  /** the settings and their values **/
  static Hashtable<String, String> settings = new Hashtable<String, String>();

  /***********************************************************************************************
   * Gets the <code>String</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>String</code>
   ***********************************************************************************************/
  public static String get(String code) {
    return getString(code);
  }

  /***********************************************************************************************
   * Gets the <code>boolean</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>boolean</code>
   ***********************************************************************************************/
  public static boolean getBoolean(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      if (result.equals("true")) {
        return true;
      }
      else {
        return false;
      }
    }
    catch (Throwable t) {
      logError(code);
      return true;
    }
  }

  /***********************************************************************************************
   * Gets the <code>byte</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>byte</code>
   ***********************************************************************************************/
  public static byte getByte(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      return Byte.parseByte(result);
    }
    catch (Throwable t) {
      logError(code);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the <code>char</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>char</code>
   ***********************************************************************************************/
  public static char getChar(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      if (result == null || result.length() <= 1) {
        throw new Exception("Missing Setting: " + code);
      }
      return result.charAt(0);
    }
    catch (Throwable t) {
      logError(code);
      return Character.UNASSIGNED;
    }
  }

  /***********************************************************************************************
   * Gets the <code>double</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>double</code>
   ***********************************************************************************************/
  public static double getDouble(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      return Double.parseDouble(result);
    }
    catch (Throwable t) {
      logError(code);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the <code>float</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>float</code>
   ***********************************************************************************************/
  public static float getFloat(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      return Float.parseFloat(result);
    }
    catch (Throwable t) {
      logError(code);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the <code>int</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>int</code>
   ***********************************************************************************************/
  public static int getInt(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      return Integer.parseInt(result);
    }
    catch (Throwable t) {
      logError(code);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the <code>long</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>long</code>
   ***********************************************************************************************/
  public static long getLong(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      return Long.parseLong(result);
    }
    catch (Throwable t) {
      logError(code);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the <code>short</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>short</code>
   ***********************************************************************************************/
  public static short getShort(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      return Short.parseShort(result);
    }
    catch (Throwable t) {
      logError(code);
      return -1;
    }
  }

  /***********************************************************************************************
   * Gets the <code>String</code> value for the <code>code</code> setting
   * @param code the setting to get the value for
   * @return the setting value as a <code>String</code>
   ***********************************************************************************************/
  public static String getString(String code) {
    try {
      String result = settings.get(code);
      if (result == null) {
        result = TemporarySettings.get(code); // maybe it is a temporary setting?
      }

      if (result == null) {
        throw new Exception("Missing Setting: " + code);
      }
      return result;
    }
    catch (Throwable t) {
      logError(code);
      return "";
    }
  }

  /***********************************************************************************************
   * Loads the settings from the <code>file</code>
   * @param file the file to load the settings from
   ***********************************************************************************************/
  public static void loadSettings(File file) {
    try {

      if (!file.exists()) {
        throw new FileNotFoundException("The settings file " + file.getAbsolutePath() + " does not exist");
      }

      XMLNode settingsTree = XMLReader.read(file);
      loadSettings(settingsTree);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Loads the settings from the <code>tree</code>
   * @param tree a tree of codes and values
   ***********************************************************************************************/
  public static void loadSettings(XMLNode tree) {
    try {

      XMLNode settingNode = tree.getChild("settings");

      int numSettings = settingNode.getChildCount();
      if (settings == null) {
        settings = new Hashtable<String, String>(numSettings);
      }

      for (int i = 0; i < numSettings; i++) {
        XMLNode text = settingNode.getChild(i);

        String settingCode = text.getAttribute("code");
        String settingString = text.getAttribute("value");

        String changable = text.getAttribute("changable");
        if (changable != null && changable.equals("false")) {
          // not changable - make it a TemporarySetting
          TemporarySettings.set(settingCode, settingString);
        }
        else {
          // normal - make it a Setting
          settings.put(settingCode, settingString);
        }

      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Logs a missing setting <code>code</code> to the <code>ErrorLogger</code>, but not more than
   * once for each setting <code>code</code>
   * @param code the setting code that was missing
   ***********************************************************************************************/
  public static void logError(String code) {
    if (TemporarySettings.getQuietBoolean("LoggedSettingError-" + code)) {
      // we've already logged an error against this code, so don't log it again!
      return;
    }

    // log the error
    if ((!code.equals("DebugMode")) && Settings.getBoolean("DebugMode")) {
      ErrorLogger.log("Missing Setting: " + code);
    }

    // set the temporary setting so that this error is not logged again
    TemporarySettings.set("LoggedSettingError-" + code, true);
  }

  /***********************************************************************************************
   * Writes out the <code>settings</code> to the command prompt
   ***********************************************************************************************/
  public static void outputSettings() {
    try {

      Enumeration<String> keys = settings.keys();
      Enumeration<String> values = settings.elements();

      while (keys.hasMoreElements() && values.hasMoreElements()) {
        String key = keys.nextElement();
        String value = values.nextElement();

        System.out.println(key + " = " + value);
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Reloads the default values for the settings
   ***********************************************************************************************/
  public static void revertToDefaults() {
    loadSettings(new File("settings" + File.separator + "default.xml"));

  }

  /***********************************************************************************************
   * Saves the settings to the user settings file.
   ***********************************************************************************************/
  public static void saveSettings() {
    saveSettings(new File("settings" + File.separator + "settings.xml"));
  }

  /***********************************************************************************************
   * Saves the settings to the <code>file</code>
   * @param file the file to save the settings to
   ***********************************************************************************************/
  public static void saveSettings(File file) {
    try {

      File path = new File(getString("SettingsFile"));
      if (!path.exists()) {
        // Lets try swapping the / and \ characters, in case we're on Unix
        path = new File(new File(path.getAbsolutePath().replace('\\', '/')).getAbsolutePath());
      }

      // Write to a temporary file first
      File tempPath = new File(path.getAbsolutePath() + ".tmp");
      if (tempPath.exists()) {
        tempPath.delete();
      }

      // build an XML tree of the settings
      XMLNode settingsStore = new XMLNode("settingsStore");

      // TODO - implement a way of writing the actual program name back into the settings
      settingsStore.addChild(new XMLNode("program", "WATTO Studios Program"));

      XMLNode settingsTree = new XMLNode("settings");
      settingsStore.addChild(settingsTree);

      Enumeration<String> keys = settings.keys();
      Enumeration<String> values = settings.elements();

      while (keys.hasMoreElements() && values.hasMoreElements()) {
        String key = keys.nextElement();
        String value = values.nextElement();

        XMLNode setting = new XMLNode("setting");
        setting.addAttribute("code", key);
        setting.addAttribute("value", value);
        settingsTree.addChild(setting);
      }

      boolean success = XMLWriter.writeWithValidation(tempPath, settingsStore);
      if (!success) {
        return; // something went wrong when writing the settings, so don't replace the real file with the corrupt one
      }

      // if all is OK, remove the Proper file and then rename the temp one to it.
      // This helps to avoid the occasional issue where the settings file becomes corrupt during write, due to stream being closed.
      if (path.exists()) {
        path.delete();
      }
      tempPath.renameTo(path);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Sets the <code>boolean</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>boolean</code>
   ***********************************************************************************************/
  public static void set(String code, boolean value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>byte</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>byte</code>
   ***********************************************************************************************/
  public static void set(String code, byte value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>char</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>char</code>
   ***********************************************************************************************/
  public static void set(String code, char value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>double</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>double</code>
   ***********************************************************************************************/
  public static void set(String code, double value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>float</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>float</code>
   ***********************************************************************************************/
  public static void set(String code, float value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>int</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as an <code>int</code>
   ***********************************************************************************************/
  public static void set(String code, int value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>long</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>long</code>
   ***********************************************************************************************/
  public static void set(String code, long value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>short</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>short</code>
   ***********************************************************************************************/
  public static void set(String code, short value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
   * Sets the <code>String</code> <code>value</code> of the setting <code>code</code>
   * @param code the setting to set the <code>value</code> of
   * @param value the new value of the setting, as a <code>String</code>
   ***********************************************************************************************/
  public static void set(String code, String value) {
    if (value != null) {
      settings.put(code, "" + value);
    }
  }

  /***********************************************************************************************
   * Loads the default settings followed by the user settings
   ***********************************************************************************************/
  public Settings() {
    loadSettings(new File("settings" + File.separator + "default.xml"));
    loadSettings(new File("settings" + File.separator + "settings.xml"));
  }

  /***********************************************************************************************
   * Loads the settings from the <code>settingsFile</code>
   * @param settingsFile the file that contains the settings to load
   ***********************************************************************************************/
  public Settings(File settingsFile) {
    loadSettings(settingsFile);
  }

  /***********************************************************************************************
   * Loads the <code>defaultSettings</code> and the <code>userSettings</code> from the files
   * @param defaultSettings the file that contains the default settings
   * @param userSettings the file that contains the user settings
   ***********************************************************************************************/
  public Settings(File defaultSettings, File userSettings) {
    loadSettings(defaultSettings);
    loadSettings(userSettings);
  }
}