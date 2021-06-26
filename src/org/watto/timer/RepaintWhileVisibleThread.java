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


/***********************************************************************************************
A <code>Thread</code>that repaints a <code>Component</code> every <code>interval</code>
milliseconds. It stops repainting when <code>stopThread()</code> is called, or when the
<code>Component</code> is no longer visible.
***********************************************************************************************/
public class RepaintWhileVisibleThread extends Thread {

  /** The component to repaint **/
  Component panel;

  /** Whether the thread is running or not **/
  boolean running = true;

  /** The number of milliseconds between each repaint **/
  int interval = 1000;


  /***********************************************************************************************
  Creates the <code>Thread</code> to repaint the <code>panel</code> every <code>interval</code>
  milliseconds
  @param panel the <code>Component</code> to repaint
  @param interval the number of milliseconds between each repaint
  ***********************************************************************************************/
  public RepaintWhileVisibleThread(Component panel, int interval){
    this.panel = panel;
    this.interval = interval;
  }


  /***********************************************************************************************
  Repaints the <code>panel</code> every <code>interval</code> milliseconds. <i><b>You must call
  <code>start()</code></b></i> in order to run this class in a separate <code>Thread</code> - 
  <i><b>calling run() will execute the code in the current <code>Thread</code></b></i>. This
  <code>Thread</code> will continue running until <code>stopThread()</code> is called, or until
  the <code>Component</code> is no longer visible.
  @see Thread
  ***********************************************************************************************/
  public void run(){
    while (running) {
      panel.repaint();
      //System.out.println("painting");
      try {
        if (!panel.isVisible()) {
          running = false;
        }
        else {
          Thread.sleep(interval);
        }
      }
      catch (Throwable t) {
        running = false;
      }
    }
  }


  /***********************************************************************************************
  Stops the thread from running
  ***********************************************************************************************/
  public synchronized void stopThread(){
    running = false;
  }
}