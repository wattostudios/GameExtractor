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

package org.watto.task;

import java.awt.Color;
import org.watto.Language;
import org.watto.component.ComponentRepository;
import org.watto.component.WSPanel;
import org.watto.component.WelcomeWizardPanel;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_Popup_WelcomeWizard extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_Popup_WelcomeWizard() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {

    //System.out.println("TASK-->SHOW WELCOME WIZARD");

    try {
      // This is required, so that the Wizard isn't overwritten by any other repaint/refresh tasks
      Thread.sleep(250);
    }
    catch (Throwable t) {
    }

    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null) {
      // Remove the existing panel on the overlay  
      overlayPanel.removeAll();

      // Add the Wizard Panel
      overlayPanel.add(new WelcomeWizardPanel());

      // set the background color around the panel
      overlayPanel.setObeyBackgroundColor(true);
      overlayPanel.setBackground(new Color(255, 255, 255, 160));

      // Validate and show the display
      overlayPanel.validate();
      overlayPanel.setVisible(true);
      overlayPanel.repaint();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
