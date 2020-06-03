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

import java.util.Enumeration;


/***********************************************************************************************
An <code>Enumeration</code> of the children in an <code>XMLNode</code>
@see org.watto.xml.XMLNode
***********************************************************************************************/
public class XMLChildEnumeration implements Enumeration<XMLNode> {

  /** The node whose children are being enumerated over **/
  XMLNode node;

  /** the position of the current child in the array **/
  int childPos = 0;


  /***********************************************************************************************
  Creates an <code>Enumeration</code> over the children of the given <code>node</code>
  @param node the <code>XMLNode</code> to enumerate over
  ***********************************************************************************************/
  public XMLChildEnumeration(XMLNode node){
    this.node = node;
  }


  /***********************************************************************************************
  Are there any more children on this <code>node</code>?
  @return <b>true</b> if there are more children<br />
          <b>false</b> if there are no more children
  ***********************************************************************************************/
  public boolean hasMoreElements(){
    if (childPos < node.getChildCount()) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Gets the next child from the <code>node</code>
  @return the next child
  ***********************************************************************************************/
  public XMLNode nextElement(){
    XMLNode child;
    
    try {
      child = node.getChild(childPos);
    }
    catch (Throwable t) {
      child = null;
    }
    
    childPos++;
    return child;
  }
}