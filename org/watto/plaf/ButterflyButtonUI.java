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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import org.watto.component.WSButton;

/***********************************************************************************************
Used to paint the GUI for <code>WSButton</code>s
***********************************************************************************************/
public class ButterflyButtonUI extends BasicButtonUI {

  /***********************************************************************************************
  Sets up the painting properties for painting on the <code>Component</code>
  @param component the <code>Component</code> that will be painted
  ***********************************************************************************************/
  public void installUI(JComponent component) {
    super.installUI(component);

    JButton button = (JButton) component;

    button.setBorderPainted(false);
    button.setOpaque(false);
  }

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  public void paint(Graphics graphics, JComponent component) {
    JButton button = (JButton) component;

    int borderWidth = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");

    int x = 0;
    int y = 0;
    int w = button.getWidth();
    int h = button.getHeight();

    int orientation = 0;
    if (button instanceof WSButton) {
      orientation = ((WSButton) button).getTextOrientation();
    }
    boolean sideways = (orientation == WSButton.ORIENTATION_LEFT || orientation == WSButton.ORIENTATION_RIGHT);

    ButterflyPainter.setSidewaysGradient(sideways);

    ButtonModel model = button.getModel();
    if (model.isArmed() && model.isPressed()) {
      ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, x, y, w, h);
    }
    //    else if (component instanceof WSButton && ((WSButton)component).isCurrentPanel()){
    //      AquanauticPainter.paintFocusedBorder((Graphics2D)graphics,x,y,w,h);
    //      }
    else {
      ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, x, y, w, h, LookAndFeelManager.getLightColor(), LookAndFeelManager.getMidColor());
    }

    ButterflyPainter.setSidewaysGradient(false);

    Icon icon = button.getIcon();
    String text = button.getText();

    if (icon != null) {
      int iconLeft = borderWidth;
      int iconTop = borderWidth;

      int iconWidth = icon.getIconWidth();
      int iconHeight = icon.getIconHeight();

      if (iconTop + borderWidth + iconHeight < h) {
        iconTop = h / 2 - iconHeight / 2;
      }

      if (text == null || text.equals("")) {
        if (iconLeft + borderWidth + iconWidth < w) {
          iconLeft = w / 2 - iconWidth / 2;
        }
      }

      paintIcon(graphics, button, new Rectangle(iconLeft, iconTop, iconWidth, iconHeight));
    }

    if (text != null && !text.equals("")) {

      FontMetrics metrics = graphics.getFontMetrics();
      int textWidth = metrics.stringWidth(text);

      int textLeft = button.getWidth() / 2 - textWidth / 2;
      int textTop = button.getHeight() / 2 + borderWidth - 2;

      if (icon != null) {
        if (sideways) {
          textTop += icon.getIconHeight() / 2 + borderWidth / 2;
        }
        else {
          textLeft += icon.getIconWidth() / 2 + borderWidth / 2;
        }
      }

      if (button instanceof WSButton && ((!sideways && ((textLeft <= borderWidth) || (textLeft + textWidth + borderWidth >= w))) || (sideways && ((textTop <= borderWidth) || (textTop + textWidth + borderWidth >= h))))) {
        // try to get the small text
        text = ((WSButton) button).getSmallText();

        textWidth = metrics.stringWidth(text);
        textLeft = button.getWidth() / 2 - textWidth / 2;

        if (icon != null) {
          textLeft += icon.getIconWidth() / 2 + borderWidth / 2;
        }
      }

      // If the orientation is not normal (UP), rotate the painting canvas
      AffineTransform originalTransformation = ((Graphics2D) graphics).getTransform();
      if (orientation == WSButton.ORIENTATION_UP) {
        // here to speed things up slightly, as most components are painted the right way up!
      }
      else if (orientation == WSButton.ORIENTATION_LEFT) {
        ((Graphics2D) graphics).rotate(-Math.PI / 2, (w + borderWidth) / 2 + 1, textTop);
      }
      else if (orientation == WSButton.ORIENTATION_RIGHT) {
        ((Graphics2D) graphics).rotate(Math.PI / 2, (w - borderWidth) / 2, textTop);
      }
      else if (orientation == WSButton.ORIENTATION_DOWN) {
        ((Graphics2D) graphics).rotate(Math.PI, (w - borderWidth) / 2 + 2, textTop);
      }

      if (model.isArmed() && model.isPressed()) {
        ButterflyPainter.paintShadowText((Graphics2D) graphics, text, textLeft, textTop);
      }
      else {
        if (!button.isEnabled()) {
          ButterflyPainter.paintShadowText((Graphics2D) graphics, text, textLeft, textTop, LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor());
        }
        else {
          ButterflyPainter.paintText((Graphics2D) graphics, text, textLeft, textTop);
        }
      }

      // Return the transformation on the graphics back to the original transform
      ((Graphics2D) graphics).setTransform(originalTransformation);

    }
  }

  /***********************************************************************************************
  Removes the painting properties from the <code>Component</code>
  @param component the <code>Component</code> to remove the properties from
  ***********************************************************************************************/
  public void uninstallUI(JComponent component) {
    super.uninstallUI(component);

    JButton button = (JButton) component;

    button.setBorderPainted(true);
    button.setOpaque(true);
  }

  /***********************************************************************************************
  Creates a <code>ButterflyButtonUI</code> instance for rendering the <code>component</code>
  @param component the <code>Component</code> to get the painter for
  @return a new <code>ButterflyButtonUI</code> instance
  ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyButtonUI();
  }
}