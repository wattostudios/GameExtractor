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
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Enumeration;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSDoubleClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.WSMotionableInterface;
import org.watto.event.WSMouseReleasableInterface;
import org.watto.event.WSRightClickableInterface;
import org.watto.event.WSTableColumnableInterface;
import org.watto.event.WSTransferableInterface;
import org.watto.event.listener.WSClickableListener;
import org.watto.event.listener.WSTransferableListener;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLReader;

public class FileListPanel_Table extends FileListPanel implements WSClickableInterface,
    WSDoubleClickableInterface,
    WSKeyableInterface,
    WSTableColumnableInterface,
    WSMotionableInterface,
    WSTransferableInterface,
    WSRightClickableInterface,
    WSMouseReleasableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSTable table;

  FileListModel_Table model;

  WSTable propTable;

  FileListModel_Table propModel;

  JPanel propPanel;

  /** Set to true when reload() so that all the column widths aren't reset to 75 **/
  boolean initColumnSizes = false;

  /** so the column sizes are only changed once **/
  int changeColumnNumber = 0;

  /** for tooltips **/
  //String lastMotionObject = "";
  int lastMotionRow = -1; // for the table

  int lastMotionColumn = -1; // for the table

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
  public FileListPanel_Table() {
    super("Table");
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
    table.changeSelection(row, 0, true, false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void constructInterface() {
    removeAll();

    table = new WSTable(XMLReader.read("<WSTable code=\"FileList\" repository=\"false\" />"));
    propTable = new WSTable(XMLReader.read("<WSTable code=\"PropList\" />"));

    // pressing enter defaults to moving to the next row - don't want to do this
    table.disableAutomaticKeyEvent("pressed ENTER");
    propTable.disableAutomaticKeyEvent("pressed ENTER");

    // option to auto-resize columns to fit, or allow overflow horizontally (with scrollbar)
    if (!Settings.getBoolean("AutoResizeTableColumns")) {
      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      propTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }
    else {
      table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
      propTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    JScrollPane scrollPane = new JScrollPane(table);
    add(scrollPane, BorderLayout.CENTER);

    propPanel = new WSPanel(XMLReader.read("<WSPanel code=\"FileListPanel_Table_Properties\" showLabel=\"true\" showBorder=\"true\" border-width=\"3\" />"));
    WSPanel propTableHolder = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" opaque=\"false\" />"));
    propTableHolder.add(propTable, BorderLayout.CENTER);
    propPanel.add(propTableHolder, BorderLayout.CENTER);
    add(propPanel, BorderLayout.SOUTH);

    //propPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 100));
    propPanel.setVisible(false);

    table.setColumnSelectionAllowed(false);
    propTable.setColumnSelectionAllowed(false);

    table.setTransferHandler(new WSTransferableListener(this));
    scrollPane.setTransferHandler(new WSTransferableListener(this));

    JTableHeader tableHeader = table.getTableHeader();
    tableHeader.setReorderingAllowed(false);
    tableHeader.setResizingAllowed(true);
    tableHeader.addMouseListener(new WSClickableListener(this));

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
  @Override
  public int getFirstSelectedRow() {
    return table.getSelectedRow();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getNumSelected() {
    return table.getSelectedRowCount();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource getResource(int row) {
    return model.getResource(row);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] getSelected() {
    int[] selectedRows = table.getSelectedRows();
    Resource[] resources = new Resource[selectedRows.length];
    for (int i = 0; i < resources.length; i++) {
      resources[i] = model.getResource(selectedRows[i]);
    }

    return resources;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {
    if (c instanceof JTableHeader) {
      TableColumnModel columnModel = table.getColumnModel();
      int viewColumn = columnModel.getColumnIndexAtX(e.getX());
      int column = table.convertColumnIndexToModel(viewColumn);

      if (column != -1) {
        // if inline editing, stop the editing first.
        if (table.isEditing()) {
          table.getCellEditor().stopCellEditing();
        }

        FileListModel_Table model = (FileListModel_Table) table.getModel();
        model.sortResources(column);
        table.repaint();
        return true;
      }
    }
    else if (c == table) {
      int column = table.getSelectedColumn();
      int row = table.getSelectedRow();
      if (model.isCellEditable(row, column)) {
        table.editCellAt(row, column);
        JTextField textField = ((JTextField) ((DefaultCellEditor) table.getCellEditor()).getComponent());
        textField.selectAll();
        textField.requestFocus();
      }
    }
    else {
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
          int[] selRows = table.getSelectedRows();
          table.setRowSelectionInterval(rightClickSelectedRow, rightClickSelectedRow);

          setSidePanel("Preview");

          // re-select the rows
          table.clearSelection();
          for (int i = 0; i < selRows.length; i++) {
            changeSelection(selRows[i]);
          }
          return true;
        }
        else if (code.equals("FileList_RightClick_HexEditor")) {
          // select only the 1 row chosen, for the hex editor
          int[] selRows = table.getSelectedRows();
          table.setRowSelectionInterval(rightClickSelectedRow, rightClickSelectedRow);

          setSidePanel("HexEditor");

          // re-select the rows
          table.clearSelection();
          for (int i = 0; i < selRows.length; i++) {
            changeSelection(selRows[i]);
          }
          return true;
        }
        else if (code.equals("FileList_RightClick_ImageInvestigator")) {
          // select only the 1 row chosen, for the preview
          int[] selRows = table.getSelectedRows();
          table.setRowSelectionInterval(rightClickSelectedRow, rightClickSelectedRow);

          setSidePanel("ImageInvestigator");

          // re-select the rows
          table.clearSelection();
          for (int i = 0; i < selRows.length; i++) {
            changeSelection(selRows[i]);
          }
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
      ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onClick(c, e);
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onColumnResize(TableColumnModel c, javax.swing.event.ChangeEvent ce) {
    // 3.01 REPLACED WITH onRelease() SO THE COLUMNS DON'T SHRINK LEFT OVER TIME
    /*
    if (initColumnSizes) {
      return false;
    }
    
    ArchivePlugin readPlugin = Archive.getReadPlugin();
    if (readPlugin == null) {
      return false;
    }
    WSTableColumn[] columns = readPlugin.getViewingColumns();
    if (columns == null) {
      return false;
    }
    
    if (changeColumnNumber != columns.length - 1) {
      changeColumnNumber++;
      return true;
    }
    
    changeColumnNumber = 0;
    
    DefaultTableColumnModel model = (DefaultTableColumnModel) c;
    Enumeration<TableColumn> e = model.getColumns();
    
    int i = 0;
    while (e.hasMoreElements()) {
      TableColumn column = e.nextElement();
      columns[i].setWidth(column.getWidth());
      i++;
    }
    */
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
  The event that is triggered from a WSHoverableListener when the mouse moves out of an object
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    //lastMotionObject = "";
    lastMotionRow = -1;
    lastMotionColumn = -1;
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent c, java.awt.event.KeyEvent e) {
    if (c == table) {
      // move to the next file starting with this letter

      char keyCode = e.getKeyChar();
      if (keyCode == KeyEvent.CHAR_UNDEFINED || keyCode == '?') {
        ((WSPanelPlugin) ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).getCurrentPanel()).onKeyPress(c, e);
        return true; // not a letter or number
      }

      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
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
            table.requestFocus();
          }
          return true;
        }
      }

      keyCode = ("" + keyCode).toLowerCase().charAt(0);
      char keyCodeCaps = ("" + keyCode).toUpperCase().charAt(0);

      int numFiles = table.getRowCount();
      int selectedIndex = table.getSelectedRow() + 1;

      if (selectedIndex >= numFiles) {
        selectedIndex = 0;
      }

      char columnChar = 'P';
      ArchivePlugin plugin = Archive.getReadPlugin();

      // search the bottom half of the list
      for (int i = selectedIndex; i < numFiles; i++) {
        String filename = (String) plugin.getColumnValue(model.getResource(i), columnChar);
        if (filename.length() > 0) {
          char currentChar = filename.charAt(0);
          if (currentChar == keyCode || currentChar == keyCodeCaps) {
            table.setRowSelectionInterval(i, i);

            // scroll to the matching item
            Rectangle rect = table.getCellRect(i, 0, true);
            table.scrollRectToVisible(rect);

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
        String filename = (String) plugin.getColumnValue(model.getResource(i), columnChar);
        if (filename.length() > 0) {
          char currentChar = filename.charAt(0);
          if (currentChar == keyCode || currentChar == keyCodeCaps) {
            table.setRowSelectionInterval(i, i);

            // scroll to the matching item
            Rectangle rect = table.getCellRect(i, 0, true);
            table.scrollRectToVisible(rect);

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
    if (c instanceof WSTable) {
      if (c == table) {
        Point point = e.getPoint();
        int row = table.rowAtPoint(point);
        int column = table.columnAtPoint(point);

        // Don't continue if we're still hovering over the same object as last time
        if (row == lastMotionRow && column == lastMotionColumn) {
          return true; // still over the same object on the list
        }
        lastMotionRow = row;
        lastMotionColumn = column;

        String selectedObject = table.getValueAt(row, column).toString();
        if (selectedObject == null) {
          return true; // catch-all
        }

        // If the selectedObject is an icon (eg for the table columns Icon, Renamed, Replaced, ...)
        // then change it to a text-value reflecting the proper value of the field
        if (selectedObject.startsWith("images/WSTable/")) {
          selectedObject = Language.get("FileListPanel_" + selectedObject.substring(15) + "_Tooltip");
        }

        String columnHeading;
        try {
          columnHeading = Archive.getReadPlugin().getViewingColumn(column).toString();
        }
        catch (Throwable t) {
          columnHeading = Language.get("ColumnValue");
        }

        ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(columnHeading + ": " + selectedObject);
        return true;
      }
      else if (c == propTable) {
        Point point = e.getPoint();
        int row = propTable.rowAtPoint(point);
        int column = propTable.columnAtPoint(point);

        // Don't continue if we're still hovering over the same object as last time
        if (row == lastMotionRow && column == lastMotionColumn) {
          return true; // still over the same object on the list
        }
        lastMotionRow = row;
        lastMotionColumn = column;

        String selectedObject = propTable.getValueAt(row, column).toString();
        if (selectedObject == null) {
          return true; // catch-all
        }

        // If the selectedObject is an icon (eg for the table columns Icon, Renamed, Replaced, ...)
        // then change it to a text-value reflecting the proper value of the field
        if (selectedObject.startsWith("images/WSTable/")) {
          selectedObject = Language.get("FileListPanel_" + selectedObject.substring(15) + "_Tooltip");
        }

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
  
   **********************************************************************************************
   **/
  @Override
  public boolean onRelease(JComponent source, MouseEvent event) {

    if (source instanceof JTableHeader && source == table.getTableHeader()) {

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin == null) {
        return false;
      }
      WSTableColumn[] columns = readPlugin.getViewingColumns();
      if (columns == null) {
        return false;
      }

      DefaultTableColumnModel model = (DefaultTableColumnModel) table.getColumnModel();
      Enumeration<TableColumn> e = model.getColumns();

      e = model.getColumns();
      int i = 0;
      while (e.hasMoreElements()) {
        TableColumn column = e.nextElement();
        System.out.println("Column: " + columns[i].getWidth() + " vs E: " + column.getWidth());
        columns[i].setWidth(column.getWidth());
        i++;
      }

      return true;
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
    if (c == table) {
      Point p = e.getPoint();
      dropPoint = p;

      rightClickSelectedRow = table.rowAtPoint(p);
      if (!table.isRowSelected(rightClickSelectedRow)) {
        // if not selected, just select the current row.
        // otherwise, it is already selected (and maybe more are selected too) so don't change the selection
        table.setRowSelectionInterval(rightClickSelectedRow, rightClickSelectedRow);
      }

      WSPopupMenu menu = getRightClickMenu();
      menu.show(table, (int) p.getX() - 10, (int) p.getY() - 10);

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

    TableColumnModel columnModel = table.getColumnModel();
    WSTableColumn[] columns = Archive.getReadPlugin().getViewingColumns();

    // If the column counts are different, we need to build a new model.
    // This is because columns have been added or removed from the table
    // and we thus need to rebuild the model. Refreshing the model is not
    // sufficient in this case - crashes with ArrayIndexOutOfBounds.

    if (model == null || columnModel.getColumnCount() != columns.length) {
      model = new FileListModel_Table();
      table.setModel(model);
      columnModel = table.getColumnModel();
    }
    else {
      model.reload();
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
    JTableHeader header = table.getTableHeader();
    header.setBackground(LookAndFeelManager.getLightColor());
    header.setBorder(new LineBorder(LookAndFeelManager.getDarkColor(), 1));

    initColumnSizes = false;

    table.revalidate();
    table.repaint();

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

    Point location = table.getLocationOnScreen();

    x -= location.getX();
    y -= location.getY();

    int row = table.rowAtPoint(new Point(x, y));
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
    table.selectAll();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void selectInverse() {
    table.setVisible(false);
    for (int i = 1; i < table.getRowCount(); i++) {
      changeSelection(i);
    }
    changeSelection(0);
    table.setVisible(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void selectNone() {
    table.clearSelection();
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
    if (table.isEditing()) {
      table.getCellEditor().stopCellEditing();
    }
    if (propTable.isEditing()) {
      propTable.getCellEditor().stopCellEditing();
    }
  }

}