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
import java.io.File;
import java.util.Calendar;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.model.UneditableTableModel;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.WSMotionableInterface;
import org.watto.io.FileManipulator;
import org.watto.io.converter.BooleanArrayConverter;
import org.watto.io.converter.CharConverter;
import org.watto.io.converter.DoubleConverter;
import org.watto.io.converter.FloatConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.plaf.ButterflyHexEditorTableCellRenderer;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

public class WSHexEditor extends WSPanel implements WSKeyableInterface,
    WSClickableInterface,
    WSMotionableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  // radio buttons to switch between hex/char/bytee display
  boolean showDisplayOptions = false;

  // table to display information on the selected byte
  boolean showSelectedValues = false;

  // the table displaying the data
  WSTable preview;

  WSScrollPane scrollPane;

  /** for tooltips **/
  String lastMotionObject = "";

  byte[] bytes = new byte[0];

  /**
   **********************************************************************************************
   * Constructor for extended classes only
   **********************************************************************************************
   **/
  WSHexEditor() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSHexEditor(XMLNode node) {
    // NEED TO DO THIS HERE, OTHERWISE THE SETTING VARIABLE DOESN'T GET SAVED!!! (not sure why)
    //super(node);
    super();
    toComponent(node);
    registerEvents();
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
  public void buildByteTable(String[][] tableData) {
    for (int i = 0, row = 0, cell = 0; i < bytes.length; i++) {
      if (cell == 0) {
        tableData[row][cell] = buildRowHeading(i);
        cell++;
      }

      int currentByte = bytes[i];
      if (currentByte < 0) {
        currentByte = 256 + currentByte;
      }

      if (currentByte == 0) {
        tableData[row][cell] = "<null>0";
      }
      else {
        tableData[row][cell] = "" + currentByte;
      }

      cell++;

      if (cell >= 9) {
        cell = 0;
        row++;
      }
    }
  }

  /**
   **********************************************************************************************
   * Builds an XMLNode that describes this object
   * @return an XML node with the details of this object
   **********************************************************************************************
   **/
  /*
   * public XMLNode toXML(){ return super.toXML(); }
   */

  /**
   **********************************************************************************************
   * Registers the events that this component generates
   **********************************************************************************************
   **/
  /*
   * public void registerEvents(){ super.registerEvents(); }
   */

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildCharTable(String[][] tableData) {
    for (int i = 0, row = 0, cell = 0; i < bytes.length; i++) {
      if (cell == 0) {
        tableData[row][cell] = buildRowHeading(i);
        cell++;
      }

      int currentByte = bytes[i];
      if (currentByte < 0) {
        currentByte = 256 + currentByte;
      }

      if (currentByte == 0) {
        tableData[row][cell] = "<null>";
      }
      else {
        tableData[row][cell] = "" + (char) currentByte;
      }

      cell++;

      if (cell >= 9) {
        cell = 0;
        row++;
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildHexTable(String[][] tableData) {
    for (int i = 0, row = 0, cell = 0; i < bytes.length; i++) {
      if (cell == 0) {
        tableData[row][cell] = buildRowHeading(i);
        cell++;
      }

      int currentByte = bytes[i];
      if (currentByte < 0) {
        currentByte = 256 + currentByte;
      }

      if (currentByte == 0) {
        tableData[row][cell] = "<null>00";
      }
      else {
        String cellVal = Integer.toHexString(currentByte).toUpperCase();
        if (cellVal.length() < 2) {
          cellVal = "0" + cellVal;
        }
        tableData[row][cell] = cellVal;
      }

      cell++;

      if (cell >= 9) {
        cell = 0;
        row++;
      }
    }
  }

  /**
   **********************************************************************************************
   * Row Heading (Offset Position)
   **********************************************************************************************
   **/
  public String buildRowHeading(int offset) {
    String hexVal = Integer.toHexString(offset).toUpperCase();
    if (hexVal.length() % 2 == 1) {
      hexVal = "0" + hexVal;
    }

    return hexVal + " (" + offset + ")";
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void constructInterface() {

    if (showDisplayOptions) {
      Component displayOptionsPanel = WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_HexEditor_DisplayOptions.xml")));
      add(displayOptionsPanel, BorderLayout.NORTH);
      setCurrentDisplayOption();
    }
    else {
      Settings.set("HexEditorDisplayType", "Hex");
    }

    if (showSelectedValues) {
      Component selectedValuesPanel = WSHelper.toComponent(XMLReader.read(new File("interface" + File.separator + "SidePanel_HexEditor_SelectedValues.xml")));
      add(selectedValuesPanel, BorderLayout.SOUTH);

      String[] tableHeadings = new String[] { " ", Language.get("HexEditor_LittleEndian"), Language.get("HexEditor_BigEndian") };

      WSTable valuesTable = (WSTable) ComponentRepository.get("SidePanel_HexEditor_SelectedValues");
      valuesTable.setModel(new UneditableTableModel(new String[0][3], tableHeadings));
      valuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

      WSPanel tablePanel = (WSPanel) ComponentRepository.get("SidePanel_HexEditor_SelectedValues_Main");
      tablePanel.add(valuesTable.getTableHeader(), BorderLayout.NORTH);
    }

    preview = new WSTable(XMLReader.read("<WSTable code=\"SidePanel_HexEditor_Table\" />"));

    preview.setCellSelectionEnabled(true);
    preview.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    preview.removeEditor();
    preview.getTableHeader().setReorderingAllowed(false);

    // Special renderer for this table, so we can do things like coloring null values specially
    preview.setDefaultRenderer(Object.class, new ButterflyHexEditorTableCellRenderer());

    scrollPane = new WSScrollPane(XMLReader.read("<WSScrollPane showInnerBorder=\"true\" />"));
    scrollPane.setViewportView(preview);

    add(scrollPane, BorderLayout.CENTER);

    //preview.addKeyListener(new WSKeyableListener(this));
    //preview.addMouseListener(new WSClickableListener(this));

    try {
      preview.setColumnSelectionInterval(1, 1);
      preview.setRowSelectionInterval(0, 0);
    }
    catch (Exception e) {
      // catch errors for files with length=0
    }

    reloadSelectedValue();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadData(File file) {
    try {
      int length = (int) file.length();
      int sizeLimit = Settings.getInt("HexEditorFileSizeLimit");
      if (sizeLimit <= 0) {
        // view the full file
      }
      else if (length > sizeLimit) {
        length = sizeLimit;
      }

      FileManipulator fm = new FileManipulator(file, false);
      bytes = fm.readBytes(length);
      fm.close();
    }
    catch (Throwable t) {
      bytes = new byte[0];
    }

    reloadData();
    reloadSelectedValue();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code.equals("SidePanel_HexEditor_Table")) {
        reloadSelectedValue();
        return true;
      }
      else if (code.equals("SidePanel_HexEditor_ShowHex")) {
        Settings.set("HexEditorDisplayType", "Hex");
        reloadData();
        return true;
      }
      else if (code.equals("SidePanel_HexEditor_ShowChar")) {
        Settings.set("HexEditorDisplayType", "Char");
        reloadData();
        return true;
      }
      else if (code.equals("SidePanel_HexEditor_ShowByte")) {
        Settings.set("HexEditorDisplayType", "Byte");
        reloadData();
        return true;
      }

    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent c, java.awt.event.KeyEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code.equals("SidePanel_HexEditor_Table")) {
        reloadSelectedValue();
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
    // Shows the value of the cell in the statusbar
    if (c instanceof WSTable) {
      WSTable table = (WSTable) c;
      String code = table.getCode();

      if (code.equals("SidePanel_HexEditor_SelectedValues")) {
        Point point = e.getPoint();

        int row = table.rowAtPoint(point);
        if (row < 0) {
          return true;
        }

        Object selectedObject = table.getValueAt(row, 1);

        String selectedValue = "";
        if (selectedObject != null) {
          selectedValue = selectedObject.toString();
        }

        String columnHeading = table.getValueAt(row, 0).toString();

        if (columnHeading == null || lastMotionObject.equals(columnHeading)) {
          return true; // still over the same object on the list
        }
        lastMotionObject = columnHeading;

        ((WSStatusBar) ComponentRepository.get("StatusBar")).setText(columnHeading + ": " + selectedValue);
        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadData() {
    if (bytes == null) {
      return;
    }

    int numRows = bytes.length / 8;
    if (bytes.length % 8 > 0) {
      numRows++;
    }

    String displayType = Settings.get("HexEditorDisplayType");

    String[][] tableData = new String[numRows][9];

    if (displayType.equals("Char")) {
      buildCharTable(tableData);
    }
    else if (displayType.equals("Byte")) {
      buildByteTable(tableData);
    }
    else { // hex by default
      buildHexTable(tableData);
    }

    preview.setModel(new UneditableTableModel(tableData, new String[] { "", "", "", "", "", "", "", "", "" }));

    TableColumnModel model = preview.getColumnModel();

    for (int i = 0; i < preview.getColumnCount(); i++) {
      //model.getColumn(i).sizeWidthToFit();
      TableColumn column = model.getColumn(i);

      if (i > 0) {
        column.setMinWidth(0);
        column.setPreferredWidth(25);
      }

    }

    try {
      scrollPane.getColumnHeader().setVisible(false);
    }
    catch (Throwable t) {
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadSelectedValue() {
    if (!showSelectedValues) {
      return;
    }

    int row = preview.getSelectedRow();
    int col = preview.getSelectedColumn();

    if (col == 0) {
      //return;

      // we actually set column to -1, so that reloadSelectedValue(row,col) will render an empty table
      col = -1;
    }

    reloadSelectedValue(row, col);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadSelectedValue(int row, int col) {
    if (!showSelectedValues) {
      return;
    }

    boolean showBinary = Settings.getBoolean("ShowBinaryHexEditorField");
    boolean showByte = Settings.getBoolean("ShowByteHexEditorField");
    boolean showShort = Settings.getBoolean("ShowShortHexEditorField");
    boolean showInt = Settings.getBoolean("ShowIntHexEditorField");
    boolean showLong = Settings.getBoolean("ShowLongHexEditorField");
    boolean showFloat = Settings.getBoolean("ShowFloatHexEditorField");
    boolean showDouble = Settings.getBoolean("ShowDoubleHexEditorField");
    boolean showHex = Settings.getBoolean("ShowHexHexEditorField");
    boolean showChar = Settings.getBoolean("ShowCharHexEditorField");
    boolean showUnicode = Settings.getBoolean("ShowUnicodeHexEditorField");
    boolean showTimestamp = Settings.getBoolean("ShowTimestampHexEditorField");

    int numRows = 0;
    if (showBinary) {
      numRows++;
    }
    ;
    if (showByte) {
      numRows++;
    }
    ;
    if (showShort) {
      numRows++;
    }
    ;
    if (showInt) {
      numRows++;
    }
    ;
    if (showLong) {
      numRows++;
    }
    ;
    if (showFloat) {
      numRows++;
    }
    ;
    if (showDouble) {
      numRows++;
    }
    ;
    if (showHex) {
      numRows++;
    }
    ;
    if (showChar) {
      numRows++;
    }
    ;
    if (showUnicode) {
      numRows++;
    }
    ;
    if (showTimestamp) {
      numRows++;
    }
    ;

    String[][] tableData = new String[numRows][3];

    // populate the headings
    int curRow = 0;

    // BINARY
    if (showBinary) {
      tableData[curRow][0] = Language.get("HexEditor_BinaryValue");
      curRow++;
    }
    // BYTE
    if (showByte) {
      tableData[curRow][0] = Language.get("HexEditor_ByteValue");
      curRow++;
    }
    // SHORT
    if (showShort) {
      tableData[curRow][0] = Language.get("HexEditor_ShortValue");
      curRow++;
    }
    // INT
    if (showInt) {
      tableData[curRow][0] = Language.get("HexEditor_IntValue");
      curRow++;
    }
    // LONG
    if (showLong) {
      tableData[curRow][0] = Language.get("HexEditor_LongValue");
      curRow++;
    }
    // FLOAT
    if (showFloat) {
      tableData[curRow][0] = Language.get("HexEditor_FloatValue");
      curRow++;
    }
    // DOUBLE
    if (showDouble) {
      tableData[curRow][0] = Language.get("HexEditor_DoubleValue");
      curRow++;
    }
    // HEX
    if (showHex) {
      tableData[curRow][0] = Language.get("HexEditor_HexValue");
      curRow++;
    }
    // CHAR
    if (showChar) {
      tableData[curRow][0] = Language.get("HexEditor_CharValue");
      curRow++;
    }
    // UNICODE
    if (showUnicode) {
      tableData[curRow][0] = Language.get("HexEditor_UnicodeValue");
      curRow++;
    }
    // TIMESTAMP
    if (showTimestamp) {
      tableData[curRow][0] = Language.get("HexEditor_TimestampValue");
      curRow++;
    }

    String[] tableHeadings = new String[] { " ", Language.get("HexEditor_LittleEndian"), Language.get("HexEditor_BigEndian") };

    // display an empty table with the headings in it, if no file is loaded or no value is selected
    if (row == -1 || col == -1) {
      if (Settings.getBoolean("AlwaysShowHexEditorSelectedTable")) {
      }
      else {
        tableData = new String[0][0];
      }

      WSTable valuesTable = (WSTable) ComponentRepository.get("SidePanel_HexEditor_SelectedValues");
      valuesTable.setModel(new UneditableTableModel(tableData, tableHeadings));
      valuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

      // Work out the size of column 1 (the headings) and set it, so that column 2 (the value) is maximized as much as possible
      FontMetrics metrics = valuesTable.getFontMetrics(LookAndFeelManager.getFont());
      int width = -1;

      for (int i = 0; i < numRows; i++) {
        int textWidth = metrics.stringWidth(((Object) tableData[i][0]).toString());
        if (textWidth > width) {
          width = textWidth;
        }
      }
      width += 10;

      TableColumn column0 = valuesTable.getColumnModel().getColumn(0);
      column0.setMinWidth(width);
      column0.setMaxWidth(width);
      column0.setPreferredWidth(width);

      return;
    }

    // prepare the bytes
    int offset = row * 8 + (col - 1);

    int[] data = new int[8];
    java.util.Arrays.fill(data, (byte) 0);

    for (int i = 0; i < 8; i++) {
      if (offset < 0) {
        data[i] = 0;
      }
      else if (offset < bytes.length) {
        int dataVal = bytes[offset];

        if (dataVal < 0) {
          dataVal = 256 + dataVal;
        }

        data[i] = dataVal;
      }
      offset++;
    }

    // show the data
    try {

      byte[] byteArray2 = new byte[] { (byte) data[0], (byte) data[1] };
      byte[] byteArray4 = new byte[] { (byte) data[0], (byte) data[1], (byte) data[2], (byte) data[3] };
      byte[] byteArray8 = new byte[] { (byte) data[0], (byte) data[1], (byte) data[2], (byte) data[3], (byte) data[4], (byte) data[5], (byte) data[6], (byte) data[7] };

      curRow = 0;

      // BINARY
      if (showBinary) {
        boolean[] binary = BooleanArrayConverter.convertLittle((byte) data[0]);
        String binaryL = "";
        for (int i = 0; i < binary.length; i++) {
          String value = "0";
          if (binary[i]) {
            value = "1";
          }
          binaryL += value;
        }
        tableData[curRow][1] = binaryL;
        tableData[curRow][2] = binaryL; // big and little are the same
        curRow++;
      }

      // BYTE
      if (showByte) {
        String byteL = "" + data[0];
        tableData[curRow][1] = byteL;
        tableData[curRow][2] = byteL; // big and little are the same
        curRow++;
      }

      // SHORT
      if (showShort) {
        String shortL = "" + ShortConverter.convertLittle(byteArray2);
        tableData[curRow][1] = shortL;
        String shortB = "" + ShortConverter.convertBig(byteArray2);
        tableData[curRow][2] = shortB;
        curRow++;
      }

      // INT
      if (showInt) {
        String intL = "" + IntConverter.convertLittle(byteArray4);
        tableData[curRow][1] = intL;
        String intB = "" + IntConverter.convertBig(byteArray4);
        tableData[curRow][2] = intB;
        curRow++;
      }

      // LONG
      if (showLong) {
        String longL = "" + LongConverter.convertLittle(byteArray8);
        tableData[curRow][1] = longL;
        String longB = "" + LongConverter.convertBig(byteArray8);
        tableData[curRow][2] = longB;
        curRow++;
      }

      // FLOAT
      if (showFloat) {
        String floatL = "" + FloatConverter.convertLittle(byteArray4);
        tableData[curRow][1] = floatL;
        String floatB = "" + FloatConverter.convertBig(byteArray4);
        tableData[curRow][2] = floatB;
        curRow++;
      }

      // DOUBLE
      if (showDouble) {
        String doubleL = "" + DoubleConverter.convertLittle(byteArray8);
        tableData[curRow][1] = doubleL;
        String doubleB = "" + DoubleConverter.convertBig(byteArray8);
        tableData[curRow][2] = doubleB;
        curRow++;
      }

      // HEX
      if (showHex) {
        String hexL = Integer.toHexString(data[0]).toUpperCase();
        if (hexL.length() < 2) {
          hexL = "0" + hexL;
        }
        tableData[curRow][1] = hexL;
        tableData[curRow][2] = hexL; // big and little are the same
        curRow++;
      }

      // CHAR
      if (showChar) {
        String charL = "" + (char) data[0];
        tableData[curRow][1] = charL;
        tableData[curRow][2] = charL; // big and little are the same
        curRow++;
      }

      // UNICODE
      if (showUnicode) {
        String unicodeL = "" + CharConverter.convertLittle(byteArray2);
        tableData[curRow][1] = unicodeL;
        String unicodeB = "" + CharConverter.convertBig(byteArray2);
        tableData[curRow][2] = unicodeB;
        curRow++;
      }

      // TIMESTAMP
      if (showTimestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(LongConverter.convertLittle(byteArray8));
        String timestampL = "" + cal.getTime().toString();
        tableData[curRow][1] = timestampL;
        cal.setTimeInMillis(LongConverter.convertBig(byteArray8));
        String timestampB = "" + cal.getTime().toString();
        tableData[curRow][2] = timestampB;
        curRow++;
      }

      WSTable valuesTable = (WSTable) ComponentRepository.get("SidePanel_HexEditor_SelectedValues");
      valuesTable.setModel(new UneditableTableModel(tableData, tableHeadings));
      valuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

      // Work out the size of column 1 (the headings) and set it, so that column 2 (the value) is maximized as much as possible
      FontMetrics metrics = valuesTable.getFontMetrics(LookAndFeelManager.getFont());
      int width = -1;

      for (int i = 0; i < numRows; i++) {
        int textWidth = metrics.stringWidth(((Object) tableData[i][0]).toString());
        if (textWidth > width) {
          width = textWidth;
        }
      }
      width += 10;

      TableColumn column0 = valuesTable.getColumnModel().getColumn(0);
      column0.setMinWidth(width);
      column0.setMaxWidth(width);
      column0.setPreferredWidth(width);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    preview.requestFocus();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setCurrentDisplayOption() {
    WSRadioButton radio;

    String option = Settings.get("HexEditorDisplayType");
    if (option.equals("Byte")) {
      radio = (WSRadioButton) ComponentRepository.get("SidePanel_HexEditor_ShowByte");
    }
    else if (option.equals("Char")) {
      radio = (WSRadioButton) ComponentRepository.get("SidePanel_HexEditor_ShowChar");
    }
    else { // hex by default
      radio = (WSRadioButton) ComponentRepository.get("SidePanel_HexEditor_ShowHex");
    }
    radio.setSelected(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setShowDisplayOptions(boolean showDisplayOptions) {
    this.showDisplayOptions = showDisplayOptions;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setShowSelectedValues(boolean showSelectedValues) {
    this.showSelectedValues = showSelectedValues;
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

    String tag;

    tag = node.getAttribute("showDisplayOptions");
    if (tag != null) {
      setShowDisplayOptions(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("showSelectedValues");
    if (tag != null) {
      setShowSelectedValues(WSHelper.parseBoolean(tag));
    }

    constructInterface();
  }

}