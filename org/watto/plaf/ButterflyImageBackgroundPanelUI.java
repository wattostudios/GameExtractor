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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import org.watto.Settings;
import org.watto.component.WSPanel;

/***********************************************************************************************
 * Used to paint the GUI for <code>WSPanel</code>s, where the background can be white, black, or
 * a tiled pattern of greys
 ***********************************************************************************************/
public class ButterflyImageBackgroundPanelUI extends BasicPanelUI {

  /** static instance of the GUI painter **/
  private static final ButterflyImageBackgroundPanelUI panelUI = new ButterflyImageBackgroundPanelUI();

  /***********************************************************************************************
   * Gets the static <code>panelUI</code> instance
   * @param component the <code>Component</code> to get the painter for
   * @return the painter <code>ComponentUI</code>
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return panelUI;
  }

  /***********************************************************************************************
   * Sets up the painting properties for painting on the <code>Component</code>
   * @param component the <code>Component</code> that will be painted
   ***********************************************************************************************/
  @Override
  public void installUI(JComponent component) {
    super.installUI(component);
    //component.setOpaque(false);
    //component.setBackground(Color.WHITE);
  }

  /***********************************************************************************************
   * Paints the <code>component</code> on the <code>graphics</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   * @param component the <code>Component</code> to paint
   ***********************************************************************************************/
  @Override
  public void paint(Graphics graphics, JComponent component) {
    int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");
    int th = LookAndFeelManager.getTextHeight();

    int x = 0;
    int y = 0;
    int w = component.getWidth();
    int h = component.getHeight();

    ButterflyPainter.paintOpaque(graphics, component);

    if (component.getParent() instanceof JLayeredPane) {
      ButterflyPainter.paintSolidBackground((Graphics2D) graphics, 0, 0, w, h, LookAndFeelManager.getBackgroundColor());
    }

    WSPanel wsPanel = null;
    if (component instanceof WSPanel) {
      wsPanel = (WSPanel) component;
    }

    //
    // THIS IS THE ONLY CHANGED SECTION, OVER ButterflyPanelUI
    //
    // Start
    //
    if (wsPanel != null && wsPanel.obeyBackgroundColor()) {
      Color bgColor = wsPanel.getBackground();
      ButterflyPainter.paintSolidBackground((Graphics2D) graphics, 0, 0, w, h, bgColor);
      if (Settings.getBoolean("TiledPreviewBackground")) {
        paintTiledBackground((Graphics2D) graphics, 0, 0, w, h, bgColor);
      }
    }
    //
    // End
    //
    // THIS IS THE ONLY CHANGED SECTION, OVER ButterflyPanelUI
    //

    if (wsPanel != null && wsPanel.getShowLabel()) {
      y += th;
      h -= th;
    }

    if (wsPanel != null && wsPanel.getShowBorder()) {
      if (wsPanel.getPaintBackground()) {
        //ButterflyPainter.paintCurvedGradient((Graphics2D)graphics,x,y,w,h);
        ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, x, y, w, h);
      }
      else {
        // paint the border only, no background (KINDA - actually paints the solid background then a white background over the top)
        ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, x, y, w, h);
        ButterflyPainter.paintSolidBackground((Graphics2D) graphics, x + 6, y + 6, w - 12, h - 12, LookAndFeelManager.getBackgroundColor());
      }
    }

    if (wsPanel != null && wsPanel.getShowLabel()) {
      // paint the label
      String labelText = wsPanel.getLabel();

      // set the font before getting the metrics
      graphics.setFont(LookAndFeelManager.getFont().deriveFont(Font.BOLD));

      FontMetrics metrics = graphics.getFontMetrics();
      int textHeight = metrics.getHeight();
      int textWidth = metrics.stringWidth(labelText);

      // paint the border around the label
      graphics.setColor(LookAndFeelManager.getDarkColor());

      if (!wsPanel.getShowBorder()) {
        // clip - we only want the top border
        graphics.setClip(0, 0, w, th + 4);
        ButterflyPainter.paintSquareSolid((Graphics2D) graphics, x, y, w, h);
        ButterflyPainter.paintSolidBackground((Graphics2D) graphics, x + 4, y + 4, w - 8, h - 8, LookAndFeelManager.getBackgroundColor());
        graphics.setClip(null);
      }

      // Paint the border around the label
      graphics.setClip(0, 0, w, th + 2);
      ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, pad / 2, 0, textWidth + pad + pad, th + pad + 3, LookAndFeelManager.getMidColor());
      // paint to "remove" the bottom border from around the label, so it looks like the label border is attached to the main border
      graphics.setClip(pad / 2, th + 2, textWidth + pad + pad, 1);
      ButterflyPainter.paintSolidBackground((Graphics2D) graphics, pad / 2 + 2, 0, textWidth + pad + pad - 4, th + pad + 3, LookAndFeelManager.getMidColor());
      // reset the clip
      graphics.setClip(null);

      ButterflyPainter.paintShadowText((Graphics2D) graphics, labelText, pad + pad / 2, pad + textHeight / 2 + 1);
    }
  }

  /***********************************************************************************************
   * Paints a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param background the background <code>Color</code>
   ***********************************************************************************************/
  public static void paintTiledBackground(Graphics2D graphics, int x, int y, int w, int h, Color bgColor) {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

    if (bgColor.equals(Color.WHITE)) {
      graphics.setColor(new Color(230, 230, 230));
    }
    else if (bgColor.equals(Color.BLACK)) {
      graphics.setColor(new Color(25, 25, 25));
    }
    else {
      graphics.setColor(new Color(255, 255, 255, 20));
    }

    int blockSize = 10;
    int jumpSize = blockSize * 2;

    boolean alternateRow = true;
    for (int j = 0; j < h; j += blockSize) {
      alternateRow = !alternateRow;

      int startPos = 0;
      if (alternateRow) {
        startPos = blockSize;
      }

      for (int i = startPos; i < w; i += jumpSize) {
        graphics.fillRect(i, j, blockSize, blockSize);
      }
    }

  }

  /***********************************************************************************************
   * Removes the painting properties from the <code>Component</code>
   * @param component the <code>Component</code> to remove the properties from
   ***********************************************************************************************/
  @Override
  public void uninstallUI(JComponent component) {
    super.uninstallUI(component);
    //component.setOpaque(true);
  }
}