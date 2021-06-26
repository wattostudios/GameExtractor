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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JComponent;
import org.watto.event.WSEnterableInterface;


/***********************************************************************************************
Listens for <i>pressed Enter</i> <code>KeyEvent</code>s and passes them to the
<code>WSEnterableInterface</code> handler class
@see java.awt.event.KeyEvent
@see org.watto.event.WSEnterableInterface
***********************************************************************************************/
public class WSEnterableListener implements KeyListener {

  /** the event handling class **/
  WSEnterableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSEnterableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSEnterableListener(WSEnterableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>KeyEvent</code> event
  ***********************************************************************************************/
  public void keyPressed(KeyEvent event){}


  /***********************************************************************************************
  Calls <code>handler.onEnter()</code> when a <i>pressed Enter</i> <code>KeyEvent</code> is
  triggered
  @param event the <code>KeyEvent</code> event
  ***********************************************************************************************/
  public void keyReleased(KeyEvent event){
    handler.onEnter((JComponent)event.getSource(),event);
  }


  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>KeyEvent</code> event
  ***********************************************************************************************/
  public void keyTyped(KeyEvent event){}
}