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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.xml.XMLNode;

/**
**********************************************************************************************
A ExtendedTemplate
**********************************************************************************************
**/
public class WSOptionComboBox_Language extends WSOptionComboBox {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSOptionComboBox_Language(XMLNode node) {
    // NEED TO DO THIS HERE, OTHERWISE THE SETTING VARIABLE DOESN'T GET SAVED!!! (not sure why)
    //super(node);
    super();
    toComponent(node);
    registerEvents();
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
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadComboBoxData() {
    String[] languages = Language.getLanguageNames();

    int selected = 0;
    String value = Settings.get(setting);

    for (int i = 0; i < languages.length; i++) {
      if (languages[i].equals(value)) {
        selected = i;
        break;
      }
    }

    comboBox.setModel(new DefaultComboBoxModel(languages));
    comboBox.setSelectedIndex(selected);
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSSelectableListener when an item is selected
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    super.onSelect(c, e);
    Language.changeLanguage(comboBox.getSelectedItem().toString());
    return true;
  }

}