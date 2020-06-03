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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.event.HyperlinkEvent;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.event.WSLinkableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
**********************************************************************************************
A PanelPlugin
**********************************************************************************************
**/
public class SidePanel_Help extends WSPanelPlugin implements WSLinkableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /**
  **********************************************************************************************
  Constructor for extended classes only
  **********************************************************************************************
  **/
  public SidePanel_Help() {
    super(new XMLNode());
  }

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  @param caller the object that contains this component, created this component, or more formally,
              the object that receives events from this component.
  **********************************************************************************************
  **/
  public SidePanel_Help(XMLNode node) {
    super(node);
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

  /**
  **********************************************************************************************
  Gets the plugin description
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
  Gets the plugin name
  **********************************************************************************************
  **/
  @Override
  public String getText() {
    return super.getText();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadIndex() {
    loadFile(new File("help/index.html"));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadFile(File htmlFile) {
    /*
    try {
      WSEditorPane display = (WSEditorPane) ComponentRepository.get("SidePanel_Help_Display");
      display.setEditable(false);
      display.setContentType("text/html");
      display.setPage(htmlFile.toURL());
    
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    */

    Platform.runLater(() -> { // FX components need to be managed by JavaFX
      if (engine == null) {
        return;
      }
      engine.load(htmlFile.toURI().toString());
      return;

      //webView.setMaxWidth(((WSPanel) ComponentRepository.get("SidePanel_Help")).getWidth() - 50);

    });

  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************
  The event that is triggered from a WSClickableListener when a click occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    return false;
  }

  /**
  **********************************************************************************************
  Performs any functionality that needs to happen when the panel is to be closed. This method
  does nothing by default, but can be overwritten to do anything else needed before the panel is
  closed, such as garbage collecting and closing pointers to temporary objects.
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSDoubleClickableListener when a double click occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onDoubleClick(JComponent c, MouseEvent e) {
    return false;
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSHoverableListener when the mouse moves over an object
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onHover(JComponent c, MouseEvent e) {
    return super.onHover(c, e);
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSHoverableListener when the mouse moves out of an object
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onHoverOut(JComponent c, MouseEvent e) {
    return super.onHoverOut(c, e);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onHyperlink(JComponent c, HyperlinkEvent e) {
    if (c instanceof WSEditorPane) {
      String code = ((WSComponent) c).getCode();
      if (code.equals("SidePanel_Help_Display")) {
        WSEditorPane display = (WSEditorPane) c;

        // load the page
        try {
          display.setPage(e.getURL());
        }
        catch (Throwable t) {
          ErrorLogger.log(t);
        }

        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSKeyableListener when a key press occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    return false;
  }

  /**
  **********************************************************************************************
  Performs any functionality that needs to happen when the panel is to be opened. By default,
  it just calls checkLoaded(), but can be overwritten to do anything else needed before the
  panel is displayed, such as resetting or refreshing values.
  **********************************************************************************************
  **/
  @Override
  public void onOpenRequest() {
    //loadIndex();
  }

  ///////////////
  //
  // Default Implementations
  //
  ///////////////

  /**
  **********************************************************************************************
  Registers the events that this component generates
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
  @Override
  public void requestFocus() {
    //((WSEditorPane) ComponentRepository.get("SidePanel_Help_Display")).requestFocus();
  }

  /**
  **********************************************************************************************
  Sets the description of the plugin
  @param description the description
  **********************************************************************************************
  **/
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
  }

  WebEngine engine = null;

  /**
  **********************************************************************************************
  Build this object from the <i>node</i>
  @param node the XML node that indicates how to build this object
  **********************************************************************************************
  **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "SidePanel_Help.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);

    JFXPanel jfxPanel = new JFXPanel(); // Scrollable JCompenent
    ((WSPanel) ComponentRepository.get("SidePanel_Help")).add(jfxPanel, BorderLayout.CENTER);
    //((WSScrollPane) ComponentRepository.get("SidePanel_Help_ScrollPane")).setViewportView(jfxPanel);

    Platform.setImplicitExit(false); // stop the JavaFX from dying when the thread finishes
    Platform.runLater(() -> { // FX components need to be managed by JavaFX
      WebView webView = new WebView();
      webView.setMaxWidth(((WSPanel) ComponentRepository.get("SidePanel_Help")).getWidth());
      engine = webView.getEngine();
      //engine.load(new File("help/index.html").toURI().toString()); // "help/index.html"
      jfxPanel.setScene(new Scene(webView));
    });

    loadIndex();
  }

  /**
  **********************************************************************************************
  Builds an XMLNode that describes this object
  @return an XML node with the details of this object
  **********************************************************************************************
  **/
  @Override
  public XMLNode toXML() {
    return super.toXML();
  }

}