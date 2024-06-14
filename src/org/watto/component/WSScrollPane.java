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
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import org.watto.ErrorLogger;
import org.watto.event.WSEvent;
import org.watto.event.WSEventHandler;
import org.watto.event.WSEventableInterface;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;

/***********************************************************************************************
 * A Scroll Pane GUI <code>Component</code>
 ***********************************************************************************************/

public class WSScrollPane extends JScrollPane implements WSComponent, WSEventableInterface {

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

  /** The layout used to position the children in this panel **/
  String layout = "";

  /** Whether to show a label on this <code>WSComponent</code> or not **/
  boolean showLabel = false;

  /** Whether to show a border on this <code>WSComponent</code> or not **/
  boolean showBorder = false;

  /**
   * Whether to show a border around the <code>WSComponent</code> in the main viewport, or not
   **/
  boolean showInnerBorder = false;

  /** Whether to show the background on this <code>WSScrollPane</code> or not **/
  boolean showBackground = false;

  /**
   * Whether this <code>WSComponent</code> is initialised or not. Used to determine whether to
   * fire events.
   **/
  boolean initialised = false;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSScrollPane() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSScrollPane(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
   * Changes the <code>Border</code> of the <code>WSPanel</code> if the <code>showBorder</code>
   * or <code>showLabel</code> has been changed
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

      if (showLabel) {
        borderPadding += 1;
        n = LookAndFeelManager.getTextHeight() + borderPadding + borderPadding + borderPadding / 2;
      }
    }

    // NOTE: This doesn't seem to work in Java 8 (border doesn't paint with proper sizes) - created method getInsets() instead!
    setBorder(new EmptyBorder(n, w, s, e));
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
   * Fires a <code>WSEvent</code> when the <code>showBorder</code> has changed
   ***********************************************************************************************/
  private void fireShowBorderChangedEvent() {
    if (!initialised) {
      return;
    }
    WSHelper.fireEvent(new WSEvent(this, WSEvent.SHOW_BORDER_CHANGED), this);
  }

