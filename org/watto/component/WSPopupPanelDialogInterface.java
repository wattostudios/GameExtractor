////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2020  WATTO Studios                           //
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

import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;

/***********************************************************************************************
 * The actual popup dialog that is shown to the user. Users should create popups using the class
 * <code>WSPopup</code> or <code>WSPopupPanel</code> rather than using this class directly.
 * @see org.watto.component.WSPopup
 ***********************************************************************************************/

public interface WSPopupPanelDialogInterface {

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  ***********************************************************************************************/
  public void constructInterface(JPanel panel, String code);

  /***********************************************************************************************
  Shows the <code>panel</code> as a popup
  @param panel the <code>JPanel</code> to show in the popup
  @param code the text code of the popup
  @param focusComponent the <code>Component</code> to have focus
  ***********************************************************************************************/
  public void constructInterface(JPanel panel, String code, JComponent focusComponent);

  /***********************************************************************************************
   * Closes the popup
   ***********************************************************************************************/
  public void dispose();

  /***********************************************************************************************
   * Closes the popup when a button was pressed, and sets the <code>pressedEvent</code>.
   * @param component the <code>Component</code> that triggered the <code>event</code>
   * @param event the <code>MouseEvent</code> that was triggered
   * @return true
   ***********************************************************************************************/
  public boolean onClick(JComponent component, MouseEvent event);

  /***********************************************************************************************
   * Waits for the user to click something before continuing with the Thread
   ***********************************************************************************************/
  public void waitForClick();

}