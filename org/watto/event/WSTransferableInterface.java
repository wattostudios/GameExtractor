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

package org.watto.event;

import javax.swing.TransferHandler;
import javax.swing.JComponent;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Transferable;


/***********************************************************************************************
A class that reacts to drag and drop events<br />
<br />
<i>For components that support dragging...</i><br />
component.setDragEnabled(true);<br />
component.addMouseMotionListener(new WSTransferableListener(this));<br />
<br />
<i>For components that support dropping...</i><br />
component.setTransferHandler(new WSTransferableListener(this));

@see java.awt.event.MouseEvent
@see org.watto.event.listener.WSTransferableListener
@see java.awt.datatransfer.Transferable
***********************************************************************************************/
public interface WSTransferableInterface {

  /***********************************************************************************************
  Creates a <code>TransferHandler</code> for this <code>Component</code>, allowing it to be dragged.
  @param source the <code>JComponent</code> that will be dragged
  @param event the dragging <code>MouseEvent</code>
  @return the <code>TransferHandler</code> for this <code>Component</code>
  ***********************************************************************************************/
  public TransferHandler onDrag(JComponent source,MouseEvent event);


  /***********************************************************************************************
  Drops the <code>Transferable</code> object from the <code>Component</code>
  @param transferData the transferred data
  @return <b>true</b> if the event was handled by this class<br />
          <b>false</b> if the event wasn't handled by this class, and thus should be passed on to
          the parent class for handling.
  ***********************************************************************************************/
  public boolean onDrop(Transferable transferData);
}