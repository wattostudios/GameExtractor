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
import java.util.Hashtable;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 Provides details about a language pack file, and the ability to load the language pack in to a
 <code>Hashtable</code> for use in the program.
 ***********************************************************************************************/
public class LanguagePack {

  String name = "";
  File file;

  /***********************************************************************************************
  Defines a language pack
  @param name the language name
  @param file the file that contains the language strings
   ***********************************************************************************************/
  public LanguagePack(String name, File file) {
    this.name = name;
    this.file = file;
  }

  /***********************************************************************************************
  Gets the language file
  @return the file that contains the language strings
   ***********************************************************************************************/
  public File getFile() {
    return file;
  }

  /***********************************************************************************************
  Gets the language name
  @return the name of the language
   ***********************************************************************************************/
  public String getName() {
    return name;
  }

  /***********************************************************************************************
  Loads the language pack into a <code>Hashtable</code>
  @param strings the <code>Hashtable</code> to load the strings in to.
   ***********************************************************************************************/
  public void loadLanguage(Hashtable<String, String> strings) {
    try {

      XMLNode languageTree = XMLReader.read(file);
      languageTree = languageTree.getChild("texts");

      int textCount = languageTree.getChildCount();
      for (int i = 0; i < textCount; i++) {
        XMLNode text = languageTree.getChild(i);

        String textCode = text.getAttribute("code");
        String textValue = text.getAttribute("value");

        //ErrorLogger.log("Adding Language \"" + textCode + "\" with value \"" + textValue + "\"");

        if (textCode == null || textValue == null) {
          continue;
        }

        String existingValue = strings.get(textCode);
        if (existingValue != null && existingValue.equals("")) {
        }
        else {
          // Only put it in if the existing value does not exist, and is not blank.
          // This is important, as showText="false" on buttons sets the text to blank.
          strings.put(textCode, textValue);
        }

      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Gets a string representation of the language pack, which is the language name
  @return the language name
   ***********************************************************************************************/
  @Override
  public String toString() {
    return name;
  }
}