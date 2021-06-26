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

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import org.watto.event.WSMotionableInterface;


/***********************************************************************************************
Listens for <i>mouse moved</i> <code>MouseEvent</code>s and passes them to the
<code>WSMotionableInterface</code> handler class
@see java.awt.event.MouseEvent
@see org.watto.event.WSClickableInterface
***********************************************************************************************/
public class WSMotionableListener implements MouseMotionListener {

  /** the event handling class **/
  WSMotionableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSMotionableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSMotionableListener(WSMotionableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  public void mouseDragged(MouseEvent event){

  }


  /***********************************************************************************************
  Calls <code>handler.onMotion()</code> when a <i>mouse moved</i> <code>MouseEvent</code> is triggered
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  public void mouseMoved(MouseEvent event){
    handler.onMotion((JComponent)event.getSource(),event);
  }
}