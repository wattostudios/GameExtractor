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
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollBarUI;

/***********************************************************************************************
 * Used to paint the GUI for <code>JScrollBar</code>s
 ***********************************************************************************************/
public class ButterflyScrollBarUI extends MetalScrollBarUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyScrollBarUI</code> instance for rendering the
   * <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyScrollBarUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyScrollBarUI();
  }

  /***********************************************************************************************
   * Creates the <code>JButton</code> used to decrease the current position
   * @param orientation the orientation of the <code>JScrollPane</code>
   * @return the decrease <code>JButton</code>
   ***********************************************************************************************/
  @Override
  protected JButton createDecreaseButton(int orientation) {
    decreaseButton = new ButterflyScrollButton(orientation, scrollBarWidth, isFreeStanding);
    return decreaseButton;
  }

  /***********************************************************************************************
   * Creates the <code>JButton</code> used to increase the current position
   * @param orientation the orientation of the <code>JScrollPane</code>
   * @return the increase <code>JButton</code>
   ***********************************************************************************************/
  @Override
  protected JButton createIncreaseButton(int orientation) {
    increaseButton = new ButterflyScrollButton(orientation, scrollBarWidth, isFreeStanding);
    return increaseButton;
  }

  /***********************************************************************************************
   * Paints the position drag bar of the <code>component</code>, in the <code>thumbBounds</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   * @param component the <code>Component</code> to paint
   * @param trackBounds the painting bounds of the track
   ***********************************************************************************************/
  @Override
  protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
    int x = (int) thumbBounds.getX();
    int y = (int) thumbBounds.getY();
    int w = (int) thumbBounds.getWidth();
    int h = (int) thumbBounds.getHeight();

    if (((JScrollBar) component).getOrientation() == JScrollBar.VERTICAL) {
      // Make it narrower left-right
      w -= 4;
      x += 2;

      // Move it slightly off the top and bottom
      h -= 2;
      y += 1;
    }
    else {
      // Make it narrower up-down
      h -= 4;
      y += 2;

      // Move it slightly off the left and right
      w -= 2;
      x += 1;
    }

    Graphics thumbCrop = graphics.create(x, y, w, h);

    ButterflyPainter.paintCurvedGradient((Graphics2D) thumbCrop, 0, 0, w, h);

  }

  /***********************************************************************************************
   * Paints the track of the <code>component</code>, in the <code>trackBounds</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   * @param component the <code>Component</code> to paint
   * @param trackBounds the painting bounds of the track
   ***********************************************************************************************/
  @Override
  protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
    ButterflyPainter.paintOpaque(graphics, component);

    int x = trackBounds.x;
    int y = trackBounds.y;
    int w = trackBounds.width;
    int h = trackBounds.height;

    if (((JScrollBar) component).getOrientation() == JScrollBar.VERTICAL) {
      // Make it narrower left-right
      w -= 8;
      x += 4;

      // Move it slightly off the top and bottom
      h -= 6;
      y += 3;
    }
    else {
      // Make it narrower up-down
      h -= 8;
      y += 4;

      // Move it slightly off the left and right
      w -= 6;
      x += 3;
    }

    ButterflyPainter.paintSquareSolid((Graphics2D) graphics, x, y, w, h);

    // paint the thumb as a full-sized thing when it isn't scrollable but it is displayed
    Rectangle thumbBounds = getThumbBounds();
    if (thumbBounds.getX() == 0 && thumbBounds.getY() == 0 && thumbBounds.getWidth() == 0 && thumbBounds.getHeight() == 0) {
      paintThumb(graphics, component, trackBounds);
    }
  }
}