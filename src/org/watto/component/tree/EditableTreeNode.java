////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2022  WATTO Studios                           //
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

package org.watto.component.tree;

import javax.swing.tree.DefaultMutableTreeNode;


/***********************************************************************************************
A TreeNode that can be be enabled/disabled for editing. Needs to be used with a WSTable.
***********************************************************************************************/
public class EditableTreeNode extends DefaultMutableTreeNode {

  /***********************************************************************************************

   ***********************************************************************************************/
  public boolean isEditable() {
    return editable;
  }

  /***********************************************************************************************

   ***********************************************************************************************/
  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  /**  **/
  private static final long serialVersionUID = 1L;
  boolean editable = false;

  /***********************************************************************************************

   ***********************************************************************************************/
  public EditableTreeNode(){
    super();
  }
  
  /***********************************************************************************************

   ***********************************************************************************************/
  public EditableTreeNode(String value){
    super(value);
  }
  
  /***********************************************************************************************

   ***********************************************************************************************/
  public EditableTreeNode(String value, boolean isEditable){
    super(value);
    this.editable = isEditable;
  }
  
  Object originalObject = null;
  
  
  /***********************************************************************************************

   ***********************************************************************************************/
  public void setUserObject(Object userObject) {
    if (originalObject == null) {
      originalObject = getUserObject();
    }
    super.setUserObject(userObject);
  }
  
  
  /***********************************************************************************************

   ***********************************************************************************************/
  public boolean wasEdited() {
    if (originalObject == null) {
      return false;
    }
    
    if (!(originalObject.equals(super.getUserObject()))){
      return true;
    }
    
    return false;

  }


}