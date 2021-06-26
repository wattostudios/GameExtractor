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

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuSeparatorUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSPopupMenu</code>s
***********************************************************************************************/
public class ButterflyPopupMenuSeparatorUI extends BasicPopupMenuSeparatorUI {

  /** static instance of the GUI painter **/
  private static final ButterflyPopupMenuSeparatorUI separatorUI = new ButterflyPopupMenuSeparatorUI();


  /***********************************************************************************************
  Gets the preferred size of the <code>component</code>
  @param component the <code>Component</code> to get the size of
  @return the preferred size <b>new Dimension(0,2)</b>
  ***********************************************************************************************/
  public Dimension getPreferredSize(JComponent component){
    return new Dimension(0,2);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    Dimension dimensions = component.getSize();

    int leftPos = 0;
    int w = (int)dimensions.getWidth();

    graphics.setColor(LookAndFeelManager.getMidColor());
    graphics.drawLine(leftPos,0,w,0);
    graphics.setColor(LookAndFeelManager.getLightColor());
    graphics.drawLine(leftPos,1,w,1);
  }


  /***********************************************************************************************
  Gets the static <code>separatorUI</code> instance
  @param component the <code>Component</code> to get the painter for
  @return the painter <code>ComponentUI</code>
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return separatorUI;
  }
}