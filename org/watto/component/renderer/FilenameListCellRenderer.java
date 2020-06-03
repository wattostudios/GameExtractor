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

package org.watto.component.renderer;

import java.awt.Component;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/***********************************************************************************************
A <code>ListCellRenderer</code> that only shows the name of a <code>File</code> rather than the
full path of the <code>File</code>
***********************************************************************************************/

public class FilenameListCellRenderer extends DefaultListCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public FilenameListCellRenderer() {
  }

  /***********************************************************************************************
  Gets the <code>Component</code> used to render the <code>value</code> in the <code>list</code>
  @param list the <code>JList</code> that contains the <code>value</code> to render
  @param value the <code>Object</code> to render
  @param index the index of the <code>value</code> in the <code>list</code>
  @param isSelected <b>true</b>  if this <code>value</code> is selected in the <code>list</code><br />
                    <b>false</b> if this <code>value</code> is not selected
  @param cellHasFosus <b>true</b>  if this <code>value</code> has focus in the <code>list</code><br />
                      <b>false</b> if this <code>value</code> does not have focus
  @return the renderer <code>Component</code> for this <code>value</code> in the <code>list</code>
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value instanceof File && renderer instanceof JLabel) {
      ((JLabel) renderer).setText(((File) value).getName());
    }
    return renderer;
  }

}