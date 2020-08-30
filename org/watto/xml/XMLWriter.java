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

package org.watto.xml;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.io.FileManipulator;
import org.watto.io.Manipulator;

/***********************************************************************************************
Writes a tree of <code>XMLNode</code>s to an XML-format document 
@see org.watto.xml.XMLNode
***********************************************************************************************/
public class XMLWriter {

  /** The stream for reading the file **/
  protected static Manipulator manipulator = null;

  /** The depth of the currently-processed tag. Used for padding the lines **/
  protected static int level = 0;

  /** Is this the start of a new line? Used to determine when to apply line padding **/
  protected static boolean newLine = true;

  /** Whether the file is being read as Unicode or non-Unicode **/
  protected static boolean unicode = false;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public XMLWriter() {
  }

  /***********************************************************************************************
  Prepares the output for the next line of input
  ***********************************************************************************************/
  public static void nextLine() {
    try {
      if (unicode) {
        manipulator.writeUnicodeString("\n\r");
      }
      else {
        manipulator.writeString("\n\r");
      }
      newLine = true;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>file</code> in non-Unicode format
  @param file the <code>File</code> to write to
  @param root the tree to write
  ***********************************************************************************************/
  public static boolean writeWithValidation(File file, XMLNode root) {
    return writeWithValidation(file, root, false);
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>file</code> in non-Unicode format
  @param file the <code>File</code> to write to
  @param root the tree to write
  ***********************************************************************************************/
  public static void write(File file, XMLNode root) {
    write(file, root, false);
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>file</code> in the <code>unicodeFormat</code>
  @param file the <code>File</code> to write to
  @param root the tree to write
  @param unicodeFormat <b>true</b> to write in Unicode format<br />
                       <b>false</b> to write in non-Unicode format
  ***********************************************************************************************/
  public static boolean writeWithValidation(File file, XMLNode root, boolean unicodeFormat) {
    return writeWithValidation(new FileManipulator(file, true), root, unicodeFormat);
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>file</code> in the <code>unicodeFormat</code>
  @param file the <code>File</code> to write to
  @param root the tree to write
  @param unicodeFormat <b>true</b> to write in Unicode format<br />
                       <b>false</b> to write in non-Unicode format
  ***********************************************************************************************/
  public static void write(File file, XMLNode root, boolean unicodeFormat) {
    write(new FileManipulator(file, true), root, unicodeFormat);
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>Manipulator</code> in non-Unicode format
  @param newManipulator the <code>Manipulator</code> to write to
  @param root the tree to write
  ***********************************************************************************************/
  public static void write(Manipulator newManipulator, XMLNode root) {
    write(newManipulator, root, false);
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>Manipulator</code> in the <code>unicodeFormat</code>
  @param newManipulator the <code>Manipulator</code> to write to
  @param root the tree to write
  @param unicodeFormat <b>true</b> to write in Unicode format<br />
                       <b>false</b> to write in non-Unicode format
  ***********************************************************************************************/
  public static boolean writeWithValidation(Manipulator newManipulator, XMLNode root, boolean unicodeFormat) {
    try {
      unicode = unicodeFormat;
      manipulator = newManipulator;

      if (unicode) {
        // write unicode header
        manipulator.writeByte((byte) 255);
        manipulator.writeByte((byte) 254);
      }

      level = 0;

      writeTag(root);

      //File writePath = manipulator.getFile();
      manipulator.close();

      //if (writePath.getAbsolutePath() != path.getAbsolutePath()){
      //  path.delete();
      //  writePath.renameTo(path);
      //  }

      return true;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return false;
    }
  }

  /***********************************************************************************************
  Writes the <code>root</code> to the <code>Manipulator</code> in the <code>unicodeFormat</code>
  @param newManipulator the <code>Manipulator</code> to write to
  @param root the tree to write
  @param unicodeFormat <b>true</b> to write in Unicode format<br />
                       <b>false</b> to write in non-Unicode format
  ***********************************************************************************************/
  public static void write(Manipulator newManipulator, XMLNode root, boolean unicodeFormat) {
    try {
      unicode = unicodeFormat;
      manipulator = newManipulator;

      if (unicode) {
        // write unicode header
        manipulator.writeByte((byte) 255);
        manipulator.writeByte((byte) 254);
      }

      level = 0;

      writeTag(root);

      //File writePath = manipulator.getFile();
      manipulator.close();

      //if (writePath.getAbsolutePath() != path.getAbsolutePath()){
      //  path.delete();
      //  writePath.renameTo(path);
      //  }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Writes the attributes of the <code>currentNode</code>
  @param currentNode the current node
  ***********************************************************************************************/
  public static void writeAttributes(XMLNode currentNode) {
    try {

      String[][] attributes = currentNode.getAttributes();

      for (int i = 0; i < attributes.length; i++) {
        writePartialLine(" " + attributes[i][0] + "=\"" + attributes[i][1] + "\"");
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Writes the XML DocType
  @param DTDFile the file with the document type declaration (DTD)
  ***********************************************************************************************/
  public static void writeDocType(String DTDFile) {
    writeLine("<!DOCTYPE resources SYSTEM \"" + DTDFile + "\">");
  }

  /***********************************************************************************************
  Writes the XML header
  @param version the XML version
  @param encoding the XML encoding
  ***********************************************************************************************/
  public static void writeHeader(double version, String encoding) {
    writeLine("<?xml version=\"" + version + "\" encoding=\"" + encoding + "\"?>");
  }

  /***********************************************************************************************
  Pads the beginning of the line out to the correct position for the node depth
  ***********************************************************************************************/
  public static void writeLevel() {
    try {
      if (unicode) {
        for (int i = 0; i < level; i++) {
          manipulator.writeUnicodeString("\t");
        }
      }
      else {
        for (int i = 0; i < level; i++) {
          manipulator.writeString("\t");
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Writes a <code>line</code> of data to the <code>manipulator</code>
  @param line the <code>String</code> to write
  ***********************************************************************************************/
  public static void writeLine(String line) {
    writePartialLine(line);
    nextLine();
  }

  /***********************************************************************************************
  Appends the <code>partialLine</code> to the <code>manipulator</code>
  @param partialLine the <code>String</code> to write
  ***********************************************************************************************/
  public static void writePartialLine(String partialLine) {
    try {
      if (newLine) {
        writeLevel();
        newLine = false;
      }

      if (unicode) {
        manipulator.writeUnicodeString(partialLine);
      }
      else {
        manipulator.writeString(partialLine);
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Writes the <code>currentNode</code>
  @param currentNode the current node
  ***********************************************************************************************/
  public static void writeTag(XMLNode currentNode) {
    try {

      String name = currentNode.getTag();

      // write the start tag
      writePartialLine("<" + name);

      if (currentNode.hasAttributes()) {
        // write the attributes
        writeAttributes(currentNode);
      }

      if (currentNode.hasChildren() && currentNode.hasContent()) {
        // close the start tag
        writePartialLine(">");
        nextLine();

        // write the children
        level++;

        XMLNode[] children = currentNode.getChildren();
        for (int i = 0; i < children.length; i++) {
          writeTag(children[i]);
        }

        level--;

        // write the content
        writeText(currentNode);

        // write the end tag
        writePartialLine("</" + name + ">");
        nextLine();
      }

      else if (currentNode.hasChildren()) {
        // close the start tag
        writePartialLine(">");
        nextLine();

        // write the children
        level++;

        XMLNode[] children = currentNode.getChildren();
        for (int i = 0; i < children.length; i++) {
          writeTag(children[i]);
        }

        level--;

        // write the end tag
        writePartialLine("</" + name + ">");
        nextLine();
      }

      else if (currentNode.hasContent()) {
        // close the start tag
        writePartialLine(">");

        // write the content
        writeText(currentNode);

        // write the end tag
        writePartialLine("</" + name + ">");
        nextLine();
      }

      else {
        // close the single tag
        writePartialLine(" />");
        nextLine();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Writes the content text of the <code>currentNode</code>
  @param currentNode the current node
  ***********************************************************************************************/
  public static void writeText(XMLNode currentNode) {
    try {
      writePartialLine(currentNode.getContent());
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }
}