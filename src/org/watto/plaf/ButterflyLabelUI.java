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
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;
import org.watto.component.WSLabel;
import org.watto.component.WordWrap;

/***********************************************************************************************
Used to paint the GUI for <code>WSLabel</code>s
***********************************************************************************************/
public class ButterflyLabelUI extends BasicLabelUI {

  /***********************************************************************************************
  Creates a <code>ButterflyLabelUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyLabelUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyLabelUI();
  }

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  @Override
  public void paint(Graphics graphics, JComponent component) {
    if (component instanceof WSLabel) {
      WSLabel wsLabel = (WSLabel) component;

      if (wsLabel.getShowBorder()) {
        ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, 0, 0, component.getWidth(), component.getHeight());
      }

      if (wsLabel.getWrap()) {
        paintWrap(graphics, component);
      }
      else {
        super.paint(graphics, component);
      }

    }
    else {
      super.paint(graphics, component);
    }
  }

  /***********************************************************************************************
  Paints the <code>text</code> if the <code>label</code> is disabled
  <br />
  <b>NOTE: Uses <code>label.getText()</code> instead of <code>text</code> so that we don't have
  the <i>"..."</i> in the display
  @param label the <code>JLabel</code> being painted
  @param graphics the <code>Graphics</code> to paint on
  @param text the <code>String</code> to write
  @param textLeft the x position to start writing the <code>text</code>
  @param textTop the y position to start writing the <code>text</code>
  ***********************************************************************************************/
  @Override
  public void paintDisabledText(JLabel label, Graphics graphics, String text, int textLeft, int textTop) {
    ButterflyPainter.paintShadowText((Graphics2D) graphics, label.getText(), textLeft, textTop, LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
  Paints the <code>text</code> if the <code>label</code> is enabled
  <br />
  <b>NOTE: Uses <code>label.getText()</code> instead of <code>text</code> so that we don't have
  the <i>"..."</i> in the display
  @param label the <code>JLabel</code> being painted
  @param graphics the <code>Graphics</code> to paint on
  @param text the <code>String</code> to write
  @param textLeft the x position to start writing the <code>text</code>
  @param textTop the y position to start writing the <code>text</code>
  ***********************************************************************************************/
  @Override
  public void paintEnabledText(JLabel label, Graphics graphics, String text, int textLeft, int textTop) {

    String textToWrite = label.getText();

    /*
    if (label instanceof WSLabel) {
      WSLabel ws = (WSLabel) label;
      if (ws.getShortenLongText() && textToWrite != null) {
        // shorten the text if it's too long for the component
        int width = ws.getWidth();
    
        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(textToWrite);
    
        if (width == textWidth) {
          // exactly the right size
        }
        else {
          width -= 14;
          while (textWidth > width) {
            textToWrite = textToWrite.substring(0, textToWrite.length() - 1);
            textWidth = metrics.stringWidth(textToWrite);
          }
          // recalculate the textLeft so that the text is centered
          textLeft = (ws.getWidth() - textWidth) / 2; // ws.getWidth() so we avoid the -=14 above;
        }
      }
    }
    */

    ButterflyPainter.paintText((Graphics2D) graphics, textToWrite, textLeft, textTop, label.getForeground());
  }

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>, with the text wrapping on to
  new lines if required.
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paintWrap(Graphics graphics, JComponent component) {

    Rectangle paintIconR = new Rectangle();
    Rectangle paintTextR = new Rectangle();
    Rectangle paintViewR = new Rectangle();
    Insets paintViewInsets = new Insets(0, 0, 0, 0);

    WSLabel label = (WSLabel) component;
    String text = label.getText();
    Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

    int w = label.getWidth();
    int h = label.getHeight();

    if (label.isEnabled()) {
      //AquanauticPainter.paintBorder((Graphics2D)g,x,y,w,h);
    }

    if ((icon == null) && (text == null)) {
      return;
    }

    FontMetrics fm = graphics.getFontMetrics();
    Insets insets = component.getInsets(paintViewInsets);

    paintViewR.x = insets.left;
    paintViewR.y = insets.top;
    paintViewR.width = w - (insets.left + insets.right);
    paintViewR.height = h - (insets.top + insets.bottom);

    paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
    paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

    boolean centered = false;
    FontMetrics metric = null;
    if (label.getHorizontalAlignment() == SwingConstants.CENTER) {
      centered = true;
      try {
        metric = component.getGraphics().getFontMetrics();
      }
      catch (Throwable t2) {
      }
    }

    if (icon != null) {
      icon.paintIcon(component, graphics, paintIconR.x, paintIconR.y);
    }

    if (text != null) {
      // paint multiple lines
      String[] lines = WordWrap.wrap(text, component);

      //int textX = paintTextR.x;
      //int textY = paintTextR.y + fm.getAscent();
      int textX = insets.left;
      int textY = insets.top + fm.getAscent();

      int textHeight = fm.getHeight();

      textY += (h - (lines.length * textHeight)) / 2;

      for (int i = 0; i < lines.length; i++) {
        if (centered) {
          textX = (w - metric.stringWidth(lines[i])) / 2;
        }

        paintWrapText(label, graphics, lines[i], textX, textY);

        textY += textHeight;
      }

    }
    else {
      // paint single line, as per normal
      int textX = paintTextR.x;
      int textY = paintTextR.y + fm.getAscent();

      String clippedText = layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);
      paintWrapText(label, graphics, clippedText, textX, textY);

    }

  }

  /***********************************************************************************************
  Paints the <code>text</code> if the <code>label</code> is enabled
  <br />
  <b>NOTE: Does not use <code>label.getText()</code> because this method is used for wrapping and
  should use the <code>text</code> given to it.
  @param label the <code>JLabel</code> being painted
  @param graphics the <code>Graphics</code> to paint on
  @param text the <code>String</code> to write
  @param textLeft the x position to start writing the <code>text</code>
  @param textTop the y position to start writing the <code>text</code>
  ***********************************************************************************************/
  public void paintWrapText(JLabel label, Graphics graphics, String text, int textLeft, int textTop) {
    ButterflyPainter.paintText((Graphics2D) graphics, text, textLeft, textTop, label.getForeground());
  }
}