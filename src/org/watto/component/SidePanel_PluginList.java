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
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.exporter.Exporter_Custom_ADF;
import org.watto.ge.plugin.exporter.Exporter_Custom_ARCH00_LTAR;
import org.watto.ge.plugin.exporter.Exporter_Custom_ARF_AR;
import org.watto.ge.plugin.exporter.Exporter_Custom_CA2;
import org.watto.ge.plugin.exporter.Exporter_Custom_CAR;
import org.watto.ge.plugin.exporter.Exporter_Custom_COBI;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAM_RZ;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_25;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_DBPF;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_DRPK;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_FAR;
import org.watto.ge.plugin.exporter.Exporter_Custom_DLL_MZ_BMP;
import org.watto.ge.plugin.exporter.Exporter_Custom_DS2RES;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB_Audio;
import org.watto.ge.plugin.exporter.Exporter_Custom_GSC_GSCFMT;
import org.watto.ge.plugin.exporter.Exporter_Custom_HPI_HAPI;
import org.watto.ge.plugin.exporter.Exporter_Custom_JDLZ;
import org.watto.ge.plugin.exporter.Exporter_Custom_JFL;
import org.watto.ge.plugin.exporter.Exporter_Custom_MHK_MHWK_WAV;
import org.watto.ge.plugin.exporter.Exporter_Custom_MHTML_Base64;
import org.watto.ge.plugin.exporter.Exporter_Custom_MHTML_QuotedPrintable;
import org.watto.ge.plugin.exporter.Exporter_Custom_PAK_20;
import org.watto.ge.plugin.exporter.Exporter_Custom_PAK_30;
import org.watto.ge.plugin.exporter.Exporter_Custom_PKG_3;
import org.watto.ge.plugin.exporter.Exporter_Custom_RGSSAD_RGSSAD;
import org.watto.ge.plugin.exporter.Exporter_Custom_RPKG_GKPR_Multi;
import org.watto.ge.plugin.exporter.Exporter_Custom_UE3_SoundNodeWave_451;
import org.watto.ge.plugin.exporter.Exporter_Custom_UE3_SoundNodeWave_648;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Palette_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Sound_141;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Texture_127;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Texture_141;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Texture_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_WAV_119;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_WAV_127;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_WAV_159;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_WAV_172;
import org.watto.ge.plugin.exporter.Exporter_Custom_VFS;
import org.watto.ge.plugin.exporter.Exporter_Custom_VPK;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAD_12;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio_Chunks;
import org.watto.ge.plugin.exporter.Exporter_Custom_ZSM_ZSNDXBOX;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.ge.plugin.exporter.Exporter_Deflate_XOR;
import org.watto.ge.plugin.exporter.Exporter_Explode;
import org.watto.ge.plugin.exporter.Exporter_GZip;
import org.watto.ge.plugin.exporter.Exporter_JDLZ;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.exporter.Exporter_LZ4X;
import org.watto.ge.plugin.exporter.Exporter_LZ4_Framed;
import org.watto.ge.plugin.exporter.Exporter_LZMA;
import org.watto.ge.plugin.exporter.Exporter_LZO;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.ge.plugin.exporter.Exporter_LZSS_Old;
import org.watto.ge.plugin.exporter.Exporter_MSZIP;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.ge.plugin.exporter.Exporter_RAR_RAR;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.ge.plugin.exporter.Exporter_SFL_Bits;
import org.watto.ge.plugin.exporter.Exporter_SFL_Block;
import org.watto.ge.plugin.exporter.Exporter_SFL_Nulls;
import org.watto.ge.plugin.exporter.Exporter_SFL_RLE;
import org.watto.ge.plugin.exporter.Exporter_SLLZ;
import org.watto.ge.plugin.exporter.Exporter_Snappy;
import org.watto.ge.plugin.exporter.Exporter_SplitChunkDefault;
import org.watto.ge.plugin.exporter.Exporter_SplitChunk_ZLib;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.ge.plugin.exporter.Exporter_XOR_RepeatingKey;
import org.watto.ge.plugin.exporter.Exporter_ZIP;
import org.watto.ge.plugin.exporter.Exporter_ZIP_Single;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLibX;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.ge.script.ScriptArchivePlugin_MexCom3;
import org.watto.ge.script.ScriptArchivePlugin_QuickBMS;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_PluginList extends WSPanelPlugin implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_PluginList() {
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
  public SidePanel_PluginList(XMLNode node) {
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
  public void changePluginType() {
    WSComboBox pluginTypes = (WSComboBox) ComponentRepository.get("SidePanel_PluginList_PluginTypes");
    changePluginType((String) pluginTypes.getSelectedItem());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void changePluginType(String type) {

    DefaultListModel model = new DefaultListModel();

    WSPlugin[] plugins = new WSPlugin[0];
    if (type.equals(Language.get("Plugin_Archive_Name"))) {
      plugins = WSPluginManager.getGroup("Archive").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_DirectoryList_Name"))) {
      plugins = WSPluginManager.getGroup("DirectoryList").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_Exporter_Name"))) {
      plugins = loadExporters();
    }
    else if (type.equals(Language.get("Plugin_FileList_Name"))) {
      plugins = WSPluginManager.getGroup("FileList").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_FileListExporter_Name"))) {
      plugins = WSPluginManager.getGroup("FileListExporter").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_OptionGroup_Name"))) {
      plugins = WSPluginManager.getGroup("Options").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_Renamer_Name"))) {
      WSPluginGroup group = WSPluginManager.getGroup("Renamer");
      if (group != null) {
        plugins = group.getPlugins();
      }
      else {
        plugins = new WSPlugin[0];
      }
    }
    else if (type.equals(Language.get("Plugin_Scanner_Name"))) {
      WSPluginGroup group = WSPluginManager.getGroup("Scanner");
      if (group != null) {
        plugins = group.getPlugins();
      }
      else {
        plugins = new WSPlugin[0];
      }
    }
    else if (type.equals(Language.get("Plugin_ScriptArchive_Name"))) {
      plugins = loadScriptArchivePlugins();
    }
    else if (type.equals(Language.get("Plugin_Script_Name"))) {
      //plugins = loadScripts();
      plugins = WSPluginManager.getGroup("Script").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_SidePanel_Name"))) {
      plugins = WSPluginManager.getGroup("SidePanel").getPlugins();
    }
    else if (type.equals(Language.get("Plugin_Viewer_Name"))) {

      if (GameExtractor.isFullVersion()) {
        // The Viewer plugins exist in the Basic Version for showing Thumbnails, but we need to pretend they don't exist
        // (they can't be used for real previews anyway, due to other functionality being removed)
        WSPluginGroup group = WSPluginManager.getGroup("Viewer");
        if (group != null) {
          plugins = group.getPlugins();
        }
        else {
          plugins = new WSPlugin[0];
        }
      }
    }

    if (Settings.getBoolean("SortPluginLists")) {
      java.util.Arrays.sort(plugins);
    }

    model.ensureCapacity(plugins.length);
    for (int i = 0; i < plugins.length; i++) {
      model.addElement(plugins[i]);
    }

    WSList pluginsList = (WSList) ComponentRepository.get("SidePanel_PluginList_Plugins");
    pluginsList.clearSelection();
    pluginsList.setModel(model);

    WSTextArea descriptionField = (WSTextArea) ComponentRepository.get("SidePanel_PluginList_PluginDescription");
    descriptionField.setText("");

    // For the Basic Version, show a message in the description box if there are no plugins of this type (eg Viewers, Scanners, etc).
    if (plugins.length == 0) {
      descriptionField.setText(Language.get("PluginsOnlyInFullVersion"));
    }

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

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

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
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadComboBox() {

    WSComboBox pluginTypes = (WSComboBox) ComponentRepository.get("SidePanel_PluginList_PluginTypes");

    pluginTypes.removeAllItems();

    String[] types = new String[] { "Archive", "DirectoryList", "Exporter", "FileList", "FileListExporter", "OptionGroup", "Renamer", "Scanner", "ScriptArchive", "Script", "SidePanel", "Viewer" };

    String[] items = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      items[i] = Language.get("Plugin_" + types[i] + "_Name");
    }

    if (Settings.getBoolean("SortPluginLists")) {
      java.util.Arrays.sort(items);
    }

    pluginTypes.setModel(new DefaultComboBoxModel(items));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSPlugin[] loadExporters() {
    WSPlugin[] plugins = new WSPlugin[] {
        new Exporter_Custom_ADF(),
        new Exporter_Custom_ARCH00_LTAR(),
        new Exporter_Custom_ARF_AR(),
        new Exporter_Custom_CA2(),
        new Exporter_Custom_CAR(),
        new Exporter_Custom_COBI(),
        new Exporter_Custom_DAM_RZ(),
        new Exporter_Custom_DAT_25(),
        new Exporter_Custom_DAT_DBPF(),
        new Exporter_Custom_DAT_DRPK(),
        new Exporter_Custom_DAT_FAR(),
        new Exporter_Custom_DLL_MZ_BMP(),
        new Exporter_Custom_DS2RES(),
        new Exporter_Custom_FSB_Audio(),
        new Exporter_Custom_GSC_GSCFMT(),
        new Exporter_Custom_HPI_HAPI(),
        new Exporter_Custom_JDLZ(),
        new Exporter_Custom_JFL(),
        new Exporter_Custom_MHK_MHWK_WAV(),
        new Exporter_Custom_MHTML_Base64(),
        new Exporter_Custom_MHTML_QuotedPrintable(),
        new Exporter_Custom_PAK_20(),
        new Exporter_Custom_PAK_30(),
        new Exporter_Custom_PKG_3(),
        new Exporter_Custom_RGSSAD_RGSSAD(),
        new Exporter_Custom_RPKG_GKPR_Multi(),
        new Exporter_Custom_U_Palette_Generic(),
        new Exporter_Custom_U_Sound_141(),
        new Exporter_Custom_U_Texture_127(),
        new Exporter_Custom_U_Texture_141(),
        new Exporter_Custom_U_Texture_Generic(),
        new Exporter_Custom_U_WAV_119(),
        new Exporter_Custom_U_WAV_127(),
        new Exporter_Custom_U_WAV_159(),
        new Exporter_Custom_U_WAV_172(),
        new Exporter_Custom_UE3_SoundNodeWave_451(),
        new Exporter_Custom_UE3_SoundNodeWave_648(),
        new Exporter_Custom_VFS(),
        new Exporter_Custom_VPK(),
        new Exporter_Custom_WAD_12(),
        new Exporter_Custom_WAV_RawAudio(),
        new Exporter_Custom_WAV_RawAudio_Chunks(),
        new Exporter_Custom_ZSM_ZSNDXBOX(),
        new Exporter_Default(),
        new Exporter_Deflate_XOR(),
        new Exporter_Deflate(),
        new Exporter_Explode(),
        new Exporter_GZip(),
        new Exporter_JDLZ(),
        new Exporter_LZ4_Framed(),
        new Exporter_LZ4(),
        new Exporter_LZ4X(),
        new Exporter_LZMA(),
        new Exporter_LZO(),
        new Exporter_LZO_SingleBlock(),
        new Exporter_LZSS(),
        new Exporter_LZSS_Old(),
        new Exporter_MSZIP(),
        new Exporter_QuickBMS_Decompression(""),
        new Exporter_RAR_RAR(),
        new Exporter_REFPACK(),
        new Exporter_SFL_Bits(),
        new Exporter_SFL_Block(),
        new Exporter_SFL_Nulls(),
        new Exporter_SFL_RLE(),
        new Exporter_SLLZ(),
        new Exporter_Snappy(),
        new Exporter_SplitChunk_ZLib(),
        new Exporter_SplitChunkDefault(),
        new Exporter_XOR(),
        new Exporter_XOR_RepeatingKey(),
        new Exporter_ZIP_Single(),
        new Exporter_ZIP(),
        new Exporter_ZLib(),
        new Exporter_ZLib_CompressedSizeOnly(),
        new Exporter_ZLibX(),
    };
    return plugins;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSPlugin[] loadScriptArchivePlugins() {
    WSPlugin[] plugins = new WSPlugin[] { new ScriptArchivePlugin_MexCom3(), new ScriptArchivePlugin_QuickBMS() };
    return plugins;
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
    changePluginType();
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
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code.equals("SidePanel_PluginList_Plugins")) {
        showPluginDescription();
        return true;
      }
      else if (code.equals("SidePanel_PluginList_PluginTypes")) {
        changePluginType();
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    ((WSComboBox) ComponentRepository.get("SidePanel_PluginList_PluginTypes")).requestFocus();
  }

  ///////////////
  //
  // Default Implementations
  //
  ///////////////

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
  public void showPluginDescription() {
    WSList pluginsList = (WSList) ComponentRepository.get("SidePanel_PluginList_Plugins");
    showPluginDescription(pluginsList.getSelectedValue());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void showPluginDescription(Object pluginObj) {
    if (pluginObj == null) {
      return;
    }

    WSPlugin plugin = (WSPlugin) pluginObj;

    WSTextArea descriptionField = (WSTextArea) ComponentRepository.get("SidePanel_PluginList_PluginDescription");
    descriptionField.setText(plugin.getDescription());
    descriptionField.setCaretPosition(0);
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
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_PluginList.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);

    loadComboBox();
    // Moved to onOpenRequest() due to startup dump
    //changePluginType();
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