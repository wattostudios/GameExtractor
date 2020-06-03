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

package org.watto.component.timer;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import org.watto.Language;
import org.watto.component.WSButton;
import org.watto.component.WSLabel;
import org.watto.component.WSPanel;
import org.watto.component.WSPopupMenu;
import org.watto.component.WSProgressDialog;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class WSButton_HoverTimer implements Runnable {

  WSButton button;

  /** The popup **/
  WSPopupMenu popupMenu;

  boolean stopRequested = false;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public WSButton_HoverTimer(WSButton button) {
    this.button = button;
  }

  /**
  **********************************************************************************************
  Hides the tooltip popup
  **********************************************************************************************
  **/

  public void hidePopup() {
    if (popupMenu != null) {
      popupMenu.setVisible(false);
    }
  }

  /**
  **********************************************************************************************
  Wait for 2 seconds then show a popup tooltip
  **********************************************************************************************
  **/
  @Override
  public void run() {
    try {
      Thread.sleep(1500);

      if (stopRequested) {
        return;
      }

      if (WSProgressDialog.getInstance().isVisible()) {
        return; // otherwise it will interrupt the progress display
      }

      showPopup();

      Thread.sleep(10000);

      hidePopup();
    }
    catch (Throwable t) {
    }

    // catch-all to hide the popup
    hidePopup();
  }

  /**
  **********************************************************************************************
  Shows a tooltip popup
  **********************************************************************************************
  **/

  public void showPopup() {
    try {

      String buttonCode = button.getCode();
      if (buttonCode == null) {
        return;
      }

      String langCode = "WSButton_" + buttonCode + "_Name";
      if (!Language.has(langCode)) {
        return;
      }

      String buttonText = Language.get(langCode);
      if (buttonText == null || buttonText.equals("")) {
        return;
      }

      // Work out where to show it, based on the location of the button
      Point locationOnScreen = button.getLocationOnScreen();

      int buttonWidth = button.getWidth();
      int buttonHeight = button.getHeight();

      int xPos = locationOnScreen.x + buttonWidth / 2;
      int yPos = locationOnScreen.y + buttonHeight / 2;

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int screenWidth = screenSize.width;
      int screenHeight = screenSize.height;

      float xRatio = ((float) xPos / (float) screenWidth);
      float yRatio = ((float) yPos / (float) screenHeight);

      // whether to display above or below the button
      boolean popupLocationBelowButton = true;
      if (yRatio > 0.5) {
        popupLocationBelowButton = false;
      }

      // Build the PopupMenu
      WSLabel buttonLabel = new WSLabel(new XMLNode());
      buttonLabel.setText_Super(buttonText);

      WSPanel buttonPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" border-width=\"4\" />"));
      buttonPanel.add(buttonLabel);

      popupMenu = new WSPopupMenu(new XMLNode());
      popupMenu.add(buttonPanel);

      // how far left/right of the button to display the popup
      int popupXPos = 0;
      int popupYPos = 0;
      try {
        Graphics g = button.getGraphics();
        FontMetrics metric = g.getFontMetrics();
        int popupWidth = metric.stringWidth(buttonText) + 18; // +18 as an approximate for the borders around the label
        popupXPos = buttonWidth / 2 - popupWidth / 2;
        popupYPos = 0 - metric.getHeight() - 18; // -18 for the borders

        // add an additional radio
        if (xRatio < 0.5) {
          popupXPos += (buttonWidth / 2 * (0.5f - xRatio));
        }
        else {
          popupXPos -= (buttonWidth / 2 * (xRatio - 0.5f));
        }
      }
      catch (Throwable t) {
      }

      if (popupLocationBelowButton) {
        popupMenu.show(button, popupXPos, button.getHeight());
      }
      else {
        popupMenu.show(button, popupXPos, popupYPos);
      }

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  Request the thread to stop, and hide the popup if it's showing
  **********************************************************************************************
  **/

  public void stop() {
    stopRequested = true;
    hidePopup();
  }

}
