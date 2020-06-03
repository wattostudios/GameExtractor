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

import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollPaneUI;
import org.watto.component.WSScrollPane;

/***********************************************************************************************
 * Used to paint the GUI for <code>WSScrollPane</code>s
 ***********************************************************************************************/
public class ButterflyScrollPaneUI extends MetalScrollPaneUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyScrollPaneUI</code> instance for rendering the
   * <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyScrollPaneUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyScrollPaneUI();
  }

  /***********************************************************************************************
   * Sets up the painting properties for painting on the <code>Component</code>
   * @param component the <code>Component</code> that will be painted
   ***********************************************************************************************/
  @Override
  public void installUI(JComponent component) {
    super.installUI(component);

    JScrollPane scrollPane = (JScrollPane) component;

    int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");

    //JViewport colHeader = scrollPane.getColumnHeader();
    //if (colHeader != null){
    //  scrollPane.setViewportBorder(new EmptyBorder(0,pad,pad,pad));
    //  ((JComponent)colHeader.getView()).setBorder(new EmptyBorder(0,pad,0,pad));
    //  }
    //else {
    if (scrollPane instanceof WSScrollPane) {
      if (((WSScrollPane) scrollPane).getShowLabel()) {
        // showing a label
        scrollPane.setViewportBorder(new EmptyBorder(2, pad, pad, pad));
      }
      else {
        // no label
        scrollPane.setViewportBorder(new EmptyBorder(pad, pad, pad, pad));
      }
    }
    //  }
  }

  /***********************************************************************************************
   * Paints the <code>component</code> on the <code>graphics</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   * @param component the <code>Component</code> to paint
   ***********************************************************************************************/
  @Override
  public void paint(Graphics graphics, JComponent component) {
    JScrollPane scrollPane = (JScrollPane) component;

    WSScrollPane wssp = null;
    if (scrollPane instanceof WSScrollPane) {
      wssp = (WSScrollPane) scrollPane;
    }

    int x = 0;
    int y = 0;
    int w = component.getWidth();
    int h = component.getHeight();

    int pad = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");
    int textHeight = LookAndFeelManager.getTextHeight();

    if (wssp != null && wssp.getShowLabel()) {
      y += textHeight;
      h -= textHeight;
    }

    //AquanauticPainter.paintOpaque(g,c);

    if (wssp != null && !wssp.getShowBorder()) {
      // don't show the border
      if (wssp != null && wssp.getShowBackground()) {
        // paint the white background bit if the attribute = true
        ButterflyPainter.paintSolidBackground((Graphics2D) graphics, x, y, w, h, LookAndFeelManager.getBackgroundColor());
      }
      else {
      }
    }
    else {
      //AquanauticPainter.paintScrollPaneBorder((Graphics2D)g,x,y,w,h);
      //ButterflyPainter.paintSquareSolidNew((Graphics2D)graphics,x,y,w,h);

      Container parent = component.getParent();
      if (parent instanceof ButterflyComboPopup) {
        // don't paint the border - the popup itself contains the border
      }
      else {
        // paint the border, as it's on a regular panel
        ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, x, y, w, h);
      }

      if ((wssp != null && wssp.getShowBackground()) || wssp == null) {
        // paint the white background bit if it is a JComponent (ie not WSComponent) or if the attribute = true
        ButterflyPainter.paintSolidBackground((Graphics2D) graphics, x + 4, y + 4, w - 8, h - 8, LookAndFeelManager.getBackgroundColor());
      }
    }

    if (!(component.getParent() instanceof JSplitPane)) {
      //AquanauticPainter.paintBorder((Graphics2D)g,x,y,w,h);
    }

    if (wssp != null && wssp.getShowLabel()) {
      // paint the label
      String labelText = wssp.getLabel();

      // set the font before getting the metrics
      //g.setFont(g.getFont().deriveFont(Font.BOLD));
      graphics.setFont(LookAndFeelManager.getFont().deriveFont(Font.BOLD));

      FontMetrics metrics = graphics.getFontMetrics();
      int textWidth = metrics.stringWidth(labelText);

      // paint the border around the label
      graphics.setColor(LookAndFeelManager.getDarkColor());
      // don't want to clip the border for this one, because we want the border to go fully around the label,
      // instead of looking just like a tab.
      //g.setClip(0,0,textWidth+pad+pad+pad+pad/2,textHeight+3);
      //g.clipRect(textHeight/4*3,pad/2+2,textWidth+pad+pad,1);
      //AquanauticPainter.paintFillBorder((Graphics2D)g,pad/2,0,textWidth+pad+pad,textHeight+pad+pad,AquanauticTheme.COLOR_MID);
      ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, pad / 2, 0, textWidth + pad + pad, textHeight + pad + 2, LookAndFeelManager.getMidColor());
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
      textHeight = metrics.getHeight();
      ButterflyPainter.paintShadowText((Graphics2D) graphics, labelText, pad + pad / 2, pad + textHeight / 2 + 2);

    }

    if (wssp != null && wssp.getShowInnerBorder()) {
      // paint the border around the viewport
      Rectangle viewportBounds = wssp.getViewportBorderBounds();

      x = viewportBounds.x;
      y = viewportBounds.y;
      w = viewportBounds.width;
      h = viewportBounds.height;

      // enlarge the bounds to allow for the 2-pixel border witdh
      //x -= 2;
      //y -= 2;
      //w += 4;
      //h += 4;

      ButterflyPainter.paintCurvedSolid((Graphics2D) graphics, x, y, w, h, LookAndFeelManager.getMidColor());

    }

  }
}