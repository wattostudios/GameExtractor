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
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.Archive;
import org.watto.event.WSEnterableInterface;
import org.watto.task.Task;
import org.watto.task.Task_ReloadFileListPanel;
import org.watto.task.Task_SearchFileList;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_Search extends WSPanelPlugin implements WSEnterableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSObjectCheckBox[] checkboxes;

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_Search() {
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
  public SidePanel_Search(XMLNode node) {
    super(node);
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
   **********************************************************************************************
   * Adds the checkboxes for each table column
   **********************************************************************************************
   **/
  @SuppressWarnings("rawtypes")
  public void addCheckboxes() {

    // get the panel that holds the checkboxes
    WSPanel checkboxPanel = (WSPanel) ComponentRepository.get("SidePanel_Search_ColumnsHolder");
    checkboxPanel.removeAll();

    WSTableColumn[] columns = Archive.getColumns();

    // only allow searching on columns that are String, Long, or Boolean (eg not Icons, etc)
    int numColumns = columns.length;
    for (int i = 0; i < columns.length; i++) {
      Class type = columns[i].getType();
      if (type != String.class && type != Long.class && type != Boolean.class) {
        numColumns--;
      }
    }

    checkboxPanel.setLayout(new GridLayout(numColumns, 1, 0, 0));

    checkboxes = new WSObjectCheckBox[numColumns];

    for (int i = 0, j = 0; i < columns.length; i++) {
      Class type = columns[i].getType();
      if (type != String.class && type != Long.class && type != Boolean.class) {
        continue;
      }

      WSObjectCheckBox checkbox = new WSObjectCheckBox(XMLReader.read("<WSObjectCheckBox horizontal-alignment=\"left\" />"), columns[i]);
      checkbox.setSelected(Settings.getBoolean("SearchColumn_" + columns[i].getCode()));

      checkboxes[j] = checkbox;

      checkboxPanel.add(checkbox);
      j++;
      //checkboxPanel.add(new JCheckBox("BOO"));
    }

    /*
     * // build the XML that will be used to construct the interface String xml =
     * "<WSPanel layout=\"GridLayout\" columns=\"1\" rows=\"" + numColumns + "\" >";
     *
     * xml += "</WSPanel>";
     *
     * // build and add the components to the panel
     * checkboxPanel.add(WSHelper.toComponent(XMLReader.read(xml)));
     */

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
   * The event that is triggered from a WSClickableListener when a click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (c instanceof WSObjectCheckBox) {
      WSObjectCheckBox checkbox = (WSObjectCheckBox) c;
      WSTableColumn column = (WSTableColumn) checkbox.getObject();
      Settings.set("SearchColumn_" + column.getCode(), "" + checkbox.isSelected());
      return true;
    }
    else if (c instanceof WSButton) {
      String code = ((WSButton) c).getCode();
      if (code.equals("SidePanel_Search_FindNextButton")) {
        //findNextMatch();
        performSearch(true);
      }
      else if (code.equals("SidePanel_Search_SelectAllButton")) {
        //selectAllMatches();
        performSearch(false);
      }
      else if (code.equals("SidePanel_Search_FilterButton")) {
        applyFilter();
      }
      return true;
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
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();

      if (code.equals("SidePanel_Search_InputText")) {
        applyFilter();
        //performSearch(true);
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

      if (code.equals("SidePanel_Search_InputText") && e.getKeyCode() == KeyEvent.VK_F3) {
        performSearch(true);
        //applyFilter();
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
    addCheckboxes();
    ((WSTextField) ComponentRepository.get("SidePanel_Search_InputText")).requestFocus();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void performSearch(boolean firstMatchOnly) {
    // determine the columns to search
    int numColumns = 0;
    WSTableColumn[] columns = new WSTableColumn[checkboxes.length];

    for (int i = 0; i < checkboxes.length; i++) {
      if (checkboxes[i].isSelected()) {
        columns[numColumns] = (WSTableColumn) checkboxes[i].getObject();
        numColumns++;
      }
    }

    if (numColumns < columns.length) {
      WSTableColumn[] temp = columns;
      columns = new WSTableColumn[numColumns];
      System.arraycopy(temp, 0, columns, 0, numColumns);
    }

    // determine the search value
    String searchVal = ((WSTextField) ComponentRepository.get("SidePanel_Search_InputText")).getText();

    Task_SearchFileList task = new Task_SearchFileList(columns, searchVal, firstMatchOnly);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void applyFilter() {
    String searchVal = ((WSTextField) ComponentRepository.get("SidePanel_Search_InputText")).getText();

    if (searchVal.equals("")) {
      // reset
      SingletonManager.remove("FileListFilterValue");
      SingletonManager.remove("FileListFilterColumns");
    }
    else {
      // determine the columns to filter
      int numColumns = 0;
      WSTableColumn[] columns = new WSTableColumn[checkboxes.length];

      for (int i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].isSelected()) {
          columns[numColumns] = (WSTableColumn) checkboxes[i].getObject();
          numColumns++;
        }
      }

      if (numColumns < columns.length) {
        WSTableColumn[] temp = columns;
        columns = new WSTableColumn[numColumns];
        System.arraycopy(temp, 0, columns, 0, numColumns);
      }

      SingletonManager.set("FileListFilterValue", searchVal);
      SingletonManager.set("FileListFilterColumns", columns);
    }

    // Reload the File List
    WSFileListPanelHolder fileListPanelHolder = (WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder");
    Task_ReloadFileListPanel task = new Task_ReloadFileListPanel(fileListPanelHolder);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();

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
    ((WSTextField) ComponentRepository.get("SidePanel_Search_InputText")).requestFocus();
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
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_Search.xml"));

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