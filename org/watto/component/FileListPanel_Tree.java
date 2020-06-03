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
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.StringTokenizer;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.FakeResource;
import org.watto.datatype.Resource;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSDoubleClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.WSMotionableInterface;
import org.watto.event.WSRightClickableInterface;
import org.watto.event.WSTransferableInterface;
import org.watto.event.listener.WSClickableListener;
import org.watto.event.listener.WSTransferableListener;
import org.watto.ge.helper.FileListSorter;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLReader;

public class FileListPanel_Tree extends FileListPanel implements WSClickableInterface,
    WSDoubleClickableInterface,
    WSMotionableInterface,
    WSTransferableInterface,
    WSKeyableInterface,
    WSRightClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSTree tree;

  FileListModel_Tree root;

  WSTable propTable;

  FileListModel_Table propModel;

  JPanel propPanel;

  /** for tooltips **/
  String lastMotionObject = "";

  /** Set to true when reload() so that all the column widths aren't reset to 75 **/
  boolean initColumnSizes = false;

  // files being dropped from a drag-drop operation
  File[] dropFiles;

  // the point where the drop occurred
  Point dropPoint;

  /** the selected row when right-clicking, for things like PreviewFile that only want 1 file **/
  int rightClickSelectedRow = -1;

  /** for wrapping around when pressing up or down on the table **/
  boolean moveAgain = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListPanel_Tree() {
    super("Tree");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void addFilesFromDrop() {
    addFilesFromDrop(dropFiles);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void changeSelection(int row) {
    if (tree.isRowSelected(row)) {
      tree.removeSelectionRow(row);
    }
    else {
      tree.addSelectionRow(row);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void constructInterface() {
    removeAll();

    tree = new WSTree(XMLReader.read("<WSTree code=\"FileList\" repository=\"false\" />"));
    tree.setModel(new DefaultTreeModel(root));

    propTable = new WSTable(XMLReader.read("<WSTable code=\"PropList\" />"));
    propTable.disableAutomaticKeyEvent("pressed ENTER");

    // option to auto-resize columns to fit, or allow overflow horizontally (with scrollbar)
    if (!Settings.getBoolean("AutoResizeTableColumns")) {
      propTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }
    else {
      propTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    add(new JScrollPane(tree), BorderLayout.CENTER);

    propPanel = new WSPanel(XMLReader.read("<WSPanel code=\"FileListPanel_Tree_Properties\" showLabel=\"true\" showBorder=\"true\" border-width=\"3\" />"));
    WSPanel propTableHolder = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" opaque=\"false\" />"));
    propTableHolder.add(propTable, BorderLayout.CENTER);
    propPanel.add(propTableHolder, BorderLayout.CENTER);
    add(propPanel, BorderLayout.SOUTH);

    //propPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 100));
    propPanel.setVisible(false);

    propTable.setColumnSelectionAllowed(false);

    JTableHeader propTableHeader = propTable.getTableHeader();
    propTableHeader.setReorderingAllowed(false);
    propTableHeader.setResizingAllowed(true);
    propTableHeader.addMouseListener(new WSClickableListener(this));

    try {
      ((DefaultCellEditor) propTable.getDefaultEditor(Object.class)).setClickCountToStart(1);
      ((DefaultCellEditor) propTable.getDefaultEditor(Number.class)).setClickCountToStart(1);
    }
    catch (Throwable t) {
    }

    tree.setTransferHandler(new WSTransferableListener(this));

    // remove the key listener that listens for letter keys and selects the next match
    // (as we have implemented it manually, and better for our purpose, below).
    java.awt.event.KeyListener[] keys = tree.getKeyListeners();
    for (int i = 0; i < keys.length; i++) {
      tree.removeKeyListener(keys[i]);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void dropFiles(File[] files) {
    dropPoint = MouseInfo.getPointerInfo().getLocation();
    WSPopupMenu menu = getDropFilesMenu();

    int x = (int) dropPoint.getX();
    int y = (int) dropPoint.getY();

    Point location = getLocationOnScreen();

    x -= location.getX();
    y -= location.getY();

    // move the menu a little to the top-left
    if (x >= 5) {
      x -= 5;
    }
    if (y >= 5) {
      y -= 5;
    }

    dropFiles = files;

    boolean showMenu = !checkDragDropOption();
    if (showMenu) {
      menu.show(this, x, y);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void editNextCell(JTable table, boolean alreadyMoved) {
    // move to the next editable cell, and start editing it

    int row = table.getEditingRow();
    int col = table.getEditingColumn();

    if (row == -1) {
      row = table.getSelectedRow();
    }
    if (col == -1) {
      col = table.getSelectedColumn();
    }
    //col++; // already moved to the next column by the TAB key

    int origRow = row;
    int origCol = col;

    int numRows = table.getRowCount();
    int numCols = table.getColumnCount();

    boolean found = false;

    while (!found && row < numRows) {
      while (!found && col < numCols) {
        if (table.isCellEditable(row, col)) {
          found = true;
        }
        else {
          col++;
        }
      }
      if (!found) {
        row++;
        col = 0;
      }
    }

    if (!found) {
      //start again from the top
      row = 0;
      col = 0;

      while (!found && row <= origRow) {
        while (!found && col < numCols) {
          if (table.isCellEditable(row, col)) {
            found = true;
          }
          else {
            col++;
          }
        }
        if (!found) {
          row++;
          col = 0;
        }
      }

    }

    if (found) {
      if (table.isEditing()) {
        table.getCellEditor().stopCellEditing();
      }

      if (row < table.getRowCount()) {
        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(col, col);
        table.editCellAt(row, col);
        try {
          JTextField textField = ((JTextField) ((DefaultCellEditor) table.getCellEditor()).getComponent());
          textField.selectAll();
          textField.requestFocus();
        }
        catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void editNextRow(JTable table, boolean alreadyMoved) {
    // move to the next row, and start editing it

    int numRows = table.getRowCount();

    int row = table.getSelectedRow();
    if (!alreadyMoved) {
      row++;
      moveAgain = false;
    }
    else if (alreadyMoved && row == numRows - 1) {
      if (moveAgain) {
        row++;
        //moveAgain = false;
      }
      else {
        moveAgain = true;
      }
    }
    else {
      moveAgain = false;
    }
    int col = table.getSelectedColumn();

    if (row >= numRows) {
      row = 0; // start again from the top
    }

    if (row > numRows) {
      // no rows in the table
      return;
    }

    if (table.isEditing()) {
      table.getCellEditor().stopCellEditing();
    }

    if (row < numRows) {
      table.setRowSelectionInterval(row, row);
      table.setColumnSelectionInterval(col, col);
      table.scrollRectToVisible(table.getCellRect(row, col, true));
      table.editCellAt(row, col);
      try {
        JTextField textField = ((JTextField) ((DefaultCellEditor) table.getCellEditor()).getComponent());
        textField.selectAll();
        textField.requestFocus();
      }
      catch (Throwable t) {
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void editPreviousCell(JTable table, boolean alreadyMoved) {
    // move to the next editable cell, and start editing it

    int row = table.getEditingRow();
    int col = table.getEditingColumn();

    if (row == -1) {
      row = table.getSelectedRow();
    }
    if (col == -1) {
      col = table.getSelectedColumn();
    }
    //col++; // already moved to the next column by the LEFT key

    int origRow = row;
    int origCol = col;

    int numRows = table.getRowCount();
    int numCols = table.getColumnCount();

    boolean found = false;

    while (!found && row >= 0) {
      while (!found && col >= 0) {
        if (table.isCellEditable(row, col)) {
          found = true;
        }
        else {
          col--;
        }
      }
      if (!found) {
        row--;
        col = numCols - 1;
      }
    }

    if (!found) {
      //start again from the top
      row = numRows - 1;
      col = numCols - 1;

      while (!found && row >= origRow) {
        while (!found && col >= 0) {
          if (table.isCellEditable(row, col)) {
            found = true;
          }
          else {
            col--;
          }
        }
        if (!found) {
          row--;
          col = numCols - 1;
        }
      }

    }

    if (found) {
      if (table.isEditing()) {
        table.getCellEditor().stopCellEditing();
      }

      if (row < table.getRowCount()) {
        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(col, col);
        table.editCellAt(row, col);
        try {
          JTextField textField = ((JTextField) ((DefaultCellEditor) table.getCellEditor()).getComponent());
          textField.selectAll();
          textField.requestFocus();
        }
        catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void editPreviousRow(JTable table, boolean alreadyMoved) {
    // move to the next row, and start editing it

    int row = table.getSelectedRow();
    if (!alreadyMoved) {
      row--;
      moveAgain = false;
    }
    else if (alreadyMoved && row == 0) {
      if (moveAgain) {
        row--;
        //moveAgain = false;
      }
      else {
        moveAgain = true;
      }
    }
    else {
      moveAgain = false;
    }
    int col = table.getSelectedColumn();

    if (row < 0) {
      row = table.getRowCount() - 1; // start again from the top
    }

    if (row < 0) {
      // no rows in the table
      return;
    }

    if (table.isEditing()) {
      table.getCellEditor().stopCellEditing();
    }

    if (row < table.getRowCount()) {
      table.setRowSelectionInterval(row, row);
      table.setColumnSelectionInterval(col, col);
      table.scrollRectToVisible(table.getCellRect(row, col, true));
      table.editCellAt(row, col);
      try {
        JTextField textField = ((JTextField) ((DefaultCellEditor) table.getCellEditor()).getComponent());
        textField.selectAll();
        textField.requestFocus();
      }
      catch (Throwable t) {
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  private void expandAll() {

    // runs backwards so that it only expands the first level of nodes, not every node
    for (int i = tree.getRowCount() - 1; i >= 0; i--) {
      tree.expandRow(i);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getFirstSelectedRow() {
    int selected = tree.getLeadSelectionRow();
    if (selected < 0) {
      return -1;
    }
    return selected;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getNumSelected() {
    //return tree.getSelectionCount();  //can't use this - don't want to include FakeResources
    return getSelected().length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource getResource(int row) {
    return ((FileListModel_Tree) tree.getPathForRow(row).getLastPathComponent()).getResource();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] getSelected() {

    TreePath[] paths = tree.getSelectionPaths();
    if (paths == null || paths.length <= 0) {
      return new Resource[0];
    }

    Resource[] resources = new Resource[paths.length];
    int resourcePos = 0;
    for (int i = 0; i < paths.length; i++) {
      //resources[i] = ((Resource)paths[i].getLastPathComponent());
      resources[resourcePos] = ((FileListModel_Tree) paths[i].getLastPathComponent()).getResource();
      if (!(resources[resourcePos] instanceof FakeResource)) {
        resourcePos++;
      }
    }

    // removing nulls created by directories
    if (resources.length != resourcePos) {
      Resource[] temp = resources;
      resources = new Resource[resourcePos];
      System.arraycopy(temp, 0, resources, 0, resourcePos);
    }

    return resources;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {
    if (c instanceof WSMenuItem) {
      String code = ((WSMenuItem) c).getCode();
      if (code.equals("FileListDrop_Add")) {
        addFilesFromDrop();
        return true;
      }
      else if (code.equals("FileListDrop_ReplaceCurrent")) {
        replaceCurrentFileFromDrop();
        return true;
      }
      else if (code.equals("FileListDrop_ReplaceMatching")) {
        replaceMatchingFilesFromDrop();
        return true;
      }
      else if (code.equals("FileListDrop_ReadArchive")) {
        readArchiveFromDrop();
        return true;
      }

      else if (code.equals("FileList_RightClick_PreviewResource")) {
        // select only the 1 row chosen, for the preview
        int[] selRows = tree.getSelectionRows();
        tree.setSelectionRow(rightClickSelectedRow);

        setSidePanel("Preview");

        // re-select the rows
        tree.setSelectionRows(selRows);
        return true;
      }
      else if (code.equals("FileList_RightClick_HexEditor")) {
        // select only the 1 row chosen, for the hex editor
        int[] selRows = tree.getSelectionRows();
        tree.setSelectionRow(rightClickSelectedRow);

        setSidePanel("HexEditor");

        // re-select the rows
        tree.setSelectionRows(selRows);
        return true;
      }
      else if (code.equals("FileList_RightClick_ImageInvestigator")) {
        // select only the 1 row chosen, for the preview
        int[] selRows = tree.getSelectionRows();
        tree.setSelectionRow(rightClickSelectedRow);

        setSidePanel("ImageInvestigator");

        // re-select the rows
        tree.setSelectionRows(selRows);
        return true;
      }
      else if (code.equals("FileList_RightClick_ExtractResources_Selected")) {
        ((SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList")).exportSelectedFiles();
        return true;
      }
      else if (code.equals("FileList_RightClick_ExtractResources_All")) {
        ((SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList")).exportAllFiles();
        return true;
      }
      else if (code.equals("FileList_RightClick_RemoveResources")) {
        ((SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList")).removeFiles(getSelected());
        return true;
      }
      else if (code.equals("FileList_RightClick_RenameResources")) {
        setSidePanel("RenameFile");
        return true;
      }
      else if (code.equals("FileList_RightClick_SelectResources_All")) {
        selectAll();
        return true;
      }
      else if (code.equals("FileList_RightClick_SelectResources_None")) {
        selectNone();
        return true;
      }
      else if (code.equals("FileList_RightClick_SelectResources_Inverse")) {
        selectInverse();
        return true;
      }
      else if (code.equals("FileList_RightClick_FileListView_Table")) {
        setFileListPanel("Table");
        return true;
      }
      else if (code.equals("FileList_RightClick_FileListView_Tree")) {
        setFileListPanel("Tree");
        return true;
      }
      else if (code.equals("FileList_RightClick_FileListView_TreeTable")) {
        setFileListPanel("TreeTable");
        return true;
      }
      else if (code.equals("FileList_RightClick_FileListView_Thumbnails")) {
        setFileListPanel("Thumbnails");
        return true;
      }

    }
    else {
      ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onClick(c, e);
      return true;
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onDoubleClick(JComponent c, java.awt.event.MouseEvent e) {
    if (Settings.getBoolean("PreviewOnDoubleClick")) {
      // double-clicking the filelist does previewing of the file, unless on the hex side panel
      WSSidePanelHolder holder = ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder"));
      WSPanel panel = holder.getCurrentPanel();
      if (panel == null || (!panel.getCode().equals("SidePanel_HexEditor") && !panel.getCode().equals("SidePanel_Preview"))) {
        holder.loadPanel("SidePanel_Preview", false);
        return true;
      }
    }

    ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onDoubleClick(c, e);
    return true;
  }

  /**
  **********************************************************************************************
  Creates a TransferHandler for this component, allowing it to be dragged.
  @param c the component that will be dragged
  @param e the dragging event
  @return the TransferHandler for this component
  **********************************************************************************************
  **/
  @Override
  public TransferHandler onDrag(JComponent c, MouseEvent e) {
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public boolean onKeyPress(JComponent c, java.awt.event.KeyEvent e) {
    if (c == tree) {
      // move to the next file starting with this letter

      char keyCode = e.getKeyChar();
      if (keyCode == KeyEvent.CHAR_UNDEFINED || keyCode == '?') {
        ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onKeyPress(c, e);
        return true; // not a letter or number
      }

      if (e.getKeyCode() == KeyEvent.VK_ENTER) {

        // first, determine if a branch or a file
        TreeNode node = (TreeNode) tree.getLastSelectedPathComponent();
        if (!node.isLeaf()) {
          // branch
          TreePath path = tree.getSelectionPath();
          if (tree.isExpanded(path)) {
            // collapse
            tree.collapsePath(path);
          }
          else {
            // expand
            tree.expandPath(path);
          }
        }

        // preview the selected file
        if (Settings.getBoolean("PreviewOnFileListEnter")) {
          WSSidePanelHolder holder = ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder"));
          WSPanel panel = holder.getCurrentPanel();
          if (panel != null) {
            if (panel.getCode().equals("SidePanel_HexEditor")) {
              // load the file into the hex editor
              ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onKeyPress(c, e);
            }
            else {
              // load the file into the preview
              holder.loadPanel("SidePanel_Preview", false);
            }
            tree.requestFocus();
          }
          return true;
        }
      }

      keyCode = ("" + keyCode).toLowerCase().charAt(0);
      char keyCodeCaps = ("" + keyCode).toUpperCase().charAt(0);

      int numFiles = tree.getRowCount();
      int[] selectedRows = tree.getSelectionRows();
      if (selectedRows.length <= 0) {
        return true;
      }
      int selectedIndex = selectedRows[0] + 1;

      if (selectedIndex >= numFiles) {
        selectedIndex = 0;
      }

      char columnChar = 'P';
      //ArchivePlugin plugin = Archive.getReadPlugin();

      // search the bottom half of the list
      for (int i = selectedIndex; i < numFiles; i++) {
        String filename = tree.getPathForRow(i).getLastPathComponent().toString();
        if (filename.length() > 0) {
          char currentChar = filename.charAt(0);
          if (currentChar == keyCode || currentChar == keyCodeCaps) {
            tree.setSelectionRow(i);
            tree.scrollRowToVisible(i);
            return true;
          }
        }
      }

      if (selectedIndex == 0) {
        // we started searching from the start of the list, so we don't want to re-search
        return false;
      }

      //  search the top half of the list, if not found in the bottom half.
      for (int i = 0; i <= selectedIndex; i++) {
        String filename = tree.getPathForRow(i).getLastPathComponent().toString();
        if (filename.length() > 0) {
          char currentChar = filename.charAt(0);
          if (currentChar == keyCode || currentChar == keyCodeCaps) {
            tree.setSelectionRow(i);
            tree.scrollRowToVisible(i);
            return true;
          }
        }
      }

    }
    else if (c == propTable) {
      // move to the next file starting with this letter

      int keyCodeInt = e.getKeyCode();
      if (keyCodeInt == KeyEvent.VK_ENTER) {
        editNextRow(propTable, false);
        return true;
      }
      else if (keyCodeInt == KeyEvent.VK_TAB) {
        editNextCell(propTable, true);
        return true;
      }
      else if (keyCodeInt == KeyEvent.VK_UP) {
        editPreviousRow(propTable, true);
        return true;
      }
      else if (keyCodeInt == KeyEvent.VK_DOWN) {
        editNextRow(propTable, true);
        return true;
      }

      char keyCode = e.getKeyChar();
      if (keyCode == KeyEvent.CHAR_UNDEFINED || keyCode == '?') {
        ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onKeyPress(c, e);
        return true; // not a letter or number
      }

      if (propTable.isEditing()) {
        // if editing, don't change the selection
        ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onKeyPress(c, e);
        return true;
      }

      keyCode = ("" + keyCode).toLowerCase().charAt(0);
      char keyCodeCaps = ("" + keyCode).toUpperCase().charAt(0);

      int numFiles = propTable.getRowCount();
      int selectedIndex = propTable.getSelectedRow() + 1;

      if (selectedIndex >= numFiles) {
        selectedIndex = 0;
      }

      char columnChar = 'O';
      ArchivePlugin plugin = Archive.getReadPlugin();

      // search the bottom half of the list
      for (int i = selectedIndex; i < numFiles; i++) {
        String filename = (String) plugin.getColumnValue(propModel.getResource(i), columnChar);
        if (filename.length() > 0) {
          char currentChar = filename.charAt(0);
          if (currentChar == keyCode || currentChar == keyCodeCaps) {
            propTable.setRowSelectionInterval(i, i);
            return true;
          }
        }
      }

      if (selectedIndex == 0) {
        // we started searching from the start of the list, so we don't want to re-search
        return false;
      }

      //  search the top half of the list, if not found in the bottom half.
      for (int i = 0; i <= selectedIndex; i++) {
        String filename = (String) plugin.getColumnValue(propModel.getResource(i), columnChar);
        if (filename.length() > 0) {
          char currentChar = filename.charAt(0);
          if (currentChar == keyCode || currentChar == keyCodeCaps) {
            propTable.setRowSelectionInterval(i, i);
            return true;
          }
        }
      }

    }

    ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onKeyPress(c, e);
    return true;
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSMotionableListener when a component is moved over
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onMotion(JComponent c, java.awt.event.MouseEvent e) {
    // Shows the value of the cell in the statusbar
    if (c instanceof WSTree) {
      if (c == tree) {
        String selectedObject = tree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent().toString();
        if (selectedObject == null || lastMotionObject.equals(selectedObject)) {
          return true; // still over the same object on the list
        }
        lastMotionObject = selectedObject;

        String columnHeading = Language.get("WSTableColumn_FilePath_Text");

        ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(columnHeading + ": " + selectedObject);
        return true;
      }
    }
    if (c instanceof WSTable) {
      if (c == propTable) {
        Point point = e.getPoint();
        int row = propTable.rowAtPoint(point);
        int column = propTable.columnAtPoint(point);

        String selectedObject = propTable.getValueAt(row, column).toString();
        if (selectedObject == null || lastMotionObject.equals(selectedObject)) {
          return true; // still over the same object on the list
        }
        lastMotionObject = selectedObject;

        String columnHeading;
        try {
          columnHeading = Archive.getReadPlugin().getViewingPropColumn(column).toString();
        }
        catch (Throwable t) {
          columnHeading = Language.get("ColumnValue");
        }

        ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(columnHeading + ": " + selectedObject);
        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSRightClickableListener when a right click occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onRightClick(JComponent c, java.awt.event.MouseEvent e) {
    if (c == tree) {
      Point p = e.getPoint();
      dropPoint = p;

      rightClickSelectedRow = tree.getRowForLocation((int) p.getX(), (int) p.getY());
      if (!tree.isRowSelected(rightClickSelectedRow)) {
        // if not selected, just select the current row.
        // otherwise, it is already selected (and maybe more are selected too) so don't change the selection
        tree.setSelectionRow(rightClickSelectedRow);
      }

      WSPopupMenu menu = getRightClickMenu();
      menu.show(tree, (int) p.getX() - 10, (int) p.getY() - 10);

      return true;
    }

    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void readArchiveFromDrop() {
    readArchiveFromDrop(dropFiles);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void reload() {
    root = new FileListModel_Tree("Archive");
    tree.setModel(new DefaultTreeModel(root));

    if (Archive.getColumn(0) == null) {
      // on startup
      return;
    }

    Resource[] resources = FileListSorter.sort(Archive.getColumn(0), false);

    for (int i = 0; i < resources.length; i++) {
      // get the resource
      String name = resources[i].getName();

      int left = name.lastIndexOf("\\");
      int right = name.lastIndexOf("/");

      StringTokenizer token;
      if (left > right) {
        token = new StringTokenizer(name, "\\");
      }
      else {
        token = new StringTokenizer(name, "/");
      }

      int numTokens = token.countTokens();

      FileListModel_Tree parent = root;
      for (int j = 0; j < numTokens - 1; j++) {
        String directory = token.nextToken();
        boolean foundParent = false;
        //System.out.println("In Directory: " + ((String)parent.getUserObject()));
        for (int k = 0; k < parent.getChildCount(); k++) {
          if (((String) ((FileListModel_Tree) parent.getChildAt(k)).getUserObject()).equals(directory)) {
            //System.out.println("Found: " + directory);
            // found the correct parent directory
            FileListModel_Tree node = (FileListModel_Tree) parent.getChildAt(k);
            foundParent = true;
            k = parent.getChildCount();
            parent = node;
          }
        }

        if (!foundParent) {
          //System.out.println("Making Directory: " + directory);
          FileListModel_Tree node = new FileListModel_Tree(directory);
          parent.add(node);
          parent = node;
        }
      }

      // by now we should have the correct location for the file, so add the resource
      //System.out.println("Adding file to: " + ((String)parent.getUserObject()));
      String filename = token.nextToken();
      parent.add(new FileListModel_Tree(filename, resources[i]));
      //System.out.println("File: " + filename);
    }

    // sorts the tree under this node, and places the directories at the top of each branch
    root.sort();

    tree.repaint();
    expandAll();

    reloadPropTable();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadPropTable() {

    TableColumnModel columnModel = propTable.getColumnModel();
    WSTableColumn[] columns = Archive.getReadPlugin().getViewingPropColumns();

    // If the column counts are different, we need to build a new model.
    // This is because columns have been added or removed from the table
    // and we thus need to rebuild the model. Refreshing the model is not
    // sufficient in this case - crashes with ArrayIndexOutOfBounds.
    if (propModel == null || columnModel.getColumnCount() != columns.length) {
      propModel = new FileListModel_Table_Properties();
      propTable.setModel(propModel);
      columnModel = propTable.getColumnModel();
    }
    else {
      propModel.reload();
    }

    int screenWidth = getWidth();
    if (screenWidth <= 0) {
      // The FileListPanel hasn't been displayed yet.
      // Therefore, get the width of the FileListPanelHolder instead
      screenWidth = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getWidth();
    }

    initColumnSizes = true;
    for (int i = 0; i < columns.length; i++) {
      WSTableColumn columnDetails = columns[i];

      TableColumn column = columnModel.getColumn(i);
      column.setHeaderValue(columnDetails.getName());

      int minWidth = columnDetails.getMinWidth();
      int maxWidth = columnDetails.getMaxWidth();

      if (minWidth < 0) {
        minWidth = 0;
      }
      if (maxWidth < 0) {
        maxWidth = screenWidth;
      }

      column.setMinWidth(minWidth);
      column.setMaxWidth(maxWidth);
      column.setPreferredWidth(columnDetails.getWidth());
    }

    // repaint the table header in the correct colors
    JTableHeader header = propTable.getTableHeader();
    header.setBackground(LookAndFeelManager.getLightColor());
    header.setBorder(new LineBorder(LookAndFeelManager.getDarkColor(), 1));

    initColumnSizes = false;

    // Resize the properties table appropriately
    //propPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, propTable.getPreferredSize().height + 50));

    if (Archive.getReadPlugin().getNumProperties() > 0) {
      propPanel.setVisible(true);
    }
    else {
      propPanel.setVisible(false);
    }

    propTable.revalidate();
    propTable.repaint();

  }

  /**
  **********************************************************************************************
  Replace the current hovered file
  **********************************************************************************************
  **/
  @Override
  public void replaceCurrentFileFromDrop() {
    File newFile = null;
    for (int i = 0; i < dropFiles.length; i++) {
      if (!dropFiles[i].isDirectory()) {
        newFile = dropFiles[i];
        break;
      }
    }

    if (newFile == null) {
      // error - no file selected
      return;
    }

    int x = (int) dropPoint.getX();
    int y = (int) dropPoint.getY();

    Point location = tree.getLocationOnScreen();

    x -= location.getX();
    y -= location.getY();

    int row = tree.getRowForLocation(x, y);
    if (row <= -1) {
      // error - no row under cursor
      return;
    }
    Resource currentFile = getResource(row);

    replaceCurrentFileFromDrop(currentFile, newFile);
  }

  /**
  **********************************************************************************************
  Replace matching files
  **********************************************************************************************
  **/
  @Override
  public void replaceMatchingFilesFromDrop() {
    if (dropFiles.length == 1 && dropFiles[0].isDirectory()) {
      replaceMatchingFilesFromDrop(Archive.getResources(), dropFiles[0]);
    }
    else {
      File file = dropFiles[0].getParentFile();
      if (file != null) {
        replaceMatchingFilesFromDrop(Archive.getResources(), file);
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void selectAll() {
    tree.setSelectionInterval(0, tree.getRowCount());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void selectInverse() {

    tree.setVisible(false);

    int[] rows = tree.getSelectionRows();
    tree.clearSelection();

    int rowPos = 0;
    for (int i = 0; i < tree.getRowCount(); i++) {
      if (rowPos < rows.length && rows[rowPos] == i) {
        rowPos++;
      }
      else {
        tree.addSelectionRow(i);
      }
    }

    tree.setVisible(true);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void selectNone() {
    tree.clearSelection();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void selectResource(int row) {
    selectNone();
    changeSelection(row);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFileListPanel(String name) {
    Settings.set("FileListView", name);
    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).loadPanel(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSidePanel(String name) {
    Settings.set("AutoChangedToHexPreview", "false");
    ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_" + name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void stopInlineEditing() {
    // if inline editing, stop the editing first.
    if (propTable.isEditing()) {
      propTable.getCellEditor().stopCellEditing();
    }
  }

}