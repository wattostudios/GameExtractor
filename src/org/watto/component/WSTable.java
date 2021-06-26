////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.component;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import org.watto.event.WSEventHandler;
import org.watto.plaf.ButterflyTableHeaderUI;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;

/***********************************************************************************************
 * A Table GUI <code>Component</code>
 ***********************************************************************************************/

public class WSTable extends JTable implements WSComponent {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The code for the language and settings **/
  String code = null;

  /** The position of this <code>WSComponent</code> in its parent <code>Container</code> **/
  String position = null;

  /** The width of this <code>WSComponent</code>s <code>Border</code> **/
  int borderWidth = -1;

  /** Whether the height of this <code>WSComponent</code> is fixed or not **/
  boolean fixedHeight = false;

  /** Whether the width of this <code>WSComponent</code> is fixed or not **/
  boolean fixedWidth = false;

  /** Whether the minimum height of this <code>WSComponent</code> is fixed or not **/
  boolean fixedMinimumHeight = false;

  /** Whether the minimum width of this <code>WSComponent</code> is fixed or not **/
  boolean fixedMinimumWidth = false;

  /**
   * Whether this <code>WSComponent</code> is initialised or not. Used to determine whether to
   * fire events.
   **/
  boolean initialised = false;

  /**
   * true if a double-click event should be triggered when editing a cell (because the event is
   * consumed before it gets to processEvent())
   **/
  private boolean buildingEditor = false;

