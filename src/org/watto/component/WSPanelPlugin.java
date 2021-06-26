////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                            WATTO STUDIOS JAVA PROGRAM TEMPLATE                             //
//                  Template Classes and Helper Utilities for Java Programs                   //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2006  WATTO Studios                           //
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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSDoubleClickableInterface;
import org.watto.event.WSHoverableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.xml.XMLNode;

/**
 **********************************************************************************************
 * A WSPlugin that is used to provide an interface component. In other words, a panel that can be
 * dynamically loaded, and interfaces with the program it is loaded in to. <br>
 * <br>
 * The panel can do anything that you want, and may be dependant upon a particular program if you
 * want - ie it does not guarantee that all plugins will work with all other programs that
 * support WSPlugin, it simply provides a common interface. <br>
 * <br>
 * These plugins are loaded dynamically, but are not constructed until they are requested to be
 * shown - this helps to reduce loading time and memory requirements. <br>
 * <br>
 * When the panel is painted, language texts will be taken from Language if they exist. Any
 * component on the panel will also automatically have the language applied to it assuming that
 * you use WSComponents - all non-WSComponents will need to be altered in the changeLanguage()
 * method.
 **********************************************************************************************
 **/

public abstract class WSPanelPlugin extends WSPanel implements WSPlugin,
    WSDoubleClickableInterface,
    WSClickableInterface,
    WSKeyableInterface,
    WSHoverableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
   **********************************************************************************************
   * Sends an error to the ErrorLogger for recording
   * @param t the error that occurred
   **********************************************************************************************
   **/
  public static void logError(Throwable t) {
    ErrorLogger.log(t);
  }

  /** The name of this plugin **/
  String name = "WS Panel Plugin";

  /** A description of this plugin **/
  String description = "A panel plugin.";

  /** Has this panel been constructed yet? **/
  boolean loaded = false;

  /** Is this plugin enabled? **/
  boolean enabled = true;

  /** The type of plugin **/
  String type = "PanelPlugin";

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public WSPanelPlugin() {
    super();
  }

  /**
   **********************************************************************************************
   * Constructor
   **********************************************************************************************
   **/
  public WSPanelPlugin(String name) {
    super();
    this.code = name;
    this.name = name;
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSPanelPlugin(XMLNode node) {
    super(node);
  }

  /**
   **********************************************************************************************
   * Updates any non-WSComponents with new language texts after the program Language was changed.
   * If you are using WSComponents, they will have their language texts changed automatically on
   * the next repaint().
   **********************************************************************************************
   **/
  public void changeLanguage() {
  }

  /**
   **********************************************************************************************
   * Constructs the interface of the component.
   **********************************************************************************************
   **/
  //  public abstract void constructInterface();

  /**
   **********************************************************************************************
   * Checks whether the panel has been constructed or not. If is hasn't, constructInterface() is
   * called.
   **********************************************************************************************
   **/
  public void checkLoaded() {
    if (!loaded) {
      setOpaque(true);
      loaded = true;
    }

    changeLanguage();
  }

  /**
   **********************************************************************************************
   * Gets the description of the plugin
   * @return the description
   **********************************************************************************************
   **/
  @Override
  public String getDescription() {
    String descriptionCode = "WSPanelPlugin_" + code + "_Description";
    if (Language.has(descriptionCode)) {
      return Language.get(descriptionCode);
    }
    return description;
  }

  /**
   **********************************************************************************************
   * Gets the name of the plugin
   * @return the name
   **********************************************************************************************
   **/
  @Override
  public String getName() {
    String nameCode = "WSPanelPlugin_" + code + "_Name";
    if (Language.has(nameCode)) {
      return Language.get(nameCode);
    }
    return name;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getType() {
    return type;
  }

  /**
   **********************************************************************************************
   * Is this plugin enabled?
   * @return tru if the plugin is enabled, false if disabled
   **********************************************************************************************
   **/
  @Override
  public boolean isEnabled() {
    return enabled;
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
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be closed. This method
   * does nothing by default, but can be overwritten to do anything else needed before the panel
   * is closed, such as garbage collecting and closing pointers to temporary objects.
   **********************************************************************************************
   **/
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
   * The event that is triggered from a WSHoverableListener when the mouse moves over an object
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    WSComponent statusbar = ComponentRepository.get("StatusBar");
    if (statusbar != null && statusbar instanceof WSStatusBar) {
      ((WSStatusBar) statusbar).setText(c.getToolTipText());
    }
    return true;
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
    WSComponent statusbar = ComponentRepository.get("StatusBar");
    if (statusbar != null && statusbar instanceof WSStatusBar) {
      ((WSStatusBar) statusbar).revertText();
    }
    return true;
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
    return false;
  }

  /**
   **********************************************************************************************
   * Performs any functionality that needs to happen when the panel is to be opened. By default,
   * it just calls checkLoaded(), but can be overwritten to do anything else needed before the
   * panel is displayed, such as resetting or refreshing values.
   **********************************************************************************************
   **/
  public void onOpenRequest() {
    checkLoaded();
  }

  /**
   **********************************************************************************************
   * Sets the description of the plugin
   * @param description the description
   **********************************************************************************************
   **/
  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   **********************************************************************************************
   * Sets whether this plugin is enabled
   * @param enabled the enabled status
   **********************************************************************************************
   **/
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   **********************************************************************************************
   * Sets the name of the plugin
   * @param name the name
   **********************************************************************************************
   **/
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setType(String type) {
    this.type = type;

  }

  /**
   **********************************************************************************************
   * Gets the name of the plugin
   * @return the name
   **********************************************************************************************
   **/
  @Override
  public String toString() {
    return getText();
  }

}