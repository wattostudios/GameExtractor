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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSMenu</code>s
***********************************************************************************************/
public class ButterflyMenuUI extends BasicMenuUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);

    JMenu menu = (JMenu)component;
    menu.setOpaque(false);
    menu.setBorderPainted(false);
  }


  /***********************************************************************************************
  Paints the <code>menuItem</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>menuItem</code> on
  @param menuItem the <code>JMenuItem</code> to paint
  @param backgroundColor the background <code>Color</code>
  ***********************************************************************************************/
  public void paintBackground(Graphics graphics,JMenuItem menuItem,Color backgroundColor){
    ButtonModel model = menuItem.getModel();

    int x = 0;
    int y = 0;
    int w = menuItem.getWidth();
    int h = menuItem.getHeight();

    if (menuItem.isOpaque()) {
      graphics.setColor(LookAndFeelManager.getBackgroundColor());
      graphics.fillRect(0,0,w,h);
    }

    if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
      ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w,h,LookAndFeelManager.getLightColor(),LookAndFeelManager.getMidColor());

      //Shape oldClip = graphics.getClip();
      //graphics.setClip(0,0,w,h);

      /*
      if (menuItem.getParent() instanceof JPopupMenu) {
        // sub-menu
        ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,0,0,w + 10,h);
      }
      else {
        // main menu
        //int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");
        //ButterflyPainter.paintCurvedSolid((Graphics2D)graphics,pad / 2,0,w - pad / 2,h + 10,LookAndFeelManager.getMidColor());
        ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w+4,h);
      }
      //graphics.setClip(oldClip);
       */

    }

  }


  /***********************************************************************************************
  Paints the text of the <code>menuItem</code>
  @param graphics the <code>Graphics</code> to paint the <code>menuItem</code> on
  @param menuItem the <code>JMenuItem</code> to paint
  @param textBounds the bounds to paint the <code>text</code> in
  @param text the text <code>String</code>
  ***********************************************************************************************/
  public void paintText(Graphics graphics,JMenuItem menuItem,Rectangle textBounds,String text){
    ButterflyPainter.enableAntialias(graphics);

    ButtonModel model = menuItem.getModel();
    FontMetrics metrics = graphics.getFontMetrics();

    int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");

    int textHeight = metrics.getHeight();
    int textTop = pad + textHeight / 2 + 3;
    int textLeft = (int)textBounds.getX();

    int underline = menuItem.getDisplayedMnemonicIndex();

    if (!(menuItem.getParent() instanceof JPopupMenu)) {
      // main menu
      textLeft += pad / 2;
    }

    if (!model.isEnabled()) {
      // Disabled
      ButterflyPainter.paintUnderlineShadowText((Graphics2D)graphics,text,underline,textLeft,textTop,LookAndFeelManager.getMidColor(),LookAndFeelManager.getLightColor());
    }
    else {
      // Enabled
      if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
        ButterflyPainter.paintUnderlineShadowText((Graphics2D)graphics,text,underline,textLeft,textTop);
      }
      else {
        ButterflyPainter.paintUnderlineText((Graphics2D)graphics,text,underline,textLeft,textTop);
      }
    }

  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);

    JMenu menu = (JMenu)component;
    menu.setOpaque(true);
    menu.setBorderPainted(true);
  }


  /***********************************************************************************************
  Creates a <code>ButterflyMenuUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyMenuUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflyMenuUI();
  }
}