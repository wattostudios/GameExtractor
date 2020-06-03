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
import org.watto.Settings;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/

public class WSSidePanelHolder extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  WSPanel currentPanel = new WSPanel(XMLReader.read("<WSPanel />"));

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSSidePanelHolder(XMLNode node) {
    super(node);
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
  public WSPanel getCurrentPanel() {
    return currentPanel;
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
  public String getCurrentPanelCode() {
    return currentPanel.getCode();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void loadPanel(String code) {
    loadPanel(code, true);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void loadPanel(String code, boolean changeSetting) {
    loadPanel(code, changeSetting, false);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void loadPanel(String code, boolean changeSetting, boolean doubleClickPreview) {
    //if (!Settings.getBoolean("LoadingPreviewFromHex")){
    WSPanel newPanel = (WSPanelPlugin) WSPluginManager.getGroup("SidePanel").getPlugin(code);
    if (doubleClickPreview) {
      return;
    }
    currentPanel = newPanel;
    //if (code.equals("SidePanel_Preview") && Settings.getBoolean("AutoChangedToHexPreview")){
    //reload():
    // need this here because otherwise the hex preview is overwritten by the blank preview
    //  return;
    //  }

    //    System.out.println("F-->" + currentPanel.getCode());
    //System.out.println("Found " + code + " as " + currentPanel);
    if (currentPanel == null) {
      currentPanel = new WSPanel(XMLReader.read("<WSPanel />"));
    }
    else {
      if (changeSetting) {
        Settings.set("CurrentSidePanel", currentPanel.getCode());
      }
    }

    removeAll();
    add(currentPanel, BorderLayout.CENTER);
    //DirectoryListHolder holder = DirectoryListHolder.getInstance();
    //holder.loadPanel("List");
    //add(holder,BorderLayout.CENTER);

    /*
     * System.out.println("reloading"); // System.out.println("E-->" + code); try { throw new
     * Exception(); } catch (Throwable t){ t.printStackTrace(); }
     */
    reload();

    onOpenRequest();

    //}

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void onCloseRequest() {
    if (currentPanel != null && currentPanel instanceof WSPanelPlugin) {
      ((WSPanelPlugin) currentPanel).onCloseRequest();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void onOpenRequest() {
    if (currentPanel != null && currentPanel instanceof WSPanelPlugin) {
      ((WSPanelPlugin) currentPanel).onOpenRequest();
    }
  }

  /**
   **********************************************************************************************
   * Rebuilds the panels from their XML
   **********************************************************************************************
   **/
  public void rebuild() {
    WSPlugin[] plugins = WSPluginManager.getGroup("SidePanel").getPlugins();
    for (int i = 0; i < plugins.length; i++) {
      ((WSPanelPlugin) plugins[i]).toComponent(new XMLNode());
    }

    //reload();
    loadPanel(currentPanel.getCode());
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void reload() {
    revalidate();
    repaint();
    currentPanel.requestFocus();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void reloadPanel() {
    try {
      loadPanel(currentPanel.getCode());
    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * Builds an XMLNode that describes this object
   * @return an XML node with the details of this object
   **********************************************************************************************
   **/
  @Override
  public XMLNode toXML() {
    return WSHelper.toXML(this);
  }

}