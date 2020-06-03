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
public abstract class WSOptionComboBox extends WSOptionPanel implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  WSComboBox comboBox;
  WSLabel label;

  /**
   **********************************************************************************************
   * FOR EXTENDED CLASSES ONLY
   **********************************************************************************************
   **/
  public WSOptionComboBox() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSOptionComboBox(XMLNode node) {
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
  public String getComparison() {
    return label.getText();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void loadComboBoxData();

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is deselected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
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
    Settings.set(setting, comboBox.getSelectedItem().toString());
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSelectedItem(String item) {
    comboBox.setSelectedItem(item);
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

    comboBox = new WSComboBox(XMLReader.read("<WSComboBox code=\"" + getSetting() + "\" />"));
    add(comboBox, BorderLayout.CENTER);

    //label = new WSLabel(XMLReader.read("<WSLabel code=\"" + getSetting() + "\" />"));
    //add(label,BorderLayout.WEST);

    //if (icon != null){
    //  JLabel label = new JLabel(icon);
    //  add(label,BorderLayout.WEST);
    //  }

    loadComboBoxData();

    //setSelectedItem(Settings.get(setting));
  }

}