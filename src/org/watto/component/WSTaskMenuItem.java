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

package org.watto.component;

import org.watto.task.Task;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Task Menu Item GUI <code>Component</code>
***********************************************************************************************/

public class WSTaskMenuItem extends WSMenuItem {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  public final static int PARENT_UNDO = 1;

  public final static int PARENT_REDO = 2;

  /** The <code>Task</code> **/
  Task task = null;
  /** the type of parent holding this task - either Redo or Undo **/
  int parentType = -1;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSTaskMenuItem() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  ***********************************************************************************************/
  public WSTaskMenuItem(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>, and sets the
  <code>Task</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  @param task the <code>Task</code>
  ***********************************************************************************************/
  public WSTaskMenuItem(XMLNode node, Task task) {
    this(node);
    setTask(task);
  }

  public int getParentType() {
    return parentType;
  }

  /***********************************************************************************************
  Gets the <code>Task</code>
  @return the <code>task</code>
  ***********************************************************************************************/
  public Task getTask() {
    return task;
  }

  /***********************************************************************************************
  Gets the text code for this <code>WSComponent</code>, which is used for <code>Language</code>s
  and other functionality
  @return the text code for this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public String getText() {
    return code;
  }

  public void setParentType(int parentType) {
    this.parentType = parentType;
  }

  /***********************************************************************************************
  Sets the <code>Task</code>
  @param task the <code>Task</code>
  ***********************************************************************************************/
  public void setTask(Task task) {
    this.task = task;
    setCode(task.toString());
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    // Sets the generic properties of this component
    WSHelper.setAttributes(node, this);

    setOpaque(false);
  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);
    return node;
  }

}