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
import java.awt.event.ComponentListener;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.event.WSResizableInterface;
import org.watto.event.listener.WSResizableListener;
import org.watto.ge.GameExtractor;
import org.watto.task.Task;
import org.watto.task.Task_ReloadFileListPanel;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
A ExtendedTemplate
**********************************************************************************************
**/

public class WSFileListPanelHolder extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSPanel currentPanel = new WSPanel(XMLReader.read("<WSPanel />"));

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSFileListPanelHolder(XMLNode node) {
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
  public Resource[] getAllSelectedFiles() {
    if (currentPanel != null && currentPanel instanceof FileListPanel) {
      return ((FileListPanel) currentPanel).getSelected();
    }
    return new Resource[0];
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
  public int getFirstSelectedRow() {
    if (currentPanel instanceof FileListPanel) {
      return ((FileListPanel) currentPanel).getFirstSelectedRow();
    }
    return -1;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getNumSelected() {
    if (currentPanel instanceof FileListPanel) {
      return ((FileListPanel) currentPanel).getNumSelected();
    }
    return 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource(int row) {
    if (currentPanel instanceof FileListPanel) {
      return ((FileListPanel) currentPanel).getResource(row);
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] getSelected() {
    if (currentPanel instanceof FileListPanel) {
      return ((FileListPanel) currentPanel).getSelected();
    }
    return new Resource[0];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getSelectedFile() {
    if (currentPanel != null && currentPanel instanceof FileListPanel) {
      Resource[] resources = ((FileListPanel) currentPanel).getSelected();
      if (resources != null && resources.length >= 1) {
        return resources[0];
      }
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadPanel(String code) {
    //System.out.println("WSFileListPanelHolder-->LOADPANEL(STRING)");
    WSPanel newPanel = (WSPanel) WSPluginManager.getGroup("FileList").getPlugin(code);
    if (newPanel == null) {
      return;
    }
    else {
      if (currentPanel != null && currentPanel instanceof WSPanelPlugin) {
        ((WSPanelPlugin) currentPanel).onCloseRequest();
      }
      currentPanel = newPanel;
      if (currentPanel != null && currentPanel instanceof WSPanelPlugin) {
        ((WSPanelPlugin) currentPanel).onOpenRequest();
      }
      Settings.set("CurrentFileList", currentPanel.getCode());
    }

    loadPanel(currentPanel);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadPanel(WSPanel panel) {
    //System.out.println("WSFileListPanelHolder-->LOADPANEL(WSPANEL)");
    if (panel == null) {
      return;
    }

    boolean listenerRemoved = false;

    if (panel instanceof FileListPanel) {

      //Task_ReloadFileListPanel task = new Task_ReloadFileListPanel((FileListPanel) panel);
      //task.setDirection(Task.DIRECTION_REDO);
      //new Thread(task).start();

      //((FileListPanel) panel).reload();

      // Remove the ResizeListener so it doesn't trigger onResize() before loadPanel()
      ComponentListener[] listeners = panel.getComponentListeners();
      for (int i = 0; i < listeners.length; i++) {
        if (listeners[i] instanceof WSResizableListener) {
          panel.removeComponentListener(listeners[i]);
          listenerRemoved = true;
        }
      }
    }

    removeAll();
    add(panel, BorderLayout.CENTER);

    revalidate();
    repaint();

    if (panel instanceof FileListPanel) {
      if (!GameExtractor.isFullVersion() && Settings.getBoolean("ShowWelcomeWizard")) { // isFullVersion() is IMPORTANT to not break the File List loading
        // if we're showing the welcome wizard, we don't want to overwrite it with this reload
        return;
      }
      Task_ReloadFileListPanel task = new Task_ReloadFileListPanel((FileListPanel) panel);
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();

      if (listenerRemoved && panel instanceof WSResizableInterface) {
        panel.addComponentListener(new WSResizableListener((WSResizableInterface) panel));
      }

      //((FileListPanel) panel).reload();

    }

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
    //System.out.println("WSFileListPanelHolder-->ONOPENREQUEST");
    if (currentPanel != null && currentPanel instanceof WSPanelPlugin) {
      ((WSPanelPlugin) currentPanel).onOpenRequest();
    }
  }

  /**
  **********************************************************************************************
  Rebuilds the panels from their XML
  **********************************************************************************************
  **/
  public void rebuild() {
    //System.out.println("WSFileListPanelHolder-->REBUILD");
    WSPlugin[] plugins = WSPluginManager.getGroup("FileList").getPlugins();
    for (int i = 0; i < plugins.length; i++) {
      ((FileListPanel) plugins[i]).constructInterface();
    }
    reload();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload() {
    //System.out.println("WSFileListPanelHolder-->RELOAD");

    /*
    try {
      throw new Exception("WSFileListPanelHolder: Trigger reload");
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
    */

    if (Settings.getBoolean("ShowWelcomeWizard")) {
      // if we're showing the welcome wizard, we don't want to overwrite it with this reload
      return;
    }

    if (currentPanel instanceof FileListPanel) {

      //Task_ReloadFileListPanel task = new Task_ReloadFileListPanel((FileListPanel) currentPanel);
      //task.setDirection(Task.DIRECTION_REDO);
      //new Thread(task).start();

      ((FileListPanel) currentPanel).reload();
    }
    revalidate();
    repaint();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void selectAll() {
    if (currentPanel instanceof FileListPanel) {
      ((FileListPanel) currentPanel).selectAll();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void selectInverse() {
    if (currentPanel instanceof FileListPanel) {
      ((FileListPanel) currentPanel).selectInverse();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void selectNone() {
    if (currentPanel instanceof FileListPanel) {
      ((FileListPanel) currentPanel).selectNone();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void selectResource(int row) {
    if (currentPanel instanceof FileListPanel) {
      ((FileListPanel) currentPanel).selectResource(row);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopInlineEditing() {
    if (currentPanel instanceof FileListPanel) {
      ((FileListPanel) currentPanel).stopInlineEditing();
    }
  }

  /**
  **********************************************************************************************
  Builds an XMLNode that describes this object
  @return an XML node with the details of this object
  **********************************************************************************************
  **/
  @Override
  public XMLNode toXML() {
    return WSHelper.toXML(this);
  }

}