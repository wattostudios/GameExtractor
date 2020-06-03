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
import javax.swing.JToolTip;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicToolTipUI;


/***********************************************************************************************
Used to paint the GUI for tooltips
***********************************************************************************************/
public class ButterflyToolTipUI extends BasicToolTipUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    //String text = ((JToolTip)c).getTipText();
    //if (text == null || text.length() <= 0){
    //  c.setBorder(new EmptyBorder(0,0,0,0));
    //  return;
    //  }

    int pad = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
    component.setBorder(new EmptyBorder(pad,pad,pad,pad));
    component.setOpaque(false);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    JToolTip tip = (JToolTip) component;

    String text = tip.getTipText();

    if (text == null || text.length() <= 0){
      return;
      }


    int pad = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");

    int w = tip.getWidth();
    int h = tip.getHeight();

    ButterflyPainter.paintSquareGradient((Graphics2D)graphics,0,0,w,h);

    int textLeft = pad+pad;
    int textTop = pad+pad+2;


    FontMetrics metrics = graphics.getFontMetrics();
    int textHeight =  metrics.getHeight();

    textTop += textHeight/2;

    ButterflyPainter.paintText((Graphics2D)graphics,text,textLeft,textTop);
  }



  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);

    component.setBorder(BasicBorders.getTextFieldBorder());
    component.setOpaque(true);
  }


  /***********************************************************************************************
  Creates a <code>ButterflyToolTipUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyToolTipUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyToolTipUI();
  }
}