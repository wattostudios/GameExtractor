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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.io.File;
import java.net.URL;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.event.WSEvent;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * Utility methods for <code>WSComponent</code>s
 ***********************************************************************************************/
public class WSHelper {

  /** the class that defines the root directory to locate images etc. **/
  @SuppressWarnings("rawtypes")
  static Class resourcePath = null;

  /***********************************************************************************************
   * Compares the <code>getText()</code> of the <code>mainComponent</code> to the
   * <code>comparedComponent</code>
   * @param mainComponent the main <code>WSComponent</code>
   * @param comparedComponent the <code>WSComponent</code> to compare against
   * @return <b>0</b> if the 2 components are the same<br />
   *         <b>1</b> if the <code>mainComponent</code> comes before the
   *         <code>comparedComponent</code><br />
   *         <b>-1</b> if the <code>mainComponent</code> comes after the
   *         <code>comparedComponent</code>
   ***********************************************************************************************/
  public static int compare(WSComparable mainComponent, WSComparable comparedComponent) {
    if (mainComponent instanceof WSComponent && comparedComponent instanceof WSComponent) {
      return ((WSComponent) mainComponent).getText().compareTo(((WSComponent) comparedComponent).getText());
    }
    return mainComponent.getName().compareTo(comparedComponent.getName());
  }

  /***********************************************************************************************
   * Builds the default version of this <code>WSComponent</code> using the <i>classname</i>.xml
   * file
   * @param classname the <code>Class</code> name of the <code>WSComponent</code> to create
   * @return the <code>WSComponent</code>
   ***********************************************************************************************/
  public static WSComponent create(String classname) {
    try {
      return (WSComponent) toComponent(XMLReader.read(new File(ClassLoader.getSystemResource("xml" + File.separator + classname + ".xml").toURI())));
    }
    catch (Throwable t) {
      return new WSPanel(XMLReader.read("<WSPanel />"));
    }
  }

  /***********************************************************************************************
   * Fires an <code>event</code> on a <code>component</code>
   * @param event the event to be fired
   * @param component the <code>WSComponent</code> to fire the <code>event</code> on
   ***********************************************************************************************/
  public static void fireEvent(WSEvent event, WSComponent component) {
    component.processEvent(event);
  }

  /***********************************************************************************************
   * Gets the <code>Class</code> name of the <code>component</code>
   * @param component the <code>WSComponent</code> to get the name of
   * @return the <code>Class</code> name
   ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public static String getClassName(WSComponent component) {
    Class classObject = component.getClass();
    String name = classObject.getName();
    Package pack = classObject.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return name;
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> label of a <code>component</code>
   * @param component the <code>WSComponent</code> to get the label of
   * @return the label for the <code>component</code>
   ***********************************************************************************************/
  public static String getLabel(WSComponent component) {
    String code = component.getCode();
    if (code == null) {
      return "";
    }
    String langCode = getClassName(component) + "_" + code + "_Label";
    if (Language.has(langCode)) {
      return Language.get(langCode);
    }
    return "";
  }

