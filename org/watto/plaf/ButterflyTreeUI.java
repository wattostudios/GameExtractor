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
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;


/***********************************************************************************************
Used to paint the GUI for <code>WSTree</code>s
***********************************************************************************************/
public class ButterflyTreeUI extends BasicTreeUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    JTree tree = (JTree)component;

    ButterflyTreeCellRenderer rend = new ButterflyTreeCellRenderer();
    tree.setCellRenderer(rend);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param clipBounds the painting bounds
  @param insets the <code>Insets</code> surrounding the component
  @param bounds the component bounds
  @param path the current <code>TreePath</code>
  @param row the current row in the tree
  @param isExpanded whether this cell is expanded or not
  @param hasBeenExpanded whether this cell has been expanded before
  @param isLeaf whether this cell is a leaf or not
  ***********************************************************************************************/
  protected void paintExpandControl(Graphics graphics,Rectangle clipBounds,Insets insets,Rectangle bounds,TreePath path,int row,boolean isExpanded,boolean hasBeenExpanded,boolean isLeaf){
    int leftPos = (int)bounds.getX() - 17;
    int topPos = (int)bounds.getY() + 5;

    int width = 9;
    int height = 9;

    graphics.setColor(Color.WHITE);
    graphics.fillRect(leftPos,topPos,width,height);

    graphics.setColor(Color.BLACK);
    graphics.drawRect(leftPos,topPos,width - 1,height - 1);

    graphics.drawLine(leftPos + 2,topPos + 4,leftPos + width - 3,topPos + 4);

    if (!isExpanded) {
      graphics.drawLine(leftPos + 4,topPos + 2,leftPos + 4,topPos + height - 3);
    }
  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);

    JTree tree = (JTree)component;
    tree.setCellRenderer(new DefaultTreeCellRenderer());
  }


  /***********************************************************************************************
  Creates a <code>ButterflyTreeUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyTreeUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyTreeUI();
  }
}