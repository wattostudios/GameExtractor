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

package org.watto.timer;

import java.awt.Component;
import java.util.TimerTask;


/***********************************************************************************************
Repaints a <code>Component</code> every time this event is called. To be used with a
java.util.Timer
@see java.util.Timer
***********************************************************************************************/
public class RepaintTimer extends TimerTask {

  /** The component to repaint **/
  Component panel;


  /***********************************************************************************************
  Creates the <code>Timer</code> to repaint the <code>panel</code>
  @param panel the <code>Component</code> to repaint
  ***********************************************************************************************/
  public RepaintTimer(Component panel){
    this.panel = panel;
  }


  /***********************************************************************************************
  Repaints the <code>Component</code> at the <code>Timer</code> trigger.
  ***********************************************************************************************/
  public void run(){
    panel.repaint();
  }
}