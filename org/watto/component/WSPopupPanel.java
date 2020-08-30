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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;

/***********************************************************************************************
Creates and shows a <code>WSPanel</code> as a popup. The popup can be interacted with like a
normal <code>WSPanel</code>, including assigning listeners. The popup is closed by clicking a
<code>WSButton</code> and sending the event to this class, which in turn closes the popup and
sends the pressed button back to the caller.
***********************************************************************************************/

public class WSPopupPanel extends JComponent implements WSClickableInterface, WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** so only 1 popup will ever be displayed to the user at a time! **/
  static WSPopupPanelDialogInterface instance = null;

  /** The value of the button that was pressed **/
  static String pressedEvent = null;

  /***********************************************************************************************
  Gets the singleton <code>instance</code> of the <code>WSPopupPanelDialog</code>
  @return the <code>instance</code> <code>WSPopupPanelDialog</code>
  ***********************************************************************************************/
  public static WSPopupPanelDialogInterface getInstance() {
    return instance;
  }

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static String show(JPanel panel, String code) {
    if (instance == null) {
      // builds an instance
      new WSPopupPanel();
    }
    instance.constructInterface(panel, code);
    return pressedEvent;
  }

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  @param focusComponent the <code>Component</code> to have focus
  @return the button that was pressed
  ***********************************************************************************************/
  public static String show(JPanel panel, String code, JComponent focusComponent) {
    if (instance == null) {
      // builds an instance
      new WSPopupPanel();
    }
    instance.constructInterface(panel, code, focusComponent);
    return pressedEvent;
  }

  /***********************************************************************************************
  Creates the <code>instance</code> <code>WSPopupPanelDialog</code>
  ***********************************************************************************************/
  public WSPopupPanel() {
    super();

    /*
    if (instance == null) {
      instance = new WSPopupPanelDialog(this);
    }
    */

    if (ComponentRepository.has("PopupOverlay")) {
      // use the OverlayPopupDialog
      instance = new WSOverlayPopupPanelDialog(this);
    }
    else {
      // use the Popup PopupDialog
      instance = new WSPopupPanelDialog(this);
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
      pressedEvent = ((WSComponent) component).getCode();
      instance.dispose();
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
    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
      if (component instanceof WSButton) {
        WSButton button = (WSButton) component;
        instance.onClick(button, new MouseEvent(button, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 1, false));
      }
    }

    return true;

  }

}