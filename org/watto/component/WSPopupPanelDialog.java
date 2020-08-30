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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;

/***********************************************************************************************
The actual popup dialog that is shown to the user. Users should create popups using the class
<code>WSPopupPanel</code> or <code>WSPopup</code> rather than using this class directly.
@see org.watto.component.WSPopupPanel
***********************************************************************************************/

public class WSPopupPanelDialog extends JDialog implements WSPopupPanelDialogInterface, WSClickableInterface, WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** the popup that this dialog belongs to **/
  WSPopupPanel popup;

  /***********************************************************************************************
  Creates the <code>WSPopupPanelDialog</code> for the parent <code>popup</code>
  @param popup the popup owner
  ***********************************************************************************************/
  public WSPopupPanelDialog(WSPopupPanel popup) {
    super();
    this.popup = popup;
    setModal(true);
    getContentPane().setLayout(new BorderLayout());
  }

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  ***********************************************************************************************/
  public void constructInterface(JPanel panel, String code) {
    constructInterface(panel, code, null);
  }

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  @param focusComponent the <code>Component</code> to have focus
  ***********************************************************************************************/
  public void constructInterface(JPanel panel, String code, JComponent focusComponent) {
    try {

      getContentPane().removeAll();

      setTitle(Language.get("WSPopupDialog_" + code + "_Title"));

      getContentPane().add(panel, BorderLayout.CENTER);

      pack();
      setSize(400, getHeight() + 10);
      setLocationRelativeTo(null);

      if (focusComponent != null) {
        focusComponent.requestFocus();
      }

      setVisible(true);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Closes the popup when a button was pressed, and sets the <code>pressedEvent</code>.
  @param component the <code>Component</code> that triggered the <code>event</code>
  @param event the <code>MouseEvent</code> that was triggered
  @return true
  ***********************************************************************************************/
  @Override
  public boolean onClick(JComponent component, MouseEvent event) {
    if (component instanceof JButton) {
      popup.onClick(component, event);
    }
    return true;
  }

  /***********************************************************************************************
  Pressed the button when the Enter key is pressed
  @param component the <code>Component</code> that triggered the <code>event</code>
  @param event the <code>KeyEvent</code> that was triggered
  @return true
  ***********************************************************************************************/
  @Override
  public boolean onKeyPress(JComponent component, KeyEvent event) {
    popup.onKeyPress(component, event);
    return true;
  }

  /***********************************************************************************************
   * Waits for the user to click something before continuing with the Thread
   ***********************************************************************************************/
  @Override
  public void waitForClick() {
    // don't need to do anything - it's a modal dialog already!
  }

}