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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import org.watto.event.WSSelectableInterface;


/***********************************************************************************************
Listens for various <i>select</i> events and passes them to the
<code>WSSelectableInterface</code> handler class
@see java.awt.event.ActionEvent
@see java.awt.event.ItemEvent
@see javax.swing.event.ListSelectionEvent
@see javax.swing.event.TreeSelectionEvent
@see org.watto.event.WSSelectableInterface
***********************************************************************************************/
public class WSSelectableListener implements ActionListener, ListSelectionListener, ItemListener, TreeSelectionListener {

  /** the event handling class **/
  WSSelectableInterface handler;


  /***********************************************************************************************
  Registers the <code>WSSelectableInterface</code> handler class
  @param handler the event handling class
  ***********************************************************************************************/
  public WSSelectableListener(WSSelectableInterface handler){
    this.handler = handler;
  }


  /***********************************************************************************************
  Calls <code>handler.onSelect()</code> when a <i>select</i> <code>ActionEvent</code> is triggered
  @param event the <code>ActionEvent</code> event
  ***********************************************************************************************/
  public void actionPerformed(ActionEvent event){
    handler.onSelect((JComponent)event.getSource(),event);
  }


  /***********************************************************************************************
  Calls <code>handler.onSelect()</code> when a <i>select</i> <code>ItemEvent</code> is triggered,
  and <code>handler.onDeselect()</code> when a <i>deselect</i> <code>ItemEvent</code> is triggered
  @param event the <code>ItemEvent</code> event
  ***********************************************************************************************/
  public void itemStateChanged(ItemEvent event){
    if (event.getStateChange() == ItemEvent.SELECTED) {
      handler.onSelect((JComponent)event.getSource(),event);
    }
    else {
      handler.onDeselect((JComponent)event.getSource(),event);
    }
  }


  /***********************************************************************************************
  Calls <code>handler.onSelect()</code> when a <i>select</i> <code>ListSelectionEvent</code> is triggered
  @param event the <code>ListSelectionEvent</code> event
  ***********************************************************************************************/
  public void valueChanged(ListSelectionEvent event){
    if (!event.getValueIsAdjusting()) {
      handler.onSelect((JComponent)event.getSource(),event);
    }
  }


  /***********************************************************************************************
  Calls <code>handler.onSelect()</code> when a <i>select</i> <code>TreeSelectionEvent</code> is triggered,
  and <code>handler.onDeselect()</code> when a <i>deselect</i> <code>TreeSelectionEvent</code> is triggered
  @param event the <code>TreeSelectionEvent</code> event
  ***********************************************************************************************/
  public void valueChanged(TreeSelectionEvent event){
    if (event.isAddedPath()) {
      handler.onSelect((JComponent)event.getSource(),event);
    }
    else {
      handler.onDeselect((JComponent)event.getSource(),event);
    }
  }
}