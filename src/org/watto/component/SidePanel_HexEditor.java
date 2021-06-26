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
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.task.Task_ExportFiles;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_HexEditor extends WSPanelPlugin {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_HexEditor() {
    super(new XMLNode());
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   * @param caller the object that contains this component, created this component, or more
   *        formally, the object that receives events from this component.
   **********************************************************************************************
   **/
  public SidePanel_HexEditor(XMLNode node) {
    super(node);
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
   **********************************************************************************************
   * Gets the plugin description
   **********************************************************************************************
   **/
  @Override
  public String getDescription() {
    String description = toString() + "\n\n" + Language.get("Description_SidePanel");

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;
  }

  /**
   **********************************************************************************************
   * Gets the plugin name
   **********************************************************************************************
   **/
  @Override
  public String getText() {
    return super.getText();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void loadFile() {

    //System.out.println("loadFile");

    if (Settings.getBoolean("AutoChangedToHexPreview")) {
      // The hex editor was opened automatically by SidePanel_Preview when a preview failed,
      // therefore switch back to SidePanel_Preview when trying to preview the next file
      Settings.set("AutoChangedToHexPreview", "false");
      Settings.set("LoadingPreviewFromHex", "true");
      ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_Preview", false);
      Settings.set("LoadingPreviewFromHex", "false");
      return;
    }

    // Determine the file to preview
    Resource selected = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getSelectedFile();
    if (selected == null) {
      return;
    }

    // extract the file
    File path = selected.getExportedPath();
    if (path == null || !path.exists()) {
      File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
      Task_ExportFiles task = new Task_ExportFiles(directory, selected);
      task.setShowPopups(false);
      task.redo();
      path = selected.getExportedPath();
    }

    // now preview it
    WSHexEditor hexEditor = (WSHexEditor) ComponentRepository.get("SidePanel_HexEditor_Editor");
    hexEditor.loadData(path);

    //System.out.println("loadFile FINISHED");

  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
   **********************************************************************************************
   * The event that is triggered from a WSClickableListener when a click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be closed. This method
   * does nothing by default, but can be overwritten to do anything else needed before the panel
   * is closed, such as garbage collecting and closing pointers to temporary objects.
   **********************************************************************************************
   **/
  @Override
  public void onCloseRequest() {
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSDoubleClickableListener when a double click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDoubleClick(JComponent c, MouseEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code.equals("FileList")) {
        loadFile();
        return true;
      }

    }
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when the mouse moves over an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    return super.onHover(c, e);
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSHoverableListener when the mouse moves out of an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    return super.onHoverOut(c, e);
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSKeyableListener when a key press occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code.equals("FileList") && e.getKeyCode() == KeyEvent.VK_ENTER) {
        loadFile();
        return true;
      }

    }
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be opened. By default,
   * it just calls checkLoaded(), but can be overwritten to do anything else needed before the
   * panel is displayed, such as resetting or refreshing values.
   **********************************************************************************************
   **/
  @Override
  public void onOpenRequest() {
    loadFile();
  }

  ///////////////
  //
  // Default Implementations
  //
  ///////////////

  /**
   **********************************************************************************************
   * Registers the events that this component generates
   **********************************************************************************************
   **/
  @Override
  public void registerEvents() {
    super.registerEvents();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    ((WSHexEditor) ComponentRepository.get("SidePanel_HexEditor_Editor")).requestFocus();
  }

  /**
   **********************************************************************************************
   * Sets the description of the plugin
   * @param description the description
   **********************************************************************************************
   **/
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
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

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_HexEditor.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);

  }

  /**
   **********************************************************************************************
   * Builds an XMLNode that describes this object
   * @return an XML node with the details of this object
   **********************************************************************************************
   **/
  @Override
  public XMLNode toXML() {
    return super.toXML();
  }

}