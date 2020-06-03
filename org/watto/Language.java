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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import org.watto.io.FilenameSplitter;

/***********************************************************************************************
 * A singleton class that provides language-specific <code>String</code>s for use by any class in
 * the JVM. Maintains a list of available <code>LanguagePack</code>s, and the ability to change
 * languages.
 ***********************************************************************************************/
public class Language extends ResourceBundle {

  /** the translation codes and texts for the current language **/
  static Hashtable<String, String> texts = new Hashtable<String, String>();

  /** the current language **/
  static LanguagePack currentLanguage = null;

  /** all loaded languages **/
  static LanguagePack[] languages = new LanguagePack[0];

  /***********************************************************************************************
   * Changes the language to the <code>languageName</code>. Loads the <code>String</code>s from
   * the <code>LanguagePack</code> and populates the global <code>Hashtable</code>
   * @param languageName the name of the <code>LanguagePack</code> you wish to load
   ***********************************************************************************************/
  public static void changeLanguage(String languageName) {
    try {
      for (int i = 0; i < languages.length; i++) {
        if (languages[i].getName().equalsIgnoreCase(languageName)) {
          // found the language
          currentLanguage = languages[i];
          languages[i].loadLanguage(texts);
          Settings.set("CurrentLanguage", languageName);
          return;
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Gets the language <code>String</code> for the <code>code</code>
   * @param code the code to get the <code>String</code> for
   * @return the language value for the <code>code</code>
   ***********************************************************************************************/
  public static String get(String code) {
    String text = (texts.get(code));
    if (text == null) {
      logError(code);
      return code;
    }
    else {
      return text;
    }
  }

  /***********************************************************************************************
   * Gets the current <code>LanguagePack</code>
   * @return the current <code>LanguagePack</code>
   ***********************************************************************************************/
  public static LanguagePack getCurrentLanguage() {
    return currentLanguage;
  }

  /***********************************************************************************************
   * Gets the name of the current language
   * @return the name of the current language
   ***********************************************************************************************/
  public static String getCurrentLanguageName() {
    return currentLanguage.getName();
  }

  /***********************************************************************************************
   * Gets the number of <code>LanguagePack</code>s
   * @return the number of <code>LanguagePack</code>s
   ***********************************************************************************************/
  public static int getLanguageCount() {
    return languages.length;
  }

  /***********************************************************************************************
   * Gets the names of all the <code>LanguagePack</code>s
   * @return the names of all the <code>LanguagePack</code>s
   ***********************************************************************************************/
  public static String[] getLanguageNames() {
    int languageCount = languages.length;
    String[] languageNames = new String[languageCount];
    for (int i = 0; i < languageCount; i++) {
      languageNames[i] = languages[i].getName();
    }
    return languageNames;
  }

  /***********************************************************************************************
   * Gets all the <code>LanguagePack</code>s
   * @return all the <code>LanguagePack</code>s
   ***********************************************************************************************/
  public static LanguagePack[] getLanguages() {
    return languages;
  }

  /***********************************************************************************************
   * Is there a language <code>String</code> for the <code>code</code>?
   * @param code the code to search for
   * @return true if there is a language <code>String</code> for this <code>code</code>, false
   *         otherwise
   ***********************************************************************************************/
  public static boolean has(String code) {
    if (texts.containsKey(code)) {
      return true;
    }
    else {
      //logError(code);
      return false;
    }
  }

  /***********************************************************************************************
   * Scans a <code>directory</code> and adds any <code>LanguagePack</code>s that exist into the
   * array
   * @param directory the directory to scan
   ***********************************************************************************************/
  public static void loadDirectory(File directory) {

    File[] languageFiles = directory.listFiles();

    int directoryFileCount = languageFiles.length;
    int oldLanguageCount = languages.length;
    int newLanguageCount = directoryFileCount + oldLanguageCount;

    languages = resize(languages, newLanguageCount);

    for (int i = oldLanguageCount; i < newLanguageCount; i++) {
      File languageFile = languageFiles[i];
      String languageName = FilenameSplitter.getFilename(languageFile);
      languages[i] = new LanguagePack(languageName, languageFile);
    }

  }

  /***********************************************************************************************
   * Logs a missing language text <code>code</code> to the <code>ErrorLogger</code>, but not more
   * than once for each language text <code>code</code>
   * @param code the language text code that was missing
   ***********************************************************************************************/
  public static void logError(String code) {
    if (TemporarySettings.getQuietBoolean("LoggedLanguageError-" + code)) {
      // we've already logged an error against this code, so don't log it again!
      return;
    }

    // log the error
    ErrorLogger.log("Missing Language: " + code);

    // set the temporary setting so that this error is not logged again
    TemporarySettings.set("LoggedLanguageError-" + code, true);
  }

  /***********************************************************************************************
   * Writes out the <code>texts</code> to the command prompt
   ***********************************************************************************************/
  public static void outputLanguage() {
    try {

      Enumeration<String> keys = texts.keys();
      Enumeration<String> values = texts.elements();

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
   * Resizes an array of <code>LanguagePack</code>s
   * @param source the source array
   * @param newSize the new size of the array
   * @return the new resized array
   ***********************************************************************************************/
  static LanguagePack[] resize(LanguagePack[] source, int newSize) {
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    LanguagePack[] target = new LanguagePack[newSize];
    System.arraycopy(source, 0, target, 0, copySize);

    return target;
  }

  /***********************************************************************************************
   * Sets the language <code>String</code> of a <code>code</code>
   * @param code the code to set the value of
   * @param text the language <code>String</code> for the <code>code</code>
   ***********************************************************************************************/
  public static void set(String code, String text) {
    texts.put(code, text);
  }

  /***********************************************************************************************
   * Scans for <code>LanguagePack</code>s in the <i>LanguageDirectory</i> and changes to the
   * <i>CurrentLanguage</i>.
   ***********************************************************************************************/
  public Language() {
    loadDirectory(new File(Settings.getString("LanguageDirectory")));

    if (languages.length <= 0) {
      // try loading languages from the "language" directory
      ErrorLogger.log("No languages found in directory \"" + Settings.getString("LanguageDirectory") + "\" so load from \"language\" directory instead");
      loadDirectory(new File("language"));
    }

    if (languages.length <= 0) {
      // failed to find any languages
      ErrorLogger.log("FATAL: Couldn't find any language files to load.");
    }

    changeLanguage(Settings.getString("CurrentLanguage"));

    if (currentLanguage == null) {
      // failed to load the language, so try English
      ErrorLogger.log("Failed to change to currentLanguage \"" + Settings.getString("CurrentLanguage") + "\" so trying to load \"English\" instead");
      changeLanguage("English");
    }

    if (currentLanguage == null) {
      // still failed to load the language, so just use the first language file that was found
      ErrorLogger.log("Failed to change to \"English\", so trying to load the first language instead");
      if (languages.length >= 1) {
        changeLanguage(languages[0].getName());
      }
    }

    if (languages.length <= 0) {
      // failed to find any languages
      ErrorLogger.log("FATAL: Couldn't find a suitable language to assign as the current/default.");
    }

  }

  /***********************************************************************************************
   * Gets all the codes and their language <code>String</code>s for the current language
   * @return an array of all the codes and their language <code>String</code>s
   * @see java.util.ResourceBundle
   ***********************************************************************************************/
  public Object[][] getContents() {
    Enumeration<String> keys = texts.keys();
    Enumeration<String> values = texts.elements();

    int textCount = texts.size();
    Object[][] contents = new Object[textCount][2];
    for (int i = 0; i < textCount; i++) {
      contents[i] = new Object[] { keys.nextElement(), values.nextElement() };
    }

    return contents;
  }

  /***********************************************************************************************
   * Gets all the codes for the current language
   * @return an <code>Enumeration</code> of all the codes
   * @see java.util.ResourceBundle
   ***********************************************************************************************/
  @Override
  public Enumeration<String> getKeys() {
    return texts.keys();
  }

  /***********************************************************************************************
   * Gets the language <code>String</code>s for the <code>code</code>
   * @param code the code to get the language <code>String</code> for
   * @return the language <code>String</code> for the <code>code</code>
   * @see java.util.ResourceBundle
   ***********************************************************************************************/

  @Override
  public Object handleGetObject(String code) {
    return get(code);
  }
}