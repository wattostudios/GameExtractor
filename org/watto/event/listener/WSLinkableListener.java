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

import javax.swing.JComponent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.watto.event.WSLinkableInterface;


/***********************************************************************************************
Listens for <i>link clicked</i> <code>HyperlinkEvent</code>s and passes them to the
<code>WSLinkableInterface</code> handler class
@see javax.swing.event.HyperlinkEvent
@see org.watto.event.WSLinkableInterface
***********************************************************************************************/
public class WSLinkableListener implements HyperlinkListener {

  /** the event handling class **/
  WSLinkableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSLinkableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSLinkableListener(WSLinkableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  Calls <code>handler.onHyperlink()</code> when a <i>link clicked</i> <code>HyperlinkEvent</code>
  is triggered
  @param event the <code>HyperlinkEvent</code> event
  ***********************************************************************************************/
  public void hyperlinkUpdate(HyperlinkEvent event){
    handler.onHyperlink((JComponent)event.getSource(),event);
  }
}