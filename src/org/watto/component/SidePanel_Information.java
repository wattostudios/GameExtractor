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
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.watto.Language;
import org.watto.component.model.UneditableTableModel;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.event.WSDoubleClickableInterface;
import org.watto.event.WSMotionableInterface;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.plaf.LookAndFeelManager;
import org.watto.task.Task;
import org.watto.task.Task_ReadArchiveWithPlugin;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A PanelPlugin
 **********************************************************************************************
 **/
public class SidePanel_Information extends WSPanelPlugin implements WSMotionableInterface,
    WSDoubleClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** for tooltips **/
  String lastMotionObject = "";

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  public SidePanel_Information() {
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
  public SidePanel_Information(XMLNode node) {
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
  public void buildTable(JTable table, Object[][] tableData) {
    table.setModel(new UneditableTableModel(tableData, new String[] { "", "" }));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    //table.doLayout();

    // Work out the size of column 1 (the headings) and set it, so that column 2 (the value) is maximized as much as possible
    FontMetrics metrics = table.getFontMetrics(LookAndFeelManager.getFont());
    int width = -1;

    int numRows = tableData.length;
    for (int i = 0; i < numRows; i++) {
      int textWidth = metrics.stringWidth(((Object) tableData[i][0]).toString());
      if (textWidth > width) {
        width = textWidth;
      }
    }
    width += 10;

    TableColumn column0 = table.getColumnModel().getColumn(0);
    column0.setMinWidth(width);
    column0.setMaxWidth(width);
    column0.setPreferredWidth(width);

    /*
     * TableColumnModel columnModel = table.getColumnModel(); if (columnModel.getColumnCount() >
     * 0){ TableColumn column = columnModel.getColumn(0);
     * column.setPreferredWidth(column.getMinWidth()); }
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
  
  **********************************************************************************************
  **/
  public void loadArchiveTable() {
    WSTable archiveTable = (WSTable) ComponentRepository.get("SidePanel_Information_ArchiveTable");
    archiveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    String[][] tableData = new String[9][2];
    int row = 0;

    if (Archive.getReadPlugin() != null) {
      ArchivePlugin plugin = Archive.getReadPlugin();

      tableData[row] = new String[] { Language.get("Information_PluginName"), plugin.getName() };
      row++;

      tableData[row] = new String[] { Language.get("Information_PluginClass"), plugin.getClass().toString() };
      row++;

      tableData[row] = new String[] { Language.get("Information_SupportedExtensions"), plugin.getExtensionsList() };
      row++;

      tableData[row] = new String[] { Language.get("Information_SupportedPlatforms"), plugin.getPlatformsList() };
      row++;

      tableData[row] = new String[] { Language.get("Information_SupportedGames"), plugin.getGamesList() };
      row++;

      tableData[row] = new String[] { Language.get("Information_AllowedFunctions"), plugin.getAllowedFunctionsList() };
      row++;
    }

    if (Archive.getBasePath() != null) {
      tableData[row] = new String[] { Language.get("Information_BasePath"), Archive.getBasePath().getAbsolutePath() };
      row++;
    }

    if (Archive.getResources() != null) {
      Resource[] resources = Archive.getResources();

      tableData[row] = new String[] { Language.get("Information_NumberOfFiles"), resources.length + "" };
      row++;

      long arcSize = 0;
      for (int i = 0; i < resources.length; i++) {
        arcSize += resources[i].getDecompressedLength();
      }

      tableData[row] = new String[] { Language.get("Information_ArchiveSize"), arcSize + "" };
      row++;
    }

    if (row == 0) {
      loadEmptyTable(archiveTable);
      return;
    }
    else if (row < 9) {
      String[][] temp = tableData;
      tableData = new String[row][2];
      System.arraycopy(temp, 0, tableData, 0, row);
    }

    buildTable(archiveTable, tableData);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadEmptyTable(JTable table) {
    table.setModel(new DefaultTableModel(new String[][] { { "" } }, new String[] { "" }));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadPluginTable(File path) {
    WSTable pluginTable = (WSTable) ComponentRepository.get("SidePanel_Information_PluginTable");
    pluginTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    RatedPlugin[] plugins = PluginFinder.findPlugins(path, ArchivePlugin.class);

    if (plugins == null || plugins.length == 0) {
      loadEmptyTable(pluginTable);
      return;
    }

    java.util.Arrays.sort(plugins);

    Object[][] tableData = new Object[plugins.length][2];

    for (int i = 0; i < plugins.length; i++) {
      //tableData[i] = new String[]{plugins[i].getPlugin().getName(),plugins[i].getRating() + "%"};
      tableData[i] = new Object[] { plugins[i], plugins[i].getRating() + "%" };
    }

    buildTable(pluginTable, tableData);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadResourceTable(Resource resource) {
    WSTable resourceTable = (WSTable) ComponentRepository.get("SidePanel_Information_ResourceTable");
    resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    ArchivePlugin readPlugin = Archive.getReadPlugin();
    WSTableColumn[] columns = Archive.getColumns();

    // also list any resource-specific properties stored on it (eg image width/height/format/etc)
    Resource_Property[] properties = resource.getProperties();

    int rowCount = columns.length;
    if (properties != null) {
      rowCount += properties.length;
    }

    /*
    // also list any Unreal properties (should already be handled by the above properties)
    if (resource instanceof Resource_Unreal) {
      UnrealProperty[] unrealProperties = ((Resource_Unreal) resource).getUnrealProperties();
      if (unrealProperties != null) {
        rowCount += unrealProperties.length;
      }
    }
    */

    Object[][] tableData = new Object[rowCount][2];

    // Fill with the standard resource information
    //int fillPos = 0;
    for (int i = 0; i < columns.length; i++) {
      tableData[i][0] = columns[i].getName();
      Object value = readPlugin.getColumnValue(resource, columns[i].getCharCode());
      if (value == null) {
        tableData[i][1] = "";
      }
      else {
        tableData[i][1] = value;
      }

      //fillPos++;
    }

    // Fill with the properties
    if (properties != null) {
      for (int i = 0, j = columns.length; i < properties.length; i++, j++) {
        Resource_Property property = properties[i];

        tableData[j][0] = Language.get("ResourceProperty_" + property.getCode());
        tableData[j][1] = property.getValue();

        //fillPos++;
      }
    }

    /*
    // also fill with any Unreal properties (should already be handled by the above properties)
    if (resource instanceof Resource_Unreal) {
      UnrealProperty[] unrealProperties = ((Resource_Unreal) resource).getUnrealProperties();
      if (unrealProperties != null) {
    
        for (int i = 0, j = fillPos; i < unrealProperties.length; i++, j++) {
          UnrealProperty unrealProperty = unrealProperties[i];
    
          tableData[j][0] = Language.get(unrealProperty.getName());
          tableData[j][1] = unrealProperty.getValue();
        }
      }
    }
    */

    buildTable(resourceTable, tableData);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadResourceTable(Resource[] resources) {
    WSTable resourceTable = (WSTable) ComponentRepository.get("SidePanel_Information_ResourceTable");
    resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    ArchivePlugin readPlugin = Archive.getReadPlugin();
    String[][] tableData = new String[5][2];
    tableData[0] = new String[] { Language.get("Information_NumSelectedFiles"), "" + resources.length };

    long fs = 0;
    long dfs = 0;
    WSTableColumn fsColumn = Archive.getColumn('c');
    WSTableColumn dfsColumn = Archive.getColumn('d');
    for (int i = 0; i < resources.length; i++) {
      fs += ((Long) readPlugin.getColumnValue(resources[i], 'c')).longValue();
      dfs += ((Long) readPlugin.getColumnValue(resources[i], 'd')).longValue();
    }

    tableData[1] = new String[] { fsColumn.getName(), "" + fs };
    tableData[2] = new String[] { dfsColumn.getName(), "" + dfs };

    tableData[3] = new String[] { Archive.getColumn('C').getName(), "" + (fs / 1024) };
    tableData[4] = new String[] { Archive.getColumn('D').getName(), "" + (dfs / 1024) };

    buildTable(resourceTable, tableData);
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
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code.equals("FileList")) {
        reloadTable();
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
   * The event that is triggered from a WSDoubleClickableListener when a double click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onDoubleClick(JComponent c, MouseEvent e) {
    if (c instanceof WSTable) {
      WSTable table = (WSTable) c;
      String code = table.getCode();

      if (code.equals("SidePanel_Information_PluginTable")) {

        ArchivePlugin plugin = (ArchivePlugin) ((RatedPlugin) table.getValueAt(table.getSelectedRow(), 0)).getPlugin();

        // open the archive using the selected plugin
        Task_ReadArchiveWithPlugin task = new Task_ReadArchiveWithPlugin(Archive.getBasePath(), plugin);
        task.setDirection(Task.DIRECTION_REDO);
        new Thread(task).start();

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
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code.equals("FileList")) {
        reloadTable();
        return true;
      }
    }
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
    try {
      // Shows the value of the cell in the statusbar
      if (c instanceof WSTable) {
        WSTable table = (WSTable) c;
        String code = table.getCode();

        if (code.equals("SidePanel_Information_ResourceTable") || code.equals("SidePanel_Information_ArchiveTable") || code.equals("SidePanel_Information_PluginTable")) {
          Point point = e.getPoint();

          int row = table.rowAtPoint(point);
          if (row < 0) {
            return true;
          }

          String selectedObject = table.getValueAt(row, 1).toString();
          String columnHeading = table.getValueAt(row, 0).toString();

          if (columnHeading == null || (lastMotionObject != null && lastMotionObject.equals(columnHeading))) {
            return true; // still over the same object on the list
          }
          lastMotionObject = columnHeading;

          // If the selectedObject is an icon (eg for the table columns Icon, Renamed, Replaced, ...)
          // then change it to a text-value reflecting the proper value of the field
          if (selectedObject.startsWith("images/WSTable/")) {
            selectedObject = Language.get("FileListPanel_" + selectedObject.substring(15) + "_Tooltip");
          }

          ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(columnHeading + ": " + selectedObject);
          return true;
        }
      }
      return false;
    }
    catch (Throwable t) {
      return false;
    }
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
    reloadTable();
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
  public void reloadTable() {

    WSTable pluginTable = (WSTable) ComponentRepository.get("SidePanel_Information_PluginTable");
    WSTable resourceTable = (WSTable) ComponentRepository.get("SidePanel_Information_ResourceTable");

    loadArchiveTable();

    File basePath = Archive.getBasePath();
    if (basePath != null && basePath.exists()) {
      loadPluginTable(basePath);
    }
    else {
      loadEmptyTable(pluginTable);
    }

    WSPanel fileListPanel = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel();
    if (fileListPanel == null || !(fileListPanel instanceof FileListPanel)) {
      loadEmptyTable(resourceTable);
    }
    else {
      Resource[] selected = ((FileListPanel) fileListPanel).getSelected();
      if (Archive.getNumFiles() <= 0) {
        selected = null;
      }

      if (selected == null || selected.length <= 0 || selected[0] == null) {
        loadEmptyTable(resourceTable);
      }
      else if (selected.length == 1) {
        loadResourceTable(selected[0]);
      }
      else {
        loadResourceTable(selected);
      }
    }
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
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_Information.xml"));

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