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
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSToolBar</code>s
***********************************************************************************************/
public class ButterflyToolBarSeparatorUI extends BasicToolBarSeparatorUI {

  /** static instance of the GUI painter **/
  private static final ButterflyToolBarSeparatorUI separatorUI = new ButterflyToolBarSeparatorUI();


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
  //g.setColor(Color.RED);
  //g.fillRect(0,0,c.getWidth(),c.getHeight());
  //g.fillRect(0,0,c.getWidth(),c.getHeight());
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