/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.component;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
**********************************************************************************************
Allows WSComponents to be shown in a WSList (typicaly WSButton or WSComboButton - small
components like that)
**********************************************************************************************
**/
public class WSComponentListCellRenderer extends DefaultListCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSComponentListCellRenderer() {
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if (value instanceof String) {
      WSComponentListLabel label = new WSComponentListLabel((String) value);
      label.setSelected(isSelected);
      return label;
    }
    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
  }

}