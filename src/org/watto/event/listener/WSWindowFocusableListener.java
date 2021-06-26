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

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import org.watto.event.WSWindowFocusableInterface;


/***********************************************************************************************
Listens for <i>gain focus</i> <code>WindowEvent</code>s and passes them to the
<code>WSWindowFocusableInterface</code> handler class
@see java.awt.event.WindowEvent
@see org.watto.event.WSWindowFocusableInterface
***********************************************************************************************/
public class WSWindowFocusableListener implements WindowFocusListener {

  /** the event handling class **/
  WSWindowFocusableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSWindowFocusableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSWindowFocusableListener(WSWindowFocusableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  Calls <code>handler.onWindowFocus()</code> when a <i>gain focus</i> <code>WindowEvent</code> is triggered
  @param event the <code>WindowEvent</code> event
  ***********************************************************************************************/
  public void windowGainedFocus(WindowEvent event){
    if (event.getID() == WindowEvent.WINDOW_GAINED_FOCUS && event.getOppositeWindow() == null) {
      handler.onWindowFocus(event);
    }
  }


  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>WindowEvent</code> event
  ***********************************************************************************************/
  public void windowLostFocus(WindowEvent event){

  }
}