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
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/***********************************************************************************************
Used to paint the divider GUI for <code>WSSplitPane</code>s
***********************************************************************************************/

public class ButterflySplitPaneDivider extends BasicSplitPaneDivider {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ButterflySplitPaneDivider(BasicSplitPaneUI ui) {
    super(ui);
  }

  /***********************************************************************************************
  Paints the <code>component</code> on the <code>graphics</code>
  @param graphics the <code>Graphics</code> to paint the <code>component</code> on
  @param component the <code>Component</code> to paint
  ***********************************************************************************************/
  @Override
  public void paint(Graphics graphics) {

    int w = getWidth();
    int h = getHeight();

    Graphics blockCrop = graphics.create(0, 0, w, h);

    if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
      ButterflyPainter.paintSquareGradient((Graphics2D) blockCrop, 0, 0, w, h);
    }
    else {
      ButterflyPainter.paintSquareGradient((Graphics2D) blockCrop, 0, 0, w, h);
    }

  }
}