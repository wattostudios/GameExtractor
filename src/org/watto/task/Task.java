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

/***********************************************************************************************
 A task that can be performed <b>(redo)</b> or reversed <b>(undo)</b>. <code>Task</code>s can
 also be chained together to run one after the other.
 ***********************************************************************************************/
public interface Task extends Runnable {

  /** Indicates a <code>redo()</code> operation **/
  public static int DIRECTION_REDO = 1;
  /** Indicates an <code>undo()</code> operation **/
  public static int DIRECTION_UNDO = 2;


  /***********************************************************************************************
  Adds a <code>Task</code> to the end of this <code>Task</code> chain
  @param nextTask the next <code>Task</code> to add to the end of the chain
  ***********************************************************************************************/
  public void addNextTask(Task nextTask);


  /***********************************************************************************************
  Gets the <code>Task</code> to run when this <code>Task</code> has finished
  @return the <code>nextTask</code> to run
  ***********************************************************************************************/
  public Task getNextTask();


  /***********************************************************************************************
  Gets whether the <code>Task</code> and its <code>nextTask</code> have finished running or not
  @return <b>true</b> if the <code>Task</code> and its <code>nextTask</code> have finished running<br />
          <b>false</b> if the <code>Task</code> or its <code>nextTask</code> is still running
  ***********************************************************************************************/
  public boolean hasChainFinished();


  /***********************************************************************************************
  Gets whether the <code>Task</code> has finished running or not
  @return <b>true</b> if the <code>Task</code> has finished running<br />
          <b>false</b> if the <code>Task</code> is still running
  ***********************************************************************************************/
  public boolean hasFinished();


  /***********************************************************************************************
  Gets whether there is a <code>Task</code> that should be run after this <code>Task</code> has
  finished
  @return <b>true</b> if there is a next <code>Task</code><br />
          <b>false</b> if there is no next <code>Task</code>
  ***********************************************************************************************/
  public boolean hasNextTask();


  /***********************************************************************************************
   Perform (redo) the task
   ***********************************************************************************************/
  public void redo();


  /***********************************************************************************************
  Sets the direction of the <code>Task</code>, for use when calling <code>start()</code>
  @param direction the direction
  ***********************************************************************************************/
  public void setDirection(int direction);


  /***********************************************************************************************
  Sets the <code>Task</code> to run when this <code>Task</code> has finished running
  @param nextTask the next <code>Task</code> to run
  ***********************************************************************************************/
  public void setNextTask(Task nextTask);


  /***********************************************************************************************
  A description of the task
  ***********************************************************************************************/
  public String toString();


  /***********************************************************************************************
   Reverse (undo) the task
   ***********************************************************************************************/
  public void undo();
}