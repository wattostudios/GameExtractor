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

package org.watto.component.model;

import java.util.Vector;
import javax.swing.table.DefaultTableModel;

/***********************************************************************************************
A <code>DefaultTableModel</code> that does not allow editing
@see javax.swing.table.DefaultTableModel
***********************************************************************************************/

public class UneditableTableModel extends DefaultTableModel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public UneditableTableModel() {
    super();
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> with a number of rows and columns
  @param rowCount the number of table rows
  @param columnCount the number of table columns
  ***********************************************************************************************/
  public UneditableTableModel(int rowCount, int columnCount) {
    super(rowCount, columnCount);
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> with a number of named columns and an initial number of rows
  @param columnNames the names for each table column
  @param rowCount the number of table rows
  ***********************************************************************************************/
  public UneditableTableModel(Object[] columnNames, int rowCount) {
    super(columnNames, rowCount);
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> populated with <code>data</code> and given <code>columnNames</code>
  @param data the table data
  @param columnNames the names for each table column
  ***********************************************************************************************/
  public UneditableTableModel(Object[][] data, Object[] columnNames) {
    super(data, columnNames);
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> with a number of named columns and an initial number of rows
  @param columnNames the names for each table column
  @param rowCount the number of table rows
  ***********************************************************************************************/
  public UneditableTableModel(Vector<String> columnNames, int rowCount) {
    super(columnNames, rowCount);
  }

  /***********************************************************************************************
  Creates a <code>TableModel</code> with a number of named columns and populated with <code>data</code>
  @param data the table data
  @param columnNames the names for each table column
  ***********************************************************************************************/
  public UneditableTableModel(Vector<String> data, Vector<String> columnNames) {
    super(data, columnNames);
  }

  /***********************************************************************************************
  Is the cell at <code>row</code> and <code>column</code> editable
  @param row the row that contains the table cell
  @param column the column that contains the table cell
  @return false
  ***********************************************************************************************/
  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }

}