  /***********************************************************************************************
   * Fires a <code>WSEvent</code> when the <code>showBorder</code> has changed
   ***********************************************************************************************/
  private void fireShowLabelChangedEvent() {
    if (!initialised) {
      return;
    }
    WSHelper.fireEvent(new WSEvent(this, WSEvent.SHOW_LABEL_CHANGED), this);
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
   * Alters the insets based on the ShowBorder and ShowLabel properties
   ***********************************************************************************************/
  @Override
  public Insets getInsets() {
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

      if (showLabel) {
        borderPadding += 1;
        n = LookAndFeelManager.getTextHeight() + borderPadding + borderPadding + borderPadding / 2;
      }
    }
    else {
      if (showLabel) {
        //borderPadding += 1;
        n = LookAndFeelManager.getTextHeight() + borderPadding;
      }
    }

    return new Insets(n, w, s, e);
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> label for this <code>WSComponent</code>
   * @return the label for this <code>WSComponent</code>
   ***********************************************************************************************/
  public String getLabel() {
    return WSHelper.getLabel(this);
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
   * Gets the maximum size of this <code>WSComponent</code>
   * @return the maximum size
   ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    return increaseSize(super.getMaximumSize());
  }

  /***********************************************************************************************
   * Gets the minimum size of this <code>WSComponent</code>
   * @return the minimum size
   ***********************************************************************************************/
  @Override
  public Dimension getMinimumSize() {
    return increaseSize(super.getMinimumSize());
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
   * Gets the preferred size of this <code>WSComponent</code>
   * @return the preferred size
   ***********************************************************************************************/
  @Override
  public Dimension getPreferredSize() {
    return increaseSize(super.getPreferredSize());
  }

  /***********************************************************************************************
   * Gets whether this <code>WSComponent</code> has a painted background or not
   * @return <b>true</b> if the background is painted<br />
   *         <b>false</b> if the background is not painted
   ***********************************************************************************************/
  public boolean getShowBackground() {
    return showBackground;
  }

  /***********************************************************************************************
   * Gets whether this <code>WSComponent</code> has a visible <code>Border</code> or not
   * @return <b>true</b> if the <code>Border</code> is visible<br />
   *         <b>false</b> if the <code>Border</code> is not visible
   ***********************************************************************************************/
  public boolean getShowBorder() {
    return showBorder;
  }

  public boolean getShowInnerBorder() {
    return showInnerBorder;
  }

  /***********************************************************************************************
   * Gets whether this <code>WSComponent</code> has a visible label or not
   * @return <b>true</b> if the label is visible<br />
   *         <b>false</b> if the label is not visible
   ***********************************************************************************************/
  public boolean getShowLabel() {
    return showLabel;
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
   * Gets the <code>Language</code> tooltip text of this <code>WSComponent</code>
   * @return the tooltip text
   ***********************************************************************************************/
  @Override
  public String getToolTipText() {
    return WSHelper.getToolTipText(this);
  }

  /***********************************************************************************************
   * Adjusts the <code>Dimension</code> for the <code>size</code> of this
   * <code>WSComponent</code>, to take into account the <code>showBorder</code> and
   * <code>showLabel</code>
   * @param size the <code>Dimension</code> to adjust
   * @return the adjusted <code>Dimension</code>
   ***********************************************************************************************/
  private Dimension increaseSize(Dimension size) {
    double w = size.getWidth();
    double h = size.getHeight();

    // still need this method so that it gets the height correctly before it is actually set in setShowLabel
    if (showLabel) {
      h += LookAndFeelManager.getTextHeight();
    }
    if (showBorder) {
      int borderWidth = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
      h += borderWidth;
      w += borderWidth + borderWidth;
    }

    size.setSize(w, h);
    return size;
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
   * Performs an action when a <code>WSEvent</code> event is triggered
   * @param source the <code>Object</code> that triggered the event
   * @param event the <code>WSEvent</code>
   * @param type the events type ID
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onEvent(Object source, WSEvent event, int type) {
    if (type == WSEvent.SHOW_BORDER_CHANGED) {
      changeBorder();
    }
    else if (type == WSEvent.SHOW_LABEL_CHANGED) {
      int borderWidth = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH") + 1;
      if (showLabel) {
        setBorder(new EmptyBorder(LookAndFeelManager.getTextHeight() + borderWidth + borderWidth + borderWidth / 2, borderWidth, borderWidth, borderWidth));
      }
      else {
        setBorder(new EmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));
      }
    }
    else {
      return false;
    }
    return true;
  }

  /***********************************************************************************************
   * Processes an <code>event</code> that was triggered on this <code>WSComponent</code>
   * @param event the <code>AWTEvent</code> that was triggered
   ***********************************************************************************************/
  @Override
  public void processEvent(AWTEvent event) {
    super.processEvent(event); // handles any normal listeners
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }

  /***********************************************************************************************
   * Registers the <code>AWTEvent</code>s that this <code>WSComponent</code> generates
   ***********************************************************************************************/
  @Override
  public void registerEvents() {
    enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.HIERARCHY_EVENT_MASK | //Children changes (add/remove, show/hide)
        AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK | //Children changes  (size)
        AWTEvent.MOUSE_WHEEL_EVENT_MASK | WSComponent.WS_EVENT_MASK);
  }

  /***********************************************************************************************
   * Sets the border width attribute value. <i>This does not actually set the width of the
   * border!</i>
   * @param borderWidth the new border width attribute value
   ***********************************************************************************************/
  @Override
  public void setBorderWidth(int borderWidth) {
    this.borderWidth = borderWidth;
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
   * Sets the position attribute value. <i>This does not actually set the position of the
   * <code>WSComponent</code>!</i>
   * @param position the new position attribute value
   ***********************************************************************************************/
  @Override
  public void setPosition(String position) {
    this.position = position;
  }

  /***********************************************************************************************
   * Sets whether this <code>WSComponent</code> has a painted background or not
   * @param showBackground <b>true</b> if the background is painted<br />
   *        <b>false</b> if the background is not painted
   ***********************************************************************************************/
  public void setShowBackground(boolean showBackground) {
    this.showBackground = showBackground;
  }

  /***********************************************************************************************
   * Sets whether this <code>WSComponent</code> has a visible <code>Border</code> or not
   * @param showBorder <b>true</b> if the <code>Border</code> is visible<br />
   *        <b>false</b> if the <code>Border</code> is not visible
   ***********************************************************************************************/
  public void setShowBorder(boolean showBorder) {
    this.showBorder = showBorder;
    fireShowBorderChangedEvent();
  }

  public void setShowInnerBorder(boolean showInnerBorder) {
    this.showInnerBorder = showInnerBorder;
  }

  /***********************************************************************************************
   * Sets whether this <code>WSComponent</code> has a visible label or not
   * @param showLabel <b>true</b> if the label is visible<br />
   *        <b>false</b> if the label is not visible
   ***********************************************************************************************/
  public void setShowLabel(boolean showLabel) {
    this.showLabel = showLabel;
    fireShowLabelChangedEvent();
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

    String tag;

    tag = node.getAttribute("horizontal-scrollbar");
    if (tag != null) {
      setHorizontalScrollBarPolicy(WSHelper.parseHorizontalScrollBarPolicy(tag));
    }

    tag = node.getAttribute("vertical-scrollbar");
    if (tag != null) {
      setVerticalScrollBarPolicy(WSHelper.parseVerticalScrollBarPolicy(tag));
    }

    tag = node.getAttribute("showLabel");
    if (tag != null) {
      setShowLabel(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("showBorder");
    if (tag != null) {
      this.showBorder = WSHelper.parseBoolean(tag);
      changeBorder();
    }

    tag = node.getAttribute("showInnerBorder");
    if (tag != null) {
      this.showInnerBorder = WSHelper.parseBoolean(tag);
    }

    tag = node.getAttribute("showBackground");
    if (tag != null) {
      setShowBackground(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("opaque");
    if (tag != null) {
      boolean opaque = WSHelper.parseBoolean(tag);
      viewport.setOpaque(opaque);
      horizontalScrollBar.setOpaque(opaque);
      verticalScrollBar.setOpaque(opaque);
    }

    Component scrollComponent = null;
    if (node.getChildCount() > 0) {
      try {
        scrollComponent = WSHelper.toComponent(node.getChild(0));
        setViewportView(scrollComponent);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    // if the horizontal/vertical scrollbar is disabled,
    // set the viewport to have a maximum size of the actual scrollpane.
    if (scrollComponent != null) {
      if (getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER || getVerticalScrollBarPolicy() == VERTICAL_SCROLLBAR_NEVER) {
        getViewport().getView().setPreferredSize(getSize());
      }
    }

    //int pad = AquanauticTheme.BORDER_WIDTH;
    //setViewportBorder(new EmptyBorder(pad,pad,pad,pad));

    tag = node.getAttribute("viewport-border-width");
    if (tag != null) {
      int borderSize = WSHelper.parseInt(tag);
      setViewportBorder(new EmptyBorder(borderSize, borderSize, borderSize, borderSize));
    }
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

    node.addAttribute("horizontal-scrollbar", "" + WSHelper.parseHorizontalScrollBarPolicy(getHorizontalScrollBarPolicy()));
    node.addAttribute("vertical-scrollbar", "" + WSHelper.parseVerticalScrollBarPolicy(getVerticalScrollBarPolicy()));

    if (getShowLabel()) {
      node.addAttribute("showLabel", "true");
    }
    if (getShowBorder()) {
      node.addAttribute("showBorder", "true");
    }
    if (getShowInnerBorder()) {
      node.addAttribute("showInnerBorder", "true");
    }
    if (getShowBackground()) {
      node.addAttribute("showBackground", "true");
    }

    // add the children
    try {
      Component child = getViewport().getView();
      if (child instanceof WSComponent) {
        node.addChild(((WSComponent) child).toXML());
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    return node;
  }
}