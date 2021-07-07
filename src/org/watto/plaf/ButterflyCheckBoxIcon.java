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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.plaf.metal.MetalCheckBoxIcon;

/***********************************************************************************************
Used to paint the icon for <code>WSCheckbox</code>es
***********************************************************************************************/

public class ButterflyCheckBoxIcon extends MetalCheckBoxIcon {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Gets the dimension of the icon
  @return <b>14</b>, the dimension
  ***********************************************************************************************/
  @Override
  protected int getControlSize() {
    return 17;
  }

  /***********************************************************************************************
  Paints the icon
  @param component the <code>Component</code> to paint the icon for
  @param graphics the <code>Graphics</code> to paint on
  @param x the x position to start painting from
  @param y the y position to start painting from
  ***********************************************************************************************/
  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    JCheckBox checkbox = (JCheckBox) component;

    int w = getControlSize();
    int h = w;

    Graphics2D cropBlock = (Graphics2D) graphics.create(x, y, w, h);
    ButtonModel model = checkbox.getModel();

    //ButterflyPainter.paintSquareGradient(cropBlock,0,0,w,h);

    Color lightColor = LookAndFeelManager.getLightColor();
    Color midColor = LookAndFeelManager.getMidColor();
    Color darkColor = LookAndFeelManager.getDarkColor();
    Color crossColor = LookAndFeelManager.getTextColor();
    if (!checkbox.isEnabled()) {
      lightColor = new Color(180, 180, 180);
      midColor = new Color(160, 160, 160);
      darkColor = new Color(140, 140, 140);
      crossColor = midColor;
    }

    if (model.isSelected()) {
      ButterflyPainter.paintSquareGradient(cropBlock, 0, 0, w, h, darkColor, lightColor, lightColor, midColor);
      ButterflyPainter.paintCross(cropBlock, 1, 1, w - 1, h - 1, crossColor);
    }
    else {
      ButterflyPainter.paintSquareGradient(cropBlock, 0, 0, w, h, darkColor, lightColor, lightColor, midColor);
    }

  }
}