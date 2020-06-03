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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import javax.swing.plaf.PanelUI;
import org.watto.event.WSEventHandler;
import org.watto.plaf.ButterflyWSComponentListLabelUI;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/

public class WSComponentListLabel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  String code = "";
  boolean selected = false;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  WSComponentListLabel() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSComponentListLabel(String code) {
    this(new XMLNode(""));
    this.code = code;
    buildLabel();
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSComponentListLabel(XMLNode node) {
    super();
    node.setAttribute("code", ""); // so codes aren't overwritten in the ComponentRepository - this is basically a temp object
    toComponent(node);
    registerEvents();
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildLabel() {

    // label
    WSLabel label = new WSLabel(XMLReader.read("<WSLabel opaque=\"false\" code=\"ComponentList_" + code + "\" />"));
    label.setForeground(LookAndFeelManager.getTextColor());
    add(label, BorderLayout.CENTER);

    // image
    WSButton button = new WSButton(XMLReader.read("<WSButton repository=\"false\" code=\"" + code + "\" showText=\"false\" />"));
    add(button, BorderLayout.WEST);
    button.setCode(code);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isSelected() {
    return selected;
  }

  /**
   **********************************************************************************************
   * Processes the given event
   * @param event the event that was triggered
   **********************************************************************************************
   **/
  @Override
  public void processEvent(AWTEvent event) {
    super.processEvent(event); // handles any normal listeners
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
   **********************************************************************************************
   * Registers the events that this component generates
   **********************************************************************************************
   **/
  @Override
  public void registerEvents() {
    enableEvents(
        AWTEvent.COMPONENT_EVENT_MASK |
            AWTEvent.CONTAINER_EVENT_MASK |
            AWTEvent.MOUSE_EVENT_MASK |
            AWTEvent.MOUSE_MOTION_EVENT_MASK |
            AWTEvent.HIERARCHY_EVENT_MASK |
            AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK |
            AWTEvent.INPUT_METHOD_EVENT_MASK |
            WSComponent.WS_EVENT_MASK);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  /**
   **********************************************************************************************
   * Overwritten to force use of AquanauticBorderLabelUI
   * @param ui not used
   **********************************************************************************************
   **/
  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ButterflyWSComponentListLabelUI.createUI(this));
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
  }

  /**
   **********************************************************************************************
   * Builds an XMLNode that describes this object
   * @return an XML node with the details of this object
   **********************************************************************************************
   **/
  @Override
  public XMLNode toXML() {
    XMLNode node = super.toXML();
    return node;
  }

}