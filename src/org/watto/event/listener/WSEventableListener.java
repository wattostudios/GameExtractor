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

import org.watto.event.WSEvent;
import org.watto.event.WSEventableInterface;


/***********************************************************************************************
Listens for generic <code>WSEvent</code>s and passes them to the
<code>WSEventableInterface</code> handler class
@see org.watto.event.WSEvent
@see org.watto.event.WSEventableInterface
***********************************************************************************************/
public class WSEventableListener implements WSEventListener {

  /** the event handling class **/
  WSEventableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSEventableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSEventableListener(WSEventableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  Calls <code>handler.onEvent()</code> when a <code>WSEvent</code> is triggered
  @param event the <code>WSEvent</code> event
  ***********************************************************************************************/
  public void eventOccurred(WSEvent event){
    handler.onEvent(event.getSource(),event,event.getType());
  }
}