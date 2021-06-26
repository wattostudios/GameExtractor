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
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalSliderUI;


/***********************************************************************************************
Used to paint the GUI for <code>WSSlider</code>s
***********************************************************************************************/
public class ButterflySliderUI extends MetalSliderUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component){
    super.installUI(component);
    ((JSlider)component).setOpaque(false);
  }


  /***********************************************************************************************
  Paints the position drag bar of the <code>component</code>
  @param graphics the <code>Graphics</code> to paint the component on
  ***********************************************************************************************/
  public void paintThumb(Graphics graphics){

    int x = (int)thumbRect.getX();
    int y = (int)thumbRect.getY();
    int w = (int)thumbRect.getWidth();
    int h = (int)thumbRect.getHeight();

    Graphics thumbCrop = graphics.create(x,y,w,h);

    if (slider.hasFocus()) {
      ButterflyPainter.paintCurvedGradient((Graphics2D)thumbCrop,0,0,w,h,LookAndFeelManager.getMidColor(),LookAndFeelManager.getLightColor());
    }
    else {
      ButterflyPainter.paintCurvedGradient((Graphics2D)thumbCrop,0,0,w,h);
    }

  }


  /***********************************************************************************************
  Paints the track of the <code>component</code>
  @param graphics the <code>Graphics</code> to paint the component on
  ***********************************************************************************************/
  public void paintTrack(Graphics graphics){
    int bor = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");

    int x = (int)trackRect.getX();
    int y = (int)trackRect.getY();
    int w = (int)trackRect.getWidth();
    int h = (int)trackRect.getHeight();

    if (slider.isOpaque()) {
      graphics.setColor(LookAndFeelManager.getBackgroundColor());
      graphics.fillRect(x,y,w,h);
    }

    if (slider.getOrientation() == SwingConstants.VERTICAL) {
      x += bor;
      w -= (bor + bor);
    }
    else {
      y += bor;
      h -= (bor + bor);
    }

    Graphics trackCrop = graphics.create(x,y,w,h);

    ButterflyPainter.paintSquareGradient((Graphics2D)trackCrop,0,0,w,h);

  }


  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component){
    super.uninstallUI(component);
    ((JSlider)component).setOpaque(true);
  }


  /***********************************************************************************************
  Creates a <code>ButterflySliderUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflySliderUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return new ButterflySliderUI();
  }
}