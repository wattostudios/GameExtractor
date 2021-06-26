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
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
The Wizard panel that appears when running Game Extractor for the first time.
**********************************************************************************************
**/
public class WelcomeWizardPanel extends WSPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  String[] screens = new String[] { "screen01.png", "screen02.png", "screen03.png", "screen04.png" };

  String[] labels = new String[] { "WelcomeWizard_Screen01_Label", "WelcomeWizard_Screen02_Label", "WelcomeWizard_Screen03_Label", "WelcomeWizard_Screen04_Label" };

  int currentScreen = 0;

  /**
  **********************************************************************************************
  Constructor for extended classes only
  **********************************************************************************************
  **/
  public WelcomeWizardPanel() {
    super(new XMLNode());
    currentScreen = 0;
    loadScreen();
  }

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  @param caller the object that contains this component, created this component, or more formally,
              the object that receives events from this component.
  **********************************************************************************************
  **/
  public WelcomeWizardPanel(XMLNode node) {
    super(node);
    currentScreen = 0;
    loadScreen();
  }

  ///////////////
  //
  // Configurable
  //
  ///////////////

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
  public void loadScreen() {
    try {

      // Change the screen image
      WSLabel screenImage = (WSLabel) ComponentRepository.get("WelcomeWizard_ScreenImage");
      screenImage.setIcon(new ImageIcon("images" + File.separatorChar + "WelcomeWizard" + File.separatorChar + screens[currentScreen]));

      // Change the screen image label
      String label = Language.get(labels[currentScreen]);
      String[] labelLines = label.split("\\\\n", 3);

      int numLines = labelLines.length;

      WSLabel screenLabel1 = (WSLabel) ComponentRepository.get("WelcomeWizard_ScreenLabel1");
      WSLabel screenLabel2 = (WSLabel) ComponentRepository.get("WelcomeWizard_ScreenLabel2");
      WSLabel screenLabel3 = (WSLabel) ComponentRepository.get("WelcomeWizard_ScreenLabel3");

      if (numLines == 1) {
        screenLabel1.setText_Super("");
        screenLabel2.setText_Super(labelLines[0]);
        screenLabel3.setText_Super("");
      }
      else if (numLines == 2) {
        screenLabel1.setText_Super(labelLines[0]);
        screenLabel2.setText_Super(labelLines[1]);
        screenLabel3.setText_Super("");
      }
      else if (numLines == 3) {
        screenLabel1.setText_Super(labelLines[0]);
        screenLabel2.setText_Super(labelLines[1]);
        screenLabel3.setText_Super(labelLines[2]);
      }

      // Change the label showing what screen it's up to
      WSLabel counterLabel = (WSLabel) ComponentRepository.get("WelcomeWizard_ScreenCount");
      counterLabel.setText_Super((currentScreen + 1) + " / " + screens.length);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSClickableListener when a click occurs
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (!(c instanceof WSComponent)) {
      return false;
    }

    String code = ((WSComponent) c).getCode();

    if (code.equals("WelcomeWizard_CloseWizard")) {
      WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
      if (overlayPanel != null) {
        overlayPanel.removeAll();
        overlayPanel.setVisible(false);

        overlayPanel.validate();
        overlayPanel.repaint();

        // only show the wizard once
        Settings.set("ShowWelcomeWizard", false);

        WSFileListPanelHolder fileListPanelHolder = (WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder");
        fileListPanelHolder.loadPanel(Settings.get("FileListView"));

        // TEMP FOR TESTING ONLY
        //Settings.set("ShowWelcomeWizard", true);
      }
    }
    else if (code.equals("WelcomeWizard_PreviousScreen")) {
      currentScreen--;
      if (currentScreen < 0) {
        currentScreen = 0;
      }
      loadScreen();
    }
    else if (code.equals("WelcomeWizard_NextScreen")) {
      currentScreen++;
      if (currentScreen >= screens.length) {
        currentScreen = screens.length - 1;
      }
      loadScreen();
    }
    else {
      return false;
    }

    return true;
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

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
  Build this object from the <i>node</i>
  @param node the XML node that indicates how to build this object
  **********************************************************************************************
  **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    // Build an XMLNode tree containing all the elements on the screen
    XMLNode srcNode = XMLReader.read(new File("interface" + File.separator + "WelcomeWizard.xml"));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    add(component, BorderLayout.CENTER);

    // setting up this object in the repository (overwrite the base panel with this object)
    setCode(((WSComponent) component).getCode());
    ComponentRepository.add(this);
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