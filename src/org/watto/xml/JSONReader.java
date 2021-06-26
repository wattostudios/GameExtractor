////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2020  WATTO Studios                           //
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
import org.watto.io.converter.ByteConverter;

/***********************************************************************************************
Reads a JSON-format document into a tree of <code>JSONNode</code>s
***********************************************************************************************/
public class JSONReader {

  /** The stream for reading the file **/
  protected static Manipulator manipulator = null;

  /** The currently-read character **/
  protected static char character;

  /** Whether the file is being read as Unicode or non-Unicode **/
  protected static boolean unicode = false;

  /***********************************************************************************************
  Reads an JSON document <code>file</code> into a tree of <code>JSONNode</code>s
  @param file the JSON document to read
  @return the tree of <code>JSONNode</code>s representing the JSON document
  ***********************************************************************************************/
  public static JSONNode read(File file) {
    JSONNode root = new JSONNode();
    read(file, root);
    try {
      return root.getChild(0);
    }
    catch (Throwable t) {
      return root;
    }
  }

  /***********************************************************************************************
  Reads an JSON document <code>file</code> into a tree of <code>JSONNode</code>s. The tree is
  constructed using the given <code>root</code> node
  @param file the JSON document to read
  @param root the root node of the tree, into which the tree will be constructed
  ***********************************************************************************************/
  public static void read(File file, JSONNode root) {
    read(new FileManipulator(file, false), root);
  }

  /***********************************************************************************************
  Reads an JSON-format <code>String</code> from a <code>Manipulator</code> into a tree of
  <code>JSONNode</code>s
  @param manipulator the <code>Manipulator</code> where the JSON-format <code>String</code> is
  being read from
  @return the tree of <code>JSONNode</code>s representing the JSON-format <code>String</code>
  ***********************************************************************************************/
  public static JSONNode read(Manipulator manipulator) {
    JSONNode root = new JSONNode();
    read(manipulator, root);
    try {
      return root.getChild(0);
    }
    catch (Throwable t) {
      return root;
    }
  }

  /***********************************************************************************************
  Reads an JSON-format <code>String</code> from a <code>Manipulator</code> into a tree of
  <code>JSONNode</code>s. The tree is constructed using the given <code>root</code> node
  @param newManipulator the <code>Manipulator</code> where the JSON-format <code>String</code> is
  being read from
  @param root the root node of the tree, into which the tree will be constructed
  ***********************************************************************************************/
  public static void read(Manipulator newManipulator, JSONNode root) {
    try {

      manipulator = newManipulator;

      // look for unicode header
      if (ByteConverter.unsign(manipulator.readByte()) == 255 && ByteConverter.unsign(manipulator.readByte()) == 254) {
        unicode = true;
      }
      else {
        unicode = false;
        manipulator.seek(0);
      }

      readTag(root);
      manipulator.close();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Reads an JSON-format <code>String</code> into a tree of <code>JSONNode</code>s
  @param text the JSON-format <code>String</code>
  @return the tree of <code>JSONNode</code>s representing the JSON-format <code>String</code>
  ***********************************************************************************************/
  public static JSONNode read(String text) {
    JSONNode root = new JSONNode();
    read(text, root);
    try {
      return root.getChild(0);
    }
    catch (Throwable t) {
      return root;
    }
  }

  /***********************************************************************************************
  Reads an JSON-format <code>String</code> into a tree of <code>JSONNode</code>s. The tree is
  constructed using the given <code>root</code> node
  @param text the JSON-format <code>String</code>
  @param root the root node of the tree, into which the tree will be constructed
  ***********************************************************************************************/
  public static void read(String text, JSONNode root) {
    read(new FileManipulator(new org.watto.io.buffer.StringBuffer(text)), root);
  }

  /***********************************************************************************************
  Reads the next <code>character</code> from the <code>manipulator</code>
  ***********************************************************************************************/
  public static void readChar() {
    try {
      if (unicode) {
        character = manipulator.readChar();
      }
      else {
        character = (char) manipulator.readByte();
        //System.out.println(character);
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Reads a single tag from the current position in the <code>manipulator</code>, adding it as a
  child to the <code>root</code> node
  @param root the root node to add this new tag to
  ***********************************************************************************************/
  public static void readTag(JSONNode root) {
    try {

      boolean keepReading = true; // true so that it enters the loop at least once

      //System.out.println("Tag");

      long fileLength = manipulator.getLength();

      readChar();

      while (keepReading) {
        keepReading = false;

        // skip leading whitespace
        while (character == ' ' || character == '\n' || character == '\r' || character == '\t') {
          readChar();
          if (manipulator.getOffset() >= fileLength) {
            return;
          }
        }

        if (character == '{') {
          readChar();
        }

        // skip spaces etc, get to the tag name
        while (character != '\"') {
          readChar();
          if (manipulator.getOffset() >= fileLength) {
            return;
          }
        }

        // skip over the quote to the next character
        readChar();

        // now we're ready to read the tag name
        String tag = "";
        while (character != '"') {
          tag += character;
          readChar();
          if (manipulator.getOffset() >= fileLength) {
            return;
          }
        }

        // now we have the tag, find the separator
        while (character != ':') {
          readChar();
          if (manipulator.getOffset() >= fileLength) {
            return;
          }
        }

        // skip over the separator to the next character
        readChar();

        // now we have the separator, need to find the value
        //while (character != '\"' && character != '{') { // not all values need quotes around them - ie numbers
        while (character == ' ' || character == '\n' || character == '\r' || character == '\t') {
          readChar();
          if (manipulator.getOffset() >= fileLength) {
            return;
          }
        }

        // ready for the value. determine if it's an array, or a single value
        if (character == '\"') {
          // single value (quoted) - read it

          // skip over the quote to the next character
          readChar();

          String value = "";
          while (character != '"') {
            value += character;
            readChar();
            if (manipulator.getOffset() >= fileLength) {
              return;
            }
          }

          root.addChild(new JSONNode(tag, value));
        }
        else if (character == '{') {
          // start of an array
          JSONNode arrayNode = new JSONNode(tag);
          readTag(arrayNode);

          root.addChild(arrayNode);

          // skip over the closing bracket to the next character
          if (character == '}') {
            readChar();
          }

        }
        else {
          // single value (NOT quoted) - read it
          String value = "";
          while (character != ' ' && character != ',' && character != '}' && character != '\r' && character != '\t') {
            value += character;
            readChar();
            if (manipulator.getOffset() >= fileLength) {
              return;
            }
          }

          root.addChild(new JSONNode(tag, value));
        }

        if (manipulator.getOffset() >= fileLength) {
          return;
        }

        // look for something to indicate that we should stop or continue
        while (character != ',' && character != '}') {
          readChar();
          if (manipulator.getOffset() >= fileLength) {
            return;
          }
        }

        if (character == '}') {
          // end of array
          return;
        }
        else if (character == ',') {
          // end of this object in the array, start the next one (stay in the keepReading loop)
          keepReading = true;
          continue;
        }

        // done reading this tag
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public JSONReader() {
  }
}