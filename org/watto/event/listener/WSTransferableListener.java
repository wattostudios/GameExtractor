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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import org.watto.event.WSTransferableInterface;

/***********************************************************************************************
Listens for <i>drag and drop</i> events and passes them to the
<code>WSTransferableInterface</code> handler class<br />
<br />
<i>For components that support dragging...</i><br />
component.setDragEnabled(true);<br />
component.addMouseMotionListener(new WSTransferableListener(this));<br />
<br />
<i>For components that support dropping...</i><br />
component.setTransferHandler(new WSTransferableListener(this));

@see java.awt.event.MouseEvent
@see org.watto.event.WSTransferableInterface
@see java.awt.datatransfer.Transferable
***********************************************************************************************/

public class WSTransferableListener extends TransferHandler implements MouseMotionListener {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** the event handling class **/
  WSTransferableInterface handler;

  /***********************************************************************************************
  Registers the <code>WSTransferableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSTransferableListener(WSTransferableInterface handler) {
    this.handler = handler;
  }

  /***********************************************************************************************
   Determines whether the component can accept the <code>flavor</code> of transferable object
   being moved.
   @param source the <code>Component</code> being dragged from
   @param flavors the type of data being dropped
   @return always true - <code>handler.onDrop()</code> should determine whether the drop is
           allowed or not.
  ***********************************************************************************************/
  @Override
  public boolean canImport(JComponent source, DataFlavor[] flavors) {
    return true;
  }

  /***********************************************************************************************
  Calls <code>handler.onDrop()</code> when a <i>drop</i> event is triggered
   @param target the <code>Component</code> to drop on
   @param transferredObject the object being dropped
   @return true to indicate the drop was successful, even if the drop failed.
  ***********************************************************************************************/
  @Override
  public boolean importData(JComponent target, Transferable transferredObject) {
    handler.onDrop(transferredObject);
    return true;
  }

  /***********************************************************************************************
   If the <i>handler</i> exists, calls <code>handler.onDrag()</code> when a <i>drag</i> event is
   triggered. Otherwise calls <code>TransferHandler.exportAsDrag()</code>
   @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mouseDragged(MouseEvent event) {
    JComponent source = (JComponent) event.getSource();
    TransferHandler transferHandler = source.getTransferHandler();

    if (transferHandler == null) {
      if (handler == null) {
        return;
      }
      transferHandler = handler.onDrag(source, event);
    }

    transferHandler.exportAsDrag(source, event, TransferHandler.COPY);
  }

  /***********************************************************************************************
  <b><i>Unused</i></b>
  @param event the <code>MouseEvent</code> event
  ***********************************************************************************************/
  @Override
  public void mouseMoved(MouseEvent event) {
  }
}