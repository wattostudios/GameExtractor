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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JRadioButton;

/***********************************************************************************************
 * Used to paint the icon for <code>WSRadioButton</code>s
 ***********************************************************************************************/
public class ButterflyRadioButtonIcon implements Icon {

  /***********************************************************************************************
   * Gets the height of the icon
   * @return <b>14</b>, the width
   ***********************************************************************************************/
  @Override
  public int getIconHeight() {
    return 14;
  }

  /***********************************************************************************************
   * Gets the width of the icon
   * @return <b>14</b>, the width
   ***********************************************************************************************/
  @Override
  public int getIconWidth() {
    return 14;
  }

  /***********************************************************************************************
   * Paints the icon
   * @param component the <code>Component</code> to paint the icon for
   * @param graphics the <code>Graphics</code> to paint on
   * @param x the x position to start painting from
   * @param y the y position to start painting from
   ***********************************************************************************************/
  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    int w = getIconWidth();
    int h = getIconHeight();

    Graphics2D cropBlock = (Graphics2D) graphics.create(x, y, w, h);

    JRadioButton check = (JRadioButton) component;
    ButtonModel model = check.getModel();

    ButterflyPainter.paintCurvedGradient(cropBlock, 0, 0, w, h, 10);
    if (model.isSelected()) {
      ButterflyPainter.paintDot(cropBlock, 0, 0, w, h);
    }

  }
}