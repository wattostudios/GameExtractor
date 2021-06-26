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
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;


/***********************************************************************************************
Used to paint the GUI for <code>JProgressBar</code>s
***********************************************************************************************/
public class ButterflyProgressBarUI extends BasicProgressBarUI {

  /** static instance of the GUI painter **/
  private static final ButterflyProgressBarUI progressUI = new ButterflyProgressBarUI();


  /** Whether the indeterminate <code>JProgressBar</code> is moving right or left **/
  static boolean indeterminateGoingRight = true;


  /** The left start position of the indeterminate <code>JProgressBar</code> **/
  static int indeterminateLeftPos = 0;

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics,JComponent component){
    JProgressBar progress = (JProgressBar)component;

    int w = progress.getWidth();
    int h = progress.getHeight();

    ButterflyPainter.paintCurvedBorder((Graphics2D)graphics,0,0,w,h);

    if (progress.isIndeterminate()) {
      if (indeterminateGoingRight) {
        indeterminateLeftPos += 2;
      }
      else {
        indeterminateLeftPos -= 2;
      }

      if (indeterminateLeftPos <= 0 || indeterminateLeftPos + 50 >= w) {
        indeterminateGoingRight = !indeterminateGoingRight;
      }

      ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,indeterminateLeftPos,0,50,h);

    }
    else {
      Insets inset = progress.getInsets();
      int amountFull = getAmountFull(inset,w,h);

      // current progress
      int thumbWidth = 0;
      int thumbHeight = 0;
      if (progress.getOrientation() == JProgressBar.HORIZONTAL) {
        thumbWidth = amountFull;
        thumbHeight = h;
      }
      else {
        thumbWidth = w;
        thumbHeight = amountFull;
      }

      Graphics thumbCrop = graphics.create(0,0,thumbWidth,thumbHeight);

      ButterflyPainter.paintCurvedGradient((Graphics2D)thumbCrop,0,0,thumbWidth,thumbHeight);

      if (progress.isStringPainted()) {
        double complete = progress.getPercentComplete();
        complete = ((double)((int)(complete * 1000)) / 10);

        String text = "" + complete + "%";

        java.awt.FontMetrics metrics = graphics.getFontMetrics();
        int textHeight = metrics.getHeight();
        int textWidth = metrics.stringWidth(text);

        int textTop = thumbHeight - textHeight / 2;
        int textLeft = w / 2 - textWidth / 2;

        ButterflyPainter.paintText((Graphics2D)graphics,text,textLeft,textTop);
        //g.setColor(Color.BLACK);
        //g.drawString(text,textLeft,textTop);
      }
    }

  }
  /**
  **********************************************************************************************
  Paints an indeterminate <code>JProgressBar</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  **********************************************************************************************
  **/
  public void paintIndeterminate(Graphics graphics,JComponent component){
    paint(graphics,component);
  }


  /***********************************************************************************************
  Gets the static <code>progressUI</code> instance
  @param component the <code>Component</code> to get the painter for
  @return the painter <code>ComponentUI</code>
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component){
    return progressUI;
  }
}