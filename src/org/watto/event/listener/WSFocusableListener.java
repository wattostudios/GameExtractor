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

package org.watto.event.listener;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;
import org.watto.event.WSFocusableInterface;


/***********************************************************************************************
Listens for <code>FocusEvent</code>s and passes them to the
<code>WSFocusableInterface</code> handler class
@see java.awt.event.FocusEvent
@see org.watto.event.WSFocusableInterface
***********************************************************************************************/
public class WSFocusableListener implements FocusListener {

  /** the event handling class **/
  WSFocusableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSFocusableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSFocusableListener(WSFocusableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  Calls <code>handler.onFocus()</code> when a <i>gain focus</i> <code>FocusEvent</code> is triggered
  @param event the <code>FocusEvent</code> event
  ***********************************************************************************************/
  public void focusGained(FocusEvent event){
    handler.onFocus((JComponent)event.getSource(),event);
  }


  /***********************************************************************************************
  Calls <code>handler.onFocusOut()</code> when a <i>lose focus</i> <code>FocusEvent</code> is
  triggered
  @param event the <code>FocusEvent</code> event
  ***********************************************************************************************/
  public void focusLost(FocusEvent event){
    handler.onFocusOut((JComponent)event.getSource(),event);
  }
}