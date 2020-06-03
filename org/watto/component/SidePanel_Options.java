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
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.event.WSMotionableInterface;
import org.watto.event.WSSelectableInterface;
import org.watto.ge.GameExtractor;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_Options extends WSPanelPlugin implements WSSelectableInterface, WSMotionableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  WSPanelPlugin currentGroup = null;
  Object lastMotionObject = null;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_Options() {
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
  public SidePanel_Options(XMLNode node) {
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
  public void loadGroup(WSPanelPlugin group) {
    if (group == null) {
      return;
    }

    if (currentGroup != null) {
      currentGroup.onCloseRequest();
    }

    this.currentGroup = group;

    // reload the settings
    currentGroup.revalidate();

    currentGroup.onOpenRequest();

    WSOptionGroupHolder groupHolder = (WSOptionGroupHolder) ComponentRepository.get("SidePanel_Options_OptionGroupHolder");
    groupHolder.loadPanel(group);
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
  public void loadOptionGroups() {
    WSComboBox groupSelectionList = (WSComboBox) ComponentRepository.get("SidePanel_Options_OptionGroups");

    if (groupSelectionList.getItemCount() <= 0) {
      // need to load the list
      WSPlugin[] plugins = WSPluginManager.getGroup("Options").getPlugins();

      if (Settings.getBoolean("SortPluginLists")) {
        java.util.Arrays.sort(plugins);
      }

      groupSelectionList.setModel(new DefaultComboBoxModel(plugins));

      int selected = Settings.getInt("SelectedOptionGroup");
      if (selected >= 0 && selected < plugins.length) {
        groupSelectionList.setSelectedIndex(selected);
      }

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
      if (code.equals("SidePanel_Options_SaveOptionsButton")) {
        GameExtractor.getInstance().reload();
        WSPopup.showMessageInNewThread("Options_OptionsSaved", true);
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
    // save the options
    GameExtractor.getInstance().reload();
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
   * CHANGED The event that is triggered from a WSHoverableListener when the mouse moves over an
   * object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    String tooltip = c.getToolTipText();
    //System.out.println(c.getClass());
    if (tooltip != null && tooltip.length() > 0) {
      WSTextArea detailsArea = (WSTextArea) ComponentRepository.get("SidePanel_Options_DescriptionField");
      detailsArea.setText(tooltip);
    }
    lastMotionObject = null;
    return super.onHover(c, e);
  }

  /**
   **********************************************************************************************
   * CHANGED The event that is triggered from a WSHoverableListener when the mouse moves out of
   * an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    WSTextArea detailsArea = (WSTextArea) ComponentRepository.get("SidePanel_Options_DescriptionField");
    detailsArea.setText(" ");
    lastMotionObject = null;
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
   * The event that is triggered from a WSMotionableListener when a component is moved over
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onMotion(JComponent c, java.awt.event.MouseEvent e) {
    //if (c == lastMotionObject){
    //  return true;
    //  }

    if (c instanceof WSList) {
      String code = ((WSList) c).getCode();
      if (code.equals("Option_ToolbarButtons_CurrentList") || code.equals("Option_ToolbarButtons_ChoicesList")) {
        Object selectedObject = ((WSList) c).getModel().getElementAt(((WSList) c).locationToIndex(e.getPoint()));
        if (lastMotionObject == selectedObject || selectedObject == null) {
          return true; // still over the same object on the list
        }
        lastMotionObject = selectedObject;
        String tooltip = Language.get("WSButton_" + (String) selectedObject + "_Tooltip");

        if (tooltip != null && tooltip.length() > 0) {
          WSTextArea detailsArea = (WSTextArea) ComponentRepository.get("SidePanel_Options_DescriptionField");
          detailsArea.setText(tooltip);
        }
        ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(tooltip);
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
    WSComboBox groupSelectionList = (WSComboBox) ComponentRepository.get("SidePanel_Options_OptionGroups");
    int selectedGroup = Settings.getInt("SelectedOptionGroup");

    if (selectedGroup >= 0 && selectedGroup < groupSelectionList.getItemCount()) {
      if (selectedGroup == groupSelectionList.getSelectedIndex()) {
        // reload the current group, because the event isn't triggered if the index is the same
        loadGroup((WSPanelPlugin) groupSelectionList.getSelectedItem());
      }
      else {
        // changing the index will trigger the event to change the option group
        groupSelectionList.setSelectedIndex(selectedGroup);
      }
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

      if (code.equals("SidePanel_Options_OptionGroups")) {
        WSComboBox combo = (WSComboBox) c;
        Settings.set("SelectedOptionGroup", combo.getSelectedIndex());

        loadGroup((WSPanelPlugin) combo.getSelectedItem());

        return true;
      }
    }

    return false;
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
    ((WSComboBox) ComponentRepository.get("SidePanel_Options_OptionGroups")).requestFocus();
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
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_Options.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);

    loadOptionGroups();
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