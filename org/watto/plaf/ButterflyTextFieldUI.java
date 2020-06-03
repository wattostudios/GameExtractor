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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import org.watto.component.WSTextField;

/***********************************************************************************************
 * Used to paint the GUI for <code>WSTextField</code>s
 ***********************************************************************************************/
public class ButterflyTextFieldUI extends BasicTextFieldUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyTextFieldUI</code> instance for rendering the
   * <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyTextFieldUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyTextFieldUI();
  }

  /** The <code>JTextField</code> to be painted **/
  JTextField textField;

  /** Whether to paint the border or not **/
  boolean borderSet = false;

  /***********************************************************************************************
   * Sets up the painting properties for painting on the <code>Component</code>
   * @param component the <code>Component</code> that will be painted
   ***********************************************************************************************/
  @Override
  public void installUI(JComponent component) {
    super.installUI(component);
    textField = (JTextField) component;
    textField.setMinimumSize(new Dimension(textField.getHeight(), 0));
  }

  /**
   **********************************************************************************************
   * Paints the <code>textField</code> on the <code>graphics</code>
   * @param graphics the <code>Graphics</code> to paint the <code>textField</code> on
   **********************************************************************************************
   **/
  @Override
  public void paintBackground(Graphics graphics) {
    //public void paintSafely(Graphics g){
    WSTextField wstf = null;
    if (textField instanceof WSTextField) {
      wstf = (WSTextField) textField;
    }

    if (!borderSet) {
      // Sets the border on the editor textField so that it fits correctly inside the
      // table and list cells of a JFileChooser dialog
      if (textField.getParent().getClass().toString().indexOf("MetalFileChooserUI") >= 0 && textField.getBorder().getBorderInsets(textField).bottom != 2) {
        textField.setBorder(new EmptyBorder(2, 2, 2, 2));
      }
      borderSet = true;
    }

    int x = 0;
    int y = 0;
    int w = textField.getWidth();
    int h = textField.getHeight();

    int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");
    int th = LookAndFeelManager.getTextHeight();

    if (wstf != null && wstf.getShowLabel()) {
      y += th;
      h -= th;
    }

    ButterflyPainter.paintOpaque(graphics, textField);

    if (!(textField.getParent() instanceof JTable)) {
      ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, x, y, w, h);
    }
    else {
      // paint an opaque background - it is an inline editor
      ButterflyPainter.paintGradientBackground((Graphics2D) graphics, x, y, w, h);
    }

    // paint the tooltip in the background
    /*
     * String text = tf.getText(); if (text == null || text.equals("")){ String toolTipText =
     * tf.getToolTipText(); if (toolTipText != null && toolTipText.length() > 0){ toolTipText =
     * "Tip: " + toolTipText; Font originalFont = g.getFont();
     *
     * g.setFont(originalFont.deriveFont(Font.BOLD+Font.ITALIC));
     * g.setColor(AquanauticTheme.COLOR_TOOLTIP); g.drawString(toolTipText,pad,h+1);
     * g.setFont(originalFont); } }
     */

    if (wstf != null && wstf.getShowLabel()) {
      // paint the label
      String labelText = wstf.getLabel();

      // set the font before getting the metrics
      //g.setFont(g.getFont().deriveFont(Font.BOLD));
      graphics.setFont(LookAndFeelManager.getFont().deriveFont(Font.BOLD));

      FontMetrics metrics = graphics.getFontMetrics();
      int textHeight = metrics.getHeight();
      int textWidth = metrics.stringWidth(labelText);

      // paint the border around the label
      graphics.setColor(LookAndFeelManager.getDarkColor());
      graphics.setClip(0, 0, textWidth + pad + pad + pad / 2, th + 2);
      //g.clipRect(textHeight/4*3,pad/2+2,textWidth+pad+pad,1);
      ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, pad / 2, 0, textWidth + pad + pad, th + pad + pad + 2, LookAndFeelManager.getMidColor());
      // paint to "remove" the bottom border from around the label, so it looks like the label border is attached to the main border
      graphics.setClip(pad / 2, th + 2, textWidth + pad + pad, 1);
      ButterflyPainter.paintSolidBackground((Graphics2D) graphics, pad / 2 + 2, 0, textWidth + pad + pad - 4, th + pad + 3, LookAndFeelManager.getMidColor());
      // reset the clip
      graphics.setClip(null);

      /*
       * // paint the text AquanauticPainter.enableAntialias(g);
       * //g.setFont(g.getFont().deriveFont(Font.BOLD));
       *
       * //g.setColor(AquanauticTheme.COLOR_LIGHT); g.setColor(AquanauticTheme.COLOR_TEXT);
       * g.drawString(labelText,pad+pad/2+1,pad+textHeight/2+1);
       *
       * //g.setColor(AquanauticTheme.COLOR_TEXT); g.setColor(AquanauticTheme.COLOR_BG);
       * g.drawString(labelText,pad+pad/2,pad+textHeight/2);
       */
      ButterflyPainter.paintShadowText((Graphics2D) graphics, labelText, pad + pad / 2, pad + textHeight / 2 + 1);

    }

    /*
     * g.setFont(AquanauticTheme.FONT);
     *
     * FontMetrics metrics = g.getFontMetrics(); int textHeight = metrics.getHeight();
     *
     *
     * // paint the text Shape clip = g.getClip();
     *
     * int clipWidth = w-pad-pad; g.setClip(new
     * Rectangle2D.Float(pad,th+pad,clipWidth,textHeight));
     *
     * String text = tf.getText(); int textWidth = metrics.stringWidth(text);
     *
     * y += textHeight - pad/2 + th; x += pad; if (textWidth > clipWidth){ x = clipWidth -
     * textWidth; }
     *
     * // paint the highlighted background int startSelection = tf.getSelectionStart(); int
     * endSelection = tf.getSelectionEnd();
     *
     * if (startSelection > 0 && endSelection > 0){ int selectedWidth =
     * metrics.stringWidth(text.substring(startSelection,endSelection)); int selectedLeft = x +
     * metrics.stringWidth(text.substring(0,startSelection)); int selectedTop = th+pad;
     *
     * AquanauticPainter.paintSolidBackground((Graphics2D)g,selectedLeft,selectedTop,
     * selectedWidth,textHeight,AquanauticTheme.COLOR_MID); }
     *
     * AquanauticPainter.paintText((Graphics2D)g,text,x,y);
     *
     * g.setClip(clip);
     */
  }

  /***********************************************************************************************
   * Paints the <code>textField</code> on the <code>graphics</code>
   * @param graphics the <code>Graphics</code> to paint the <code>textField</code> on
   ***********************************************************************************************/
  @Override
  public void paintSafely(Graphics graphics) {
    if (textField instanceof WSTextField) {
      if (!textField.isOpaque()) {
        // forces the painting of the background if the component is !opaque
        // because if opaque it doesn't normally call paintBackground();
        paintBackground(graphics);
      }
    }
    super.paintSafely(graphics);
  }
}