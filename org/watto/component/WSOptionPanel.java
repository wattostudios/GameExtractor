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

import org.watto.xml.XMLNode;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/
public abstract class WSOptionPanel extends WSPanel implements Comparable<WSComparable> {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  String setting = "";

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSOptionPanel() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSOptionPanel(XMLNode node) {
    super(node);
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
  public int compareTo(WSComparable otherResource) {
    return getComparison().compareTo(((WSOptionPanel) otherResource).getComparison());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract String getComparison();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getSetting() {
    return setting;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSetting(String setting) {
    this.setting = setting;
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

    String tag = node.getAttribute("setting");
    if (tag != null) {
      setSetting(tag);
    }
  }

}