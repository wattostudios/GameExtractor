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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.watto.ErrorLogger;
import org.watto.component.layout.CenteredLayout;
import org.watto.component.layout.LayeredLayout;
import org.watto.component.layout.RelativeLayout;
import org.watto.component.layout.ReverseBorderLayout;
import org.watto.event.WSEvent;
import org.watto.event.WSEventHandler;
import org.watto.event.WSEventableInterface;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;

/***********************************************************************************************
 * A Panel GUI <code>Component</code>
 ***********************************************************************************************/

public class WSPanel extends JPanel implements WSComponent, WSEventableInterface {

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

  /** Whether to paint a solid background on this <code>WSComponent</code> or not **/
  boolean paintBackground = true;

  /** Whether to paint a solid background on this <code>WSComponent</code> using the Color set by setBackground() **/
  boolean obeyBackgroundColor = false;

  /**
   * Whether this <code>WSComponent</code> is initialised or not. Used to determine whether to
   * fire events.
   **/
  boolean initialised = false;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSPanel() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSPanel(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
   * Adds a <code>Component</code> to this <code>WSPanel</code>, making sure that the z-indexes
   * are correct so that added <code>Component</code>s are painted on top of existing
   * <code>Component</code>s
   * @param child the <code>Component</code> to add
   * @return the added <code>Component</code>
   ***********************************************************************************************/
  @Override
  public Component add(Component child) {
    //if (getLayout() instanceof LayeredLayout) {
    //  return add(child, 0);
    //}
    return super.add(child);
  }

  /***********************************************************************************************
   * Changes the <code>Border</code> of the <code>WSPanel</code> if the <code>showBorder</code>
   * or <code>showLabel</code> has been changed
   ***********************************************************************************************/
  private void changeBorder() {
    int borderPadding = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH") + 2;

    if (borderWidth != -1) {
      borderPadding += borderWidth;
    }

    int n = 0;
    int s = 0;
    int e = 0;
    int w = 0;

    if (showBorder) {
      n = borderPadding;
      s = borderPadding;
      e = borderPadding;
      w = borderPadding;
    }
    if (showLabel) {
      n += LookAndFeelManager.getTextHeight() + borderPadding + borderPadding - 2;
      if (!showBorder) {
        n += borderPadding - 1;
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
    int borderPadding = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH") + 1;

    if (borderWidth != -1) {
      borderPadding += borderWidth;
    }

    int n = 0;
    int s = 0;
    int e = 0;
    int w = 0;

    if (showBorder) {
      n = borderPadding;
      s = borderPadding;
      e = borderPadding;
      w = borderPadding;
    }
    if (showLabel) {
      n += LookAndFeelManager.getTextHeight() + borderPadding - 2;
      if (!showBorder) {
        n += borderPadding - 1;
      }
      else {
        s += 2;

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

  public boolean getPaintBackground() {
    return paintBackground;
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
    Dimension preferredSize = super.getPreferredSize();

    Dimension minimumSize = super.getMinimumSize();
    if (fixedMinimumWidth && preferredSize.width < minimumSize.width) {
      preferredSize.width = minimumSize.width;
    }
    if (fixedMinimumHeight && preferredSize.height < minimumSize.height) {
      preferredSize.height = minimumSize.height;
    }

    return increaseSize(preferredSize);
  }

  /***********************************************************************************************
   * Gets whether this <code>WSComponent</code> has a visible <code>Border</code> or not
   * @return <b>true</b> if the <code>Border</code> is visible<br />
   *         <b>false</b> if the <code>Border</code> is not visible
   ***********************************************************************************************/
  public boolean getShowBorder() {
    return showBorder;
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
  @SuppressWarnings("unused")
  private Dimension increaseSize(Dimension size) {
    double w = size.getWidth();
    double h = size.getHeight();

    // still need this method so that it gets the height correctly before it is actually set in setShowLabel
    if (showLabel) {
      int borderPadding = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
      //h += LookAndFeelManager.getTextHeight() - borderPadding - borderPadding - 1;
      //h += LookAndFeelManager.getTextHeight() + borderPadding + borderPadding + 1;
    }
    if (showBorder) {
      int pad = LookAndFeelManager.getPropertyInt("MENU_BORDER_WIDTH");
      //h += pad;
      w += pad + pad;
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

  public boolean obeyBackgroundColor() {
    return obeyBackgroundColor;
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
      changeBorder();
    }
    else {
      return false;
    }
    return true;
  }

  /***********************************************************************************************
   * Sets the properties of a <code>BorderLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>BorderLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseBorderLayout(BorderLayout layout, XMLNode node) {
    int hGap = layout.getHgap();
    if (hGap > 0) {
      node.setAttribute("horizontal-gap", "" + hGap);
    }

    int vGap = layout.getVgap();
    if (vGap > 0) {
      node.setAttribute("vertical-gap", "" + vGap);
    }
  }

  /***********************************************************************************************
   * Sets the properties of a <code>CenteredLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>CenteredLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseCenteredLayout(CenteredLayout layout, XMLNode node) {
    boolean fillWidth = layout.getFillWidth();
    if (fillWidth) {
      node.setAttribute("fill-width", "true");
    }

    boolean fillHeight = layout.getFillHeight();
    if (fillHeight) {
      node.setAttribute("fill-height", "true");
    }
  }

  /***********************************************************************************************
   * Sets the properties of a <code>FlowLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>FlowLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseFlowLayout(FlowLayout layout, XMLNode node) {
    int hGap = layout.getHgap();
    if (hGap > 0) {
      node.setAttribute("horizontal-gap", "" + hGap);
    }

    int vGap = layout.getVgap();
    if (vGap > 0) {
      node.setAttribute("vertical-gap", "" + vGap);
    }

    node.setAttribute("alignment", "" + layout.getAlignment());
  }

  /***********************************************************************************************
   * Sets the properties of a <code>GridLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>GridLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseGridLayout(GridLayout layout, XMLNode node) {
    int hGap = layout.getHgap();
    if (hGap > 0) {
      node.setAttribute("horizontal-gap", "" + hGap);
    }

    int vGap = layout.getVgap();
    if (vGap > 0) {
      node.setAttribute("vertical-gap", "" + vGap);
    }

    node.setAttribute("rows", "" + layout.getRows());
    node.setAttribute("columns", "" + layout.getColumns());
  }

  /***********************************************************************************************
   * Sets the properties of a <code>LayeredLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>LayeredLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseLayeredLayout(LayeredLayout layout, XMLNode node) {

  }

  /***********************************************************************************************
   * Sets the properties of a <code>RelativeLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>RelativeLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseRelativeLayout(RelativeLayout layout, XMLNode node) {
    boolean fillWidth = layout.getFillWidth();
    if (fillWidth) {
      node.setAttribute("fill-width", "true");
    }

    boolean fillHeight = layout.getFillHeight();
    if (fillHeight) {
      node.setAttribute("fill-height", "true");
    }

    node.setAttribute("xPosition", "" + layout.getXPosition());
    node.setAttribute("yPosition", "" + layout.getYPosition());

  }

  /***********************************************************************************************
   * Sets the properties of a <code>ReverseBorderLayout</code> <code>layout</code> on an
   * <code>XMLNode</code>
   * @param layout the <code>ReverseBorderLayout</code> to parse
   * @param node the <code>XMLNode</code> to set the properties on
   ***********************************************************************************************/
  private void parseReverseBorderLayout(ReverseBorderLayout layout, XMLNode node) {
    int hGap = layout.getHorizontalGap();
    if (hGap > 0) {
      node.setAttribute("horizontal-gap", "" + hGap);
    }

    int vGap = layout.getVerticalGap();
    if (vGap > 0) {
      node.setAttribute("vertical-gap", "" + vGap);
    }
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
    enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.HIERARCHY_EVENT_MASK | AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK | WSComponent.WS_EVENT_MASK);
  }

  /***********************************************************************************************
   * Sets the layout of this <code>WSPanel</code> to be a <code>BorderLayout</code>
   * @param node the <code>XMLNode</code> with the <code>BorderLayout</code> properties
   ***********************************************************************************************/
  private void setBorderLayout(XMLNode node) {
    BorderLayout layout = new BorderLayout();

    String tag;

    tag = node.getAttribute("horizontal-gap");
    if (tag != null) {
      layout.setHgap(Integer.parseInt(tag));
    }

    tag = node.getAttribute("vertical-gap");
    if (tag != null) {
      layout.setVgap(Integer.parseInt(tag));
    }

    setLayout(layout);

    // add the children to the correct positions
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        XMLNode child = node.getChild(i);
        tag = child.getAttribute("position");
        add(WSHelper.toComponent(child), WSHelper.parsePosition(tag, true));
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
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
   * Sets the layout of this <code>WSPanel</code> to be a <code>CenteredLayout</code>
   * @param node the <code>XMLNode</code> with the <code>CenteredLayout</code> properties
   ***********************************************************************************************/
  private void setCenteredLayout(XMLNode node) {
    CenteredLayout layout = new CenteredLayout();

    String tag;

    tag = node.getAttribute("fill-width");
    if (tag != null && tag.equals("true")) {
      layout.setFillWidth(true);
    }

    tag = node.getAttribute("fill-height");
    if (tag != null && tag.equals("true")) {
      layout.setFillHeight(true);
    }

    setLayout(layout);

    // add the children layers
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        JComponent childComponent = WSHelper.toComponent(node.getChild(i));
        add(childComponent);
        // makes sure that the later components appear on top of the earlier components
        //setComponentZOrder(childComponent,numChildren-1-i);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
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
   * Sets the layout of this <code>WSPanel</code> to be a <code>FlowLayout</code>
   * @param node the <code>XMLNode</code> with the <code>FlowLayout</code> properties
   ***********************************************************************************************/
  private void setFlowLayout(XMLNode node) {
    FlowLayout layout = new FlowLayout();

    String tag;

    tag = node.getAttribute("alignment");
    if (tag != null) {
      layout.setAlignment(Integer.parseInt(tag));
    }

    tag = node.getAttribute("horizontal-gap");
    if (tag != null) {
      layout.setHgap(Integer.parseInt(tag));
    }

    tag = node.getAttribute("vertical-gap");
    if (tag != null) {
      layout.setVgap(Integer.parseInt(tag));
    }

    setLayout(layout);

    // add the children to the correct positions
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        add(WSHelper.toComponent(node.getChild(i)));
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
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
   * Sets the layout of this <code>WSPanel</code> to be a <code>GridLayout</code>
   * @param node the <code>XMLNode</code> with the <code>GridLayout</code> properties
   ***********************************************************************************************/
  private void setGridLayout(XMLNode node) {
    GridLayout layout = new GridLayout();

    String tag;

    tag = node.getAttribute("rows");
    if (tag != null) {
      layout.setRows(Integer.parseInt(tag));
    }

    tag = node.getAttribute("columns");
    if (tag != null) {
      layout.setColumns(Integer.parseInt(tag));
    }

    tag = node.getAttribute("horizontal-gap");
    if (tag != null) {
      layout.setHgap(Integer.parseInt(tag));
    }

    tag = node.getAttribute("vertical-gap");
    if (tag != null) {
      layout.setVgap(Integer.parseInt(tag));
    }

    setLayout(layout);

    // add the children to the correct positions
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        add(WSHelper.toComponent(node.getChild(i)));
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
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
   * Sets the layout of this <code>WSPanel</code> to be a <code>LayeredLayout</code>
   * @param node the <code>XMLNode</code> with the <code>LayeredLayout</code> properties
   ***********************************************************************************************/
  private void setLayeredLayout(XMLNode node) {
    LayeredLayout layout = new LayeredLayout();

    setLayout(layout);

    // add the children layers
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        JComponent childComponent = WSHelper.toComponent(node.getChild(i));
        add(childComponent);
        // makes sure that the later components appear on top of the earlier components
        //setComponentZOrder(childComponent,numChildren-1-i);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
  }

  public void setObeyBackgroundColor(boolean obeyBackgroundColor) {
    this.obeyBackgroundColor = obeyBackgroundColor;
  }

  public void setPaintBackground(boolean paintBackground) {
    this.paintBackground = paintBackground;
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
   * Sets the layout of this <code>WSPanel</code> to be a <code>RelativeLayout</code>
   * @param node the <code>XMLNode</code> with the <code>RelativeLayout</code> properties
   ***********************************************************************************************/
  private void setRelativeLayout(XMLNode node) {
    RelativeLayout layout = new RelativeLayout();

    String tag;

    tag = node.getAttribute("fill-width");
    if (tag != null && tag.equals("true")) {
      layout.setFillWidth(true);
    }

    tag = node.getAttribute("fill-height");
    if (tag != null && tag.equals("true")) {
      layout.setFillHeight(true);
    }

    tag = node.getAttribute("xPosition");
    if (tag != null) {
      try {
        layout.setXPosition(Integer.parseInt(tag));
      }
      catch (NumberFormatException nfe) {
        layout.setXPosition(0);
      }
    }

    tag = node.getAttribute("yPosition");
    if (tag != null) {
      try {
        layout.setYPosition(Integer.parseInt(tag));
      }
      catch (NumberFormatException nfe) {
        layout.setYPosition(0);
      }
    }

    setLayout(layout);

    // add the children layers
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        JComponent childComponent = WSHelper.toComponent(node.getChild(i));
        add(childComponent);
        // makes sure that the later components appear on top of the earlier components
        //setComponentZOrder(childComponent,numChildren-1-i);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
  }

  /***********************************************************************************************
   * Sets the layout of this <code>WSPanel</code> to be a <code>ReverseBorderLayout</code>
   * @param node the <code>XMLNode</code> with the <code>ReverseBorderLayout</code> properties
   ***********************************************************************************************/
  private void setReverseBorderLayout(XMLNode node) {
    ReverseBorderLayout layout = new ReverseBorderLayout();

    String tag;

    tag = node.getAttribute("horizontal-gap");
    if (tag != null) {
      layout.setHorizontalGap(Integer.parseInt(tag));
    }

    tag = node.getAttribute("vertical-gap");
    if (tag != null) {
      layout.setVerticalGap(Integer.parseInt(tag));
    }

    setLayout(layout);

    // add the children to the correct positions
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      try {
        XMLNode child = node.getChild(i);
        tag = child.getAttribute("position");
        add(WSHelper.toComponent(child), WSHelper.parsePosition(tag, true));
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
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
    removeAll();

    // Sets the generic properties of this component
    WSHelper.setPreAttributes(node, this);

    String tag;

    tag = node.getAttribute("layout");
    if (tag == null || tag.equals("BorderLayout")) {
      this.layout = "BorderLayout";
      setBorderLayout(node);
    }
    else if (tag.equals("GridLayout")) {
      this.layout = "GridLayout";
      setGridLayout(node);
    }
    else if (tag.equals("FlowLayout")) {
      this.layout = "FlowLayout";
      setFlowLayout(node);
    }
    else if (tag.equals("LayeredLayout")) {
      this.layout = "LayeredLayout";
      setLayeredLayout(node);
    }
    else if (tag.equals("ReverseBorderLayout")) {
      this.layout = "ReverseBorderLayout";
      setReverseBorderLayout(node);
    }
    else if (tag.equals("CenteredLayout")) {
      this.layout = "CenteredLayout";
      setCenteredLayout(node);
    }
    else if (tag.equals("RelativeLayout")) {
      this.layout = "RelativeLayout";
      setRelativeLayout(node);
    }
    else {
      try {
        throw new WSComponentException("Invalid layout specified on WSPanel: " + tag);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    // Sets the generic properties of this component
    WSHelper.setPostAttributes(node, this);

    tag = node.getAttribute("showLabel");
    if (tag != null) {
      setShowLabel(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("showBorder");
    if (tag != null) {
      setShowBorder(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("paintBackground");
    if (tag != null) {
      setPaintBackground(WSHelper.parseBoolean(tag));
    }

    tag = node.getAttribute("obeyBackgroundColor");
    if (tag != null) {
      setObeyBackgroundColor(WSHelper.parseBoolean(tag));
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
    node.setAttribute("layout", this.layout);

    if (layout.equals("BorderLayout")) {
      parseBorderLayout((BorderLayout) getLayout(), node);
    }
    else if (layout.equals("GridLayout")) {
      parseGridLayout((GridLayout) getLayout(), node);
    }
    else if (layout.equals("FlowLayout")) {
      parseFlowLayout((FlowLayout) getLayout(), node);
    }
    else if (layout.equals("LayeredLayout")) {
      parseLayeredLayout((LayeredLayout) getLayout(), node);
    }
    else if (layout.equals("ReverseBorderLayout")) {
      parseReverseBorderLayout((ReverseBorderLayout) getLayout(), node);
    }
    else if (layout.equals("CenteredLayout")) {
      parseCenteredLayout((CenteredLayout) getLayout(), node);
    }
    else if (layout.equals("RelativeLayout")) {
      parseRelativeLayout((RelativeLayout) getLayout(), node);
    }

    if (getShowLabel()) {
      node.addAttribute("showLabel", "true");
    }
    if (getShowBorder()) {
      node.addAttribute("showBorder", "true");
    }

    // add the children
    Component[] children = getComponents();
    int numChildren = children.length;

    for (int i = 0; i < numChildren; i++) {
      try {
        Component child = children[i];
        if (child instanceof WSComponent) {
          node.addChild(((WSComponent) child).toXML());
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    return node;
  }

  /***********************************************************************************************
   * Gets the top-most <code>Component</code> at the position (<code>x</code>,<code>y</code>)
   * @param x the x position
   * @param y the y position
   * @return the top-most <code>Component</code> at the position
   ***********************************************************************************************/
  /*
   * public Component locate(int x,int y){ if (!contains(x,y)) { return null; } synchronized
   * (getTreeLock()) { int componentCount = getComponentCount();
   *
   * // Two passes: see comment in sun.awt.SunGraphicsCallback for (int i = 0;i <
   * componentCount;i++) { Component child = getComponent(i); if (child != null) { if
   * (child.contains(x - child.getX(),y - child.getY())) { return child; } } } for (int i = 0;i <
   * componentCount;i++) { Component child = getComponent(i); if (child != null) { if
   * (child.contains(x - child.getX(),y - child.getY())) { return child; } } } } return this; }
   */

  /***********************************************************************************************
   * Paints the children of this <code>WSPanel</code>. If the panel uses
   * <code>LayeredLayout</code>, the layers are painted on top of each other, otherwise the
   * children are painted normally.
   * @param graphics the <code>Graphics</code> to paint on
   ***********************************************************************************************/
  /*
   * public void paintChildren(Graphics graphics){ if (!(getLayout() instanceof LayeredLayout)) {
   * super.paintChildren(graphics); return; }
   *
   * // Using a LayeredLayout, so paint the children on top of each other! synchronized
   * (getTreeLock()) { int childCount = getComponentCount(); for (int c = 0;c < childCount;c++) {
   * Component child = getComponent(c); Rectangle childBounds = child.getBounds();
   *
   * //int childHeight = child.getHeight(); //int childWidth = child.getWidth();
   *
   * Graphics childPaintArea =
   * graphics.create(childBounds.x,childBounds.y,childBounds.width,childBounds.height);
   * child.paint(childPaintArea); } }
   *
   * }
   */

}