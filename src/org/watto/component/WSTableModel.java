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

import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/***********************************************************************************************
The data of a <code>WSTable</code>
***********************************************************************************************/
public class WSTableModel implements TableModel {

  /** The data in the <code>WSTable</code> **/
  Object[][] data;

  /** The table columns **/
  WSTableColumn[] columns;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSTableModel() {
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> for the <code>data</code>, and the given table <code>headings</code>
  @param data the table data
  @param headings the column headings
  ***********************************************************************************************/
  public WSTableModel(Object[][] data, String[] headings) {
    this.data = data;

    int numColumns = headings.length;
    columns = new WSTableColumn[numColumns];
    for (int i = 0; i < numColumns; i++) {
      columns[i] = new WSTableColumn(headings[i], (char) i, String.class, false, true);
    }
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> for the <code>data</code>, with the given table <code>columns</code>
  @param data the table data
  @param columns the table columns
  ***********************************************************************************************/
  public WSTableModel(Object[][] data, WSTableColumn[] columns) {
    this.data = data;
    this.columns = columns;
  }

  /***********************************************************************************************
  Adds a <code>TableModelListener</code> to this model
  @param listener the <code>TableModelListener</code> to add
  ***********************************************************************************************/
  public void addTableModelListener(TableModelListener listener) {
  }

  /***********************************************************************************************
  Sets up the <code>table</code> according to the <code>columns</code>. Sets the column widths, 
  headings, and reordering properties of the <code>table</code>.
  @param table the <code>WSTable</code> to configure
  ***********************************************************************************************/
  public void configureTable(WSTable table) {

    TableColumnModel columnModel = table.getColumnModel();

    int screenWidth = table.getWidth();
    if (screenWidth <= 0) {
      screenWidth = Integer.MAX_VALUE;
    }

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

    table.setColumnSelectionAllowed(false);

    JTableHeader tableHeader = table.getTableHeader();
    tableHeader.setReorderingAllowed(false);
    tableHeader.setResizingAllowed(true);

  }

  /***********************************************************************************************
  Gets the <code>Class</code> of data in a <code>column</code>
  @param column the column to get the <code>Class</code> of
  @return the <code>Class</code> of data
  ***********************************************************************************************/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Class getColumnClass(int column) {
    if (column >= columns.length) {
      return String.class;
    }
    return columns[column].getType();
  }

  /***********************************************************************************************
  Gets the number of <code>columns</code>
  @return the number of <code>columns</code>
  ***********************************************************************************************/
  public int getColumnCount() {
    return columns.length;
  }

  /***********************************************************************************************
  Gets the header name of a <code>column</code>
  @param column the column to get the header name of
  @return the header name
  ***********************************************************************************************/
  public String getColumnName(int column) {
    return columns[column].getName();
  }

  /***********************************************************************************************
  Gets the <code>columns</code>
  @return the <code>columns</code>
  ***********************************************************************************************/
  public WSTableColumn[] getColumns() {
    return columns;
  }

  /***********************************************************************************************
  Gets the number of rows in the table
  @return the number of rows
  ***********************************************************************************************/
  public int getRowCount() {
    return data.length;
  }

  /***********************************************************************************************
  Gets the <code>Object</code> in a given cell
  @param row the row of the cell
  @param column the column of the cell
  @return the <code>Object</code> at <code>row</code>,<code>column</code> in the table
  ***********************************************************************************************/
  public Object getValueAt(int row, int column) {
    try {
      return data[row][column];
    }
    catch (Throwable t) {
      return null;
    }
  }

  /***********************************************************************************************
  Gets whether a cell is editable or not
  @param row the row of the cell
  @param column the column of the cell
  @return <b>true</b> if the cell is editable<br />
          <b>false</b> if the cell is not editable 
  ***********************************************************************************************/
  public boolean isCellEditable(int row, int column) {
    if (columns[column].isEditable()) {
      return true;
    }
    return false;
  }

  /***********************************************************************************************
  Removes a <code>TableModelListener</code> from this model
  @param listener the <code>TableModelListener</code> to remove
  ***********************************************************************************************/
  public void removeTableModelListener(TableModelListener listener) {
  }

  /***********************************************************************************************
  Sets the <code>Object</code> in a given cell
  @param value the new <code>Object</code> value of the cell
  @param row the row of the cell
  @param column the column of the cell
  ***********************************************************************************************/
  public void setValueAt(Object value, int row, int column) {
    data[row][column] = value;
  }
}