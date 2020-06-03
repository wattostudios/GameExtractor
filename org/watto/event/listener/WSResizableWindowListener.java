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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JComponent;
import org.watto.event.WSResizableInterface;


/***********************************************************************************************
Listens for <i>resize</i> <code>WindowEvent</code>s and passes them to the
<code>WSResizableInterface</code> handler class
@see java.awt.event.WindowEvent
@see org.watto.event.WSResizableInterface
***********************************************************************************************/
public class WSResizableWindowListener extends WindowAdapter {

  /** the event handling class **/
  WSResizableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSResizableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSResizableWindowListener(WSResizableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  Calls <code>handler.onResize()</code> when a <i>resize</i> <code>WindowEvent</code> is triggered
  @param event the <code>WindowEvent</code> event
  ***********************************************************************************************/
  public void windowStateChanged(WindowEvent event){
    handler.onResize((JComponent)event.getSource(),event);
  }
}