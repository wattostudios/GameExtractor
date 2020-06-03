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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSStatusBar</code>s
***********************************************************************************************/
public class ButterflyStatusBarUI extends BasicLabelUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    int pad = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
    component.setBorder(new EmptyBorder(pad+2,pad+4,pad+1,pad));
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    JLabel label = (JLabel)component;

    int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");

    int x = 0;
    int y = 0;
    int w = label.getWidth();
    int h = label.getHeight();

    ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w,h);

    String text = label.getText();

    if (text != null && !text.equals("")) {
      FontMetrics metrics = graphics.getFontMetrics();
      int textHeight = metrics.getHeight();

      int textLeft = pad+2;
      int textTop = pad+3+textHeight/2;

      ButterflyPainter.paintText((Graphics2D)graphics,text,textLeft,textTop);
    }
  }


  /***********************************************************************************************
  Creates a <code>ButterflyStatusBarUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyButtonUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyStatusBarUI();
  }
}