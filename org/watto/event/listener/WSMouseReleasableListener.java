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
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import org.watto.event.WSMouseReleasableInterface;

/***********************************************************************************************
Listens for <i>release</i> or <i>up</i> <code>MouseEvent</code>s and passes them to the
<code>WSMouseReleasableInterface</code> handler class
@see java.awt.event.MouseEvent
@see org.watto.event.WSMouseReleasableInterface
***********************************************************************************************/
public class WSMouseReleasableListener implements MouseListener {

  /** the event handling class **/
  WSMouseReleasableInterface handler;

  /***********************************************************************************************
  Registers the <code>WSMouseReleasableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSMouseReleasableListener(WSMouseReleasableInterface handler) {
    this.handler = handler;
  }

  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mouseClicked(MouseEvent event) {

  }

  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mouseEntered(MouseEvent event) {
  }

  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mouseExited(MouseEvent event) {
  }

  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mousePressed(MouseEvent event) {
  }

  /***********************************************************************************************
  Calls <code>handler.onRelease()</code> when a <i>release</i> <code>MouseEvent</code> is triggered
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mouseReleased(MouseEvent event) {
    handler.onRelease((JComponent) event.getSource(), event);
  }
}