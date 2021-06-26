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

import java.awt.Component;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/***********************************************************************************************
Allows all <code>WSComponent</code>s and <code>JComponent</code>s to be rendered normally in a
table. For example, paints a <code>WSButton</code> as a <code>WSButton</code> instead of a
<code>String</code>
***********************************************************************************************/

public class WSTableCellRenderer extends DefaultTableCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public WSTableCellRenderer() {

  }

  /***********************************************************************************************
  Creates a <code>TableCellRenderer</code> for the <code>value</code>
  @param table the <code>JTable</code> being painted
  @param value the <code>Object</code> being rendered
  @param isSelected whether the <code>value</code> is selected or not
  @param hasFocus whether the <code>value</code> has focus or not
  @param row the <code>table</code> row that contains the <code>value</code>
  @param column the <code>table</code> column that contains the <code>value</code>
  @return the renderer for the <code>value</code>
  ***********************************************************************************************/
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (value instanceof JComponent) {
      JComponent component = (JComponent) value;
      if (value instanceof AbstractButton) {
        ((AbstractButton) value).setSelected(isSelected);
      }
      return component;
    }
    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
  }
}