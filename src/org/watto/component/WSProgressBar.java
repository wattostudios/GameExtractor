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
import java.awt.Dimension;
import javax.swing.JProgressBar;
import org.watto.event.WSEventHandler;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Progress Bar GUI <code>Component</code>
***********************************************************************************************/

public class WSProgressBar extends JProgressBar implements WSComponent {

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
  /** Whether this <code>WSComponent</code> is initialised or not. Used to determine whether to fire events. **/
  boolean initialised = false;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSProgressBar() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  ***********************************************************************************************/
  public WSProgressBar(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
  Compares the <code>getText()</code> of this <code>WSComponent</code> to the <code>getText()</code>
  of another <code>WSComponent</code>
  @param otherComponent the <code>WSComponent</code> to compare to
  @return <b>0</b> if the <code>WSComponent</code>s are equal<br />
          <b>1</b> if the <code>otherComponent</code> comes after this <code>WSComponent</code><br />
          <b>-1</b> if the <code>otherComponent</code> comes before this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public int compareTo(WSComparable otherComponent) {
    return WSHelper.compare(this, otherComponent);
  }

  /***********************************************************************************************
  Gets the width of this <code>WSComponent</code>s <code>Border</code>
  @return the <code>Border</code> width
  ***********************************************************************************************/
  @Override
  public int getBorderWidth() {
    return borderWidth;
  }

  /***********************************************************************************************
  Gets the text code for this <code>WSComponent</code>, which is used for <code>Language</code>s
  and other functionality
  @return the text code for this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public String getCode() {
    return code;
  }

  /***********************************************************************************************
  Gets the fixed height of this <code>WSComponent</code>
  @return the fixed height, or <b>-1</b> if this <code>WSComponent</code> doesn't have a fixed height
  ***********************************************************************************************/
  @Override
  public int getFixedHeight() {
    if (fixedHeight) {
      return getHeight();
    }
    return -1;
  }

  /***********************************************************************************************
  Gets the fixed minimum height of this <code>WSComponent</code>
  @return the fixed minimum height, or <b>-1</b> if this <code>WSComponent</code> doesn't have a
          fixed minimum height
  ***********************************************************************************************/
  @Override
  public int getFixedMinimumHeight() {
    if (fixedMinimumHeight) {
      return (int) getMinimumSize().getHeight();
    }
    return -1;
  }

  /***********************************************************************************************
  Gets the fixed minimum width of this <code>WSComponent</code>
  @return the fixed minimum width, or <b>-1</b> if this <code>WSComponent</code> doesn't have a
          fixed minimum width
  ***********************************************************************************************/
  @Override
  public int getFixedMinimumWidth() {
    if (fixedMinimumWidth) {
      return (int) getMinimumSize().getWidth();
    }
    return -1;
  }

  /***********************************************************************************************
  Gets the fixed width of this <code>WSComponent</code>
  @return the fixed width, or <b>-1</b> if this <code>WSComponent</code> doesn't have a fixed width
  ***********************************************************************************************/
  @Override
  public int getFixedWidth() {
    if (fixedWidth) {
      return getWidth();
    }
    return -1;
  }

  /***********************************************************************************************
  Gets the listeners registered on this <code>WSComponent</code>
  @return the registered listeners
  ***********************************************************************************************/
  @Override
  public Object[] getListenerList() {
    return listenerList.getListenerList();
  }

  /***********************************************************************************************
  Gets the maximum size of this <code>WSComponent</code>
  @return the maximum size
  ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    return increaseSize(super.getMaximumSize());
  }

  /***********************************************************************************************
  Gets the minimum size of this <code>WSComponent</code>
  @return the minimum size
  ***********************************************************************************************/
  @Override
  public Dimension getMinimumSize() {
    return increaseSize(super.getMinimumSize());
  }

  /***********************************************************************************************
  Gets the position of this <code>WSComponent</code> in its parent <code>Container</code>
  @return the position. For example, <i>"CENTER"</i> or <i>"NORTH"</i>.
  ***********************************************************************************************/
  @Override
  public String getPosition() {
    return position;
  }

  /***********************************************************************************************
  Gets the preferred size of this <code>WSComponent</code>
  @return the preferred size
  ***********************************************************************************************/
  @Override
  public Dimension getPreferredSize() {
    return increaseSize(super.getPreferredSize());
  }

  /***********************************************************************************************
  Gets the <code>Language</code> small text of this <code>WSComponent</code>
  @return the small text
  ***********************************************************************************************/
  @Override
  public String getSmallText() {
    return WSHelper.getSmallText(this);
  }

  /***********************************************************************************************
  Gets the <code>Language</code> text of this <code>WSComponent</code>
  @return the text
  ***********************************************************************************************/
  @Override
  public String getText() {
    return WSHelper.getText(this);
  }

  /***********************************************************************************************
  Gets the <code>Language</code> tooltip text of this <code>WSComponent</code>
  @return the tooltip text
  ***********************************************************************************************/
  @Override
  public String getToolTipText() {
    return WSHelper.getToolTipText(this);
  }

  /***********************************************************************************************
  Adjusts the <code>Dimension</code> for the <code>size</code> of this <code>WSComponent</code>,
  to take into account the <code>showBorder</code> and <code>showLabel</code>
  @param size the <code>Dimension</code> to adjust
  @return the adjusted <code>Dimension</code>
  ***********************************************************************************************/
  private Dimension increaseSize(Dimension size) {
    double w = size.getWidth();
    double h = size.getHeight();

    if (h < 15) {
      h = 15;
    }

    int borderPadding = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
    h += borderPadding + borderPadding;

    size.setSize(w, h);
    return size;
  }

  /***********************************************************************************************
  Gets whether this <code>WSComponent</code> is initialised or not. Used to determine whether to
  fire events.
  @return <b>true</b> if this <code>WSComponent</code> is initialised<br />
          <b>false</b> if this <code>WSComponent</code> is not initialised
  ***********************************************************************************************/
  public boolean isInitialised() {
    return initialised;
  }

  /***********************************************************************************************
  Is this <code>WSComponent</code> stored in the <code>ComponentRepository</code>?
  @return <b>true</b> if this <code>WSComponent</code> is in the <code>ComponentRepository</code><br />
          <b>false</b> if this <code>WSComponent</code> is not in the <code>ComponentRepository</code>.
  ***********************************************************************************************/
  @Override
  public boolean isInRepository() {
    return ComponentRepository.has(code);
  }

  /***********************************************************************************************
  Processes an <code>event</code> that was triggered on this <code>WSComponent</code>
  @param event the <code>AWTEvent</code> that was triggered
  ***********************************************************************************************/
  @Override
  public void processEvent(AWTEvent event) {
    super.processEvent(event); // handles any normal listeners
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }

  /***********************************************************************************************
  Registers the <code>AWTEvent</code>s that this <code>WSComponent</code> generates
  ***********************************************************************************************/
  @Override
  public void registerEvents() {
    enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.ITEM_EVENT_MASK | //A selectable item (button, checkbox, combobox, radiobutton, list)
        WSComponent.WS_EVENT_MASK);
  }

