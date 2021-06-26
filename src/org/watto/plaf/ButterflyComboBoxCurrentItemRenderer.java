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
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/***********************************************************************************************
Used to paint the GUI for the current item on <code>WSComboBox</code>es
***********************************************************************************************/

public class ButterflyComboBoxCurrentItemRenderer extends BasicComboBoxRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ButterflyComboBoxCurrentItemRenderer() {
    super();
  }

  /***********************************************************************************************
  Creates the renderer for the <code>Object</code>
  @param list the <code>JList</code> that contains the <code>Object</code>
  @param value the <code>Object</code> to render
  @param index the index of the <code>Object</code> in the <code>list</code>
  @param isSelected whether the <code>Object</code> is selected or not
  @param hasFocus whether the <code>Object</code> is focused or not
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {

    if (index == -1) {
      setBackground(new Color(0, 0, 0, 0));
      setOpaque(false);
    }
    else {
      setOpaque(true);
    }

    if (hasFocus) {
      setBackground(LookAndFeelManager.getMidColor());
    }
    else {
      setBackground(new Color(0, 0, 0, 0));
    }

    if (list != null) {
      setFont(list.getFont());
    }

    if (value instanceof Icon) {
      setIcon((Icon) value);
    }
    else {
      setText((value == null) ? "" : value.toString());
    }

    //setBorder(new EmptyBorder(0,0,0,0));

    return this;

  }
}