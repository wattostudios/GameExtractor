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

import java.awt.BorderLayout;
import java.awt.event.ItemListener;
import javax.swing.JComponent;
import org.watto.Settings;
import org.watto.event.WSSelectableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/
public class WSOptionCheckBox extends WSOptionPanel implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSCheckBox checkBox;

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSOptionCheckBox(XMLNode node) {
    // NEED TO DO THIS HERE, OTHERWISE THE SETTING VARIABLE DOESN'T GET SAVED!!! (not sure why)
    //super(node);
    super();
    toComponent(node);
    registerEvents();
    //System.out.println("SETTING for " + getCode() + " = " + getSetting());
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  Adds an ItemListener to the Checkbox, for additional processing on Select/Deselect
  **********************************************************************************************
  **/
  public void addItemListener(ItemListener listener) {
    checkBox.addItemListener(listener);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getComparison() {
    checkBox.setSelected(Settings.getBoolean(setting));
    return checkBox.getText();
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is deselected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    Settings.set(setting, "false");
    //System.out.println(setting + " is deselected");
    return true;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is selected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    Settings.set(setting, "true");
    return true;
  }

  /**
   **********************************************************************************************
   * Build this object from the <i>node</i>
   * @param node the XML node that indicates how to build this object
   **********************************************************************************************
   **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    checkBox = new WSCheckBox(XMLReader.read("<WSCheckBox code=\"" + setting + "\" horizontal-alignment=\"left\" />"));
    add(checkBox, BorderLayout.CENTER);

    //if (icon != null){
    //  JLabel label = new JLabel(icon);
    //  add(label,BorderLayout.WEST);
    //  }

    checkBox.setSelected(Settings.getBoolean(setting));
    //setSetting(setting);

    checkBox.setOpaque(isOpaque()); // so that the checkbox copies the setting of this panel
  }

}