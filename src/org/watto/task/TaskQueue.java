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

package org.watto.task;

import org.watto.ErrorLogger;

/***********************************************************************************************
 A <code>Thread</code> that runs a number of <code>Task</code>s in order. The <code>Thread</code>
 can be standalone to run a set of <code>Task</code>s then stop, or it can <code>wait()</code>
 for more <code>Task</code>s to be added, allowing it to continue running over and over.
 ***********************************************************************************************/
public class TaskQueue implements Runnable {

  /** The current <code>Task</code> in the queue **/
  Task task = null;

  /** Causes the <code>Thread</code> to stop running **/
  boolean stopThread = false;

  /** Whether to wait for more <code>Task</code>s at the end of the run(), or to end the <code>Thread</code> **/
  boolean loop = true;

  /***********************************************************************************************
  Creates an empty queue
  ***********************************************************************************************/
  public TaskQueue() {
  }

  /***********************************************************************************************
  Adds a <code>Task</code> to the end of the queue
  @param nextTask the <code>Task</code> to add
  ***********************************************************************************************/
  public synchronized void addTask(Task nextTask) {
    if (task == null) {
      // no task exists, so add it
      task = nextTask;
    }
    else {
      // add the task to the end of the queue
      task.addNextTask(nextTask);
    }

    // wake up the run(), if it is waiting for a Task to process
    notifyAll();
  }

  /***********************************************************************************************
  Gets whether the <code>Task</code>s have finished running or not
  @return <b>true</b> if the <code>Task</code>s have finished running<br />
          <b>false</b> if there is at least 1 <code>Task</code> still to run
  ***********************************************************************************************/
  public synchronized boolean hasFinished() {
    return (task == null || task.hasChainFinished());
  }

  /***********************************************************************************************
  Runs the <code>Task</code>s in the queue
  ***********************************************************************************************/
  @Override
  public synchronized void run() {
    while (willLoop()) {

      try {
        wait();
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
        return;
      }

      if (willStop()) {
        return;
      }

      task.run();
      task = null;

      if (willStop()) {
        return;
      }

      // go back and wait for the next Task
    }
  }

  /***********************************************************************************************
  Sets whether the <code>Thread</code> will wait for more <code>Task</code>s when it has finished
  the <code>run()</code>, or just stop the <code>Thread</code>
  @param loop <b>true</b> if the <code>Thread</code> will wait for more <code>Task</code>s<br />
              <b>false</b> if the <code>Thread</code> will stop
  ***********************************************************************************************/
  public synchronized void setLoop(boolean loop) {
    this.loop = loop;
  }

  /***********************************************************************************************
  Stops the <code>Thread</code> from running
  ***********************************************************************************************/
  public synchronized void stopThread() {
    stopThread = true;
    notifyAll();
  }

  /***********************************************************************************************
  Gets whether the <code>Thread</code> will wait for more <code>Task</code>s when it has finished
  the <code>run()</code>, or just stop the <code>Thread</code>
  @return <b>true</b> if the <code>Thread</code> will wait for more <code>Task</code>s<br />
          <b>false</b> if the <code>Thread</code> will stop
  ***********************************************************************************************/
  public synchronized boolean willLoop() {
    return loop;
  }

  /***********************************************************************************************
  Gets whether the <code>Thread</code> will stop at the next available time or not
  @return <b>true</b> if the <code>Thread</code> is due to be stopped<br />
          <b>false</b> if the <code>Thread</code> isn't waiting to be stopped
  ***********************************************************************************************/
  public synchronized boolean willStop() {
    return stopThread;
  }

}