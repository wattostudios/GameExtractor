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
 A <code>Task</code> that can be performed. Can also chain a number of <code>Task</code>s together
 so they run in order.
 @see org.watto.task.Task
 ***********************************************************************************************/
public abstract class AbstractTask implements Task {

  /** The direction to run the <code>Task</code> when calling <code>start()</code> **/
  int direction = DIRECTION_REDO;

  /** whether the <code>Task</code> has finished running or not **/
  boolean hasFinished = false;

  /** The <code>Task</code> to run when this <code>Task</code> is finished **/
  Task nextTask = null;

  /** So we can hide multiple popups when redoing/undoing **/
  boolean showPopups = true;

  /***********************************************************************************************
  Adds a <code>Task</code> to the end of this <code>Task</code> chain
  @param nextTask the next <code>Task</code> to add to the end of the chain
  ***********************************************************************************************/
  @Override
  public void addNextTask(Task nextTask) {
    if (this.nextTask == null) {
      // this is the last Task in the queue, so add it as the nextTask
      this.nextTask = nextTask;
    }
    else {
      // this isn't the end of the queue, so try to set it on nextTask instead
      this.nextTask.addNextTask(nextTask);
    }
  }

  /***********************************************************************************************
  Gets the <code>Task</code> to run when this <code>Task</code> has finished
  @return the <code>nextTask</code> to run
  ***********************************************************************************************/
  @Override
  public Task getNextTask() {
    return nextTask;
  }

  /***********************************************************************************************
  Gets whether the <code>Task</code> and its <code>nextTask</code> have finished running or not
  @return <b>true</b> if the <code>Task</code> and its <code>nextTask</code> have finished running<br />
          <b>false</b> if the <code>Task</code> or its <code>nextTask</code> is still running
  ***********************************************************************************************/
  @Override
  public boolean hasChainFinished() {
    if (nextTask == null) {
      return hasFinished;
    }
    else {
      return nextTask.hasFinished();
    }
  }

  /***********************************************************************************************
  Gets whether the <code>Task</code> has finished running or not
  @return <b>true</b> if the <code>Task</code> has finished running<br />
          <b>false</b> if the <code>Task</code> is still running
  ***********************************************************************************************/
  @Override
  public boolean hasFinished() {
    return hasFinished;
  }

  /***********************************************************************************************
  Gets whether there is a <code>Task</code> that should be run after this <code>Task</code> has
  finished
  @return <b>true</b> if there is a next <code>Task</code><br />
          <b>false</b> if there is no next <code>Task</code>
  ***********************************************************************************************/
  @Override
  public boolean hasNextTask() {
    return nextTask != null;
  }

  public boolean isShowPopups() {
    return showPopups;
  }

  /***********************************************************************************************
  Runs the current <code>Task</code>, in the set <code>direction</code>. Use <code>start()</code>
  instead if you wish to run this <code>Task</code> in a separate <code>Thread</code>. If the
  <code>nextTask</code> is set, the <code>nextTask</code> will be run after this <code>Task</code>
  has finished. The <code>nextTask</code> runs in the same <code>Thread</code> as this
  <code>Task</code> runs in.
  ***********************************************************************************************/
  @Override
  public void run() {
    if (direction == DIRECTION_REDO) {
      redo();
    }
    else if (direction == DIRECTION_UNDO) {
      undo();
    }

    hasFinished = true;

    if (nextTask != null) {
      nextTask.run();
    }
  }

  /***********************************************************************************************
  Sets the direction of the <code>Task</code>, for use when calling <code>start()</code>
  @param direction the direction
  ***********************************************************************************************/
  @Override
  public void setDirection(int direction) {
    if (direction != DIRECTION_REDO && direction != DIRECTION_UNDO) {
      try {
        throw new Exception("The direction must be either DIRECTION_REDO or DIRECTION_UNDO");
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
      this.direction = DIRECTION_REDO;
    }
    else {
      this.direction = direction;
    }
  }

  /***********************************************************************************************
  Sets the <code>Task</code> to run when this <code>Task</code> has finished running
  @param nextTask the next <code>Task</code> to run
  ***********************************************************************************************/
  @Override
  public void setNextTask(Task nextTask) {
    this.nextTask = nextTask;
  }

  public void setShowPopups(boolean showPopups) {
    this.showPopups = showPopups;
  }

}