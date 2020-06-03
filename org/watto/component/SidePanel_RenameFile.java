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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TypecastSingletonManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.event.WSEnterableInterface;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.RenamerPlugin;
import org.watto.task.Task;
import org.watto.task.Task_RenameFiles;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_RenameFile extends WSPanelPlugin implements WSSelectableInterface,
    WSEnterableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  WSPanel renameControls;
  WSPanel invalidControls;
  WSPanel basicVersionControls;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_RenameFile() {
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
  public SidePanel_RenameFile(XMLNode node) {
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
  public void changeControls(WSPanel panel) {
    removeAll();
    add(panel, BorderLayout.CENTER);
    revalidate();
    repaint();
  }

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

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadRenamerPlugins() {

    WSComboBox renamerPluginList = (WSComboBox) ComponentRepository.get("SidePanel_RenameFile_RenamerPlugins");

    // need to load the list
    WSPlugin[] plugins = WSPluginManager.getGroup("Renamer").getPlugins();

    if (Settings.getBoolean("SortPluginLists")) {
      java.util.Arrays.sort(plugins);
    }

    renamerPluginList.setModel(new DefaultComboBoxModel(plugins));

    int selected = Settings.getInt("SelectedRenamer");
    if (selected >= 0 && selected < plugins.length) {
      renamerPluginList.setSelectedIndex(selected);
    }

  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSClickableListener when a click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (c instanceof WSButton) {
      String code = ((WSButton) c).getCode();

      if (code.equals("SidePanel_RenameFile_RenameAllButton")) {
        renameAllFiles();
        return true;
      }
      if (code.equals("SidePanel_RenameFile_RenameSelectedButton")) {
        renameSelectedFiles();
        return true;
      }
    }

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
   * The event that is triggered from a WSSelectableListener when an item is deselected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    return false;
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
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSEnterableListener when a key is pressed
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onEnter(JComponent c, java.awt.event.KeyEvent e) {
    /*
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code.equals("SidePanel_RenameFile_ReplaceValue")) {
        renameFiles();
        return true;
      }
    }
    */

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
    if (!GameExtractor.isFullVersion()) {
      changeControls(basicVersionControls);
    }
    else if (!Archive.getReadPlugin().canRename()) {
      changeControls(invalidControls);
    }
    else {
      changeControls(renameControls);
      loadRenamerPlugins();
    }
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
    if (c instanceof WSComboBox) {
      String code = ((WSComboBox) c).getCode();

      if (code.equals("SidePanel_RenameFile_RenamerPlugins")) {
        Settings.set("SelectedRenamer", ((WSComboBox) c).getSelectedIndex());

        RenamerPlugin plugin = (RenamerPlugin) ((WSComboBox) c).getSelectedItem();

        WSTextField searchField = (WSTextField) ComponentRepository.get("SidePanel_RenameFile_SearchValue");
        if (searchField != null) {
          if (plugin != null && plugin.showSearchField()) {
            searchField.setVisible(true);
          }
          else {
            searchField.setVisible(false);
            searchField.getParent().revalidate(); // force the revalidate/repaint, so the field disappears immediately
          }
        }

        return true;
      }
    }
    return false;
  }

  /**
   **********************************************************************************************
   * Registers the events that this component generates
   **********************************************************************************************
   **/
  @Override
  public void registerEvents() {
    super.registerEvents();
  }

  ///////////////
  //
  // Default Implementations
  //
  ///////////////

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void renameAllFiles() {
    Resource[] allFiles = Archive.getResources();
    if (allFiles == null || allFiles.length <= 0) {
      return;
    }

    renameFiles(allFiles);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void renameFiles(Resource[] selectedFiles) {
    WSComboBox renamerPluginList = (WSComboBox) ComponentRepository.get("SidePanel_RenameFile_RenamerPlugins");
    RenamerPlugin plugin = (RenamerPlugin) renamerPluginList.getSelectedItem();

    //if (! ArchiveModificationMonitor.setModified(true)){
    //  return;
    //  }

    WSTextField searchInput = (WSTextField) ComponentRepository.get("SidePanel_RenameFile_SearchValue");
    String searchValue = "";

    if (searchInput.isVisible()) {
      searchValue = searchInput.getText();

      if (searchValue == null || searchValue.length() <= 0) {
        WSPopup.showErrorInNewThread("RenameFiles_NoRenameValue", true);
        return;
      }
    }

    WSTextField replaceInput = (WSTextField) ComponentRepository.get("SidePanel_RenameFile_ReplaceValue");
    String replaceValue = replaceInput.getText();

    if (replaceValue == null || replaceValue.length() < 0) {
      // want to allow ="" so that they can, for example, remove an extension or a directory
      WSPopup.showErrorInNewThread("RenameFiles_NoRenameValue", true);
      return;
    }

    Task_RenameFiles task = new Task_RenameFiles(selectedFiles, plugin, searchValue, replaceValue);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void renameSelectedFiles() {
    WSFileListPanelHolder fileListPanelHolder = (WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder");
    Resource[] selectedFiles = fileListPanelHolder.getAllSelectedFiles();
    if (selectedFiles == null || selectedFiles.length <= 0) {
      WSPopup.showErrorInNewThread("RenameFiles_NoFilesSelected", true);
      return;
    }

    renameFiles(selectedFiles);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    ((WSTextField) ComponentRepository.get("SidePanel_RenameFile_ReplaceValue")).requestFocus();
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
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_RenameFile.xml"));

    // Build the components from the XMLNode tree
    renameControls = (WSPanel) WSHelper.toComponent(srcNode);
    add(renameControls, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) renameControls).getCode());
    ComponentRepository.add(this);

    // load the invalidControls
    invalidControls = (WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel code=\"SidePanel_DirectoryList_ReadPanel_Main\" repository=\"false\" showBorder=\"true\" layout=\"BorderLayout\"><WSLabel code=\"SidePanel_DirectoryList_InvalidControls\" wrap=\"true\" vertical-alignment=\"true\" position=\"CENTER\" /></WSPanel>"));
    basicVersionControls = (WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel code=\"SidePanel_DirectoryList_ReadPanel_Main\" repository=\"false\" showBorder=\"true\" layout=\"BorderLayout\"><WSLabel code=\"SidePanel_RenameFile_FullVersionOnly\" wrap=\"true\" vertical-alignment=\"true\" position=\"CENTER\" /></WSPanel>"));
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