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
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
A ExtendedTemplate
**********************************************************************************************
**/
public class WSOptionGroupHolder extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  WSPanel currentPanel = new WSPanel(XMLReader.read("<WSPanel />"));

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSOptionGroupHolder(XMLNode node) {
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
  public WSPanel getCurrentPanel() {
    return currentPanel;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void loadPanel(WSPanel panel) {
    if (panel == null) {
      return;
    }

    currentPanel = panel;

    //if (panel instanceof PreviewPanel){
    //  ((PreviewPanel)panel).reload();
    //  }

    removeAll();
    add(panel, BorderLayout.CENTER);

    reload();

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void reload() {
    //if (currentPanel instanceof PreviewPanel){
    //  ((PreviewPanel)currentPanel).reload();
    //  }
    revalidate();
    repaint();
  }

}