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

import org.watto.ErrorLogger;


/***********************************************************************************************
 Reads an XHTML file into a tree of <code>XMLNode</code>s.<br />
 This is different to an XMLReader...<br />
 This is because, text in an XHTML document can have nested tags within the text, such as
 &lt;br /&gt; Therefore, each text has its own special node called !TEXT! which is added as a
 child, instead of being content on the node itself. This keeps the nested tags in the correct
 place.
 @see org.watto.xml.XMLNode
 @see org.watto.xml.XMLReader
***********************************************************************************************/
public class XHTMLReader extends XMLReader {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public XHTMLReader(){}


  /***********************************************************************************************
  Reads the content text of the <code>root</code> tag
  @param currentNode the tag that is having its contents read.
  ***********************************************************************************************/
  public static void readText(XMLNode currentNode){
    try {

      //System.out.println("text");

      if (character == '>') {
        readChar();
      }

      String text = "";

      boolean previousWhite = false;
      while (character != '<') {
        if (manipulator.getOffset() >= manipulator.getLength()) {
          return;
        }

        if (character == '\n' || character == '\t' || character == '\r') {
          character = ' ';
        }

        if (character == ' ') {
          if (previousWhite) {
            // don't write duplicate white spaces
          }
          else {
            // write the single white space, and set up to detect duplicates
            previousWhite = true;
            text += character;
          }
        }
        else {
          // write the normal text
          previousWhite = false;
          text += character;
        }

        readChar();
      }

      // add the text as a child on the current tag
      if (!text.equals("") && !(text.equals(" "))) {
        //root.addContent(text);
        XMLNode textNode = new XMLNode("!TEXT!",text);
        currentNode.addChild(textNode);
      }

      readTag(currentNode);

      // done reading the text

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }
}