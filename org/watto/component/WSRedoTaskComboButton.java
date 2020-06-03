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

import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.SingletonManager;
import org.watto.TypecastSingletonManager;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSEvent;
import org.watto.task.Task;
import org.watto.task.TaskManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * A Redo Task Combo Button GUI <code>Component</code>
 ***********************************************************************************************/

public class WSRedoTaskComboButton extends WSComboButton implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The <code>TaskManager</code> that this menu works over **/
  String taskManager = null;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSRedoTaskComboButton() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSRedoTaskComboButton(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
   * Adds the redo <code>Task</code>s to the menu
   ***********************************************************************************************/
  public void addMenuItems() {

    popupMenu = new WSPopupMenu(new XMLNode(""));

    Task[] tasks = new Task[0];

    try {
      TaskManager manager = (TaskManager) SingletonManager.get(taskManager);
      tasks = manager.getRedoableTasks();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    for (int i = 0; i < tasks.length; i++) {
      // add an undo task to the menu
      Task task = tasks[i];
      WSTaskMenuItem item = new WSTaskMenuItem(XMLReader.read("<WSTaskMenuItem code=\"" + task.toString() + "\" />"), task);
      item.setParentType(WSTaskMenuItem.PARENT_REDO);
      popupMenu.add(item);
    }

    if (tasks.length <= 0) {
      // add a disabled item saying that there are no undo tasks
      WSTaskMenuItem item = new WSTaskMenuItem(XMLReader.read("<WSTaskMenuItem enabled=\"false\" code=\"Empty\" />"));
      popupMenu.add(item);
    }
  }

  /***********************************************************************************************
   * Gets the <code>TaskManager</code> assigned to this menu
   * @return the <code>taskManager</code>
   ***********************************************************************************************/
  public String getTaskManager() {
    return taskManager;
  }

  /***********************************************************************************************
   * Performs an action when a <code>MouseEvent</code> event is triggered
   * @param source the <code>JComponent</code> that triggered the event
   * @param event the <code>MouseEvent</code>
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onClick(JComponent source, MouseEvent event) {
    if (source instanceof WSButton) {
      String code = ((WSComponent) source).getCode();

      if (code.equals("RedoTaskComboButton")) {

        try {
          TaskManager manager = (TaskManager) SingletonManager.get(taskManager);
          manager.redo();
        }
        catch (Throwable t) {
          ErrorLogger.log(t);
        }
        return true;
      }
    }

    return super.onClick(source, event);
  }

  /***********************************************************************************************
   * Rebuild the menu when an undo/redo task has changed
   ***********************************************************************************************/
  @Override
  public boolean onEvent(Object source, WSEvent event, int type) {
    if (type == WSEvent.TASK_LIST_CHANGED) {
      rebuild();
      return true;
    }

    return super.onEvent(source, event, type);
  }

  /***********************************************************************************************
   * Rebuilds the menu
   ***********************************************************************************************/
  public void rebuild() {
    //removeAll();
    addMenuItems();
  }

  /***********************************************************************************************
   * Sets the <code>TaskManager</code> assigned to this menu
   * @param taskManager the <code>TaskManager</code>
   ***********************************************************************************************/
  public void setTaskManager(String taskManager) {
    this.taskManager = taskManager;

    TaskManager manager = TypecastSingletonManager.getTaskManager(taskManager);
    if (manager != null) {
      manager.addMonitor(this);
    }

  }

  /***********************************************************************************************
   * Builds this <code>WSComponent</code> from the properties of the <code>node</code>
   * @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to
   *        construct
   ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    String code = node.getAttribute("code");
    if (code == null) {
      code = "RedoTaskComboButton";
    }

    node.addChild(XMLReader.read("<WSButton code=\"" + code + "\" opaque=\"false\" showText=\"false\" repository=\"false\" />"));

    super.toComponent(node);

    //if (node.getAttribute("code") == null){
    setCode(code);
    //  }

    // set the TaskManager managing this Component
    String managerAttribute = node.getAttribute("manager");
    if (managerAttribute == null) {
      setTaskManager("TaskManager");
    }
    else {
      setTaskManager(managerAttribute);
    }

    addMenuItems();
  }

  /***********************************************************************************************
   * Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
   * @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
   ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);
    node.addAttribute("manager", taskManager);
    return node;
  }

}