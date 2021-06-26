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
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSSplitPane</code>s
***********************************************************************************************/
public class ButterflySplitPaneUI extends BasicSplitPaneUI {

  /***********************************************************************************************
  Creates the divider
  @return the divider
  ***********************************************************************************************/
  public BasicSplitPaneDivider createDefaultDivider(){
    return new ButterflySplitPaneDivider(this);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    //int w = component.getWidth();
    //int h = component.getHeight();
    //AquanauticPainter.paintBorder((Graphics2D)g,0,0,w,h);

    super.paint(graphics,component);
  }


  /***********************************************************************************************
  Creates a <code>ButterflySplitPaneUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflySplitPaneUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflySplitPaneUI();
  }
}