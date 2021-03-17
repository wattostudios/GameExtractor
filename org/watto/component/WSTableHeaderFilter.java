////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       wattostudios                                         //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2020  wattostudios                            //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the wattostudios website at http://www.watto.org or email watto@watto.org               //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.JTableHeader;

/***********************************************************************************************
A table header with a <code>WSTextField</code> underneath it for a filter.
<br /><br />
Use this by calling <code>scrollPane.setColumnHeader(new WSTableHeaderFilter());</code>
***********************************************************************************************/

public class WSTableHeaderFilter extends JViewport {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSTableHeaderFilterRenderer filterPanel = null;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public WSTableHeaderFilter(JTable table) {
    filterPanel = new WSTableHeaderFilterRenderer(table);
  }

  /***********************************************************************************************
  Updates the listener if the column model is changed
  ***********************************************************************************************/
  public void columnModelChanged(JTable table) {
    if (filterPanel != null) {
      filterPanel.rebuild(table);
      //table.getColumnModel().addColumnModelListener(filterPanel);
      //filterPanel.columnMarginChanged(new ChangeEvent(table.getColumnModel()));
    }
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  @Override
  public void setView(Component view) {
    if (filterPanel == null) {
      // just in case
      super.setView(view);
    }

    if (view instanceof JTableHeader) {
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(view, BorderLayout.NORTH);
      mainPanel.add(filterPanel, BorderLayout.SOUTH);
      super.setView(mainPanel);
    }
    else {
      // fallback
      super.setView(view);
    }
  }
}