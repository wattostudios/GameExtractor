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

import java.awt.Rectangle;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicComboPopup;

/***********************************************************************************************
 * Used to paint the icon for <code>WSCheckbox</code>es
 ***********************************************************************************************/

public class ButterflyComboPopup extends BasicComboPopup {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
   * Creates the popup for the <code>JComboBox</code>
   * @param comboBox the <code>JComboBox</code> to create the popup for
   ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public ButterflyComboPopup(JComboBox comboBox) {
    super(comboBox);
  }

  /***********************************************************************************************
   * Calculates the bounds of the popup
   * @param x the x position of the popup
   * @param y the y position of the popup
   * @param w the width of the popup
   * @param h the height of the popup
   * @return the bounds of the popup, as a <code>Rectangle</code>
   ***********************************************************************************************/
  @Override
  protected Rectangle computePopupBounds(int x, int y, int w, int h) {
    int bor = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");
    Rectangle bounds = super.computePopupBounds(x + bor, y - bor, w - bor - bor, h);
    return bounds;
  }

  /***********************************************************************************************
   * Performs any setup on the scroller
   ***********************************************************************************************/
  @Override
  protected void configureScroller() {
    //scroller.setFocusable(false);
    //scroller.getVerticalScrollBar().setFocusable(false);
    scroller.setOpaque(false);
    //scroller.setBackground(AquanauticTheme.COLOR_DARK);
  }

  /***********************************************************************************************
   * Creates the <code>JScrollPane</code> scroller for the list
   * @return the <code>JScrollPane</code>
   ***********************************************************************************************/
  @Override
  protected JScrollPane createScroller() {
    return new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  }
}