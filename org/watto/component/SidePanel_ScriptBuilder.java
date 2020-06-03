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

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Hashtable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.watto.Language;
import org.watto.Settings;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.plugin.PluginList;
import org.watto.ge.plugin.PluginListBuilder;
import org.watto.ge.script.ScriptArchivePlugin;
import org.watto.ge.script.ScriptManager;
import org.watto.io.FileManipulator;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_ScriptBuilder extends WSPanelPlugin implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  String[] scriptErrors;

  int numScriptErrors;

  Hashtable<String, String> scriptVariables;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_ScriptBuilder() {
    super(new XMLNode());
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   * @param caller the object that contains this component, created this component, or more
   *        formally, the object that receives events from this component.
   **********************************************************************************************
   **/
  public SidePanel_ScriptBuilder(XMLNode node) {
    super(node);
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addScriptError(int line, String messageCode) {
    addScriptError("Line " + (line + 1) + ": " + Language.get("ScriptError_" + messageCode));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addScriptError(int line, String command, String messageCode) {
    addScriptError("Line " + (line + 1) + " (" + command + "): " + Language.get("ScriptError_" + messageCode));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addScriptError(int line, String command, String messageCode, String word) {
    addScriptError("Line " + (line + 1) + " (" + command + "): " + Language.get("ScriptError_" + messageCode) + " [" + word + "]");
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addScriptError(String message) {
    if (numScriptErrors >= scriptErrors.length) {
      String[] temp = scriptErrors;
      scriptErrors = new String[temp.length + 10];
      System.arraycopy(temp, 0, scriptErrors, 0, temp.length);
    }

    scriptErrors[numScriptErrors] = message;
    numScriptErrors++;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkAllowedValues(int line, String command, String variable, String[] allowedValues) {
    for (int i = 0; i < allowedValues.length; i++) {
      if (variable.equalsIgnoreCase(allowedValues[i])) {
        return;
      }
    }

    addScriptError(line, command, "ValueNotAllowed", variable);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkFileOpened(int line, String command, String fileNumber) {
    if (scriptVariables.get("<GEcheck:FileNumber>" + fileNumber) == null) {
      addScriptError(line, command, "FileNotOpened", fileNumber);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean checkNumWords(int line, String command, int actualNum, int targetNum) {
    if (actualNum < targetNum) {
      addScriptError(line, command, "MissingVariablesInLine", actualNum + "<" + targetNum);
    }
    else if (actualNum > targetNum) {
      addScriptError(line, command, "TooManyVariablesInLine", actualNum + ">" + targetNum);
    }
    else {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkScript() {

    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    DefaultStyledDocument document = (DefaultStyledDocument) editor.getDocument();

    String scriptData = "";
    try {
      scriptData = document.getText(0, document.getLength());
    }
    catch (Throwable t) {
      return;
    }

    // first up, determine if this is a QuickBMS script or a MexCom3 script
    int scriptType = ScriptManager.analyseBMS(scriptData);
    if (scriptType == ScriptManager.SCRIPT_QUICKBMS) {
      // show a message that we can't check the syntax of QuickBMS scripts
      WSPopup.showErrorInNewThread("ScriptBuilder_CantCheckQuickBMSScript", false);
      return;
    }

    String[] lines = scriptData.split("\n");

    scriptVariables = new Hashtable<String, String>();
    scriptErrors = new String[10];
    numScriptErrors = 0;

    scriptVariables.put("BytesRead", "!");
    scriptVariables.put("LogEntries", "!");
    scriptVariables.put("TailOffOff", "!");
    scriptVariables.put("EOF", "!");
    scriptVariables.put("SOF", "!");
    scriptVariables.put("FileDir", "!");
    scriptVariables.put("<GEcheck:FileNumber>0", "!");

    int numCommandDo = 0;
    int numCommandIf = 0;
    int numCommandFor = 0;
    int numCommandXML = 0;

    boolean correctMistakes = Settings.getBoolean("CorrectScriptMistakes");

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];

      if (line.trim().equals("")) {
        // blank line
        if (correctMistakes) {
          lines[i] = null; // lines[i], not line, because continue; below skips the line[i]=line; further down
        }
        else {
          addScriptError(i, "BlankLineNotAllowed");
        }
        continue;
      }

      if (line.charAt(0) != '#' && line.charAt(0) != '<') {
        // comments and <BMS ... ></BMS> don't require a ';' at the end

        if (correctMistakes) {
          line = line.replaceAll(";", "").trim() + " ;";
        }
        else {
          if (line.indexOf(";") < 0) {
            // missing ';'
            addScriptError(i, "MissingEndMarker");
            continue;
          }
          else if (line.lastIndexOf(";") != line.length() - 1) {
            // ';' not at the end of the line
            addScriptError(i, "EndMarkerNotAtEnd");
            continue;
          }
          else if (line.indexOf(";") != line.length() - 1) {
            // ';' not at the end of the line
            addScriptError(i, "EndMarkerUsedInLine");
            continue;
          }
          else {
            if (line.length() > 2 && line.charAt(line.length() - 2) != ' ') {
              // no space before the ';'
              addScriptError(i, "NoSpaceBeforeEndMarker");
              continue;
            }
          }
        }
      }

      if (line.charAt(0) == '<') {
        // either <BMS ... > or </BMS>

        if (line.equalsIgnoreCase("</bms>")) {
          if (i != lines.length - 1) {
            addScriptError(i, "ClosingXMLNotAtEnd");
            continue;
          }
        }
        else if (line.length() > 3 && line.substring(0, 4).equalsIgnoreCase("<bms")) {
          if (i != 0) {
            addScriptError(i, "OpeningXMLNotAtStart");
            continue;
          }

          // correct the formatting of the XML attributes automatically, whether correctMistakes=true or false
          line = line.replaceAll(" *= *", "=").replaceAll("' *, *'", "', '").replaceAll("\" *'", "\"'").replaceAll("' *\"", "'\"");

        }
      }

      if (line.charAt(0) == ' ') {
        // spaces at the beginning of the line
        if (correctMistakes) {
          line = lines[i].trim();
        }
        else {
          addScriptError(i, "SpaceAtStart");
          continue;
        }
      }

      if (correctMistakes) {
        // detecting multiple spaces
        while (line.indexOf("  ") >= 0) {
          line = line.replaceAll("  ", " ");
        }
      }
      else {
        if (line.indexOf("  ") >= 0) {
          addScriptError(i, "MultipleSpaces");
          continue;
        }
      }

      // putting the corrections back into the array
      lines[i] = line;

      String[] words = line.split(" ");
      int numWords = words.length;

      String command = words[0];

      if (command.equalsIgnoreCase("#")) {
        // comments
      }
      else if (command.equalsIgnoreCase("<bms")) {
        // opening XML
        numCommandXML++;
      }
      else if (command.equalsIgnoreCase("</bms>")) {
        // closing XML
        numCommandXML--;
      }
      else if (command.equalsIgnoreCase("CleanExit")) {
        // CleanExit
        checkNumWords(i, command, numWords, 2);
      }
      else if (command.equalsIgnoreCase("CLog")) {
        // CLog
        if (checkNumWords(i, command, numWords, 9)) {
          checkVariableNumber(i, command, words[2]); // offset
          checkVariableNumber(i, command, words[3]); // compressed length
          checkVariableNumber(i, command, words[4]); // offset offset
          checkVariableNumber(i, command, words[5]); // compressed length offset
          checkVariableNumber(i, command, words[6]); // decompressed length
          checkVariableNumber(i, command, words[7]); // decompressed length offset
        }
      }
      else if (command.equalsIgnoreCase("ComType")) {
        // ComType
        if (checkNumWords(i, command, numWords, 3)) {
          checkAllowedValues(i, command, words[1], new String[] { "ZLib1" });
        }
      }
      else if (command.equalsIgnoreCase("Do")) {
        // Do
        checkNumWords(i, command, numWords, 2);
        numCommandDo++;
      }
      else if (command.equalsIgnoreCase("Else")) {
        // Else
        checkNumWords(i, command, numWords, 2);
        if (numCommandIf <= 0) {
          addScriptError(i, command, "ElseWithoutIf");
        }
      }
      else if (command.equalsIgnoreCase("EndIf")) {
        // EndIf
        checkNumWords(i, command, numWords, 2);
        if (numCommandIf <= 0) {
          addScriptError(i, command, "EndIfWithoutIf");
        }
        else {
          numCommandIf--;
        }
      }
      else if (command.equalsIgnoreCase("FindLoc")) {
        // FindLoc
        if (checkNumWords(i, command, numWords, 6)) {
          scriptVariables.put(words[1], "!");                             // variable
          checkAllowedValues(i, command, words[2], new String[] { "String" }); // data type
          checkFileOpened(i, command, words[4]);                        // file number
        }
      }
      else if (command.equalsIgnoreCase("For")) {
        // For
        if (checkNumWords(i, command, numWords, 7)) {
          scriptVariables.put(words[1], "!");                         // variable
          checkAllowedValues(i, command, words[2], new String[] { "=" }); // =
          checkVariableNumber(i, command, words[3]);                   // startValue
          checkAllowedValues(i, command, words[4], new String[] { "To" });// To
          checkVariableNumber(i, command, words[5]);                   // endValue
        }
        numCommandFor++;
      }
      else if (command.equalsIgnoreCase("Get")) {
        // Get
        if (checkNumWords(i, command, numWords, 5)) {
          scriptVariables.put(words[1], "!");                                                                     // variable
          checkAllowedValues(i, command, words[2], new String[] { "Long", "Int", "Byte", "String", "ThreeByte", "ASize" }); // data type
          checkFileOpened(i, command, words[3]);                                                                // file number
        }
      }
      else if (command.equalsIgnoreCase("GetCT")) {
        // GetCT
        if (checkNumWords(i, command, numWords, 6)) {
          scriptVariables.put(words[1], "!");                                    // variable
          checkAllowedValues(i, command, words[2], new String[] { "Byte", "String" }); // data type
          checkFileOpened(i, command, words[4]);                               // file number
        }
      }
      else if (command.equalsIgnoreCase("GetDString")) {
        // GetDString
        if (checkNumWords(i, command, numWords, 5)) {
          scriptVariables.put(words[1], "!");       // variable
          checkVariableNumber(i, command, words[2]); // length of string
          checkFileOpened(i, command, words[3]); // file number
        }
      }
      else if (command.equalsIgnoreCase("GoTo")) {
        // GoTo
        if (checkNumWords(i, command, numWords, 4)) {
          checkVariableNumber(i, command, words[1]); // offset
          checkFileOpened(i, command, words[2]); // file number
        }
      }
      else if (command.equalsIgnoreCase("IDString")) {
        // IDString
        if (checkNumWords(i, command, numWords, 4)) {
          checkFileOpened(i, command, words[1]); // file number
        }
      }
      else if (command.equalsIgnoreCase("If")) {
        // If
        if (checkNumWords(i, command, numWords, 5)) {
          checkVariable(i, command, words[1]);                                               // variable
          checkAllowedValues(i, command, words[2], new String[] { "=", "<", ">", "<=", ">=", "<>" }); // function
        }
        numCommandIf++;
      }
      else if (command.equalsIgnoreCase("ImpType")) {
        // ImpType
        if (checkNumWords(i, command, numWords, 3)) {
          checkAllowedValues(i, command, words[1], new String[] { "Standard", "SFileOff", "SFileSize", "None", "StandardTail" }); // replace types
        }
      }
      else if (command.equalsIgnoreCase("Log")) {
        // Log
        if (checkNumWords(i, command, numWords, 7)) {
          checkVariableNumber(i, command, words[2]); // offset
          checkVariableNumber(i, command, words[3]); // compressed length
          checkVariableNumber(i, command, words[4]); // offset offset
          checkVariableNumber(i, command, words[5]); // compressed length offset
        }
      }
      else if (command.equalsIgnoreCase("Math")) {
        // Math
        if (checkNumWords(i, command, numWords, 5)) {
          scriptVariables.put(words[1], "!");       // variable 1
          checkAllowedValues(i, command, words[2], new String[] { "=", "+=", "-=", "*=", "/=" }); // function
          checkVariableNumber(i, command, words[3]); // variable 2
        }
      }
      else if (command.equalsIgnoreCase("Next")) {
        // Next
        if (checkNumWords(i, command, numWords, 3)) {
          checkVariable(i, command, words[1]); // variable
        }

        if (numCommandFor <= 0) {
          addScriptError(i, command, "NextWithoutFor");
        }
        else {
          numCommandFor--;
        }
      }
      else if (command.equalsIgnoreCase("Open")) {
        // Open
        if (checkNumWords(i, command, numWords, 5)) {
          scriptVariables.put("<GEcheck:FileNumber>" + words[3], "!");
        }
      }
      else if (command.equalsIgnoreCase("ReverseLong")) {
        // ReverseLong
        if (checkNumWords(i, command, numWords, 3)) {
          checkVariable(i, command, words[1]); // variable
        }
      }
      else if (command.equalsIgnoreCase("SavePos")) {
        // SavePos
        if (checkNumWords(i, command, numWords, 4)) {
          scriptVariables.put(words[1], "!");   // variable
          checkFileOpened(i, command, words[2]); // file number
        }
      }
      else if (command.equalsIgnoreCase("Set")) {
        // Set
        if (checkNumWords(i, command, numWords, 5)) {
          scriptVariables.put(words[1], "!");       // variable
          if (words[2].equals("Long")) {
            checkVariableNumber(i, command, words[3]); // value
          }
        }
      }
      else if (command.equalsIgnoreCase("String")) {
        // String
        if (checkNumWords(i, command, numWords, 5)) {
          checkVariable(i, command, words[1]);                              // variable
          checkAllowedValues(i, command, words[2], new String[] { "+=", "-=" }); // function
        }
      }
      else if (command.equalsIgnoreCase("While")) {
        // While
        if (checkNumWords(i, command, numWords, 5)) {
          checkVariable(i, command, words[1]);                                               // variable
          checkAllowedValues(i, command, words[2], new String[] { "=", "<", ">", "<=", ">=", "<>" }); // function
        }

        if (numCommandDo <= 0) {
          addScriptError(i, command, "WhileWithoutDo");
        }
        else {
          numCommandDo--;
        }
      }
      else {
        addScriptError(i, command, "CommandUnknown");
      }

    }

    if (numCommandXML > 0) {
      addScriptError(Language.get("ScriptError_XMLNotMatching"));
    }
    if (numCommandIf > 0) {
      addScriptError(Language.get("ScriptError_IfNotMatching"));
    }
    if (numCommandFor > 0) {
      addScriptError(Language.get("ScriptError_ForNotMatching"));
    }
    if (numCommandDo > 0) {
      addScriptError(Language.get("ScriptError_DoNotMatching"));
    }

    if (correctMistakes) {
      // rebuild the script, with any corrections made (removing blank lines, adding ;, etc.
      String scriptText = "";
      for (int i = 0, j = 0; i < lines.length; i++) {
        String line = lines[i];
        if (line != null) {
          if (j > 0) {
            scriptText += "\n";
          }
          scriptText += line;
          j++;
        }
      }
      editor.setText(scriptText);

      highlightSyntax();
    }

    if (numScriptErrors <= 0) {
      WSPopup.showMessageInNewThread("ScriptBuilder_NoErrorsFound", false);
    }
    else {
      // build the script error message
      String errorText = "";
      for (int i = 0; i < numScriptErrors; i++) {
        if (i > 0) {
          errorText += "\n";
        }
        errorText += scriptErrors[i];
      }
      Language.set("WSLabel_ScriptBuilder_ErrorsFound_Text", errorText);

      WSPopup.showErrorInNewThread("ScriptBuilder_ErrorsFound", false);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkVariable(int line, String command, String variable) {
    if (scriptVariables.get(variable) == null) {
      addScriptError(line, command, "VariableExpected", variable);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkVariableNumber(int line, String command, String variable) {
    if (scriptVariables.get(variable) == null) {
      try {
        Long.parseLong(variable);
      }
      catch (Throwable t) {
        addScriptError(line, command, "VariableOrNumberExpected", variable);
      }
    }
  }

  /**
   **********************************************************************************************
   * Gets the plugin description
   **********************************************************************************************
   **/
  @Override
  public String getDescription() {
    String description = toString() + "\n\n" + Language.get("Description_SidePanel");

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;
  }

  /**
   **********************************************************************************************
   * Gets the plugin name
   **********************************************************************************************
   **/
  @Override
  public String getText() {
    return super.getText();
  }

  /**
   **********************************************************************************************
   * Colors the keywords in the editor
   **********************************************************************************************
   **/
  public void highlightSyntax() {
    if (!Settings.getBoolean("ColorScriptSyntax")) {
      ((WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script")).scrollRectToVisible(new Rectangle(0, 0, 0, 0));
      return;
    }

    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    DefaultStyledDocument document = (DefaultStyledDocument) editor.getDocument();

    String[] keywords = new String[] { "Append", "Break", "CallDLL", "CallFunction", "CleanExit", "CLog", "Codepage", "ComType", "Continue", "Debug", "Do", "Elif", "Else", "Encryption", "EndFunction", "EndIf", "Endian", "FileCrypt", "FileRot", "FileXOR", "FindLoc", "For", "Get", "GetArray", "GetBits", "GetCT", "GetDString", "GetVarChr", "GoTo", "IDString", "If", "Include", "ImpType", "Label", "Log", "Math", "NameCRC", "Next", "Open", "Padding", "Print", "Put", "PutArray", "PutBits", "PutCT", "PutDString", "PutVarChr", "QuickBMSver", "ReverseLong", "ReverseLongLong", "ReverseShort", "SavePos", "ScanDir", "Set", "SLog", "SortArray", "StartFunction", "String", "Strlen", "While", "XMath" };
    String[] functions = new String[] { "!", "!!", "!=", "!u", "$", "%", "%%", "%u", "&", "&&", "&u", "*", "**", "**u", "*=", "*u", "+", "+=", "+u", "-", "-=", "-u", "/", "//", "//u", "/=", "/u", "<", "<<", "<<<", "<<<u", "<<u", "<=", "<>", "<>u", "<u", "=", "==", "===", "=u", ">", ">=", ">>", ">>>", ">>>u", ">>u", ">u", "?add", "?atan", "?cos", "?sin", "^", "^=", "^u", "_", "a", "au", "b", "base16", "base2", "base3", "base32", "base4", "base64", "base8", "binary", "c", "e", "f", "h", "hex", "J", "l", "lu", "m", "n", "nu", "octal", "p", "pu", "r", "reverselong", "reverselonglong", "reverseshort", "ru", "s", "su", "t", "TO", "u", "u!", "u!!", "u!=", "u%", "u&", "u*", "u**", "u+", "u-", "u/", "u//", "u<", "u<<", "u<<<", "u<=", "u<>", "u=", "u==", "u>", "u>=", "u>>", "u>>>", "u^", "ua", "ul", "un", "up", "ur", "us", "uv", "uw", "ux", "uy", "uz", "u|", "u~", "v", "vu", "w", "wu", "x", "xu", "y", "yu", "z", "zu", "|", "|u", "~", "~u" };
    String[] presets = new String[] { "???", "ALLOC", "ASCII_STRING ", "ASize", "ASM", "ASM64", "BASENAME", "BIG", "BINARY", "BITS", "BMS_FOLDER", "BYTE", "BytesRead", "CLSID", "COMPRESSED", "CURRENT_FOLDER", "DOUBLE", "EOF", "EXTENSION", "FDDE", "FDDE2", "FDDE3", "FDSE", "FDSE2", "FDSE3", "FILE_FOLDER", "FileDir", "FILENAME", "FILENUMBER", "FILEPATH", "FLOAT", "FULLBASENAME", "FULLNAME", "GUESS", "GUESS16", "GUESS24", "GUESS64", "INPUT_FOLDER", "Int", "IPV4", "IPV6", "LINE", "LITTLE", "LogEntries", "Long", "LONGDOUBLE", "LONGLONG", "MEMORY_FILE", "MEMORY_FILE1", "MEMORY_FILE2", "MEMORY_FILE3", "MEMORY_FILE4", "MEMORY_FILE5", "None", "OUTPUT_FOLDER", "PURENUMBER", "PURETEXT", "SAVE", "SEEK_CUR", "SEEK_END", "SEEK_SET", "SFileOff", "Short", "SIGNED_BYTE", "SIGNED_LONG", "SIGNED_SHORT", "SIGNED_THREEBYTE", "SOF", "Standard", "StandardTail", "String", "SWAP", "TailOffOff", "TCC", "TEMPORARY_FILE", "TEXTORNUMBER", "ThreeByte", "TIME", "TIME64", "UNICODE", "UNICODE_STRING", "UNKNOWN", "UTF32", "VARIABLE", "VARIABLE2", "VARIABLE3", "VARIABLE4", "VARIABLE5", "VARIABLE6", "VARIABLE7", "VARIANT" };

    int position = 0;

    String text;

    String[] words;
    try {
      text = document.getText(0, document.getLength());
      words = text.split("\\s");
    }
    catch (Throwable t) {
      return;
    }

    Color colorBlue = new Color(50, 50, 200);
    Color colorRed = new Color(200, 50, 50);
    Color colorPurple = new Color(200, 50, 200);
    Color colorGreen = new Color(50, 150, 50);
    Color colorOrange = new Color(200, 150, 50);
    Color colorBlack = new Color(0, 0, 0);

    boolean inComment = false;
    boolean inQuote = false;
    int commentEndPos = 0;
    for (int w = 0; w < words.length; w++) {

      String word = words[w];
      int length = word.length();

      boolean found = false;

      if (inComment) {
        if (position < commentEndPos) {
          // still in a comment
          found = true;

          // highlight the syntax in a <BMS>
          /*if (word.indexOf("\'") >= 0) {
            int startQuote = word.indexOf("\'");
            int endQuote = word.lastIndexOf("\'");
            if (endQuote == startQuote) {
              if (inQuote) {
                setStyle(document, position, startQuote, colorRed, true);
                inQuote = false;
              }
              else {
                setStyle(document, position + startQuote + 1, length, colorRed, true);
                inQuote = true;
              }
            }
            else {
              setStyle(document, position + startQuote + 1, endQuote - startQuote - 1, colorRed, true);
            }
          }
          
          else if (inQuote) {
          */
          if (inQuote) {
            setStyle(document, position, length, colorRed, true);
          }
          else if (word.indexOf("\"") >= 0) {
            int startQuote = word.indexOf("\"");
            int endQuote = word.lastIndexOf("\"");
            if (endQuote != startQuote) {
              setStyle(document, position + startQuote + 1, endQuote - startQuote - 1, colorRed, true);
            }
          }
        }
        else {
          inComment = false;
          inQuote = false;
        }
      }

      if (!found) {
        if (word.equals("</bms>")) {
          setStyle(document, position, length, colorOrange, true);
          found = true;
        }
        else if (word.equals("#") || word.equals("<bms")) {
          // comments - highlight the whole line
          int endOfLine = text.indexOf("\n", position);
          if (endOfLine < 0) {
            endOfLine = text.indexOf("\r", position);
          }
          if (endOfLine < 0) {
            endOfLine = text.length();
          }
          commentEndPos = endOfLine;
          endOfLine -= position;

          setStyle(document, position, endOfLine, colorOrange, true);
          inComment = true;
          inQuote = false;
          found = true;
        }
      }

      if (!found) {
        // try the keywords
        for (int i = 0; i < keywords.length; i++) {
          if (word.equalsIgnoreCase(keywords[i])) {
            setStyle(document, position, length, colorBlue, true);
            found = true;
            break;
          }
        }
      }

      if (!found) {
        // try the functions
        for (int i = 0; i < functions.length; i++) {
          if (word.equalsIgnoreCase(functions[i])) {
            setStyle(document, position, length, colorRed, true);
            found = true;
            break;
          }
        }
      }

      if (!found) {
        // try the presets
        for (int i = 0; i < presets.length; i++) {
          if (word.equalsIgnoreCase(presets[i])) {
            setStyle(document, position, length, colorGreen, true);
            found = true;
            break;
          }
        }
      }

      if (!found) {
        // try 0x and \x numbers
        if (length >= 4) {
          if (word.charAt(0) == '0' && word.charAt(1) == 'x') {
            setStyle(document, position, length, colorPurple, true);
            found = true;
          }
          else if (word.charAt(0) == '\\' && word.charAt(1) == 'x') {
            setStyle(document, position, length, colorPurple, true);
            found = true;
          }
        }
        if (length >= 5) {
          if (word.charAt(0) == '\"') {
            if (word.charAt(1) == '0' && word.charAt(2) == 'x') {
              setStyle(document, position, length, colorPurple, true);
              found = true;
            }
            else if (word.charAt(1) == '\\' && word.charAt(2) == 'x') {
              setStyle(document, position, length, colorPurple, true);
              found = true;
            }
          }
        }
      }

      if (!found) {
        // try the variables <xxx>
        if (length > 1) {
          if (word.charAt(0) == '<' && word.charAt(length - 1) == '>') {
            setStyle(document, position, length, colorPurple, true);
            found = true;
          }
        }
      }

      if (!found) {
        // add the text normally
        setStyle(document, position, length, colorBlack, true);
      }

      position += length + 1; // +1 for the space character

    }
    /*
    
    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    DefaultStyledDocument document = (DefaultStyledDocument) editor.getDocument();
    
    String text = null;
    try {
      text = document.getText(0, document.getLength());
    }
    catch (Throwable t) {
    }
    
    if (text == null) {
      return;
    }
    
    editor.setContentType("text/html");
    
    //text = highlightSyntax(text);
    
    // show the document
    editor.setText(text);
    */

  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  public String highlightSyntax(String text) {

    String colorBlue = "<font color=\"4444DD\">";
    String colorRed = "<font color=\"DD4444\">";
    String colorPurple = "<font color=\"DD44DD\">";
    String colorGreen = "<font color=\"44DD44\">";
    String colorOrange = "<font color=\"DD9944\">";
    String colorBlack = "<font color=\"000000\">";

    String fontClose = "</font>";
    char spaceChar = ' ';

    // split into lines
    String lines[] = text.split("\\r?\\n");
    int numLines = lines.length;

    // get the tag from each line
    for (int i = 0; i < numLines; i++) {
      String line = lines[i];

      // color the line based on the tag (and split if necessary)
      String[] tagSplit = line.split("\\s+", 2);
      String tag = tagSplit[0].toLowerCase();

      if (tag.equals("#") || tag.startsWith("<")) {
        // comments - highlight the whole line
        line = wrapIfExists(colorOrange, line, fontClose);
      }
      else if (tag.equalsIgnoreCase("QuickBMSver")) {
        // QuickBMSver VERSION
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("FindLoc")) {
        // FindLoc VAR TYPE STRING [FILENUM] [ERR_VALUE] [END_OFF]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("For")) {
        // For [VAR] [OP] [VALUE] [COND] [VAR]
        String[] varSplit = tagSplit[1].split("\\s+", 5);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 3, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 4, fontClose);
      }
      else if (tag.equalsIgnoreCase("Next")) {
        // Next [VAR] [OP] [VALUE]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("Get")) {
        // Get VAR TYPE [FILENUM]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("GetDString")) {
        // GetDString VAR LENGTH [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("GoTo")) {
        // GoTo OFFSET [FILENUM] [TYPE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("IDString")) {
        // IDString [FILENUM] STRING
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Log")) {
        // Log NAME OFFSET SIZE [FILENUM] [XSIZE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Clog")) {
        // Clog NAME OFFSET ZSIZE SIZE [FILENUM] [XSIZE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Math")) {
        // Math VAR OP VAR
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("XMath")) {
        // XMath VAR INSTR
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Open")) {
        // Open FOLDER NAME [FILENUM] [EXISTS]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("SavePos")) {
        // SavePos VAR [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Set")) {
        // Set VAR [TYPE] VAR
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        if (varSplit.length == 3) {
          line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
        }
        else {
          line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
        }
      }
      else if (tag.equalsIgnoreCase("Do")) {
        // Do
        line = wrapIfExists(colorBlue, line, fontClose);
      }
      else if (tag.equalsIgnoreCase("While")) {
        // While VAR COND VAR
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("String")) {
        // String VAR OP VAR
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("CleanExit")) {
        // CleanExit
        line = wrapIfExists(colorBlue, line, fontClose);
      }
      else if (tag.equalsIgnoreCase("If")) {
        // If VAR COND VAR [...]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("Elif")) {
        // Elif VAR COND VAR
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("Else")) {
        // Else
        line = wrapIfExists(colorBlue, line, fontClose);
      }
      else if (tag.equalsIgnoreCase("EndIf")) {
        // EndIf
        line = wrapIfExists(colorBlue, line, fontClose);
      }
      else if (tag.equalsIgnoreCase("GetCT")) {
        // GetCT VAR TYPE CHAR [FILENUM]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("ComType")) {
        // ComType ALGO [DICT] [DICT_SIZE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("ReverseShort")) {
        // ReverseShort VAR
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("ReverseLong")) {
        // ReverseLong VAR
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("ReverseLongLong")) {
        // ReverseLongLong VAR
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Endian")) {
        // Endian TYPE [VAR]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("FileXOR")) {
        // FileXOR SEQ [OFFSET] [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("FileRot")) {
        // FileRot SEQ [OFFSET] [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("FileCrypt")) {
        // FileCrypt SEQ [OFFSET] [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Strlen")) {
        // Strlen VAR VAR [SIZE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("GetVarChr")) {
        // GetVarChr VAR VAR OFFSET [TYPE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("PutVarChr")) {
        // PutVarChr VAR OFFSET VAR [TYPE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Debug")) {
        // Debug [MODE]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Padding")) {
        // Padding VAR [FILENUM] [BASE_OFF]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Append")) {
        // Append [DIRECTION]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Encryption")) {
        // Encryption ALGO KEY [IVEC] [MODE] [KEYLEN]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Print")) {
        // Print MESSAGE
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("GetArray")) {
        // GetArray VAR ARRAY VAR_IDX
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("PutArray")) {
        // PutArray ARRAY VAR_IDX VAR
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("SortArray")) {
        // SortArray ARRAY [ALL]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("CallFunction")) {
        // CallFunction NAME [KEEP_VAR] [ARG1] [ARG2] ... [ARGn]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("StartFunction")) {
        // StartFunction NAME
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("EndFunction")) {
        // EndFunction
        line = wrapIfExists(colorBlue, line, fontClose);
      }
      else if (tag.equalsIgnoreCase("ScanDir")) {
        // ScanDir PATH NAME SIZE [FILTER]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("CallDLL")) {
        // CallDLL DLLNAME FUNC/OFF CONV RET [ARG1] [ARG2] ... [ARGn]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Put")) {
        // Put VAR TYPE [FILENUM]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("PutDString")) {
        // PutDString VAR LENGTH [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("PutCT")) {
        // PutCT VAR TYPE CHAR [FILENUM]
        String[] varSplit = tagSplit[1].split("\\s+", 3);
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 0, fontClose) + spaceChar + wrapIfExists(colorBlack, varSplit, 1, fontClose) + spaceChar + wrapIfExists(colorGreen, varSplit, 2, fontClose);
      }
      else if (tag.equalsIgnoreCase("GetBits")) {
        // GetBits VAR BITS [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("PutBits")) {
        // PutBits VAR BITS [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Include")) {
        // Include FILENAME
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("NameCRC")) {
        // NameCRC VAR CRC [LISTFILE] [TYPE] [POLYNOMIAL] [PARAMETERS]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Codepage")) {
        // Codepage VAR
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("SLog")) {
        // SLog NAME OFFSET SIZE [TYPE] [FILENUM]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Label")) {
        // Label NAME
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Break")) {
        // Break [NAME]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }
      else if (tag.equalsIgnoreCase("Continue")) {
        // Continue [NAME]
        line = wrapIfExists(colorBlue, tag, fontClose) + spaceChar + wrapIfExists(colorGreen, tagSplit, 1, fontClose);
      }

      lines[i] = line;
    }

    // now join all the lines back together, with <html></html> around it
    text = "<html><font family=\"Courier New\"><b>";
    for (int i = 0; i < numLines; i++) {
      text += lines[i] + "<br>";
    }
    text += "</b></font></html>";

    return text;
  }

  /**
   **********************************************************************************************
  Concatenates the Strings together, if they aren't null
   **********************************************************************************************
   **/
  public String wrapIfExists(String start, String middle, String end) {
    if (middle == null) {
      return "";
    }
    else {
      return start + middle + end;
    }
  }

  /**
   **********************************************************************************************
  Concatenates the Strings together, if they aren't null
   **********************************************************************************************
   **/
  public String wrapIfExists(String start, String[] middleArray, int middleArrayIndex, String end) {
    if (middleArray == null) {
      return "";
    }
    if (middleArrayIndex >= middleArrayIndex) {
      return "";
    }

    String text = middleArray[middleArrayIndex];
    if (text == null) {
      return "";
    }
    else {
      return start + text + end;
    }
  }

  /**
   **********************************************************************************************
   * Inserts a command into the textarea, at the caret position
   **********************************************************************************************
   **/
  public void insertCommand() {

    String[] commands = new String[] { "# Enter your comment here",
        "<bms ext=\"'extension1' ,'extension2'\" games=\"'Game1', 'Game2'\" author=\"YourName\" platforms=\"'PC', 'Other'\" version=\"1.0\">\n<commands>\n</bms>",
        "CleanExit ;",
        "CLog <name> <offset> <compressedLength> <offsetOffset> <compressedLengthOffset> <decompressedLength> <decompressedLengthOffset> ;",
        "ComType <compressionType> ;",
        "Do ;\n<commands>\nWhile <variable> <function> <check> ;",
        "FindLoc <variable> <type> <searchValue> <fileNumber> ;",
        "For <variable> = <startValue> To <endValue> ;\n<commands>\nNext <variable> ;",
        "Get <variable> <type> <fileNumber> ;",
        "GetCT <variable> <type> <terminator> <fileNumber> ;",
        "GetDString <variable> <length> <fileNumber> ;",
        "GoTo <variable> <fileNumber> ;",
        "IDString <fileNumber> <string> ;",
        "If <variable> <function> <check> ; \n<commands>\nElse ;\n<commands>\nEndIf ;",
        "ImpType <replaceType> ;",
        "Log <name> <offset> <length> <offsetOffset> <lengthOffset> ;",
        "Math <firstVariable> <function> <secondVariable> ;",
        "Open <directory> <filename> <fileNumber> ;",
        "ReverseLong <variable> ;",
        "SavePos <variable> <fileNumber> ;",
        "Set <variable> <value> ;",
        "String <firstVariable> <function> <secondVariable> ;" };

    WSList commandsList = (WSList) ComponentRepository.get("SidePanel_ScriptBuilder_CommandsList");
    String command = commands[commandsList.getSelectedIndex()];

    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    DefaultStyledDocument document = (DefaultStyledDocument) editor.getDocument();

    int position = editor.getCaretPosition();
    int length = document.getLength();

    boolean breakBefore = false;
    boolean breakAfter = false;

    try {
      // determine where the \n should go.
      if (position == 0) {
        if (length > 0) {
          breakAfter = true; // start of the document
        }
      }
      else if (position == length) {
        breakBefore = true; // end of the document
      }
      else {
        // check where the existing linebreaks are
        char lb = document.getText(position - 1, 1).charAt(0);
        if (lb == '\n' || lb == '\r') {
          breakAfter = true; // start of a line
        }
        else {
          lb = document.getText(position, 1).charAt(0);
          if (lb == '\n' || lb == '\r') {
            breakBefore = true; // end of a line
          }
        }
      }

      if (breakBefore) {
        command = "\n" + command;
      }
      else if (breakAfter) {
        command += "\n";
      }

      document.insertString(position, command, null); // new SimpleAttributeSet()
    }
    catch (Throwable t) {
    }

    highlightSyntax();

    editor.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

    editor.requestFocus();

  }

  /**
   **********************************************************************************************
   * Loads the list of commands into the list
   **********************************************************************************************
   **/
  @SuppressWarnings("unchecked")
  public void loadCommands() {

    String[] commands = new String[] { "# Comment", "<bms> Script Description", "CleanExit", "CLog", "ComType", "Do", "FindLoc", "For", "Get", "GetCT", "GetDString", "GoTo", "IDString", "If", "ImpType", "Log", "Math", "Open", "ReverseLong", "SavePos", "Set", "String" };

    WSList commandsList = (WSList) ComponentRepository.get("SidePanel_ScriptBuilder_CommandsList");
    commandsList.setListData(commands);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadScripts() {
    try {
      WSComboBox scriptPluginList = (WSComboBox) ComponentRepository.get("SidePanel_ScriptBuilder_ScriptsList");

      PluginList[] plugins = null;
      try {
        plugins = PluginListBuilder.getPluginList(WSPluginManager.getGroup("Script").getPlugins());
      }
      catch (Throwable t) {
        // plugins not loaded
      }
      if (plugins == null || plugins.length == 0) {
        // so we only load the scripts when they are needed
        ScriptManager.loadScripts();
        plugins = PluginListBuilder.getPluginList(WSPluginManager.getGroup("Script").getPlugins());
      }
      scriptPluginList.setModel(new DefaultComboBoxModel(plugins));

      String currentScript = Settings.get("SelectedScript");

      for (int i = 0; i < plugins.length; i++) {
        if (plugins[i].getPlugin().getName().equals(currentScript)) {
          scriptPluginList.setSelectedIndex(i);
          break;
        }
      }

    }
    catch (Throwable t) {
      // scripts not loaded yet
    }
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSClickableListener when a click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (c instanceof WSButton) {
      String code = ((WSButton) c).getCode();
      if (code.equals("SidePanel_ScriptBuilder_CheckScript")) {
        checkScript();
      }
      else if (code.equals("SidePanel_ScriptBuilder_TestScript")) {
        testScript();
      }
      else if (code.equals("SidePanel_ScriptBuilder_SaveScript")) {
        saveScript();
      }
      else if (code.equals("SidePanel_ScriptBuilder_OpenScript")) {
        openScript();
      }
      else {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be closed. This method
   * does nothing by default, but can be overwritten to do anything else needed before the panel
   * is closed, such as garbage collecting and closing pointers to temporary objects.
   **********************************************************************************************
   **/
  @Override
  public void onCloseRequest() {
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is deselected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSDoubleClickableListener when a double click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDoubleClick(JComponent c, MouseEvent e) {
    if (c instanceof WSList) {
      String code = ((WSList) c).getCode();
      if (code.equals("SidePanel_ScriptBuilder_CommandsList")) {
        insertCommand();
        return true;
      }
    }
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when the mouse moves over an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    return super.onHover(c, e);
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when the mouse moves out of an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    return super.onHoverOut(c, e);
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSKeyableListener when a key press occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    if (c instanceof WSEditorPane) {
      String code = ((WSEditorPane) c).getCode();
      if (code.equals("SidePanel_ScriptBuilder_Script")) {
        highlightSyntax();
      }
    }
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be opened. By default,
   * it just calls checkLoaded(), but can be overwritten to do anything else needed before the
   * panel is displayed, such as resetting or refreshing values.
   **********************************************************************************************
   **/
  @Override
  public void onOpenRequest() {
    loadScripts();
    highlightSyntax();
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is selected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    if (c instanceof WSComboBox) {
      String code = ((WSComboBox) c).getCode();

      if (code.equals("SidePanel_ScriptBuilder_ScriptsList")) {
        String scriptName = ((PluginList) ((WSComboBox) c).getSelectedItem()).getPlugin().getName();
        Settings.set("SelectedScript", scriptName);
        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void openScript() {
    WSComboBox scripts = (WSComboBox) ComponentRepository.get("SidePanel_ScriptBuilder_ScriptsList");
    Object pluginObj = scripts.getSelectedItem();
    if (pluginObj == null) {
      return;
    }

    ScriptArchivePlugin plugin = (ScriptArchivePlugin) (((PluginList) pluginObj).getPlugin());

    String scriptData = plugin.getScript();
    //scriptData = highlightSyntax(scriptData);

    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    editor.setText(scriptData);

    highlightSyntax();

    WSTextField scriptName = (WSTextField) ComponentRepository.get("SidePanel_ScriptBuilder_ScriptName");
    scriptName.setText(plugin.getName() + ".bms");

  }

  /**
   **********************************************************************************************
   * Registers the events that this component generates
   **********************************************************************************************
   **/
  @Override
  public void registerEvents() {
    super.registerEvents();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    ((WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script")).requestFocus();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void saveScript() {
    WSTextField scripts = (WSTextField) ComponentRepository.get("SidePanel_ScriptBuilder_ScriptName");
    String name = scripts.getText();

    // no file name given
    if (name == null || name.equals("")) {
      WSPopup.showErrorInNewThread("ScriptBuilder_NoNameSpecified", true);
      return;
    }

    // apppend the extension
    if (name.indexOf(".bms") < 0) {
      name += ".bms";
    }

    // get the script data
    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    DefaultStyledDocument document = (DefaultStyledDocument) editor.getDocument();

    String script = "";
    try {
      script = document.getText(0, document.getLength());
    }
    catch (Throwable t) {
    }

    // write the script
    String scriptName = name;
    FileManipulator fm = new FileManipulator(new File(Settings.get("ScriptsDirectory") + File.separator + name), true);
    fm.writeString(script);
    File scriptFile = fm.getFile();
    scriptName = fm.getFile().getName().replaceAll("\\.bms", "");
    fm.close();

    WSPopup.showMessageInNewThread("ScriptBuilder_ScriptSaved", true);

    // add the new script
    /* v3.10 Changed this to allow scripts to be saved as MexCom3 or QuickBMS, and loaded accordingly
    ScriptArchivePlugin_MexCom3 scriptPlugin = new ScriptArchivePlugin_MexCom3(scriptFile, scriptName);
    WSPluginGroup group = WSPluginManager.getGroup("Script");
    group.addPlugin(scriptPlugin);
    */
    WSPluginGroup group = WSPluginManager.getGroup("Script");
    ScriptManager.analyseBMS(scriptFile, scriptName, group);

    // change the settings for the current selected script in the plugin lists
    Settings.set("SelectedScript", scriptName);

    // reload the combo box
    loadScripts();

  }

  ///////////////
  //
  // Default Implementations
  //
  ///////////////

  /**
   **********************************************************************************************
   * Sets the description of the plugin
   * @param description the description
   **********************************************************************************************
   **/
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
  }

  /**
   **********************************************************************************************
   * Sets the color and properties of the text in the document
   **********************************************************************************************
   **/
  public void setStyle(DefaultStyledDocument document, int start, int length, Color color, boolean bold) {
    SimpleAttributeSet attributes = new SimpleAttributeSet();
    StyleConstants.setFontFamily(attributes, LookAndFeelManager.getFont().getFamily());
    StyleConstants.setFontSize(attributes, LookAndFeelManager.getFont().getSize());
    StyleConstants.setForeground(attributes, color);
    StyleConstants.setBold(attributes, bold);

    document.setCharacterAttributes(start, length, attributes, true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void testScript() {
    // get the script data
    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    DefaultStyledDocument document = (DefaultStyledDocument) editor.getDocument();

    String script = "";
    try {
      script = document.getText(0, document.getLength());
    }
    catch (Throwable t) {
    }

    if (script.length() <= 0) {
      return;
    }

    // save the script to a temp file
    File tempScriptFile = new File("scripts" + File.separator + "ScriptBuilder_Test.bms");
    if (tempScriptFile.exists()) {
      tempScriptFile.delete();
    }

    FileManipulator fm = new FileManipulator(tempScriptFile, true);
    fm.writeString(script);
    fm.close();

    // change the settings for the current selected script in the plugin lists
    Settings.set("SelectedScript", "ScriptBuilder_Test");

    // find the relevant plugin, and reload the data in it
    WSPluginGroup group = WSPluginManager.getGroup("Script");
    boolean hasPlugin = group.hasPlugin("ScriptBuilder_Test");

    /* v3.10 Changed this to allow scripts to be saved as MexCom3 or QuickBMS, and loaded accordingly
    ScriptArchivePlugin_MexCom3 plugin;
    
    if (!hasPlugin) {
      // the plugin doesn't exist, so add it
      group.addPlugin(new ScriptArchivePlugin_MexCom3(tempScriptFile, "ScriptBuilder_Test"));
    }
    
    plugin = (ScriptArchivePlugin_MexCom3) group.getPlugin("ScriptBuilder_Test");
    plugin.loadWrapper();
    */
    if (!hasPlugin) {
      // the plugin doesn't exist, so add it
      ScriptManager.analyseBMS(tempScriptFile, "ScriptBuilder_Test", group);
    }

    // switch over to the DirList, with the selected script ready to go
    Settings.set("SidePanel_DirectoryList_CurrentControl", "ScriptPanel");

    WSSidePanelHolder sidePanelHolder = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");
    sidePanelHolder.loadPanel("SidePanel_DirectoryList");
  }

  /**
   **********************************************************************************************
   * Build this object from the <i>node</i>
   * @param node the XML node that indicates how to build this object
   **********************************************************************************************
   **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_ScriptBuilder.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);

    loadCommands();

    WSEditorPane editor = (WSEditorPane) ComponentRepository.get("SidePanel_ScriptBuilder_Script");
    editor.setDocument(new DefaultStyledDocument());

    // disable word wrap. (because it is a StyledEditorKit, it also enables colors)
    editor.setEditorKit(new WSNoWrapStyledEditorKit());

    WSTextField scriptName = (WSTextField) ComponentRepository.get("SidePanel_ScriptBuilder_ScriptName");
    scriptName.setText("new");

  }

  /**
   **********************************************************************************************
   * Builds an XMLNode that describes this object
   * @return an XML node with the details of this object
   **********************************************************************************************
   **/
  @Override
  public XMLNode toXML() {
    return super.toXML();
  }

}