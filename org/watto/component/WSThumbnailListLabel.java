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
import java.awt.Dimension;
import javax.swing.plaf.PanelUI;
import org.watto.datatype.ImageResource;
import org.watto.event.WSEventHandler;
import org.watto.plaf.ButterflyWSThumbnailListLabelUI;
import org.watto.xml.XMLNode;

/**
**********************************************************************************************
A ExtendedTemplate
**********************************************************************************************
**/
public class WSThumbnailListLabel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  static int height;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void setHeight(int newHeight) {
    height = newHeight;
  }

  String code = "";

  boolean selected = false;

  ImageResource imageResource;

  /**
  **********************************************************************************************
  Constructor for extended classes only
  **********************************************************************************************
  **/
  WSThumbnailListLabel() {
    super();
  }

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSThumbnailListLabel(ImageResource imageResource, boolean selected, int height) {
    this(new XMLNode(""));
    this.imageResource = imageResource;
    setSelected(selected);
    setHeight(height);
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSThumbnailListLabel(XMLNode node) {
    super();
    node.setAttribute("code", ""); // so codes aren't overwritten in the WSRepository - this is basically a temp object
    toComponent(node);
    registerEvents();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getHeight() {
    return getWidth();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource getImageResource() {
    return imageResource;
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
  public Dimension getPreferredSize() {
    //return new Dimension(getWidth(),getWidth());
    return new Dimension(height, height);
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
  Processes the given event
  @param event the event that was triggered
  **********************************************************************************************
  **/
  @Override
  public void processEvent(AWTEvent event) {
    super.processEvent(event); // handles any normal listeners
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }

  /**
  **********************************************************************************************
  Registers the events that this component generates
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
  
  **********************************************************************************************
  **/
  //public void setPreferredSize(Dimension size){
  //  super.setPreferredSize(new Dimension((int)size.getWidth(),(int)size.getWidth()));
  //  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  //public void setSize(Dimension size){
  //  super.setSize(new Dimension((int)size.getWidth(),(int)size.getWidth()));
  //  }

  /**
  **********************************************************************************************
  Overwritten to force use of AquanauticBorderLabelUI
  @param ui not used
  **********************************************************************************************
  **/
  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ButterflyWSThumbnailListLabelUI.createUI(this));
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
  Builds an XMLNode that describes this object
  @return an XML node with the details of this object
  **********************************************************************************************
  **/
  @Override
  public XMLNode toXML() {
    XMLNode node = super.toXML();
    return node;
  }

}