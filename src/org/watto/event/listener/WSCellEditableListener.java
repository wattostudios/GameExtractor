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

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import org.watto.event.WSCellEditableInterface;


/***********************************************************************************************
Listens for <code>ChangeEvent</code>s on a cell, and passes them to the
<code>WSCellEditableInterface</code> handler class
@see java.awt.event.ChangeEvent
@see org.watto.event.WSCellEditableInterface
***********************************************************************************************/
public class WSCellEditableListener implements CellEditorListener {

  /** the event handling class **/
  WSCellEditableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSCellEditableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSCellEditableListener(WSCellEditableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************

  ***********************************************************************************************/
  @Override
  public void editingStopped(ChangeEvent e) {
    handler.editingStopped(e);
  }

  /***********************************************************************************************

  ***********************************************************************************************/
  @Override
  public void editingCanceled(ChangeEvent e) {
    handler.editingCanceled(e);
  }
}