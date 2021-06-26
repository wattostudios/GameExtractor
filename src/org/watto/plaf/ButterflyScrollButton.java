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
import javax.swing.ButtonModel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalScrollButton;

/***********************************************************************************************
 * Used to paint the GUI buttons for <code>WSScrollBar</code>s
 ***********************************************************************************************/

public class ButterflyScrollButton extends MetalScrollButton {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
   * Creates a scroll button
   * @param direction the direction that the scrollbar is moving
   * @param width the width of the button
   * @param freeStanding whether the button is free-standing or not
   ***********************************************************************************************/
  public ButterflyScrollButton(int direction, int width, boolean freeStanding) {
    super(direction, width, freeStanding);
    setBorder(new EmptyBorder(0, 0, 0, 0));
    setOpaque(false);
  }

  /***********************************************************************************************
   * Paints the <code>JButton</code> on the <code>graphics</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   ***********************************************************************************************/
  @Override
  public void paint(Graphics graphics) {
    int x = 0;
    int y = 0;
    int w = getWidth();
    int h = getHeight();

    ButtonModel model = getModel();
    if (model.isArmed() && model.isPressed()) {
      ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, x, y, w, h);
    }
    else {
      ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, x, y, w, h, LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor());
    }
  }

}