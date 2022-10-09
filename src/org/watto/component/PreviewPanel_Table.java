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
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import org.watto.ErrorLogger;
import org.watto.SingletonManager;
import org.watto.component.model.UneditableTableModel;
import org.watto.event.WSCellEditableInterface;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.listener.WSCellEditableListener;
import org.watto.event.listener.WSClickableListener;
import org.watto.event.listener.WSKeyableListener;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.plaf.LookAndFeelManager;
import org.watto.task.Task;
import org.watto.task.Task_WriteEditedPreview;
import org.watto.xml.XMLReader;

public class PreviewPanel_Table extends PreviewPanel implements WSKeyableInterface, WSClickableInterface, WSCellEditableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSTable preview = null;

  Object[][] tableData = null;

  WSTableColumn[] tableColumns = null;

  boolean objectChanged = false;

  WSButton saveButton = null;

  WSTable detailsTable = null;

  WSTableColumn[] detailsColumns = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSTable getTable() {
    return preview;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Object[][] getData() {
    return tableData;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_Table(Object[][] data, WSTableColumn[] columns) {
    super();

    objectChanged = false; // not edited

    WSPanel mainPanel = new WSPanel(XMLReader.read("<WSPanel obeyBackgroundColor=\"true\" code=\"PreviewPanel_Table_MainPanelHolder\" layout=\"BorderLayout\" vertical-gap=\"4\"></WSPanel>"));

    // Main Table
    preview = new WSTable(XMLReader.read("<WSTable code=\"PreviewTable\" repository=\"false\" />"));
    //preview.setCode("PreviewTable");

    this.tableData = data;
    this.tableColumns = columns;
    preview.setModel(new WSTableModel(data, columns));

    preview.addKeyListener(new WSKeyableListener(this));
    preview.addMouseListener(new WSClickableListener(this));

    preview.setColumnSelectionAllowed(false);

    JTableHeader tableHeader = preview.getTableHeader();
    tableHeader.setReorderingAllowed(false);
    tableHeader.setResizingAllowed(true);
    tableHeader.addMouseListener(new WSClickableListener(this));

    try {
      ((DefaultCellEditor) preview.getDefaultEditor(Object.class)).setClickCountToStart(1);
      ((DefaultCellEditor) preview.getDefaultEditor(String.class)).setClickCountToStart(1);
      ((DefaultCellEditor) preview.getDefaultEditor(Number.class)).setClickCountToStart(1);
    }
    catch (Throwable t) {
    }

    WSPanel tablePanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" showBorder=\"true\" border-width=\"3\"></WSPanel>"));
    JScrollPane scrollPane = new JScrollPane(preview);
    scrollPane.setOpaque(false);
    scrollPane.setBorder(new EmptyBorder(4, 4, 4, 4));
    tablePanel.add(scrollPane, BorderLayout.CENTER);

    mainPanel.add(tablePanel, BorderLayout.CENTER);

    // Details Table
    detailsTable = new WSTable(XMLReader.read("<WSTable code=\"PreviewDetailsTable\" repository=\"false\" />"));
    //preview.setCode("PreviewTable");

    // make empty data for the table
    int numDetails = 0;
    if (data != null && data[0] != null) {
      numDetails = data[0].length;
    }
    String[][] detailsData = new String[numDetails][2];
    for (int i = 0; i < numDetails; i++) {
      detailsData[i][0] = "";
      detailsData[i][1] = "";
    }

    detailsColumns = new WSTableColumn[] { new WSTableColumn("Heading", 'h', String.class, false, false), new WSTableColumn("Details", 'd', String.class, false, false) };

    detailsTable.setModel(new WSTableModel(detailsData, detailsColumns));
    detailsTable.setColumnSelectionAllowed(false);

    tableHeader = detailsTable.getTableHeader();
    tableHeader.setReorderingAllowed(false);
    tableHeader.setResizingAllowed(true);

    WSPanel detailsInnerPanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" showBorder=\"true\"></WSPanel>"));
    detailsInnerPanel.add(detailsTable, BorderLayout.CENTER);

    WSPanel detailsPanel = new WSPanel(XMLReader.read("<WSPanel obeyBackgroundColor=\"true\" code=\"PreviewPanel_Table_DetailsPanelHolder\" layout=\"BorderLayout\" showBorder=\"true\" showLabel=\"true\" border-width=\"3\"></WSPanel>"));
    detailsPanel.add(detailsInnerPanel, BorderLayout.CENTER);

    mainPanel.add(detailsPanel, BorderLayout.SOUTH);

    // add the main panel
    add(mainPanel, BorderLayout.CENTER);

  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be opened.
   **********************************************************************************************
   **/
  @Override
  public void onOpenRequest() {
    try {
      if (SingletonManager.has("CurrentViewer")) {
        ViewerPlugin viewerPlugin = (ViewerPlugin) SingletonManager.get("CurrentViewer");
        if (viewerPlugin != null) {
          if (viewerPlugin.canEdit(this)) {
            if (GameExtractor.isFullVersion()) {

              WSPanel bottomPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" layout=\"GridLayout\" rows=\"1\" columns=\"1\" />"));
              saveButton = new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_Text_SaveChanges\" showText=\"true\" />"));
              saveButton.setEnabled(false);
              bottomPanel.add(saveButton);

              add(bottomPanel, BorderLayout.SOUTH);
            }
          }

        }
      }
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    // Flush the variables clear for garbage collection

    /*
    if (objectChanged) {
      saveChanges();
    }
    */

    preview = null;
    tableData = null;
    detailsTable = null;
    tableColumns = null;
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

      reloadSelectedValue();
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
      else if (code.equals("PreviewTable")) {
        reloadSelectedValue();
        return true;
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
      /*
      int currentRow = preview.getSelectedRow();//preview.getEditingRow();
      int currentColumn = preview.getSelectedColumn();//preview.getEditingColumn();
      Object oldValue = preview.getValueAt(currentRow, currentColumn);
      Object newValue = preview.getCellEditor().getCellEditorValue();
      if (!newValue.equals(oldValue)) {
        setObjectChanged(true);
      }
      */
      setObjectChanged(true);
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadSelectedValue() {
    int row = preview.getSelectedRow();
    int col = preview.getSelectedColumn();

    reloadSelectedValue(row, col);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadSelectedValue(int row, int col) {
    try {

      // Work out the size of column 1 (the headings) and set it, so that column 2 (the value) is maximized as much as possible
      FontMetrics metrics = detailsTable.getFontMetrics(LookAndFeelManager.getFont());
      int width = -1;

      // create the details data (and measure the size of it)
      Object[] data = tableData[row];
      int numColumns = data.length;

      Object[][] detailsData = new Object[numColumns][2];
      for (int i = 0; i < numColumns; i++) {
        String columnName = tableColumns[i].getName();

        detailsData[i][0] = columnName;
        detailsData[i][1] = data[i];

        int textWidth = metrics.stringWidth(columnName);
        if (textWidth > width) {
          width = textWidth;
        }
      }

      width += 10; // small padding

      detailsTable.setModel(new UneditableTableModel(detailsData, detailsColumns));

      detailsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

      TableColumn column0 = detailsTable.getColumnModel().getColumn(0);
      column0.setMinWidth(width);
      column0.setMaxWidth(width);
      column0.setPreferredWidth(width);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}