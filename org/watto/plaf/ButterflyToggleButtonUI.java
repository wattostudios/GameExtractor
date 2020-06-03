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
import java.awt.Rectangle;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;


/***********************************************************************************************
Used to paint the GUI for <code>JToggleButton</code>s
***********************************************************************************************/
public class ButterflyToggleButtonUI extends BasicToggleButtonUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    JToggleButton button = (JToggleButton)component;

    button.setBorderPainted(false);
    button.setOpaque(false);
  }


  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    JToggleButton button = (JToggleButton)component;

    int bor = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");

    int x = 0;
    int y = 0;
    int w = button.getWidth();
    int h = button.getHeight();

    ButtonModel model = button.getModel();
    if (model.isArmed() && model.isPressed()) {
      ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w,h);
    }
    else {
      ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w,h,LookAndFeelManager.getMidColor(),LookAndFeelManager.getLightColor());
    }

    Icon icon = button.getIcon();
    if (icon != null) {
      int iconLeft = bor;
      int iconTop = bor;

      paintIcon(graphics,button,new Rectangle(iconLeft,iconTop,icon.getIconWidth(),icon.getIconHeight()));
    }

    String text = button.getText();
    if (text != null && !text.equals("")) {

      FontMetrics metrics = graphics.getFontMetrics();
      int textWidth = metrics.stringWidth(text);

      int textLeft = button.getWidth() / 2 - textWidth / 2;
      int textTop = button.getHeight() / 2 + bor - 2;

      if (icon != null) {
        textLeft += icon.getIconWidth() + bor;
        //textTop += (icon.getIconHeight()/2 - textHeight/2);
      }

      if (model.isArmed() && model.isPressed()) {
        //paintText(g,button,new Rectangle(textLeft,textTop,textWidth,textHeight),text);
        ButterflyPainter.paintShadowText((Graphics2D)graphics,text,textLeft,textTop);
      }
      else {
        //paintText(g,button,new Rectangle(textLeft,textTop,textWidth,textHeight),text);
        ButterflyPainter.paintText((Graphics2D)graphics,text,textLeft,textTop);
      }
    }
  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);

    JToggleButton button = (JToggleButton)component;

    button.setBorderPainted(true);
    button.setOpaque(true);
  }


  /***********************************************************************************************
  Creates a <code>ButterflyToggleButtonUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyToggleButtonUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyToggleButtonUI();
  }
}