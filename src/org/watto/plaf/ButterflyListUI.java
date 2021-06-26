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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;

/***********************************************************************************************
 * Used to paint the GUI for <code>WSList</code>s
 ***********************************************************************************************/
public class ButterflyListUI extends BasicListUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyListUI</code> instance for rendering the <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyListUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyListUI();
  }

  /***********************************************************************************************
   * Paints an item in the <code>JList</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   * @param row the row of the item in the <code>list</code>
   * @param rowBounds the painting bounds of the item
   * @param cellRenderer the <code>ListCellRenderer</code> used to render the item
   * @param dataModel the <code>ListModel</code> that contains the <code>list</code> data
   * @param selectionModel the model that describes the selected items in the <code>list</code>
   * @param leadIndex the index of the first item showing in the <code>list</code>
   ***********************************************************************************************/
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected void paintCell(Graphics graphics, int row, Rectangle rowBounds, ListCellRenderer cellRenderer, ListModel dataModel, ListSelectionModel selectionModel, int leadIndex) {
    Object value = dataModel.getElementAt(row);
    boolean cellHasFocus = (list.hasFocus() && (row == leadIndex));
    boolean isSelected = selectionModel.isSelectedIndex(row);

    Component rendererComponent = cellRenderer.getListCellRendererComponent(list, value, row, isSelected, cellHasFocus);

    ((JComponent) rendererComponent).setOpaque(list.isOpaque());

    if (isSelected) {
      rendererComponent.setForeground(LookAndFeelManager.getTextColor());
      rendererComponent.setBackground(LookAndFeelManager.getLightColor());
      ((JComponent) rendererComponent).setOpaque(true);
    }
    else {
      rendererComponent.setForeground(LookAndFeelManager.getTextColor());
      rendererComponent.setBackground(LookAndFeelManager.getBackgroundColor());
    }

    int cx = rowBounds.x;
    int cy = rowBounds.y;
    int cw = rowBounds.width + 2;
    int ch = rowBounds.height;

    rendererPane.paintComponent(graphics, rendererComponent, list, cx, cy, cw, ch, true);
  }
}