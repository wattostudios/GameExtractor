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
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.PluginListBuilder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FilenameChecker;
import org.watto.io.FilenameSplitter;
import org.watto.task.Task;
import org.watto.task.Task_PreviewFile;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/

public class SidePanel_Preview extends WSPanelPlugin implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** filename to use when saving the preview **/
  String saveFilename = "Preview";

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_Preview() {
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
  public SidePanel_Preview(XMLNode node) {
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
  public void loadBasicVersionPreview() {

    WSPanel basicPanel = new WSPanel(XMLReader.read("<WSPanel code=\"SidePanel_Preview_Basic\" repository=\"false\" layout=\"BorderLayout\" vertical-gap=\"8\" border-width=\"8\" position=\"CENTER\"></WSPanel>"));

    WSPanel thumbnailPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"GridLayout\" rows=\"3\" columns=\"1\" vertical-gap=\"8\" />"));

    WSPanel screen1Panel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" showBorder=\"true\" />"));
    screen1Panel.add(new JLabel(new ImageIcon("images" + File.separatorChar + "FullVersionScreenshots" + File.separatorChar + "full01.png")));
    WSPanel center1Panel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" showBorder=\"false\" />"));
    center1Panel.add(screen1Panel);

    WSPanel centerPanel = new WSPanel(XMLReader.read("<WSPanel vertical-gap=\"8\" vertical-alignment=\"center\" position=\"NORTH\" opaque=\"false\" /></WSPanel>"));

    WSPanel textPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\"><WSLabel code=\"BasicVersionPreview\" vertical-alignment=\"center\" position=\"NORTH\" border-width=\"8\" /></WSPanel>"));
    WSPanel bulletsTextPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"BorderLayout\" opaque=\"false\" ><WSPanel layout=\"GridLayout\" rows=\"4\" columns=\"1\" opaque=\"false\" vertical-gap=\"4\" ><WSLabel code=\"BasicVersionPreview_FullVersionFeature1\" horizontal-alignment=\"left\" /><WSLabel code=\"BasicVersionPreview_FullVersionFeature2\" horizontal-alignment=\"left\" /><WSLabel code=\"BasicVersionPreview_FullVersionFeature3\" horizontal-alignment=\"left\" /><WSLabel code=\"BasicVersionPreview_FullVersionFeature4\" horizontal-alignment=\"left\" /></WSPanel></WSPanel>"));

    WSPanel bulletsPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"GridLayout\" rows=\"4\" columns=\"1\" opaque=\"false\" vertical-gap=\"4\" ></WSPanel>"));
    bulletsPanel.add(new JLabel(new ImageIcon("images" + File.separatorChar + "General" + File.separatorChar + "bullet.png")));
    bulletsPanel.add(new JLabel(new ImageIcon("images" + File.separatorChar + "General" + File.separatorChar + "bullet.png")));
    bulletsPanel.add(new JLabel(new ImageIcon("images" + File.separatorChar + "General" + File.separatorChar + "bullet.png")));
    bulletsPanel.add(new JLabel(new ImageIcon("images" + File.separatorChar + "General" + File.separatorChar + "bullet.png")));
    bulletsTextPanel.add(bulletsPanel, BorderLayout.WEST);

    WSPanel bulletsCenterPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" opaque=\"false\" ></WSPanel>"));
    bulletsCenterPanel.add(bulletsTextPanel);
    textPanel.add(bulletsCenterPanel, BorderLayout.CENTER);

    WSPanel linkPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" showBorder=\"true\"  border-width=\"8\" position=\"SOUTH\"><WSLabel code=\"BasicVersionPreviewLink\" wrap=\"true\" vertical-alignment=\"center\" /></WSPanel>"));
    linkPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

    centerPanel.add(textPanel, BorderLayout.CENTER);
    centerPanel.add(linkPanel, BorderLayout.SOUTH);

    WSPanel screen2Panel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" showBorder=\"true\" />"));
    screen2Panel.add(new JLabel(new ImageIcon("images" + File.separatorChar + "FullVersionScreenshots" + File.separatorChar + "full02.png")));
    WSPanel center2Panel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" showBorder=\"false\" />"));
    center2Panel.add(screen2Panel);

    thumbnailPanel.add(center1Panel);
    thumbnailPanel.add(centerPanel);
    thumbnailPanel.add(center2Panel);

    WSPanel centerWrapperPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" />"));
    centerWrapperPanel.add(thumbnailPanel);

    basicPanel.add(centerWrapperPanel, BorderLayout.CENTER);

    loadPreview(basicPanel);

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void loadBlankPreview() {
    loadPreview((WSPanel) WSHelper.toComponent(XMLReader.read("<WSPanel><WSLabel code=\"BlankPreview\" /></WSPanel>")));
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
    if (panel instanceof PreviewPanel) {
      ((PreviewPanel) panel).onOpenRequest(); // eg checks for editing capabilities, shows the "Save" button if editing can occur, etc.
    }

    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_Preview_PreviewPanelHolder");
    previewHolder.loadPanel(panel);
    previewHolder.revalidate();
    previewHolder.repaint();

    loadPreviewWriters(panel);

    revalidate();
    repaint();

    previewHolder.requestFocus(); // 3.02 >> This forces a revalidate of the panel, to FORCE a repaint, which is still required sometimes!

    paintImmediately(0, 0, getWidth(), getHeight()); // 3.01 >> required, otherwise it STILL doesn't repaint some times, until you move the mouse

    // now, if the panel is an instance of PreviewPanel_Image, we need to reloadImage() so that it applies the zoom, which can only
    // be done once the panel has been added to the interface and sized appropriately
    if (panel instanceof PreviewPanel_Image) {
      PreviewPanel_Image imagePanel = (PreviewPanel_Image) panel;
      imagePanel.generateZoomImage();
      imagePanel.reloadImage();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadPreviewWriters(WSPanel panel) {

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_Preview_WritePreviewPlugins");

    if (!(panel instanceof PreviewPanel)) {
      pluginList.setModel(new DefaultComboBoxModel());
      return;
    }

    PreviewPanel previewPanel = (PreviewPanel) panel;

    WSPanel savePreviewPanel = (WSPanel) ComponentRepository.get("SidePanel_Preview_WritePreviewHolder");

    ViewerPlugin[] plugins = PluginListBuilder.getWriteViewers(previewPanel);
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
      if (((WSButton) c).getCode().equals("SidePanel_Preview_WritePreviewButton")) {
        savePreview();
        return true;
      }
    }
    else if (c instanceof WSLabel) {
      if (((WSLabel) c).getCode().equals("BasicVersionPreviewLink")) {
        try {
          if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI("http://www.watto.org/extract"));
          }
          return true;
        }
        catch (Throwable t) {
        }

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
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_Preview_PreviewPanelHolder");
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
        loadBasicVersionPreview();
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
    loadBasicVersionPreview();
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
    // v3.10 this now calls it in a thread, so that it paint properly (not stuck in the main java thread)
    /*
    SingletonManager.set("CurrentResource", selected); // so it can be detected by ViewerPlugins for Thumbnail Generation
    previewFile(path);
    */
    Task_PreviewFile task = new Task_PreviewFile(selected);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void previewFile(File path) {
    Settings.set("AutoChangedToHexPreview", "false");

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

      /*
      if (Settings.getBoolean("HIDDEN_ShowImageInvestigatorIfPreviewFailed")) {
        loadPreview(new PreviewPanel_ImageInvestigator(path));
      }
      else {
        loadBlankPreview();
        showHex();
      }
      */

      if (Settings.getString("PreviewType").equals("ImageInvestigator")) {
        showImageInvestigator();
      }
      else {
        loadBlankPreview();
        showHex();
      }

      return;
    }

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

      //System.out.println("Opening with Viewer " + plugin.getCode());
      PreviewPanel panel = plugin.read(path);

      if (panel != null) {
        // Remember the current Viewer plugin. This is used when Writing changes out to the filesystem in an "editor" PreviewPanel
        SingletonManager.set("CurrentViewer", plugin);

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
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_Preview_PreviewPanelHolder");
    WSPanel panel = previewHolder.getCurrentPanel();
    if (!(panel instanceof PreviewPanel)) {
      return;
    }

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_Preview_WritePreviewPlugins");

    Settings.set("ExportPreviewType_" + ((PreviewPanel) panel).getClass(), pluginList.getSelectedItem().toString());
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    ((WSComboBox) ComponentRepository.get("SidePanel_Preview_WritePreviewPlugins")).requestFocus();
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
    WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_Preview_PreviewPanelHolder");
    WSPanel panel = previewHolder.getCurrentPanel();

    if (!(panel instanceof PreviewPanel)) {
      //System.out.println(panel.getCode());
      return;
    }

    PreviewPanel previewPanel = (PreviewPanel) panel;

    WSComboBox pluginList = (WSComboBox) ComponentRepository.get("SidePanel_Preview_WritePreviewPlugins");
    ViewerPlugin plugin = (ViewerPlugin) pluginList.getSelectedItem();

    File destination = new File(Settings.get("SavedPreviewDirectory") + File.separator + saveFilename + "." + plugin.getExtension(0));
    destination = FilenameChecker.correctFilename(destination); // Remove bad characters from the save filename
    //System.out.println(destination.getAbsolutePath());

    if (destination.exists() && destination.isFile()) {
      // to cater for archives with multiple files of the same name, append a number to the end of the name
      String path = FilenameSplitter.getDirectory(destination) + File.separator + FilenameSplitter.getFilename(destination);
      String extension = "." + FilenameSplitter.getExtension(destination);

      for (int i = 1; i < 1000; i++) {
        File testDestination = new File(path + i + extension);
        if (!testDestination.exists()) {
          destination = testDestination;
          break;
        }
      }
    }

    // Making a valid filename, and building the directories
    // DON'T THINK THESE LINE IS NEEDED ANY MORE, because new FileBuffer() does it automatically
    //destination = FileBuffer.checkFilename(destination);
    //FileBuffer.makeDirectory(new File(destination.getAbsolutePath()));

    // If there are multiple frames in the image, and it's a manual transition (not an animation), we want to save each frame [3.13]
    boolean previewsWritten = false;
    if (previewPanel instanceof PreviewPanel_Image) {
      PreviewPanel_Image imagePanel = (PreviewPanel_Image) previewPanel;
      ImageResource imageResource = imagePanel.getImageResource();
      if (imageResource.getNextFrame() != null) {
        // many frames, not an animation, so save each of them

        String baseFilePath = destination.getAbsolutePath();
        String extension = "";
        int dotPos = baseFilePath.lastIndexOf('.');
        if (dotPos > 0) {
          extension = baseFilePath.substring(dotPos);
          baseFilePath = baseFilePath.substring(0, dotPos);
        }

        baseFilePath += "_ge_frame_";

        // If we've already moved to show a different frame (ie we're not starting on frame #1), move back to frame 0 for export
        int currentFrame = Settings.getInt("PreviewPanel_Image_CurrentFrame");
        if (currentFrame < 0) {
          currentFrame = 0;
        }
        for (int i = 0; i < currentFrame; i++) {
          imageResource = imageResource.getPreviousFrame();
        }

        ImageResource firstResource = imageResource;

        // now if we're on the first frame, check that it's not an animation
        //if (imageResource.isManualFrameTransition()) {
        for (int i = 0; i < 1000; i++) { // max 1000 frames to export
          imagePanel.setImageResource(imageResource); // set the current frame

          // prepare the filename
          File frameDestination = new File(baseFilePath + i + extension);
          plugin.write(previewPanel, frameDestination); // save the frame

          imageResource = imageResource.getNextFrame();// prepare for the next frame

          if (imageResource == firstResource) {
            // back at the beginning
            imagePanel.setImageResource(firstResource); // set the current frame
            break;
          }
        }

        previewsWritten = true;
        //}
      }
    }
    // Otherwise just save the current frame
    if (!previewsWritten) {
      plugin.write(previewPanel, destination);
      previewsWritten = true;
    }

    rememberSelectedExportPlugin();

    WSPopup.showMessageInNewThread("Preview_PreviewSaved", true);
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

  **********************************************************************************************
  **/
  public void showImageInvestigator() {
    ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_ImageInvestigator", false);
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
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_Preview.xml"));

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