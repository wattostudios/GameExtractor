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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.watto.SingletonManager;
import org.watto.component.tree.EditableTreeNode;
import org.watto.event.WSCellEditableInterface;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.listener.WSCellEditableListener;
import org.watto.event.listener.WSClickableListener;
import org.watto.event.listener.WSKeyableListener;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.task.Task;
import org.watto.task.Task_WriteEditedPreview;
import org.watto.xml.XMLReader;

public class PreviewPanel_Tree extends PreviewPanel implements WSKeyableInterface, WSClickableInterface, WSCellEditableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSTree preview = null;

  TreeNode rootNode = null;

  boolean objectChanged = false;

  WSButton saveButton = null;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public WSTree getTree() {
    return preview;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public TreeNode getRootNode() {
    return rootNode;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PreviewPanel_Tree(TreeNode nodes) {
    super();

    objectChanged = false; // not edited

    preview = new WSTree();
    preview.setCode("PreviewTree");

    this.rootNode = nodes;
    preview.setModel(new DefaultTreeModel(nodes));

    preview.addKeyListener(new WSKeyableListener(this));
    preview.addMouseListener(new WSClickableListener(this));
    preview.setInvokesStopCellEditing(true);

    preview.setEditable(false); // default to not-editable

    /*
     // Moved to onOpenRequest()
    try {
      if (SingletonManager.has("CurrentViewer")) {
        ViewerPlugin viewerPlugin = (ViewerPlugin) SingletonManager.get("CurrentViewer");
        if (viewerPlugin != null) {
          if (viewerPlugin.canWrite(this)) {
          }

        }
      }
    }
    catch (Throwable t) {
      preview.setEditable(false);
    }
    */

    add(new JScrollPane(preview), BorderLayout.CENTER);

  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be opened.
   **********************************************************************************************
   **/
  @Override
  public void onOpenRequest() {
    // 3.16 Added "codes" to every XML-built object, so that they're cleaned up when the object is destroyed (otherwise it was being retained in the ComponentRepository)

    try {
      if (SingletonManager.has("CurrentViewer")) {
        ViewerPlugin viewerPlugin = (ViewerPlugin) SingletonManager.get("CurrentViewer");
        if (viewerPlugin != null) {
          if (viewerPlugin.canEdit(this)) {
          }

        }
      }
    }
    catch (Throwable t) {
      preview.setEditable(false);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    // Flush the variables clear for garbage collection

    if (objectChanged) {
      saveChanges();
    }

    preview = null;
    rootNode = null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent source, KeyEvent event) {
    if (source == preview) {
      if (!objectChanged) {
        if (saveButton != null) {
          saveButton.setEnabled(true);
        }
      }
      objectChanged = true;
      return true;
    }
    return false;
  }

  boolean editorListenerSet = false; // whether the editor listener has been set on the tree or not

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent source, MouseEvent event) {

    if (!editorListenerSet) {
      try {
        // can't do this in the constructor, as the cell editor hasn't been created yet. So do it here, once-off only.
        preview.getCellEditor().addCellEditorListener(new WSCellEditableListener(this));
        editorListenerSet = true;
      }
      catch (Throwable t) {
      }
    }

    if (source instanceof WSComponent) {
      WSComponent c = (WSComponent) source;
      String code = c.getCode();
      if (code.equals("PreviewPanel_Text_SaveChanges")) {
        if (objectChanged) {
          saveChanges();
        }
        return true;
      }
      else if (c == preview) {
        if (event.getClickCount() == 1) {

          Object clickedNode = preview.getLastSelectedPathComponent();

          if (clickedNode != null && clickedNode instanceof EditableTreeNode) {
            preview.startEditingAtPath(preview.getSelectionPath());
          }
        }
      }
    }
    return false;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public boolean isObjectChanged() {
    return objectChanged;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setObjectChanged(boolean changed) {
    this.objectChanged = changed;
    saveButton.setEnabled(changed);
  }

  /**
  **********************************************************************************************
  1. Export the file, if it isn't already
  2. Save the changes to the exported file
  3. Set the Archive as being edited
  **********************************************************************************************
  **/
  public void saveChanges() {
    Task_WriteEditedPreview task = new Task_WriteEditedPreview(this);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  Enables the Save button if the value in the tree node was changed
  **********************************************************************************************
  **/
  public void editingStopped(ChangeEvent e) {
    try {
      TreePath path = preview.getEditingPath();
      if (path != null) {
        Object node = path.getLastPathComponent();
        if (node != null && node instanceof EditableTreeNode) {
          EditableTreeNode editNode = (EditableTreeNode) node;

          String newValue = (String) preview.getCellEditor().getCellEditorValue();

          if (editNode.wasEdited() || !newValue.equals(editNode.getUserObject())) {
            setObjectChanged(true);
          }
        }
      }
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  N/A
  **********************************************************************************************
  **/
  public void editingCanceled(ChangeEvent e) {
  }

}