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
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

/***********************************************************************************************
 * Used to paint cells for <code>WSTree</code>s
 ***********************************************************************************************/

public class ButterflyTreeCellRenderer extends DefaultTreeCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public ButterflyTreeCellRenderer() {
    super();

    //setClosedIcon(new ImageIcon(WSHelper.getResource("images/WSTree/ButterflyClosedIcon.png")));
    //setLeafIcon(new ImageIcon(WSHelper.getResource("images/WSTree/ButterflyFileIcon.png")));
    //setOpenIcon(new ImageIcon(WSHelper.getResource("images/WSTree/ButterflyDirectoryIcon.png")));

    setClosedIcon(new ImageIcon("images/WSTree/ButterflyClosedIcon.png"));
    setLeafIcon(new ImageIcon("images/WSTree/ButterflyFileIcon.png"));
    setOpenIcon(new ImageIcon("images/WSTree/ButterflyDirectoryIcon.png"));
  }

  /***********************************************************************************************
   * Gets the renderer for the <code>tree</code> cell <code>value</code>
   * @param tree the <code>JTree</code> being painted
   * @param value the value of the cell being painted
   * @param isSelected whether the cell is selected or not
   * @param isExpanded whether the cell is expanded or not
   * @param isLeaf whether the cell is a leaf or not
   * @param row the row in the <code>table</code> where this cell appears
   * @param hasFocus whether the cell has focus or not
   * @return the renderer for this cell
   ***********************************************************************************************/
  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
    DefaultTreeCellRenderer rend = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
    rend.setBorder(new EmptyBorder(1, 1, 1, 1));
    return rend;
  }

}