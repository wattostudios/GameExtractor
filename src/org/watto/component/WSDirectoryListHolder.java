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
import java.io.File;
import java.io.FileFilter;
import javax.swing.border.EmptyBorder;
import org.watto.Settings;
import org.watto.xml.XMLNode;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/

public class WSDirectoryListHolder extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  DirectoryListPanel currentPanel;

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSDirectoryListHolder(XMLNode node) {
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
  public void checkFiles() {
    if (currentPanel != null) {
      currentPanel.checkFilesExist();
    }
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
  public File[] getAllSelectedFiles() {
    if (currentPanel != null) {
      return currentPanel.getAllSelectedFiles();
    }
    return new File[0];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File getCurrentDirectory() {
    if (currentPanel != null) {
      return currentPanel.getCurrentDirectory();
    }
    return new File("");
  }

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
  public File getSelectedFile() {
    if (currentPanel != null) {
      return currentPanel.getSelectedFile();
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadPanel(DirectoryListPanel panel) {
    if (panel == null) {
      return;
    }

    if (this.currentPanel == panel) {
      // so we don't keep reloading the same panel over and over
      return;
    }

    //panel.reload();

    this.currentPanel = panel;

    removeAll();
    add(panel, BorderLayout.CENTER);
    revalidate();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadPanel(String code) {
    WSPluginGroup group = WSPluginManager.getGroup("DirectoryList");
    //DirectoryListPanel currentPanel = (DirectoryListPanel) WSPluginManager.getGroup("DirectoryList").getPlugin(code);
    DirectoryListPanel currentPanel = (DirectoryListPanel) group.getPlugin(code);
    loadPanel(currentPanel);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload() {
    if (currentPanel != null) {
      currentPanel.reload();
    }

    /*
    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null && overlayPanel.isVisible()) {
      // if we're coming in here because we alt-tabbed to this window and it's triggering a reload of the
      // directory list, we don't want to repaint this if there is a popup showing
    }
    else {
    */
    revalidate();
    repaint();
    /*
    }
    */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    if (currentPanel != null) {
      currentPanel.requestFocus();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void scrollToSelected() {
    if (currentPanel != null) {
      currentPanel.scrollToSelected();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setMatchFilter(FileFilter filter) {
    if (currentPanel != null) {
      currentPanel.setMatchFilter(filter);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setMultipleSelection(boolean multi) {
    if (currentPanel != null) {
      currentPanel.setMultipleSelection(multi);
    }
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
    setLayout(new BorderLayout(0, 0));
    setBorder(new EmptyBorder(0, 0, 0, 0));
    loadPanel(Settings.get("DirectoryListView"));
  }

}