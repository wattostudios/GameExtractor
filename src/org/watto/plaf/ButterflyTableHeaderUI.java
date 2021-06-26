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
import java.io.File;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.watto.ErrorLogger;

/***********************************************************************************************
 * Used to paint the GUI for <code>WSTable</code> headers
 ***********************************************************************************************/
public class ButterflyTableHeaderUI extends BasicTableHeaderUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyTableHeaderUI</code> instance for rendering the
   * <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyTableHeaderUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyTableHeaderUI();
  }

  /***********************************************************************************************
   * Sets up the painting properties for painting on the <code>Component</code>
   * @param component the <code>Component</code> that will be painted
   ***********************************************************************************************/
  @Override
  public void installUI(JComponent component) {
    super.installUI(component);
  }

  /***********************************************************************************************
   * Paints the <code>component</code> on the <code>graphics</code>
   * @param graphics the <code>Graphics</code> to paint the <code>component</code> on
   * @param component the <code>Component</code> to paint
   ***********************************************************************************************/
  @SuppressWarnings("deprecation")
  @Override
  public void paint(Graphics graphics, JComponent component) {
    JTableHeader header = (JTableHeader) component;
    JTable table = header.getTable();

    int pad = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");

    TableColumnModel model = header.getColumnModel();

    int x = 0;
    int h = header.getHeight();

    FontMetrics metrics = graphics.getFontMetrics();

    ImageIcon editableIcon = null;
    int iconWidth = 0;
    int iconHeight = 0;
    //URL icon = WSHelper.getResource("images/WSTableHeader/editable.png");
    try {
      URL icon = new File("images/WSTableHeader/editable.png").toURL();
      if (icon != null) {
        editableIcon = new ImageIcon(icon);
        iconWidth = editableIcon.getIconWidth();
        iconHeight = editableIcon.getIconHeight();
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    for (int i = 0; i < model.getColumnCount(); i++) {
      TableColumn column = model.getColumn(i);
      boolean editable = false;

      try {
        editable = table.isCellEditable(0, i);
        if (editableIcon == null) {
          editable = false;
        }
      }
      catch (Throwable t) {
        // ignore errors from non-WSTable components
        // (even though this class is only used by WSTable)
        editable = false;
      }

      String text = column.getHeaderValue().toString();
      int textWidth = metrics.stringWidth(text);

      int w = column.getWidth();
      int wDraw = w;

      if (editable) {
        w -= (iconWidth + pad); // need 6 for the editable icon
      }

      while (textWidth > (w - 4)) {
        // resize the header text so it fits
        int lastSpace = text.lastIndexOf(" ");
        if (lastSpace > 0) {
          // cut the last word off
          text = text.substring(0, lastSpace);
        }
        else {
          // cut letters off
          text = text.substring(0, text.length() - 1);
        }

        textWidth = metrics.stringWidth(text);

        if (text.equals("")) {
          break;
        }

      }

      // if less than 3 characters of text, don't paint the text
      if (text.length() < 3) {
        text = "";
      }

      //ButterflyPainter.paintSquareGradient((Graphics2D)graphics,x - 1,-1,wDraw + 2,h + 1);
      ButterflyPainter.paintSolidBackground((Graphics2D) graphics, x, -1, wDraw, h, LookAndFeelManager.getMidColor());
      ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, x, -1, wDraw, h);

      if (editable) {
        editableIcon.paintIcon(component, graphics, x + pad, (h - iconHeight) / 2);
      }

      int textLeft = x + (w / 2 - textWidth / 2);
      if (editable) {
        textLeft += iconWidth + pad;
      }

      int textHeight = metrics.getHeight() + 1;
      int textTop = pad + pad + (textHeight / 2) + 1;

      ButterflyPainter.paintText((Graphics2D) graphics, text, textLeft, textTop);

      x += wDraw;
    }
  }

  /***********************************************************************************************
   * Removes the painting properties from the <code>Component</code>
   * @param component the <code>Component</code> to remove the properties from
   ***********************************************************************************************/
  @Override
  public void uninstallUI(JComponent component) {
    super.uninstallUI(component);
  }
}