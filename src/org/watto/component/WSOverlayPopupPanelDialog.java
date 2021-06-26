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
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.watto.ErrorLogger;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.listener.WSKeyableListener;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * The actual popup dialog that is shown to the user. Users should create popups using the class
 * <code>WSPopup</code> or <code>WSPopupPanel</code> rather than using this class directly.
 * @see org.watto.component.WSPopup
 ***********************************************************************************************/

public class WSOverlayPopupPanelDialog extends JPanel implements WSPopupPanelDialogInterface, WSClickableInterface, WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** the popup that this dialog belongs to **/
  WSPopupPanel popup;

  /***********************************************************************************************
   * Creates the <code>WSPopupDialog</code> for the parent <code>popup</code>
   * @param popup the popup owner
   ***********************************************************************************************/
  public WSOverlayPopupPanelDialog(WSPopupPanel popup) {
    super();
    this.popup = popup;
    setLayout(new BorderLayout());
  }

  /**
   **********************************************************************************************
   * Builds a <code>WSButton</code> specifically for the dialog
   * @param code the <code>Language</code> code of the <code>WSButton</code>
   * @return the <code>WSButton</code>
   **********************************************************************************************
   **/
  public WSButton constructButton(String code) {
    WSButton button = new WSButton(XMLReader.read("<WSButton code=\"" + code + "\" />"));
    button.addKeyListener(new WSKeyableListener(this));
    return button;
  }

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  ***********************************************************************************************/
  public synchronized void constructInterface(JPanel panel, String code) {
    constructInterface(panel, code, null);
  }

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  @param focusComponent the <code>Component</code> to have focus
  ***********************************************************************************************/
  public synchronized void constructInterface(JPanel panel, String code, JComponent focusComponent) {
    try {

      removeAll();

      add(panel, BorderLayout.CENTER);

      setSize(400, getHeight() + 10);

      if (focusComponent != null) {
        focusComponent.requestFocus();
      }

      // Remove the existing panel on the overlay, and put this one there instead
      WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
      if (overlayPanel != null) {
        // Add the panel to the overlay
        overlayPanel.removeAll();
        overlayPanel.add(this);

        // set the background color around the panel
        overlayPanel.setObeyBackgroundColor(true);
        overlayPanel.setBackground(new Color(255, 255, 255, 110));

        // Validate and show the display
        overlayPanel.validate();
        overlayPanel.setVisible(true);
        overlayPanel.repaint();
      }

      setVisible(true);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Closes the popup
   ***********************************************************************************************/
  @Override
  public void dispose() {

    // Remove the existing panel on the overlay
    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null) {
      overlayPanel.removeAll();
      overlayPanel.setVisible(false);

      overlayPanel.validate();
      overlayPanel.repaint();
    }

    setVisible(false);
  }

  /***********************************************************************************************
   * Closes the popup when a button was pressed, and sets the <code>pressedEvent</code>.
   * @param component the <code>Component</code> that triggered the <code>event</code>
   * @param event the <code>MouseEvent</code> that was triggered
   * @return true
   ***********************************************************************************************/
  @Override
  public synchronized boolean onClick(JComponent component, MouseEvent event) {
    if (component instanceof JButton) {
      notifyAll(); // wake up the thread waiting for the click of the OK button
      popup.onClick(component, event);
    }
    return true;
  }

  /***********************************************************************************************
   * Pressed the button when the Enter key is pressed
   * @param component the <code>Component</code> that triggered the <code>event</code>
   * @param event the <code>KeyEvent</code> that was triggered
   * @return true
   ***********************************************************************************************/
  @Override
  public boolean onKeyPress(JComponent component, KeyEvent event) {
    notifyAll(); // wake up the thread waiting for the click of the OK button
    popup.onKeyPress(component, event);
    return true;
  }

  /***********************************************************************************************
   * Waits for the user to click something before continuing with the Thread
   ***********************************************************************************************/
  @Override
  public synchronized void waitForClick() {
    try {
      /*
      System.out.println("WSOverlayPopupDialog: Waiting...");
      try {
        throw new Exception();
      }
      catch (Throwable t2) {
        t2.printStackTrace();
      }
      */
      wait();
      //System.out.println("WSOverlayPopupDialog: Finished Waiting.");
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}