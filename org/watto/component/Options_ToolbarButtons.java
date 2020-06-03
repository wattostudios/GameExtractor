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
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
A PanelPlugin
**********************************************************************************************
**/
public class Options_ToolbarButtons extends WSPanelPlugin {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
  **********************************************************************************************
  Constructor for extended classes only
  **********************************************************************************************
  **/
  public Options_ToolbarButtons() {
    super(new XMLNode());
  }

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  @param caller the object that contains this component, created this component, or more formally,
              the object that receives events from this component.
  **********************************************************************************************
  **/
  public Options_ToolbarButtons(XMLNode node) {
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
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void addSelectedButton() {

    WSList currentList = (WSList) ComponentRepository.get("Option_ToolbarButtons_CurrentList");
    WSList choicesList = (WSList) ComponentRepository.get("Option_ToolbarButtons_ChoicesList");

    DefaultListModel currentModel = (DefaultListModel) currentList.getModel();
    DefaultListModel choicesModel = (DefaultListModel) choicesList.getModel();

    Object choicesItem = choicesList.getSelectedValue();
    if (choicesItem == null) {
      return;
    }

    int currentIndex = currentList.getSelectedIndex();
    int choicesIndex = choicesList.getSelectedIndex();

    if (currentIndex < 0) {
      currentIndex = 0;
    }
    if (choicesIndex < 0) {
      choicesIndex = 0;
    }

    currentModel.add(currentIndex, choicesItem);
    if (choicesItem instanceof String && !((String) choicesItem).equals("Separator")) {
      // only remove from the top if it isn't a separator
      choicesModel.remove(choicesIndex);
    }

    currentList.setModel(currentModel);
    choicesList.setModel(choicesModel);

    saveLists();

  }

  /**
  **********************************************************************************************
  Gets the plugin description
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    String description = toString() + "\n\n" + Language.get("Description_OptionsPlugin");

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
  Gets the plugin name
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
  public void loadLists() {

    WSList currentList = (WSList) ComponentRepository.get("Option_ToolbarButtons_CurrentList");
    WSList choicesList = (WSList) ComponentRepository.get("Option_ToolbarButtons_ChoicesList");

    DefaultListModel currentModel = new DefaultListModel();
    DefaultListModel choicesModel = new DefaultListModel();

    // Get the codes for all the current toolbar buttons
    WSToolBar toolbar = (WSToolBar) ComponentRepository.get("MainToolBar");
    if (toolbar == null) {
      return; // interface isn't loaded yet
    }
    Component[] currentButtons = toolbar.getComponents();

    // read the list of all available buttons
    XMLNode buttons = XMLReader.read(new File(Settings.get("ToolbarButtonListFile")));

    // fill the current list
    int numCurrent = 0;
    for (int i = 0; i < currentButtons.length; i++) {
      if (currentButtons[i] instanceof WSToolBarSeparator) {
        currentModel.add(numCurrent, "Separator");
        numCurrent++;
      }
      else if (currentButtons[i] instanceof WSButton) {
        //find the same button in the XML
        for (int j = 0; j < buttons.getChildCount(); j++) {
          try {
            if (buttons.getChild(j).getAttribute("code").equals(((WSButton) currentButtons[i]).getCode())) {
              // found
              buttons.removeChild(j);
              currentModel.add(numCurrent, ((WSButton) currentButtons[i]).getCode());
              numCurrent++;
              break;
            }
          }
          catch (Throwable t) {
            //t.printStackTrace();
          }
        }
      }
      else if (currentButtons[i] instanceof WSComboButton) {
        //find the same button in the XML
        for (int j = 0; j < buttons.getChildCount(); j++) {
          try {
            if (buttons.getChild(j).getAttribute("code").equals(((WSComboButton) currentButtons[i]).getMainCode())) {
              // found
              buttons.removeChild(j);
              currentModel.add(numCurrent, ((WSComboButton) currentButtons[i]).getMainCode());
              numCurrent++;
              break;
            }
          }
          catch (Throwable t) {
          }
        }
      }
    }

    // add the separator to the choices too
    choicesModel.add(0, "Separator");
    int numChoices = 1;

    for (int i = 0; i < buttons.getChildCount(); i++) {
      try {
        // these items are not in the current toolbar, so add to Choices
        choicesModel.add(numChoices, buttons.getChild(i).getAttribute("code"));
        numChoices++;
      }
      catch (Throwable t) {
      }
    }

    currentList.setModel(currentModel);
    choicesList.setModel(choicesModel);

    currentList.setCellRenderer(new WSComponentListCellRenderer());
    choicesList.setCellRenderer(new WSComponentListCellRenderer());

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void moveButtonDown() {

    WSList currentList = (WSList) ComponentRepository.get("Option_ToolbarButtons_CurrentList");
    DefaultListModel currentModel = (DefaultListModel) currentList.getModel();

    int currentIndex = currentList.getSelectedIndex();
    if (currentIndex < 0 || currentIndex >= currentModel.getSize() - 1) {
      // not selected, or can't move down
      return;
    }

    Object currentItem = currentList.getSelectedValue();

    currentModel.remove(currentIndex);
    currentModel.add(currentIndex + 1, currentItem);

    currentList.setSelectedIndex(currentIndex + 1);

    //currentList.setModel(currentModel); // automatic
    saveLists();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void moveButtonUp() {

    WSList currentList = (WSList) ComponentRepository.get("Option_ToolbarButtons_CurrentList");
    DefaultListModel currentModel = (DefaultListModel) currentList.getModel();

    int currentIndex = currentList.getSelectedIndex();
    if (currentIndex <= 0 || currentIndex >= currentModel.getSize()) {
      // not selected, or can't move up
      return;
    }

    Object currentItem = currentList.getSelectedValue();

    currentModel.remove(currentIndex);
    currentModel.add(currentIndex - 1, currentItem);

    currentList.setSelectedIndex(currentIndex - 1);

    //currentList.setModel(currentModel); // automatic
    saveLists();
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSClickableListener when a click occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (c instanceof WSButton) {
      String code = ((WSButton) c).getCode();
      if (code.equals("Option_ToolbarButtons_AddButton")) {
        addSelectedButton();
      }
      else if (code.equals("Option_ToolbarButtons_RemoveButton")) {
        removeSelectedButton();
      }
      else if (code.equals("Option_ToolbarButtons_MoveUp")) {
        moveButtonUp();
      }
      else if (code.equals("Option_ToolbarButtons_MoveDown")) {
        moveButtonDown();
      }
      else {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  Performs any functionality that needs to happen when the panel is to be closed. This method
  does nothing by default, but can be overwritten to do anything else needed before the panel is
  closed, such as garbage collecting and closing pointers to temporary objects.
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSDoubleClickableListener when a double click occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onDoubleClick(JComponent c, MouseEvent e) {
    return false;
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSHoverableListener when the mouse moves over an object
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    return super.onHover(c, e);
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSHoverableListener when the mouse moves out of an object
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    return super.onHoverOut(c, e);
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSKeyableListener when a key press occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    return false;
  }

  /**
  **********************************************************************************************
  Performs any functionality that needs to happen when the panel is to be opened. By default,
  it just calls checkLoaded(), but can be overwritten to do anything else needed before the
  panel is displayed, such as resetting or refreshing values.
  **********************************************************************************************
  **/
  @Override
  public void onOpenRequest() {
    loadLists();
  }

  /**
  **********************************************************************************************
  Registers the events that this component generates
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
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void removeSelectedButton() {

    WSList currentList = (WSList) ComponentRepository.get("Option_ToolbarButtons_CurrentList");
    WSList choicesList = (WSList) ComponentRepository.get("Option_ToolbarButtons_ChoicesList");

    DefaultListModel currentModel = (DefaultListModel) currentList.getModel();
    DefaultListModel choicesModel = (DefaultListModel) choicesList.getModel();

    Object currentItem = currentList.getSelectedValue();
    if (currentItem == null) {
      return;
    }

    int currentIndex = currentList.getSelectedIndex();
    int choicesIndex = choicesList.getSelectedIndex();

    if (currentIndex < 0) {
      currentIndex = 0;
    }
    if (choicesIndex <= 0) {
      choicesIndex = 1; // don't want to add it before the separator (position 0)
    }

    if (currentItem instanceof String && !((String) currentItem).equals("Separator")) {
      // only add to the top if it isn't a separator
      choicesModel.add(choicesIndex, currentItem);
    }
    currentModel.remove(currentIndex);

    currentList.setModel(currentModel);
    choicesList.setModel(choicesModel);

    saveLists();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "rawtypes" })
  public void saveLists() {

    WSList currentList = (WSList) ComponentRepository.get("Option_ToolbarButtons_CurrentList");
    DefaultListModel currentModel = (DefaultListModel) currentList.getModel();

    WSToolBar toolbar = (WSToolBar) ComponentRepository.get("MainToolBar");
    if (toolbar == null) {
      return; // interface isn't loaded yet
    }
    toolbar.removeAll();

    XMLNode buttons = XMLReader.read(new File(Settings.get("ToolbarButtonListFile")));

    int numButtons = currentModel.getSize();
    for (int i = 0; i < numButtons; i++) {
      // put the buttons on the toolbar
      String code = (String) currentModel.getElementAt(i);
      if (code.equals("Separator")) {
        // add separator
        toolbar.add(WSHelper.toComponent(XMLReader.read("<WSToolBarSeparator orientation=\"vertical\" />")));
      }
      else {
        // find the button
        for (int j = 0; j < buttons.getChildCount(); j++) {
          try {
            if (buttons.getChild(j).getAttribute("code").equals(code)) {
              toolbar.add(WSHelper.toComponent(buttons.getChild(j)));
              break;
            }
          }
          catch (Throwable t) {
          }
        }
      }
    }

    toolbar.revalidate();
    toolbar.repaint();

  }

  /**
  **********************************************************************************************
  Sets the description of the plugin
  @param description the description
  **********************************************************************************************
  **/
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
  }

  /**
  **********************************************************************************************
  Build this object from the <i>node</i>
  @param node the XML node that indicates how to build this object
  **********************************************************************************************
  **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_Options_ToolbarButtons.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);
  }

  /**
  **********************************************************************************************
  Builds an XMLNode that describes this object
  @return an XML node with the details of this object
  **********************************************************************************************
  **/
  @Override
  public XMLNode toXML() {
    return super.toXML();
  }

}