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
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.Resource;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.plugin.PluginListBuilder;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FilenameChecker;
import org.watto.task.Task;
import org.watto.task.Task_PreviewFile;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/

public class SidePanel_ImageInvestigator extends WSPanelPlugin implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** filename to use when saving the preview **/
  String saveFilename = "ImageInvestigator";

  /** so we don't reload the panel if it's already loaded and showing **/
  boolean alreadyLoaded = false;

  PreviewPanel_ImageInvestigator previewPanel = null;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_ImageInvestigator() {
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
  public SidePanel_ImageInvestigator(XMLNode node) {
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

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadPreview(WSPanel panel) {
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_ImageInvestigator_PreviewPanelHolder");
    previewHolder.loadPanel(panel);
    previewHolder.revalidate();
    previewHolder.repaint();

    loadPreviewWriters(panel);

    revalidate();
    repaint();

    previewHolder.requestFocus(); // 3.02 >> This forces a revalidate of the panel, to FORCE a repaint, which is still required sometimes!

    paintImmediately(0, 0, getWidth(), getHeight()); // 3.01 >> required, otherwise it STILL doesn't repaint some times, until you move the mouse
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadPreviewWriters(WSPanel panel) {

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_ImageInvestigator_WritePreviewPlugins");

    if (!(panel instanceof PreviewPanel)) {
      pluginList.setModel(new DefaultComboBoxModel());
      return;
    }

    PreviewPanel previewPanel = (PreviewPanel) panel;

    WSPanel savePreviewPanel = (WSPanel) ComponentRepository.get("SidePanel_ImageInvestigator_WritePreviewHolder");

    //ViewerPlugin[] plugins = PluginListBuilder.getWriteViewers(previewPanel);
    ViewerPlugin[] plugins = PluginListBuilder.getWriteViewers(new PreviewPanel_Image()); // want to force loading all Image writers for this as well
    if (plugins == null || plugins.length <= 0) {
      // No write plugins - hide the "Save preview as..." options
      if (savePreviewPanel != null) {
        savePreviewPanel.setVisible(false);
      }
      return;
    }
    else {
      // show the options, in case they were hidden from one of the earlier previews
      if (savePreviewPanel != null) {
        savePreviewPanel.setVisible(true);
      }
    }

    Arrays.sort(plugins);

    pluginList.setModel(new DefaultComboBoxModel(plugins));

    // Select the last chosen plugin for this preview type
    String previewPlugin = Settings.getString("ExportPreviewType_" + previewPanel.getClass());
    if (previewPlugin != null && !previewPlugin.equals("")) {
      for (int i = 0; i < plugins.length; i++) {
        if (plugins[i].getName().equals(previewPlugin)) {
          // found the last selected plugin of this type
          pluginList.setSelectedIndex(i);
          i = plugins.length;
        }
      }
    }

    return;
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
      if (((WSButton) c).getCode().equals("SidePanel_ImageInvestigator_WritePreviewButton")) {
        savePreview();
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
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_ImageInvestigator_PreviewPanelHolder");
    WSPanel panel = previewHolder.getCurrentPanel();
    if (panel instanceof PreviewPanel) {
      ((PreviewPanel) panel).onCloseRequest();
    }
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
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code.equals("FileList")) {
        previewFile();
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
    previewFile();
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
    rememberSelectedExportPlugin();
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadBlankPreview() {
    loadPreview((WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel><WSLabel code=\"BlankImageInvestigatorPreview\" /></WSPanel>")));
    alreadyLoaded = false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void previewFile() {

    //System.out.println("previewFile");

    // Determine the file to preview
    Resource selected = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getSelectedFile();
    if (selected == null) {
      loadBlankPreview();
      return;
    }

    saveFilename = selected.getName();

    // extract the file
    File path = selected.getExportedPath();
    if (path == null || !path.exists()) {
      File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
      Task_PreviewFile task = new Task_PreviewFile(directory, selected);
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();
      return; // because the task calls previewFile(path) after the file is exported
      //path = selected.getExportedPath();
    }
    /*
     * // doesn't work - needs to build the path correctly (this has duplicates) and needs to
     * build directories as appropriate else { if (! path.getName().equals(selected.getName())){
     * File newPath = new File(path.getParentFile().getAbsolutePath() + File.separator +
     * saveFilename); path.renameTo(newPath); System.out.println(newPath.getAbsolutePath()); path
     * = newPath; } }
     */

    // now preview it
    SingletonManager.set("CurrentResource", selected); // so it can be detected by ViewerPlugins for Thumbnail Generation
    previewFile(path);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void previewFile(File path) {
    Settings.set("AutoChangedToHexPreview", "false");

    if (alreadyLoaded) {
      previewPanel.loadFile(path);
    }
    else {
      previewPanel = new PreviewPanel_ImageInvestigator(path);
      loadPreview(previewPanel);
      alreadyLoaded = true;
    }
    /*
    
    //System.out.println("previewFile(File Path)");
    
    if (path == null || path.length() == 0) {
      // could not be exported
      loadBlankPreview();
      showHex();
      return;
    }
    
    // preview the first selected file
    RatedPlugin[] plugins = PluginFinder.findPlugins(path, ViewerPlugin.class);
    
    boolean previewOpened = false;
    
    if (plugins != null && plugins.length > 0) {
      Arrays.sort(plugins);
    
      // try to open the preview using each plugin and previewFile(File,Plugin)
      for (int i = 0; i < plugins.length; i++) {
        previewOpened = previewFile(path, (ViewerPlugin) plugins[i].getPlugin());
    
        if (previewOpened) {
          i = plugins.length;
        }
    
      }
    }
    
    // if we haven't found a ViewerPlugin, see if the ArchivePlugin suggests an appropriate one...
    if (!previewOpened) {
      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin != null && !(readPlugin instanceof AllFilesPlugin)) {
        Resource selectedResource = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getSelectedFile();
        if (selectedResource != null) {
          ViewerPlugin hintViewerPlugin = readPlugin.previewHint(selectedResource);
          // try to open the preview using the hinted plugin
          previewOpened = previewFile(path, hintViewerPlugin);
    
        }
    
      }
    }
    
    // if all else fails, display the blank preview or Hex Viewer
    if (!previewOpened) {
    
      if (Settings.getBoolean("HIDDEN_ShowImageInvestigatorIfPreviewFailed")) {
        loadPreview(new PreviewPanel_ImageInvestigator(path));
      }
      else {
        loadBlankPreview();
        showHex();
      }
      return;
    }
    */

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean previewFile(File path, ViewerPlugin plugin) {
    try {
      if (path == null || plugin == null) {
        return false;
      }

      PreviewPanel panel = plugin.read(path);

      if (panel != null) {
        loadPreview(panel);
        return true;
      }

      return false;
    }
    catch (Throwable t) {
      logError(t);
      return false;
    }
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void rememberSelectedExportPlugin() {
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_ImageInvestigator_PreviewPanelHolder");
    WSPanel panel = previewHolder.getCurrentPanel();
    if (!(panel instanceof PreviewPanel)) {
      return;
    }

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_ImageInvestigator_WritePreviewPlugins");

    Settings.set("ExportPreviewType_" + ((PreviewPanel) panel).getClass(), pluginList.getSelectedItem().toString());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    ((WSComboBox) ComponentRepository.get("SidePanel_ImageInvestigator_WritePreviewPlugins")).requestFocus();
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
  public void savePreview() {
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_ImageInvestigator_PreviewPanelHolder");
    WSPanel panel = previewHolder.getCurrentPanel();

    if (!(panel instanceof PreviewPanel)) {
      //System.out.println(panel.getCode());
      return;
    }

    PreviewPanel previewPanel = (PreviewPanel) panel;

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_ImageInvestigator_WritePreviewPlugins");
    ViewerPlugin plugin = (ViewerPlugin) pluginList.getSelectedItem();

    File destination = new File(Settings.get("SavedPreviewDirectory") + File.separator + saveFilename + "." + plugin.getExtension(0));
    destination = FilenameChecker.correctFilename(destination); // Remove bad characters from the save filename
    //System.out.println(destination.getAbsolutePath());

    // Making a valid filename, and building the directories
    // DON'T THINK THESE LINE IS NEEDED ANY MORE, because new FileBuffer() does it automatically
    //destination = FileBuffer.checkFilename(destination);
    //FileBuffer.makeDirectory(new File(destination.getAbsolutePath()));

    plugin.write(previewPanel, destination);

    rememberSelectedExportPlugin();

    WSPopup.showMessageInNewThread("ImageInvestigator_PreviewSaved", true);
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
  
  **********************************************************************************************
  **/
  public void showHex() {
    //System.out.println("showHex");
    if (Settings.getBoolean("ShowHexIfPreviewFailed")) {
      Settings.set("AutoChangedToHexPreview", "false");
      //Settings.set("LoadingHex","true");
      ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_HexEditor", false);
      //Settings.set("LoadingHex","false");
      Settings.set("AutoChangedToHexPreview", "true");
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

    setLayout(new BorderLayout());

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_ImageInvestigator.xml"));

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