  /***********************************************************************************************
   * Gets a resource such as a sound or image
   * @param path the path to the resource
   * @return the resource URL, or <b>null</b> if the resource can't be found
   ***********************************************************************************************/
  public static URL getResource(String path) {
    try {
      if (resourcePath != null) {
        return resourcePath.getResource(path);
      }
      else {
        return null;
      }
    }
    catch (Throwable t) {
      return null;
    }
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> small text of a <code>component</code>
   * @param component the <code>WSComponent</code> to get the small text of
   * @return the small text of the <code>component</code>
   ***********************************************************************************************/
  public static String getSmallText(WSComponent component) {
    String code = component.getCode();
    String langCode = getClassName(component) + "_" + code + "_Small";
    if (Language.has(langCode)) {
      return Language.get(langCode);
    }
    return getText(component);
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> text of a <code>component</code>
   * @param component the <code>WSComponent</code> to get the text of
   * @return the text of the <code>component</code>
   ***********************************************************************************************/
  public static String getText(WSComponent component) {
    String code = component.getCode();
    if (code == null) {
      return "";
    }
    String langCode = getClassName(component) + "_" + code + "_Text";
    if (Language.has(langCode)) {
      return Language.get(langCode);
    }
    return "";
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> tooltip text of a <code>component</code>
   * @param component the <code>WSComponent</code> to get the tooltip text of
   * @return the tooltip text of the <code>component</code>
   ***********************************************************************************************/
  public static String getToolTipText(WSComponent component) {
    String code = component.getCode();
    String langCode = getClassName(component) + "_" + code + "_Tooltip";
    if (Language.has(langCode)) {
      return Language.get(langCode);
    }
    return "";
  }

  /***********************************************************************************************
   * Gets the alignment <code>String</code> representation of the <code>SwingConstant</code>
   * alignment value
   * @param alignment the <code>SwingConstant</code> alignment value
   * @return a <code>String</code> for the alignment
   ***********************************************************************************************/
  public static String parseAlignment(int alignment) {
    if (alignment == SwingConstants.CENTER) {
      return "center";
    }
    else if (alignment == SwingConstants.LEFT) {
      return "left";
    }
    else if (alignment == SwingConstants.RIGHT) {
      return "right";
    }
    else if (alignment == SwingConstants.TOP) {
      return "top";
    }
    else if (alignment == SwingConstants.BOTTOM) {
      return "bottom";
    }
    else if (alignment == SwingConstants.LEADING) {
      return "leading";
    }
    else if (alignment == SwingConstants.TRAILING) {
      return "trailing";
    }
    else {
      return "center";
    }
  }

  /***********************************************************************************************
   * Gets the <code>SwingConstant</code> alignment value represented by the
   * <code>alignment</code> <code>String</code>
   * @param alignment a <code>String</code> for the alignment
   * @return the <code>SwingConstant</code> alignment value
   ***********************************************************************************************/
  public static int parseAlignment(String alignment) {
    if (alignment == null) {
      return SwingConstants.CENTER;
    }
    else if (alignment.equalsIgnoreCase("center")) {
      return SwingConstants.CENTER;
    }
    else if (alignment.equalsIgnoreCase("left")) {
      return SwingConstants.LEFT;
    }
    else if (alignment.equalsIgnoreCase("right")) {
      return SwingConstants.RIGHT;
    }
    else if (alignment.equalsIgnoreCase("top")) {
      return SwingConstants.TOP;
    }
    else if (alignment.equalsIgnoreCase("bottom")) {
      return SwingConstants.BOTTOM;
    }
    else if (alignment.equalsIgnoreCase("leading")) {
      return SwingConstants.LEADING;
    }
    else if (alignment.equalsIgnoreCase("trailing")) {
      return SwingConstants.TRAILING;
    }
    else {
      return SwingConstants.CENTER;
    }
  }

  /***********************************************************************************************
   * Gets the <code>boolean</code> represented by the <code>booleanValue</code>
   * <code>String</code>
   * @param booleanValue the <code>String</code> representation of a <code>boolean</code>
   * @return <b>true</b> if the <code>booleanValue</code> = "true"<br />
   *         <b>false</b> if the <code>booleanValue</code> = "false"
   ***********************************************************************************************/
  public static boolean parseBoolean(String booleanValue) {
    if (booleanValue.equalsIgnoreCase("true")) {
      return true;
    }
    else if (booleanValue.equalsIgnoreCase("false")) {
      return false;
    }
    else {
      return true;
    }
  }

  /***********************************************************************************************
   * Sets the <code>Border</code>s on the <code>Component</code>, where the border width is
   * represented by the <code>borderWidth</code> <code>String</code> value
   * @param borderWidth a numerical value for the border width
   * @param component the <code>WSComponent</code> to set the border width of
   * @throws NumberFormatException if the <code>borderWidth</code> is not a number
   ***********************************************************************************************/
  public static void parseBorderWidthAttribute(String borderWidth, WSComponent component) {
    try {
      if (borderWidth != null) {
        int size = Integer.parseInt(borderWidth);
        component.setBorder(new EmptyBorder(size, size, size, size));
        component.setBorderWidth(size);
      }
    }
    catch (Throwable t) {
      throw new NumberFormatException("The border-width value \"" + borderWidth + "\" is invalid");
    }
  }

  /***********************************************************************************************
   * Sets the <code>code</code> of the <code>component</code>
   * @param code the code value
   * @param component the <code>WSComponent</code> to set the border width of
   ***********************************************************************************************/
  public static void parseCodeAttribute(String code, WSComponent component) {
    if (code != null) {
      component.setCode(code);
    }
  }

  /***********************************************************************************************
   * Sets whether the <code>component</code> is enabled or not, where the enabled value is
   * represented by the <code>enabled</code> <code>String</code> value
   * @param enabled <b>"true"</b> if the <code>component</code> is enabled<br />
   *        <b>"false"</b> if the <code>component</code> is not enabled
   * @param component the <code>WSComponent</code> to set the enability of
   ***********************************************************************************************/
  public static void parseEnabledAttribute(String enabled, WSComponent component) {
    if (enabled != null) {
      component.setEnabled(parseBoolean(enabled));
    }
  }

  /***********************************************************************************************
   * Gets the policy <code>String</code> value representation for a
   * <code>ScrollPaneConstants</code> scrollbar policy value
   * @param policy the <code>ScrollPaneConstants</code> scrollbar policy value
   * @return a <code>String</code> for the scrollbar policy
   ***********************************************************************************************/
  public static String parseHorizontalScrollBarPolicy(int policy) {
    if (policy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS) {
      return "true";
    }
    else if (policy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
      return "false";
    }
    else {
      return "auto";
    }
  }

  /***********************************************************************************************
   * Gets the <code>ScrollPaneConstants</code> scrollbar policy value represented by the
   * <code>policy</code> <code>String</code>
   * @param policy a <code>String</code> for the scrollbar policy
   * @return the <code>ScrollPaneConstants</code> scrollbar policy value
   ***********************************************************************************************/
  public static int parseHorizontalScrollBarPolicy(String policy) {
    if (policy == null) {
      return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
    }
    else if (policy.equalsIgnoreCase("true")) {
      return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
    }
    else if (policy.equalsIgnoreCase("false")) {
      return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
    }
    else {
      return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
    }
  }

  /***********************************************************************************************
   * Gets the <code>int</code> represented by the <code>integerValue</code> <code>String</code>
   * @param integerValue the <code>String</code> representation of an <code>int</code>
   * @return the <code>int</code> value, or <b>-1</b> if the <code>integerValue</code> is not a
   *         number
   ***********************************************************************************************/
  public static int parseInt(String integerValue) {
    try {
      return Integer.parseInt(integerValue);
    }
    catch (Throwable t) {
      return -1;
    }
  }

  /***********************************************************************************************
   * Sets the minimum width and height of the <code>Component</code>, where the width and height
   * is represented by the <code>minimum-width</code> and <code>minimum-height</code>
   * <code>String</code> values
   * @param width a numerical value for the minimum width
   * @param height a numerical value for the minimum height
   * @param component the <code>WSComponent</code> to set the minimum width and height of
   * @throws NumberFormatException if the values are not numbers
   ***********************************************************************************************/
  public static void parseMinimumSizeAttribute(String width, String height, WSComponent component) {
    int h = -1;
    int w = -1;

    //System.out.println("want to set using " + width + " and " + height);

    if (width != null) {
      try {
        w = Integer.parseInt(width);
        component.setFixedMinimumWidth(true);
      }
      catch (Throwable t) {
        throw new NumberFormatException("The minimum width value \"" + width + "\" is invalid");
      }
    }

    if (height != null) {
      try {
        h = Integer.parseInt(height);
        component.setFixedMinimumHeight(true);
      }
      catch (Throwable t) {
        throw new NumberFormatException("The minimum height value \"" + height + "\" is invalid");
      }
    }

    Dimension d;
    if (h != -1 && w != -1) {
      d = component.getMaximumSize();
      //System.out.println("Old max size is " + d.height + "," + d.width);
      if (d.height < h) {
        d.height = h;
      }
      if (d.width < w) {
        d.width = w;
      }
      //System.out.println("Setting max size to " + d.height + "," + d.width);
      component.setMaximumSize(d);

      /*
       * d = component.getPreferredSize(); //System.out.println("Old preferred size is " +
       * d.height + "," + d.width); if (d.height < h){ d.height = h; } if (d.width < w){ d.width
       * = w; } //System.out.println("Setting preferred size to " + d.height + "," + d.width);
       * component.setPreferredSize(d);
       */

      //System.out.println("Setting minimum size to " + w + "," + h);
      component.setMinimumSize(new Dimension(w, h));

    }
    else if (h != -1) {
      d = component.getMaximumSize();
      if (d.height < h) {
        d.height = h;
      }
      component.setMaximumSize(d);

      /*
       * d = component.getPreferredSize(); if (d.height < h){ d.height = h; }
       * component.setPreferredSize(d);
       */

      d = component.getMinimumSize();
      d.height = h;
      component.setMinimumSize(d);
    }
    else if (w != -1) {
      d = component.getMaximumSize();
      if (d.width < w) {
        d.width = w;
      }
      component.setMaximumSize(d);

      /*
       * d = component.getPreferredSize(); if (d.width < w){ d.width = w; }
       * component.setPreferredSize(d);
       */

      d = component.getMinimumSize();
      d.width = w;
      component.setMinimumSize(d);
    }

  }

  /***********************************************************************************************
   * Gets the mnemonic <code>String</code> representation of a <code>KeyStroke</code> mnemonic
   * @param mnemonic a <code>KeyStroke</code> mnemonic
   * @return the <code>String</code> mnemonic value
   ***********************************************************************************************/
  public static String parseMnemonic(int mnemonic) {
    if (mnemonic <= 0) {
      return null;
    }

    try {
      return KeyStroke.getKeyStroke(mnemonic, 0).toString().substring(8, 9);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /***********************************************************************************************
   * Gets the <code>KeyStroke</code> mnemonic value represented by the <code>mnemonic</code>
   * <code>String</code>, such as <i>"A"</i>
   * @param mnemonic a <code>String</code> for the mnemonic
   * @return the <code>KeyStroke</code> mnemonic value
   ***********************************************************************************************/
  public static int parseMnemonic(String mnemonic) {
    if (mnemonic == null || mnemonic.length() != 1) {
      return 0;
    }

    try {
      return KeyStroke.getKeyStroke(mnemonic.toUpperCase()).getKeyCode();
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /***********************************************************************************************
   * Sets whether the <code>component</code> is opaque or not, where the opaque value is
   * represented by the <code>opaque</code> <code>String</code> value
   * @param opaque <b>"true"</b> if the <code>component</code> is opaque<br />
   *        <b>"false"</b> if the <code>component</code> is not opaque
   * @param component the <code>WSComponent</code> to set the opaqueness of
   ***********************************************************************************************/
  public static void parseOpaqueAttribute(String opaque, WSComponent component) {
    if (opaque != null) {
      component.setOpaque(parseBoolean(opaque));
    }
    else {
      component.setOpaque(true);
    }
  }

  /***********************************************************************************************
   * Gets the orientation <code>String</code> representation for the <code>SwingConstants</code>
   * orientation value
   * @param orientation the <code>SwingConstants</code> orientation value
   * @return a <code>String</code> for the orientation
   ***********************************************************************************************/
  public static String parseOrientation(int orientation) {
    if (orientation == SwingConstants.HORIZONTAL) {
      return "horizontal";
    }
    else if (orientation == SwingConstants.VERTICAL) {
      return "vertical";
    }
    else {
      return "horizontal";
    }
  }

  /***********************************************************************************************
   * Gets the <code>SwingConstants</code> orientation value represented by the
   * <code>orientation</code> <code>String</code>
   * @param orientation a <code>String</code> for the orientation
   * @return the <code>SwingConstants</code> orientation value
   ***********************************************************************************************/
  public static int parseOrientation(String orientation) {
    if (orientation.equalsIgnoreCase("horizontal")) {
      return SwingConstants.HORIZONTAL;
    }
    else if (orientation.equalsIgnoreCase("vertical")) {
      return SwingConstants.VERTICAL;
    }
    else {
      return SwingConstants.HORIZONTAL;
    }
  }

  /***********************************************************************************************
   * Gets the <code>BorderLayout</code> position value represented by the <code>position</code>
   * <code>String</code>, or converts the <code>BorderLayout</code> position value into a
   * <code>position</code> <code>String</code>. The direction of the conversion depends on the
   * <code>fromStringToJava</code> value
   * @param position a <code>String</code> for the position, or the <code>BorderLayout</code>
   *        position value
   * @param fromStringToJava <b>"true"</b> to convert a <code>position</code> <code>String</code>
   *        into a <code>BorderLayout</code> position value<br />
   *        <b>"false"</b> to convert a <code>BorderLayout</code> <code>position</code> value
   *        into a position <code>String</code>
   * @return the <code>BorderLayout</code> position value, or the <code>String</code>
   *         representation of a <code>BorderLayout</code> position
   ***********************************************************************************************/
  public static String parsePosition(String position, boolean fromStringToJava) {
    if (fromStringToJava) {
      if (position == null) {
        return BorderLayout.CENTER;
      }
      else if (position.equalsIgnoreCase("center")) {
        return BorderLayout.CENTER;
      }
      else if (position.equalsIgnoreCase("north")) {
        return BorderLayout.NORTH;
      }
      else if (position.equalsIgnoreCase("south")) {
        return BorderLayout.SOUTH;
      }
      else if (position.equalsIgnoreCase("east")) {
        return BorderLayout.EAST;
      }
      else if (position.equalsIgnoreCase("west")) {
        return BorderLayout.WEST;
      }
      else {
        return BorderLayout.CENTER;
      }
    }
    else {
      if (position == null) {
        return "center";
      }
      else if (position.equals(BorderLayout.CENTER)) {
        return "center";
      }
      else if (position.equals(BorderLayout.NORTH)) {
        return "north";
      }
      else if (position.equals(BorderLayout.SOUTH)) {
        return "south";
      }
      else if (position.equals(BorderLayout.EAST)) {
        return "east";
      }
      else if (position.equals(BorderLayout.WEST)) {
        return "west";
      }
      else {
        return "center";
      }
    }
  }

  /***********************************************************************************************
   * Sets whether the <code>component</code> is to be stored in the
   * <code>ComponentRepository</code> or not, where the repository value is represented by the
   * <code>repository</code> <code>String</code> value
   * @param repository <b>"true"</b> if the <code>component</code> is to be stored in the
   *        <code>ComponentRepository</code><br />
   *        <b>"false"</b> if the <code>component</code> is not to be stored in the
   *        <code>ComponentRepository</code>
   ***********************************************************************************************/
  public static void parseRepositoryAttribute(String repository, WSComponent component) {
    if (repository == null || !repository.equalsIgnoreCase("false")) {
      component.setInRepository(true);
      ComponentRepository.add(component);
    }
    else {
      component.setInRepository(false);
    }
  }

  /***********************************************************************************************
   * Gets the shortcut <code>String</code> representation of a <code>KeyStroke</code> shortcut
   * @param shortcut a <code>KeyStroke</code> shortcut
   * @return the <code>String</code> shortcut value
   ***********************************************************************************************/
  public static String parseShortcut(KeyStroke shortcut) {
    if (shortcut == null) {
      return null;
    }

    try {
      String code = "";

      int leftPos = 8;

      int modifiers = shortcut.getModifiers();
      if ((modifiers & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
        code += "shift ";
        leftPos += 6;
      }
      if ((modifiers & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) {
        code += "ctrl ";
        leftPos += 5;
      }
      if ((modifiers & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
        code += "alt ";
        leftPos += 4;
      }

      //code += shortcut.getKeyChar();
      //code += shortcut.toString().substring(8,9);
      code += shortcut.toString().substring(leftPos);

      return code;
    }
    catch (Throwable t) {
      return null;
    }
  }

  /***********************************************************************************************
   * Gets the <code>KeyStroke</code> shortcut represented by the <code>String</code>
   * <code>shortcut</code>, such as <i>"ctrl t"</i>
   * @param shortcut the <code>String</code> shortcut value
   * @return a <code>KeyStroke</code> shortcut
   ***********************************************************************************************/
  public static KeyStroke parseShortcut(String shortcut) {
    if (shortcut == null) {
      return null;
    }

    try {
      return KeyStroke.getKeyStroke(shortcut);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /***********************************************************************************************
   * Sets the width and height of the <code>Component</code>, where the width and height is
   * represented by the <code>width</code> and <code>height</code> <code>String</code> values
   * @param width a numerical value for the width
   * @param height a numerical value for the height
   * @param component the <code>WSComponent</code> to set the width and height of
   * @throws NumberFormatException if the values are not numbers
   ***********************************************************************************************/
  public static void parseSizeAttribute(String width, String height, WSComponent component) {
    int h = -1;
    int w = -1;

    if (width != null) {
      try {
        w = Integer.parseInt(width);
        component.setFixedWidth(true);
      }
      catch (Throwable t) {
        throw new NumberFormatException("The width value \"" + width + "\" is invalid");
      }
    }

    if (height != null) {
      try {
        h = Integer.parseInt(height);
        component.setFixedHeight(true);
      }
      catch (Throwable t) {
        throw new NumberFormatException("The height value \"" + height + "\" is invalid");
      }
    }

    Dimension d;
    if (h != -1 && w != -1) {
      d = new Dimension(w, h);
      component.setMaximumSize(d);
      component.setPreferredSize(d);
      component.setMinimumSize(d);
    }
    else if (h != -1) {
      d = component.getMaximumSize();
      d.height = h;
      component.setMaximumSize(d);

      d = component.getPreferredSize();
      d.height = h;
      component.setPreferredSize(d);

      d = component.getMinimumSize();
      d.height = h;
      component.setMinimumSize(d);
    }
    else if (w != -1) {
      d = component.getMaximumSize();
      d.width = w;
      component.setMaximumSize(d);

      d = component.getPreferredSize();
      d.width = w;
      component.setPreferredSize(d);

      d = component.getMinimumSize();
      d.width = w;
      component.setMinimumSize(d);
    }

  }

  /***********************************************************************************************
   * Gets the <code>String</code> representation for the <code>SwingConstants</code> tab
   * placement value
   * @param tabPlacement the <code>SwingConstants</code> tab placement value
   * @return a <code>String</code> for the tab placement
   ***********************************************************************************************/
  public static String parseTabPlacement(int tabPlacement) {
    if (tabPlacement == SwingConstants.TOP) {
      return "TOP";
    }
    else if (tabPlacement == SwingConstants.BOTTOM) {
      return "BOTTOM";
    }
    else if (tabPlacement == SwingConstants.LEFT) {
      return "LEFT";
    }
    else if (tabPlacement == SwingConstants.RIGHT) {
      return "RIGHT";
    }
    else {
      return "TOP";
    }
  }

  /***********************************************************************************************
   * Gets the <code>SwingConstants</code> tab placement value represented by the
   * <code>tabPlacement</code> <code>String</code>
   * @param tabPlacement a <code>String</code> for the tab placement
   * @return the <code>SwingConstants</code> tab placement value
   ***********************************************************************************************/
  public static int parseTabPlacement(String tabPlacement) {
    if (tabPlacement.equalsIgnoreCase("TOP")) {
      return SwingConstants.TOP;
    }
    else if (tabPlacement.equalsIgnoreCase("BOTTOM")) {
      return SwingConstants.BOTTOM;
    }
    else if (tabPlacement.equalsIgnoreCase("LEFT")) {
      return SwingConstants.LEFT;
    }
    else if (tabPlacement.equalsIgnoreCase("RIGHT")) {
      return SwingConstants.RIGHT;
    }
    else {
      return SwingConstants.TOP;
    }
  }

  /***********************************************************************************************
   * Gets the policy <code>String</code> value representation for a
   * <code>ScrollPaneConstants</code> scrollbar policy value
   * @param policy the <code>ScrollPaneConstants</code> scrollbar policy value
   * @return a <code>String</code> for the scrollbar policy
   ***********************************************************************************************/
  public static String parseVerticalScrollBarPolicy(int policy) {
    if (policy == ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS) {
      return "true";
    }
    else if (policy == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
      return "false";
    }
    else {
      return "auto";
    }
  }

  /***********************************************************************************************
   * Gets the <code>ScrollPaneConstants</code> scrollbar policy value represented by the
   * <code>policy</code> <code>String</code>
   * @param policy a <code>String</code> for the scrollbar policy
   * @return the <code>ScrollPaneConstants</code> scrollbar policy value
   ***********************************************************************************************/
  public static int parseVerticalScrollBarPolicy(String policy) {
    if (policy == null) {
      return ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
    }
    else if (policy.equalsIgnoreCase("true")) {
      return ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
    }
    else if (policy.equalsIgnoreCase("false")) {
      return ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
    }
    else {
      return ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
    }
  }

  /***********************************************************************************************
   * Sets whether the <code>component</code> is visible or not, where the visible value is
   * represented by the <code>visible</code> <code>String</code> value
   * @param visible <b>"true"</b> if the <code>component</code> is visible<br />
   *        <b>"false"</b> if the <code>component</code> is not visible
   * @param component the <code>WSComponent</code> to set the visibility of
   ***********************************************************************************************/
  public static void parseVisibleAttribute(String value, WSComponent component) {
    if (value != null) {
      component.setVisible(parseBoolean(value));
    }
  }

  /***********************************************************************************************
   * Parses and sets generic properties of the <code>component</code> from the <code>node</code>
   * attributes, such as the component <i>code</i>, <i>visibility</i>, and <i>border-width</i>.
   * @param node the <code>XMLNode</code> that describes the <code>component</code>
   * @param component the <code>WSComponent</code> to set the properties of
   ***********************************************************************************************/
  public static void setAttributes(XMLNode node, WSComponent component) {
    //((JComponent)component).removeAll(); // this removes the buttons from WSComboBox, etc too - need an alternative

    parseBorderWidthAttribute(node.getAttribute("border-width"), component);
    parseCodeAttribute(node.getAttribute("code"), component);
    parseEnabledAttribute(node.getAttribute("enabled"), component);
    parseOpaqueAttribute(node.getAttribute("opaque"), component);
    parseVisibleAttribute(node.getAttribute("visible"), component);
    //parseWidthHeightAttribute(node.getAttribute("width"),node.getAttribute("height"),node.getAttribute("minimum-width"),node.getAttribute("minimum-height"),component);
    parseMinimumSizeAttribute(node.getAttribute("minimum-width"), node.getAttribute("minimum-height"), component);
    parseSizeAttribute(node.getAttribute("width"), node.getAttribute("height"), component);

    parseRepositoryAttribute(node.getAttribute("repository"), component);

    String position = node.getAttribute("position");
    if (position != null) {
      component.setPosition(position);
    }
  }

  /***********************************************************************************************
   * Parses and sets generic properties of the <code>component</code> from the <code>node</code>
   * attributes, such as the component <i>code</i>, <i>visibility</i>, and <i>border-width</i>.
   * These attributes can be set after any sub-components are set.
   * @param node the <code>XMLNode</code> that describes the <code>component</code>
   * @param component the <code>WSComponent</code> to set the properties of
   ***********************************************************************************************/
  public static void setPostAttributes(XMLNode node, WSComponent component) {
    parseMinimumSizeAttribute(node.getAttribute("minimum-width"), node.getAttribute("minimum-height"), component);
    parseSizeAttribute(node.getAttribute("width"), node.getAttribute("height"), component);
  }

  /***********************************************************************************************
   * Parses and sets generic properties of the <code>component</code> from the <code>node</code>
   * attributes, such as the component <i>code</i>, <i>visibility</i>, and <i>border-width</i>.
   * These attributes can be set before any sub-components are set.
   * @param node the <code>XMLNode</code> that describes the <code>component</code>
   * @param component the <code>WSComponent</code> to set the properties of
   ***********************************************************************************************/
  public static void setPreAttributes(XMLNode node, WSComponent component) {
    //((JComponent)component).removeAll(); // this removes the buttons from WSComboBox, etc too - need an alternative

    parseBorderWidthAttribute(node.getAttribute("border-width"), component);
    parseCodeAttribute(node.getAttribute("code"), component);
    parseEnabledAttribute(node.getAttribute("enabled"), component);
    parseOpaqueAttribute(node.getAttribute("opaque"), component);
    parseVisibleAttribute(node.getAttribute("visible"), component);

    parseRepositoryAttribute(node.getAttribute("repository"), component);

    String position = node.getAttribute("position");
    if (position != null) {
      component.setPosition(position);
    }
  }

  /***********************************************************************************************
   * Sets the <code>resourcePath</code> used to obtain resources like images and sounds
   * @param resourcePathObject the <code>Object</code> to use as a relative location for
   *        resources
   ***********************************************************************************************/
  public static void setResourcePath(Object resourcePathObject) {
    resourcePath = resourcePathObject.getClass();
  }

  /***********************************************************************************************
   * Sets the <code>text</code> of a <code>component</code>, usually used to clear the text or to
   * give it a fixed number value
   * @param component the <code>WSComponent</code> to set the text of
   * @param text the new <code>String</code> text of the <code>WSComponent</code>
   ***********************************************************************************************/
  public static void setText(WSComponent component, String text) {
    String code = component.getCode();
    if (code == null) {
      return;
    }
    String langCode = getClassName(component) + "_" + code + "_Text";
    Language.set(langCode, text);
  }

  /***********************************************************************************************
   * Builds a <code>JComponent</code> from the <code>node</code> <code>XMLNode</code>. The
   * particular <code>JComponent</code> that is built depends on the tag name of the
   * <code>node</code>. For example, a tag name of <code>WSButton</code> will build a
   * <code>WSButton</code> component. All built components that implement
   * <code>WSComponent</code> are added to the <code>ComponentRepository</code> (unless they have
   * the attribute <code>repository="false"</code>)
   * @param node the <code>XMLNode</code> that describes the <code>JComponent</code> to construct
   * @return the <code>JComponent</code> object built from the <code>node</code>, or <b>null</b>
   *         if the <code>node</code> had errors or simply isn't a valid <code>JComponent</code>
   ***********************************************************************************************/
  public static JComponent toComponent(XMLNode node) {
    String nodeTag = node.getTag();
    if (nodeTag.indexOf("org.watto") != 0) {
      nodeTag = "org.watto.component." + nodeTag;
    }

    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try {
      JComponent component = (JComponent) classLoader.loadClass(nodeTag).getConstructor(new Class[] { XMLNode.class }).newInstance(new Object[] { node });
      return component;
    }
    catch (Throwable t) {
      try {
        nodeTag = node.getTag();
        JComponent component = (JComponent) classLoader.loadClass(nodeTag).getConstructor(new Class[] { XMLNode.class }).newInstance(new Object[] { node });
        return component;
      }
      catch (Throwable t2) {
        ErrorLogger.log("ERROR", "Could not automatically generate the component " + nodeTag);
        ErrorLogger.log(t2);
        return new WSPanel(XMLReader.read("<WSPanel />"));
      }
    }
  }

  /***********************************************************************************************
   * Constructs an <code>XMLNode</code> representation of the <code>component</code>
   * @param component the <code>WSComponent</code> to convert
   * @return the <code>XMLNode</code> representation of the <code>component</code>
   ***********************************************************************************************/
  public static XMLNode toXML(WSComponent component) {
    XMLNode node = new XMLNode(getClassName(component));

    String code = component.getCode();
    try {
      Integer.parseInt(code);
    }
    catch (Throwable t) {
      node.setAttribute("code", component.getCode()); // only set if not an automatically generated hash
    }

    if (!component.isEnabled()) {
      node.setAttribute("enabled", "false"); // only set if not enabled
    }

    int fixedHeight = component.getFixedHeight();
    if (fixedHeight != -1) {
      node.setAttribute("height", "" + fixedHeight);
    }

    int fixedWidth = component.getFixedWidth();
    if (fixedWidth != -1) {
      node.setAttribute("width", "" + fixedWidth);
    }

    int minimumFixedHeight = component.getFixedMinimumHeight();
    if (minimumFixedHeight != -1) {
      node.setAttribute("minimum-height", "" + minimumFixedHeight);
    }

    int minimumFixedWidth = component.getFixedMinimumWidth();
    if (minimumFixedWidth != -1) {
      node.setAttribute("minimum-width", "" + minimumFixedWidth);
    }

    if (!component.isOpaque()) {
      node.setAttribute("opaque", "false"); // only set if not opaque
    }

    if (!component.isVisible()) {
      node.setAttribute("visible", "false"); // only set if not visible
    }

    String position = component.getPosition();
    if (position != null) {
      node.setAttribute("position", position);
    }

    int borderWidth = component.getBorderWidth();
    if (borderWidth != -1) {
      node.setAttribute("border-width", "" + borderWidth);
    }

    if (!component.isInRepository()) {
      node.setAttribute("repository", "false"); // only set if not in the repository
    }

    return node;
  }

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public WSHelper() {
  }

}