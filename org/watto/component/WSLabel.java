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
import java.awt.FontMetrics;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import org.watto.Language;
import org.watto.event.WSEvent;
import org.watto.event.WSEventHandler;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Label GUI <code>Component</code>
***********************************************************************************************/

public class WSLabel extends JLabel implements WSComponent {

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
  /** Whether to show a border on this <code>WSComponent</code> or not **/
  boolean showBorder = false;
  /** Whether this <code>WSComponent</code> is initialised or not. Used to determine whether to fire events. **/
  boolean initialised = false;
  /** Whether to allow text wrapping on this <code>WSLabel</code> **/
  boolean wrap = false;
  /** Whether to shorten text if it is too long for the label **/
  boolean shortenLongText = false;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSLabel() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  ***********************************************************************************************/
  public WSLabel(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
  Changes the <code>Border</code> of the <code>WSPanel</code> if the <code>showBorder</code> or
  <code>showLabel</code> has been changed
  ***********************************************************************************************/
  private void changeBorder() {
    int borderPadding = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH") + 2;

    int n = 0;
    int s = 0;
    int e = 0;
    int w = 0;

    if (showBorder) {
      int pad = borderWidth;
      if (pad <= -1) {
        pad = borderPadding + 2;
      }

      n = pad;
      s = pad;
      e = pad;
      w = pad;
    }

    setBorder(new EmptyBorder(n, w, s, e));
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
  Fires a <code>WSEvent</code> when the <code>showBorder</code> has changed
  ***********************************************************************************************/
  private void fireShowBorderChangedEvent() {
    if (!initialised) {
      return;
    }
    WSHelper.fireEvent(new WSEvent(this, WSEvent.SHOW_BORDER_CHANGED), this);
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

  public boolean getShortenLongText() {
    return shortenLongText;
  }

  /***********************************************************************************************
  Gets whether this <code>WSComponent</code> has a visible <code>Border</code> or not
  @return <b>true</b> if the <code>Border</code> is visible<br />
          <b>false</b> if the <code>Border</code> is not visible
  ***********************************************************************************************/
  public boolean getShowBorder() {
    return showBorder;
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
    String helperText = WSHelper.getText(this);
    if (helperText == null || helperText.equals("")) {
      helperText = super.getText();
    }
    return helperText;
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
  Gets whether this <code>WSLabel</code> allows text <code>wrap</code>
  @return <b>true</b> if this <code>WSLabel</code> allows text <code>wrap</code><br />
          <b>false</b> if this <code>WSLabel</code> does not allow text <code>wrap</code>
  ***********************************************************************************************/
  public boolean getWrap() {
    return wrap;
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

    if (wrap) {
      // increases the height for the wrapped text
      String[] lines = WordWrap.wrap(getText(), this, (int) w);

      FontMetrics metrics = getGraphics().getFontMetrics();
      h += (metrics.getHeight() * (lines.length - 1));
    }

    // still need this method so that it gets the height correctly before it is actually set in setShowLabel
    if (showBorder) {
      int pad = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
      h += pad;
      w += pad + pad;
    }

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
  Is this <code>WSLabel</code> opaque?
  @return <b>false</b>
  ***********************************************************************************************/
  @Override
  public boolean isOpaque() {
    return false;
  }

  /***********************************************************************************************
  Performs an action when a <code>WSEvent</code> event is triggered
  @param source the <code>Object</code> that triggered the event
  @param event the <code>WSEvent</code>
  @param type the events type ID
  @return <b>true</b> if the event was handled by this class<br />
          <b>false</b> if the event wasn't handled by this class, and thus should be passed on to
          the parent class for handling.
  ***********************************************************************************************/
  public boolean onEvent(Object source, WSEvent event, int type) {
    if (type == WSEvent.SHOW_BORDER_CHANGED) {
      changeBorder();
    }
    else {
      return false;
    }
    return true;
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
    enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.HIERARCHY_EVENT_MASK | AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK | AWTEvent.INPUT_METHOD_EVENT_MASK | WSComponent.WS_EVENT_MASK);
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
  Loads the icons for the label, using the <code>code</code> to get the icon <code>File</code>s
  ***********************************************************************************************/
  public void setIcons() {
    String className = WSHelper.getClassName(this);

    URL normalIcon = getClass().getResource("images/" + className + "/" + code + "_n.gif");
    if (normalIcon != null) {
      setIcon(new ImageIcon(normalIcon));
    }

    URL disabledIcon = getClass().getResource("images/" + className + "/" + code + "_d.gif");
    if (disabledIcon != null) {
      setDisabledIcon(new ImageIcon(disabledIcon));
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

  public void setShortenLongText(boolean shortenLongText) {
    this.shortenLongText = shortenLongText;
  }

  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> has a visible <code>Border</code> or not
  @param showBorder <b>true</b> if the <code>Border</code> is visible<br />
                    <b>false</b> if the <code>Border</code> is not visible
  ***********************************************************************************************/
  public void setShowBorder(boolean showBorder) {
    this.showBorder = showBorder;
    fireShowBorderChangedEvent();
  }

  /***********************************************************************************************
  Sets the <code>Language</code> text of this <code>WSComponent</code>
  @param text the text
  ***********************************************************************************************/
  @Override
  public void setText(String text) {
    String langCode = WSHelper.getClassName(this) + "_" + code + "_Text";
    Language.set(langCode, text);

    langCode = WSHelper.getClassName(this) + "_" + code + "_Small";
    Language.set(langCode, text);
  }

  /***********************************************************************************************
  Calls setText() on the super object, as <code>setText()</code> has a special meaning for
  <code>WSLabel</code>s
  ***********************************************************************************************/
  public void setText_Super(String text) {
    super.setText(text);
  }

  /***********************************************************************************************
  Sets whether this <code>WSLabel</code> allows text <code>wrap</code>
  @return <b>true</b> if this <code>WSLabel</code> allows text <code>wrap</code><br />
          <b>false</b> if this <code>WSLabel</code> does not allow text <code>wrap</code>
  ***********************************************************************************************/
  public void setWrap(boolean wrap) {
    this.wrap = wrap;
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    // Sets the generic properties of this component
    WSHelper.setAttributes(node, this);

    int align = WSHelper.parseAlignment(node.getAttribute("horizontal-alignment"));
    setHorizontalAlignment(align);
    setHorizontalTextPosition(align);

    align = WSHelper.parseAlignment(node.getAttribute("vertical-alignment"));
    setVerticalAlignment(align);
    setVerticalTextPosition(align);

    String wrap = node.getAttribute("wrap");
    if (wrap != null && wrap.equals("true")) {
      setWrap(true);
    }

    String tag = node.getAttribute("showBorder");
    if (tag != null) {
      setShowBorder(WSHelper.parseBoolean(tag));
    }

    setForeground(LookAndFeelManager.getTextColor());

    setIcons();
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

    String horizontalAlignment = WSHelper.parseAlignment(getHorizontalAlignment());
    if (!horizontalAlignment.equals("center")) {
      node.setAttribute("horizontal-alignment", horizontalAlignment);
    }

    String verticalAlignment = WSHelper.parseAlignment(getVerticalAlignment());
    if (!verticalAlignment.equals("center")) {
      node.setAttribute("vertical-alignment", verticalAlignment);
    }

    if (wrap) {
      node.setAttribute("wrap", "true");
    }

    if (getShowBorder()) {
      node.addAttribute("showBorder", "true");
    }

    return node;
  }
}