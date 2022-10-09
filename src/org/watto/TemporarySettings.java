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

import java.util.Hashtable;

/***********************************************************************************************
A singleton class that provides changable and rememberable settings for use by any class in the
JVM. Unlike the <code>Settings</code> class, these settings are not loaded from a file, and
they are not saved afterwards. Rather, this class is for settings that only need to be
remembered temporarily, such as "Yes To All" buttons and the last time a file was modified.
***********************************************************************************************/
public class TemporarySettings {

  /** the settings and their values **/
  static Hashtable<String, String> settings = new Hashtable<String, String>();

  /***********************************************************************************************
  Gets the <code>String</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>String</code>
  ***********************************************************************************************/
  public static String get(String code) {
    return getString(code);
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public static boolean has(String code) {
    try {
      return settings.containsKey(code);
    }
    catch (Throwable t) {
      return false;
    }
  }

  /***********************************************************************************************
  Gets the <code>boolean</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>boolean</code>
  ***********************************************************************************************/
  public static boolean getBoolean(String code) {
    try {
      String result = settings.get(code);

      if (result.equals("true")) {
        return true;
      }
      else {
        return false;
      }
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return true;
    }
  }

  /***********************************************************************************************
  Gets the <code>byte</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>byte</code>
  ***********************************************************************************************/
  public static byte getByte(String code) {
    try {
      String result = settings.get(code);

      return Byte.parseByte(result);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return -1;
    }
  }

  /***********************************************************************************************
  Gets the <code>char</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>char</code>
  ***********************************************************************************************/
  public static char getChar(String code) {
    try {
      String result = settings.get(code);

      if (result == null || result.length() <= 1) {
        throw new Exception("Missing Setting: " + code);
      }
      return result.charAt(0);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return Character.UNASSIGNED;
    }
  }

  /***********************************************************************************************
  Gets the <code>double</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>double</code>
  ***********************************************************************************************/
  public static double getDouble(String code) {
    try {
      String result = settings.get(code);

      return Double.parseDouble(result);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return -1;
    }
  }

  /***********************************************************************************************
  Gets the <code>float</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>float</code>
  ***********************************************************************************************/
  public static float getFloat(String code) {
    try {
      String result = settings.get(code);

      return Float.parseFloat(result);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return -1;
    }
  }

  /***********************************************************************************************
  Gets the <code>int</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>int</code>
  ***********************************************************************************************/
  public static int getInt(String code) {
    try {
      String result = settings.get(code);

      return Integer.parseInt(result);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return -1;
    }
  }

  /***********************************************************************************************
  Gets the <code>long</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>long</code>
  ***********************************************************************************************/
  public static long getLong(String code) {
    try {
      String result = settings.get(code);

      return Long.parseLong(result);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return -1;
    }
  }

  /***********************************************************************************************
  Gets the <code>boolean</code> value for the <code>code</code> setting, but doesn't output any
  errors if the setting doesn't exist. Used for writing errors to the <code>ErrorLogger</code>.
  @param code the setting to get the value for
  @return the setting value as a <code>boolean</code>
  ***********************************************************************************************/
  public static boolean getQuietBoolean(String code) {
    try {
      String result = settings.get(code);

      if (result == null) {
        return false;
      }
      else if (result.equals("true")) {
        return true;
      }
      else {
        return false;
      }
    }
    catch (Throwable t) {
      return false;
    }
  }

  /***********************************************************************************************
  Gets the <code>short</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>short</code>
  ***********************************************************************************************/
  public static short getShort(String code) {
    try {
      String result = settings.get(code);

      return Short.parseShort(result);
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return -1;
    }
  }

  /***********************************************************************************************
  Gets the <code>String</code> value for the <code>code</code> setting
  @param code the setting to get the value for
  @return the setting value as a <code>String</code>
  ***********************************************************************************************/
  public static String getString(String code) {
    try {
      String result = settings.get(code);

      if (result == null) {
        throw new Exception("Missing Setting: " + code);
      }
      return result;
    }
    catch (Throwable t) {
      logError("Missing Setting: " + code);
      return "";
    }
  }

  /***********************************************************************************************
   * Logs a missing setting <code>code</code> to the <code>ErrorLogger</code>, but not more than
   * once for each setting <code>code</code>
   * @param code the setting code that was missing
   ***********************************************************************************************/
  public static void logError(String code) {
    if (getQuietBoolean("LoggedSettingError-" + code)) {
      // we've already logged an error against this code, so don't log it again!
      return;
    }

    // log the error
    if ((!code.equals("DebugMode")) && Settings.getBoolean("DebugMode")) {
      ErrorLogger.log("Missing Setting: " + code);
    }

    // set the temporary setting so that this error is not logged again
    set("LoggedSettingError-" + code, true);
  }

  /***********************************************************************************************
  Sets the <code>boolean</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>boolean</code>
  ***********************************************************************************************/
  public static void set(String code, boolean value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>byte</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>byte</code>
  ***********************************************************************************************/
  public static void set(String code, byte value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>char</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>char</code>
  ***********************************************************************************************/
  public static void set(String code, char value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>double</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>double</code>
  ***********************************************************************************************/
  public static void set(String code, double value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>float</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>float</code>
  ***********************************************************************************************/
  public static void set(String code, float value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>int</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as an <code>int</code>
  ***********************************************************************************************/
  public static void set(String code, int value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>long</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>long</code>
  ***********************************************************************************************/
  public static void set(String code, long value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>short</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>short</code>
  ***********************************************************************************************/
  public static void set(String code, short value) {
    settings.put(code, "" + value);
  }

  /***********************************************************************************************
  Sets the <code>String</code> <code>value</code> of the setting <code>code</code>
  @param code the setting to set the <code>value</code> of
  @param value the new value of the setting, as a <code>String</code>
  ***********************************************************************************************/
  public static void set(String code, String value) {
    if (value != null) {
      settings.put(code, "" + value);
    }
  }

  /***********************************************************************************************
  Empty constructor
  ***********************************************************************************************/
  public TemporarySettings() {
  }
}