  /***********************************************************************************************
  Sets the border width attribute value. <i>This does not actually set the width of the border!</i>
  @param borderWidth the new border width attribute value
  ***********************************************************************************************/
  @Override
  public void setBorderWidth(int borderWidth) {
    this.borderWidth = borderWidth;
  }

  /***********************************************************************************************
  Sets the text <code>code</code> for this <code>WSComponent</code>
  @param code the text code
  ***********************************************************************************************/
  @Override
  public void setCode(String code) {
    this.code = code;
  }

  /***********************************************************************************************
  Sets the fixed height attribute value. <i>This does not actually set the height of the
  <code>WSComponent</code>!</i>
  @param fixedHeight the new fixed height attribute value
  ***********************************************************************************************/
  @Override
  public void setFixedHeight(boolean fixedHeight) {
    this.fixedHeight = fixedHeight;
  }

  /***********************************************************************************************
  Sets the fixed minimum height attribute value. <i>This does not actually set the height of the
  <code>WSComponent</code>!</i>
  @param fixedMinimumHeight the new fixed minimum height attribute value
  ***********************************************************************************************/
  @Override
  public void setFixedMinimumHeight(boolean fixedMinimumHeight) {
    this.fixedMinimumHeight = fixedMinimumHeight;
  }

  /***********************************************************************************************
  Sets the fixed minimum width attribute value. <i>This does not actually set the width of the
  <code>WSComponent</code>!</i>
  @param fixedMinimumWidth the new fixed minimum width attribute value
  ***********************************************************************************************/
  @Override
  public void setFixedMinimumWidth(boolean fixedMinimumWidth) {
    this.fixedMinimumWidth = fixedMinimumWidth;
  }

  /***********************************************************************************************
  Sets the fixed width attribute value. <i>This does not actually set the width of the
  <code>WSComponent</code>!</i>
  @param fixedWidth the new fixed width attribute value
  ***********************************************************************************************/
  @Override
  public void setFixedWidth(boolean fixedWidth) {
    this.fixedWidth = fixedWidth;
  }

  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is focused or not
  @param focused <b>true</b> if this <code>WSComponent</code> is focused<br />
                 <b>false</b> if this <code>WSComponent</code> is not focused.
  ***********************************************************************************************/
  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      requestFocusInWindow();
    }
  }

  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is initialised or not. Used to determine whether to
  fire events.
  @param initialised <b>true</b> if this <code>WSComponent</code> is initialised<br />
                     <b>false</b> if this <code>WSComponent</code> is not initialised
  ***********************************************************************************************/
  public void setInitialised(boolean initialised) {
    this.initialised = initialised;
  }

  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is stored in the <code>ComponentRepository</code> or not
  @param inRepository <b>true</b> if this <code>WSComponent</code> is stored in the
                                  <code>ComponentRepository</code><br />
                      <b>false</b> if this <code>WSComponent</code> is not stored in the
                                  <code>ComponentRepository</code>.
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
  Sets the position attribute value. <i>This does not actually set the position of the
  <code>WSComponent</code>!</i>
  @param position the new position attribute value
  ***********************************************************************************************/
  @Override
  public void setPosition(String position) {
    this.position = position;
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    // Sets the generic properties of this component
    WSHelper.setAttributes(node, this);

    String tag;

    tag = node.getAttribute("indeterminate");
    if (tag != null) {
      setIndeterminate(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("showText");
    if (tag != null) {
      setStringPainted(WSHelper.parseBoolean(tag));
    }
  }

  /***********************************************************************************************
  Gets the <code>Language</code> text of this <code>WSComponent</code>
  @return the text
  ***********************************************************************************************/
  @Override
  public String toString() {
    return getText();
  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);

    if (isIndeterminate()) {
      node.setAttribute("indeterminate", "true");
    }

    if (isStringPainted()) {
      node.setAttribute("showText", "true");
    }

    return node;
  }
}