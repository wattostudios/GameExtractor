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

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSPopupMenu</code>s
***********************************************************************************************/
public class ButterflyPopupMenuUI extends BasicPopupMenuUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    JPopupMenu menu = (JPopupMenu)component;
    menu.setOpaque(false);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    int x = 0;
    int y = 0;
    int w = component.getWidth();
    int h = component.getHeight();

    //ButterflyPainter.paintOpaque(graphics,component);
    ButterflyPainter.paintSquareGradient((Graphics2D)graphics,x-1,y-1,w+2,h+2);
    //ButterflyPainter.paintSquareGradient((Graphics2D)graphics,x,y,w,h);
  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);

    JPopupMenu menu = (JPopupMenu)component;
    menu.setOpaque(true);
  }


  /***********************************************************************************************
  Creates a <code>ButterflyPopupMenuUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyPopupMenuUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyPopupMenuUI();
  }
}