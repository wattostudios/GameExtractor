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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.component.timer.WSButton_HoverTimer;
import org.watto.event.WSEventHandler;
import org.watto.event.WSHoverableInterface;
import org.watto.event.listener.WSHoverableListener;
import org.watto.xml.XMLNode;

/***********************************************************************************************
 * A Button GUI <code>Component</code>
 ***********************************************************************************************/

public class WSButton extends JButton implements WSComponent, WSHoverableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** Normal text orientation **/
  public static final int ORIENTATION_UP = 0;
  /** Upside-down text orientation **/
  public static final int ORIENTATION_DOWN = 1;
  /** Left text orientation (rotated 90 degrees counter-clockwise) **/
  public static final int ORIENTATION_LEFT = 2;
  /** Right text orientation (rotated 90 degrees clockwise) **/
  public static final int ORIENTATION_RIGHT = 3;
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
   * Is this button already processing a button click? This is so that doClick() and
   * processEvent() don't interfere with each other.
   **/
  boolean doingClick = false;

  /** The way the button text is facing (the text orientation) **/
  int textOrientation = ORIENTATION_UP;

  WSButton_HoverTimer hoverTimer = null;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  WSButton() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSButton(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
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
   * This happens when the user presses Enter on the button. Just process the same as a standard
   * onClick() event, as if it were clicked by the mouse.
   * @param holdTime the time that the button should be held down for
   ***********************************************************************************************/
  @Override
  public void doClick(int holdTime) {
    super.doClick(holdTime);
    if (!doingClick) {
      WSEventHandler.processEvent(this, new MouseEvent(this, MouseEvent.MOUSE_RELEASED, 0, 0, 0, 0, 1, false, MouseEvent.BUTTON1));
    }
    doingClick = false;
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
   * Gets the maximum size of this <code>WSComponent</code>
   * @return the maximum size
   ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    if (textOrientation == ORIENTATION_LEFT || textOrientation == ORIENTATION_RIGHT) {
      // swap the width and the height around, as this button is up-down rather than left-right
      Dimension dimension = super.getMaximumSize();
      dimension = new Dimension(dimension.height, dimension.width);
      return dimension;
    }
    else {
      return super.getMaximumSize();
    }
  }

  /***********************************************************************************************
   * Gets the minimum size of this <code>WSComponent</code>
   * @return the minimum size
   ***********************************************************************************************/
  @Override
  public Dimension getMinimumSize() {
    if (textOrientation == ORIENTATION_LEFT || textOrientation == ORIENTATION_RIGHT) {
      // swap the width and the height around, as this button is up-down rather than left-right
      Dimension dimension = super.getMinimumSize();
      dimension = new Dimension(dimension.height, dimension.width);
      return dimension;
    }
    else {
      return super.getMinimumSize();
    }
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
    if (textOrientation == ORIENTATION_LEFT || textOrientation == ORIENTATION_RIGHT) {
      // swap the width and the height around, as this button is up-down rather than left-right
      Dimension dimension = super.getPreferredSize();
      dimension = new Dimension(dimension.height, dimension.width);
      return dimension;
    }
    else {
      return super.getPreferredSize();
    }
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
   * Gets the orientation of the text of this <code>WSComponent</code>
   * @return the <code>textOrientation</code>
   ***********************************************************************************************/
  public int getTextOrientation() {
    return textOrientation;
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
   * Start the hoverTimer
   ***********************************************************************************************/
  @Override
  public boolean onHover(JComponent source, MouseEvent event) {
    if (hoverTimer == null) {
      // GE 3.01 - DISABLED because it interrupts other popups (and no longer shows in the statusbar)
      //hoverTimer = new WSButton_HoverTimer(this);
      //new Thread(hoverTimer).start();
      //return true;
    }
    return false;
  }

  /***********************************************************************************************
   * Stop the hoverTimer
   ***********************************************************************************************/
  @Override
  public boolean onHoverOut(JComponent source, MouseEvent event) {
    if (hoverTimer != null) {
      // GE 3.01 - DISABLED because it interrupts other popups (and no longer shows in the statusbar)
      //hoverTimer.stop();
      //hoverTimer = null;
      //return true;
    }
    return false;
  }

  /***********************************************************************************************
   * Processes an <code>event</code> that was triggered on this <code>WSComponent</code>
   * @param event the <code>AWTEvent</code> that was triggered
   ***********************************************************************************************/
  @Override
  public void processEvent(AWTEvent event) {
    doingClick = false;
    if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_RELEASED) {
      doingClick = true;
    }

    if (event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED && ((KeyEvent) event).getKeyChar() == KeyEvent.VK_ENTER) {
      // trigger a button click when pressing enter
      doClick();
      return;
    }

    super.processEvent(event); // handles any normal listeners
    WSEventHandler.processEvent(this, event); // passes events to the caller
  }

  /***********************************************************************************************
   * Registers the <code>AWTEvent</code>s that this <code>WSComponent</code> generates
   ***********************************************************************************************/
  @Override
  public void registerEvents() {
    enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK | WSComponent.WS_EVENT_MASK);
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
   * Loads the icons for the button, using the <code>code</code> to get the icon
   * <code>File</code>s
   ***********************************************************************************************/
  public void setIcons() {
    String className = WSHelper.getClassName(this);

    String buttonCode = code;
    if (Settings.getBoolean("LargeButtonIcons")) {
      buttonCode += "_big";
    }

    //URL normalIcon = WSHelper.getResource("images/" + className + "/" + buttonCode + "_n.png");
    //if (normalIcon != null) {
    //  setIcon(new ImageIcon(normalIcon));
    //}
    //else {
    //  normalIcon = WSHelper.getResource("images/" + className + "/" + buttonCode + "_n.gif");
    //  if (normalIcon != null) {
    //    setIcon(new ImageIcon(normalIcon));
    //  }
    //  else {
    //    ErrorLogger.log("Missing Image Icon: " + className + "/" + buttonCode);
    //  }
    //}
    //
    //URL disabledIcon = WSHelper.getResource("images/" + className + "/" + buttonCode + "_d.gif");
    //if (disabledIcon != null) {
    //  setDisabledIcon(new ImageIcon(disabledIcon));
    //}
    //
    //URL hoverIcon = WSHelper.getResource("images/" + className + "/" + buttonCode + "_h.gif");
    //if (hoverIcon != null) {
    //  setRolloverIcon(new ImageIcon(hoverIcon));
    //  setRolloverEnabled(true);
    //}

    File normalIcon = new File("images/" + className + "/" + buttonCode + "_n.png");
    if (normalIcon.exists()) {
      setIcon(new ImageIcon(normalIcon.getAbsolutePath()));
    }
    else {
      normalIcon = new File("images/" + className + "/" + buttonCode + "_n.gif");
      if (normalIcon.exists()) {
        setIcon(new ImageIcon(normalIcon.getAbsolutePath()));
      }
      else {
        if (Settings.getBoolean("DebugMode")) {
          ErrorLogger.log("Missing Image Icon: " + className + "/" + code);
        }
      }
    }

    File disabledIcon = new File("images/" + className + "/" + buttonCode + "_d.png");
    if (disabledIcon.exists()) {
      setDisabledIcon(new ImageIcon(disabledIcon.getAbsolutePath()));
    }

    File hoverIcon = new File("images/" + className + "/" + buttonCode + "_h.png");
    if (hoverIcon.exists()) {
      setRolloverIcon(new ImageIcon(hoverIcon.getAbsolutePath()));
      setRolloverEnabled(true);
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
   * Sets the orientation of the text of this <code>WSComponent</code>
   * @param textOrientation the orientation of the text
   ***********************************************************************************************/
  public void setTextOrientation(int textOrientation) {
    this.textOrientation = textOrientation;
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

    setIcons();

    if (node.getAttribute("opaque") == null) {
      setOpaque(false);
    }

    String tag = node.getAttribute("showText");
    if (tag != null && tag.equals("false")) {
      WSHelper.setText(this, "");
    }

    tag = node.getAttribute("text-orientation");
    if (tag != null) {
      if (tag.equalsIgnoreCase("up")) {
        setTextOrientation(ORIENTATION_UP);
      }
      else if (tag.equalsIgnoreCase("down")) {
        setTextOrientation(ORIENTATION_DOWN);
      }
      else if (tag.equalsIgnoreCase("left")) {
        setTextOrientation(ORIENTATION_LEFT);
      }
      else if (tag.equalsIgnoreCase("right")) {
        setTextOrientation(ORIENTATION_RIGHT);
      }
      else {
        setTextOrientation(ORIENTATION_UP);
      }
    }

    // Add the HoverListener for the HoverTimer
    addMouseListener(new WSHoverableListener(this));
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

    String text = getText();
    if (text.equals("")) {
      node.setAttribute("showText", "false");
    }

    if (textOrientation == ORIENTATION_DOWN) {
      node.setAttribute("text-orientation", "down");
    }
    else if (textOrientation == ORIENTATION_LEFT) {
      node.setAttribute("text-orientation", "left");
    }
    else if (textOrientation == ORIENTATION_RIGHT) {
      node.setAttribute("text-orientation", "right");
    }

    return node;
  }

}