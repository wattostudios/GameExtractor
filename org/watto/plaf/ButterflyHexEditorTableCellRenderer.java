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

package org.watto.plaf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;

/***********************************************************************************************
Used to paint cells for <code>WSTable</code>s
***********************************************************************************************/

public class ButterflyHexEditorTableCellRenderer extends DefaultTableCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  //Font monoFont = new Font("monospaced", Font.BOLD, 12);

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ButterflyHexEditorTableCellRenderer() {
  }

  /***********************************************************************************************
  Gets the renderer for the <code>table</code> cell <code>value</code>
  @param table the <code>JTable</code> being painted
  @param value the value of the cell being painted
  @param isSelected whether the cell is selected or not
  @param hasFocus whether the cell has focus or not
  @param row the row in the <code>table</code> where this cell appears
  @param column the column in the <code>table</code> where this cell appears
  @return the renderer for this cell
  ***********************************************************************************************/
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    // detect nulls so that they can be colored specially
    boolean isNull = false;
    if (value instanceof String) {
      String stringValue = (String) value;
      if (stringValue.startsWith("<null>")) {
        value = stringValue.substring(6);
        isNull = true;
      }
    }

    // First column shouldn't be selectable
    if (column == 0) {
      isSelected = false;
    }

    // The first column will be right-aligned (down later), but add a padding space to the end of the value
    if (column == 0 && value instanceof String) {
      value = (String) value + " ";
    }

    JComponent rend = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    rend.setForeground(LookAndFeelManager.getTextColor());

    // determine whether this is the last column or row of the table.
    // this is so we don't paint double-thickness borders in the grid.
    boolean firstRow = (row == 0);
    boolean firstColumn = (column == 0);

    Color borderColor;
    Color backgroundColor;

    if (isSelected/* || hasFocus*/) {
      borderColor = LookAndFeelManager.getDarkColor();

      backgroundColor = LookAndFeelManager.getLightColor();

      if (row % 2 == 0) {
        // leave color as is
      }
      else {
        // darker row
        int r = backgroundColor.getRed() - 25;
        if (r < 0) {
          r = 0;
        }
        int g = backgroundColor.getGreen() - 25;
        if (g < 0) {
          g = 0;
        }
        int b = backgroundColor.getBlue() - 25;
        if (b < 0) {
          b = 0;
        }
        backgroundColor = new Color(r, g, b);
      }

      if (table.getSelectionModel().getMinSelectionIndex() == row) {
        firstRow = true;
      }
    }
    else {
      borderColor = LookAndFeelManager.getLightColor();
      if (row % 2 == 0) {
        // leave color as is
        backgroundColor = Color.WHITE;

        // paint null values in a different color (light yellow)
        if (isNull) {
          backgroundColor = new Color(255, 255, 230);
        }

      }
      else {
        // darker row
        backgroundColor = new Color(230, 230, 230);

        // paint null values in a different color (light yellow)
        if (isNull) {
          backgroundColor = new Color(230, 230, 205);
        }

      }

    }

    rend.setBackground(backgroundColor);

    Border coloredBorder;
    Border emptyBorder;

    // First column has a bolder border on the right
    if (isSelected) {
      coloredBorder = new MatteBorder(1, 1, 1, 1, borderColor);
      emptyBorder = null;
    }
    else if (firstColumn && firstRow) {
      coloredBorder = new MatteBorder(1, 1, 1, 3, borderColor);
      emptyBorder = null;
    }
    else if (firstColumn) {
      coloredBorder = new MatteBorder(0, 1, 1, 3, borderColor);
      emptyBorder = new EmptyBorder(1, 0, 0, 0);
    }
    else if (firstRow) {
      coloredBorder = new MatteBorder(1, 0, 1, 1, borderColor);
      emptyBorder = new EmptyBorder(0, 1, 0, 0);
    }
    else {
      coloredBorder = new MatteBorder(0, 0, 1, 1, borderColor);
      emptyBorder = new EmptyBorder(1, 1, 0, 0);
    }

    rend.setBorder(new CompoundBorder(coloredBorder, emptyBorder));

    if (rend instanceof JLabel) {
      JLabel label = (JLabel) rend;

      // First column is right-aligned
      // All other columns are centered
      if (column == 0) {
        label.setHorizontalAlignment(RIGHT);
      }
      else {
        label.setHorizontalAlignment(CENTER);
      }

      //rend.setFont(monoFont);

      if (value instanceof Icon) {
        label.setIcon((Icon) value);
        label.setText("");
        label.setBorder(new EmptyBorder(0, 0, 0, 0));
      }
      else if (value instanceof Image) {
        label.setIcon(new ImageIcon((Image) value));
        label.setText("");
        label.setBorder(new EmptyBorder(0, 0, 0, 0));
      }
      else {
        label.setIcon(null);
      }
    }

    //rend.setSize(new Dimension((int)rend.getMinimumSize().getWidth(),table.getRowHeight()));

    return rend;
  }

}