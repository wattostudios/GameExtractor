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

package org.watto.component;

import javax.swing.border.EmptyBorder;
import javax.swing.plaf.PanelUI;
import org.watto.plaf.ButterflyShadowPanelUI;
import org.watto.xml.XMLNode;

/***********************************************************************************************
This <code>WSPanel</code> will paint a shadow under the <code>Component</code> that is
<code>add()</code> to this <code>WSPanel</code>
***********************************************************************************************/

public class WSShadowPanel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSShadowPanel() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  ***********************************************************************************************/
  public WSShadowPanel(XMLNode node) {
    super(node);
    this.setBorder(new EmptyBorder(0, 0, 5, 5));
    setOpaque(false);
  }

  /***********************************************************************************************
  Sets the GUI renderer for this <code>Component</code>. Overwritten to force the use of
  <code>ButterflyShadowPanelUI</code>
  @param ui <i>not used</i>
  TODO - need to make generic
  ***********************************************************************************************/
  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ButterflyShadowPanelUI.createUI(this));
  }

}