  /**
   * keys that aren't allowed to be processed automatically by this table (because removing them
   * from the actionmap/inputmap doesn't work!)
   **/
  private Hashtable<String, String> disabledKeys = new Hashtable<String, String>(1);

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSTable() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSTable(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
   * Passes a <code>ChangeEvent</code> to the normal <code>WSEvent</code> event processing when a
   * table column is resized
   * @param event the <code>ChangeEvent</code> that was triggered
   ***********************************************************************************************/
  @Override
  public void columnMarginChanged(ChangeEvent event) {
    super.columnMarginChanged(event);
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }

  /***********************************************************************************************
   * Compares the <code>getText()</code> of this <code>WSComponent</code> to the
   * <code>getText()</code> of another <code>WSComponent</code>
   * @param otherComponent the <code>WSComponent</code> to compare to
   * @return <b>0</b> if the <code>WSComponent</code>s are equal<br />
   *         <b>1</b> if the <code>otherComponent</code> comes after this
   *         <code>WSComponent</code><br />
   *         <b>-1</b> if the <code>otherComponent</code> comes before this
   *         <code>WSComponent</code>
   ***********************************************************************************************/
  @Override
  public int compareTo(WSComparable otherComponent) {
    return WSHelper.compare(this, otherComponent);
  }

  /***********************************************************************************************
   * The InputMap/ActionMap is set in the LookAndFeel to have default key actions. Removing the
   * actions from the ActionMap/InputMap does not work, so instead add the keys to disable to the
   * <code>disabledKeys</code> <code>Hashtable</code>, and stop processing them in
   * <code>processKeyBinding()</code>.
   * @param key the key to disable
   ***********************************************************************************************/
  public void disableAutomaticKeyEvent(String key) {
    disabledKeys.put(key, "!");
  }

  /***********************************************************************************************
   * Gets the width of this <code>WSComponent</code>s <code>Border</code>
   * @return the <code>Border</code> width
   ***********************************************************************************************/
  @Override
  public int getBorderWidth() {
    return borderWidth;
  }

  /***********************************************************************************************
   * Gets the text code for this <code>WSComponent</code>, which is used for
   * <code>Language</code>s and other functionality
   * @return the text code for this <code>WSComponent</code>
   ***********************************************************************************************/
  @Override
  public String getCode() {
    return code;
  }

  /***********************************************************************************************
   * Gets the fixed height of this <code>WSComponent</code>
   * @return the fixed height, or <b>-1</b> if this <code>WSComponent</code> doesn't have a fixed
   *         height
   ***********************************************************************************************/
  @Override
  public int getFixedHeight() {
    if (fixedHeight) {
      return getHeight();
    }
    return -1;
  }

  /***********************************************************************************************
   * Gets the fixed minimum height of this <code>WSComponent</code>
   * @return the fixed minimum height, or <b>-1</b> if this <code>WSComponent</code> doesn't have
   *         a fixed minimum height
   ***********************************************************************************************/
  @Override
  public int getFixedMinimumHeight() {
    if (fixedMinimumHeight) {
      return (int) getMinimumSize().getHeight();
    }
    return -1;
  }

  /***********************************************************************************************
   * Gets the fixed minimum width of this <code>WSComponent</code>
   * @return the fixed minimum width, or <b>-1</b> if this <code>WSComponent</code> doesn't have
   *         a fixed minimum width
   ***********************************************************************************************/
  @Override
  public int getFixedMinimumWidth() {
    if (fixedMinimumWidth) {
      return (int) getMinimumSize().getWidth();
    }
    return -1;
  }

  /***********************************************************************************************
   * Gets the fixed width of this <code>WSComponent</code>
   * @return the fixed width, or <b>-1</b> if this <code>WSComponent</code> doesn't have a fixed
   *         width
   ***********************************************************************************************/
  @Override
  public int getFixedWidth() {
    if (fixedWidth) {
      return getWidth();
    }
    return -1;
  }

  /***********************************************************************************************
   * Gets the listeners registered on this <code>WSComponent</code>
   * @return the registered listeners
   ***********************************************************************************************/
  @Override
  public Object[] getListenerList() {
    return listenerList.getListenerList();
  }

  /***********************************************************************************************
   * Gets the position of this <code>WSComponent</code> in its parent <code>Container</code>
   * @return the position. For example, <i>"CENTER"</i> or <i>"NORTH"</i>.
   ***********************************************************************************************/
  @Override
  public String getPosition() {
    return position;
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> small text of this <code>WSComponent</code>
   * @return the small text
   ***********************************************************************************************/
  @Override
  public String getSmallText() {
    return WSHelper.getSmallText(this);
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> text of this <code>WSComponent</code>
   * @return the text
   ***********************************************************************************************/
  @Override
  public String getText() {
    return WSHelper.getText(this);
  }

  /***********************************************************************************************
   * Gets nothing, otherwise it causes problems when moving the mouse over the table. This is
   * different to other <code>WSComponent</code>s
   * @return null
   ***********************************************************************************************/
  @Override
  public String getToolTipText() {
    //return WSHelper.getToolTipText(this);
    return null;
  }

  /***********************************************************************************************
   * Gets whether this <code>WSComponent</code> is initialised or not. Used to determine whether
   * to fire events.
   * @return <b>true</b> if this <code>WSComponent</code> is initialised<br />
   *         <b>false</b> if this <code>WSComponent</code> is not initialised
   ***********************************************************************************************/
  public boolean isInitialised() {
    return initialised;
  }

  /***********************************************************************************************
   * Is this <code>WSComponent</code> stored in the <code>ComponentRepository</code>?
   * @return <b>true</b> if this <code>WSComponent</code> is in the
   *         <code>ComponentRepository</code><br />
   *         <b>false</b> if this <code>WSComponent</code> is not in the
   *         <code>ComponentRepository</code>.
   ***********************************************************************************************/
  @Override
  public boolean isInRepository() {
    return ComponentRepository.has(code);
  }

  /***********************************************************************************************
   * Prepares a cell editor. This has been overwritten so that it will trigger a
   * <code>WSDoubleClickEvent</code> if one should occur, as the creation of the editor consumes
   * the <code>event</code>
   * @param editor the <code>TableCellEditor</code>
   * @param row the row being edited
   * @param column the column being edited
   ***********************************************************************************************/
  @Override
  public Component prepareEditor(TableCellEditor editor, int row, int column) {
    if (buildingEditor) {
      processEvent(new MouseEvent(this, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 2, false, MouseEvent.BUTTON1));
    }
    return super.prepareEditor(editor, row, column);
  }

  /***********************************************************************************************
   * Processes an <code>event</code> that was triggered on this <code>WSComponent</code>
   * @param event the <code>AWTEvent</code> that was triggered
   ***********************************************************************************************/
  @Override
  public void processEvent(AWTEvent event) {
    if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_PRESSED) {
      buildingEditor = true; // for allowing double-clicks when the editor is being constructed
    }

    super.processEvent(event); // handles any normal listeners
    WSEventHandler.processEvent(this, event); // passes events to the caller

    buildingEditor = false;
  }

  /***********************************************************************************************
   * Disallows normal processing of any <code>KeyStroke</code>s defined in the
   * <code>disabledKeys</code> <code>Hashtable</code>
   * @param keyStroke the key to disable
   * @param event the <code>KeyEvent</code> that was triggered
   * @param condition the condition that the <code>event</code> was triggered under
   * @param pressed whether the key was pressed or not
   * @return <b>true</b> if the <code>event</code> was handled<br />
   *         <b>false</b> if the <code>event</code> was not handled.
   ***********************************************************************************************/
  @Override
  public boolean processKeyBinding(KeyStroke keyStroke, KeyEvent event, int condition, boolean pressed) {
    if (disabledKeys.get(keyStroke.toString()) != null) {
      return true;
    }
    return super.processKeyBinding(keyStroke, event, condition, pressed);
  }

  /***********************************************************************************************
   * Registers the <code>AWTEvent</code>s that this <code>WSComponent</code> generates
   ***********************************************************************************************/
  @Override
  public void registerEvents() {
    enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.ITEM_EVENT_MASK | //A selectable item (button, checkbox, combobox, radiobutton, list)
        AWTEvent.HIERARCHY_EVENT_MASK | //Children changes (add/remove, show/hide)
        AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK | //Children changes  (size)
        WSComponent.WS_EVENT_MASK);
  }

  /***********************************************************************************************
   * Sets the border width attribute value. <i>This does not actually set the width of the
   * border!</i>
   * @param borderWidth the new border width attribute value
   ***********************************************************************************************/
  @Override
  public void setBorderWidth(int borderWidth) {
    this.borderWidth = borderWidth;
    //setBorder(new EmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));
  }

  /***********************************************************************************************
   * Sets the text <code>code</code> for this <code>WSComponent</code>
   * @param code the text code
   ***********************************************************************************************/
  @Override
  public void setCode(String code) {
    this.code = code;
  }

  /***********************************************************************************************
   * Sets the fixed height attribute value. <i>This does not actually set the height of the
   * <code>WSComponent</code>!</i>
   * @param fixedHeight the new fixed height attribute value
   ***********************************************************************************************/
  @Override
  public void setFixedHeight(boolean fixedHeight) {
    this.fixedHeight = fixedHeight;
  }

  /***********************************************************************************************
   * Sets the fixed minimum height attribute value. <i>This does not actually set the height of
   * the <code>WSComponent</code>!</i>
   * @param fixedMinimumHeight the new fixed minimum height attribute value
   ***********************************************************************************************/
  @Override
  public void setFixedMinimumHeight(boolean fixedMinimumHeight) {
    this.fixedMinimumHeight = fixedMinimumHeight;
  }

  /***********************************************************************************************
   * Sets the fixed minimum width attribute value. <i>This does not actually set the width of the
   * <code>WSComponent</code>!</i>
   * @param fixedMinimumWidth the new fixed minimum width attribute value
   ***********************************************************************************************/
  @Override
  public void setFixedMinimumWidth(boolean fixedMinimumWidth) {
    this.fixedMinimumWidth = fixedMinimumWidth;
  }

  /***********************************************************************************************
   * Sets the fixed width attribute value. <i>This does not actually set the width of the
   * <code>WSComponent</code>!</i>
   * @param fixedWidth the new fixed width attribute value
   ***********************************************************************************************/
  @Override
  public void setFixedWidth(boolean fixedWidth) {
    this.fixedWidth = fixedWidth;
  }

  /***********************************************************************************************
   * Sets whether this <code>WSComponent</code> is focused or not
   * @param focused <b>true</b> if this <code>WSComponent</code> is focused<br />
   *        <b>false</b> if this <code>WSComponent</code> is not focused.
   ***********************************************************************************************/
  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      requestFocusInWindow();
    }
  }

  /***********************************************************************************************
   * Sets whether this <code>WSComponent</code> is initialised or not. Used to determine whether
   * to fire events.
   * @param initialised <b>true</b> if this <code>WSComponent</code> is initialised<br />
   *        <b>false</b> if this <code>WSComponent</code> is not initialised
   ***********************************************************************************************/
  public void setInitialised(boolean initialised) {
    this.initialised = initialised;
  }

  /***********************************************************************************************
   * Sets whether this <code>WSComponent</code> is stored in the <code>ComponentRepository</code>
   * or not
   * @param inRepository <b>true</b> if this <code>WSComponent</code> is stored in the
   *        <code>ComponentRepository</code><br />
   *        <b>false</b> if this <code>WSComponent</code> is not stored in the
   *        <code>ComponentRepository</code>.
   ***********************************************************************************************/
  @Override
  public void setInRepository(boolean inRepository) {
    if (inRepository) {
      ComponentRepository.add(this);
    }
    else {
      ComponentRepository.remove(code);
    }
  }

  /***********************************************************************************************
   * Sets the <code>TableModel</code> that contains the table data. If the <code>model</code> is
   * a <code>WSTableModel</code>, it will call <code>configureTable()</code>
   * @param model the <code>TableModel</code>
   ***********************************************************************************************/
  @Override
  public void setModel(TableModel model) {
    super.setModel(model);
    if (model instanceof WSTableModel) {
      ((WSTableModel) model).configureTable(this);
    }
  }

  /***********************************************************************************************
   * Sets the position attribute value. <i>This does not actually set the position of the
   * <code>WSComponent</code>!</i>
   * @param position the new position attribute value
   ***********************************************************************************************/
  @Override
  public void setPosition(String position) {
    this.position = position;
  }

  /***********************************************************************************************
   * Builds this <code>WSComponent</code> from the properties of the <code>node</code>
   * @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to
   *        construct
   ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    // Sets the generic properties of this component
    WSHelper.setAttributes(node, this);

    setRowHeight(LookAndFeelManager.getTextHeight() + 4 + getRowMargin());
    //ComponentRepository.add(this);

    setIntercellSpacing(new Dimension(0, 0));

    // TODO - need to see how to make this generic
    getTableHeader().setUI((TableHeaderUI) ButterflyTableHeaderUI.createUI(this));
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> text of this <code>WSComponent</code>
   * @return the text
   ***********************************************************************************************/
  @Override
  public String toString() {
    return getText();
  }

  /***********************************************************************************************
   * Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
   * @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
   ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);
    return node;
  }

  /***********************************************************************************************
   * Passes a <code>ListSelectionEvent</code> to the normal <code>WSEvent</code> event processing
   * when a table cell is selected
   * @param event the <code>ListSelectionEvent</code> that was triggered
   ***********************************************************************************************/
  @Override
  public void valueChanged(ListSelectionEvent event) {
    super.valueChanged(event);
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }
}