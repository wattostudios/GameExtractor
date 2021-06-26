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
import org.watto.Settings;
import org.watto.component.WSPopup;
import org.watto.event.WSEvent;
import org.watto.event.WSEventableInterface;
import org.watto.exception.InvalidParameterException;

/***********************************************************************************************
 Controls and manages <code>Task</code>s that can be performed <b>(redo)</b> or reversed
 <b>undo</b>. Can also be used to check whether <code>Task</code>s are running or not, to prevent
 <code>Task</code>s from running out of order.
 ***********************************************************************************************/
public class TaskManager {

  /** A queue of <code>Task</code>s to run in order **/
  static TaskQueue taskQueue = null;

  /***********************************************************************************************
  Creates a new <code>taskQueue</code>, and starts it running in a separate <code>Thread</code>
  ***********************************************************************************************/
  public static synchronized void createTaskQueue() {
    if (taskQueue != null) {
      taskQueue.stopThread();
    }
    taskQueue = new TaskQueue();
    new Thread(taskQueue).start();
  }

  /** The <code>Task</code>s that can be redone **/
  Task[] redoTasks;
  /** The number of <code>Task</code>s in the <code>redoTasks</code> array **/
  int redoTaskCount = 0;

  /** The <code>Task</code>s that can be undone **/
  Task[] undoTasks;

  /** The number of <code>Task</code>s in the <code>undoTasks</code> array **/
  int undoTaskCount = 0;

  /** Monitors that should be triggered when a task is performed **/
  WSEventableInterface[] monitors = new WSEventableInterface[0];

  /***********************************************************************************************
   Creates an empty <code>TaskManager</code>
   ***********************************************************************************************/
  public TaskManager() {
    clear();
  }

  /***********************************************************************************************
   Adds a <code>Task</code>. All redo <code>Task</code>s will be removed, otherwise
   <code>Task</code>s will be in the wrong order.
   @param task the task to add
   ***********************************************************************************************/
  public synchronized void add(Task task) {
    // remove any redo tasks
    redoTaskCount = 0;

    addUndoTask(task);

    fireTaskListChanged();
  }

  /***********************************************************************************************
   Adds a <code>monitor</code> that listens for <code>WSEvent.TASK_LIST_CHANGED</code> events.
   @param monitor the monitor to add
   ***********************************************************************************************/
  public void addMonitor(WSEventableInterface monitor) {
    int numMonitors = monitors.length;
    monitors = resize(monitors, numMonitors + 1);
    monitors[numMonitors] = monitor;
  }

  /***********************************************************************************************
  Adds a <code>Task</code> to the <code>redoTasks</code> array.
  @param task the <code>Task</code> to add
  ***********************************************************************************************/
  synchronized void addRedoTask(Task task) {

    /*
    // REDO TASKS ARE STORED IN THE NORMAL ORDER, SO THE NEWEST TASK IS AT THE END OF THE LIST index[n]
    
    int redoArrayLength = redoTasks.length;
    
    if (redoTaskCount >= redoArrayLength) {
      // the array is full, so need to move them along before adding the new task
      redoTaskCount = redoArrayLength - 1;
      System.arraycopy(redoTasks, 1, redoTasks, 0, redoTaskCount);
    }
    
    // add the task to the end
    redoTasks[redoTaskCount] = task;
    redoTaskCount++;
    */

    // REDO TASKS ARE STORED IN THE OPPOSITE ORDER, SO THE NEWEST TASK IS AT THE TOP OF THE LIST index[0]

    int redoArrayLength = redoTasks.length;

    if (redoTaskCount >= redoArrayLength) {
      // the array is full, so need to move them along before adding the new task
      redoTaskCount = redoArrayLength - 1;
      System.arraycopy(redoTasks, 0, redoTasks, 1, redoTaskCount);
    }
    else {
      // move tasks along 1 position, so the newest task can be at the front
      System.arraycopy(redoTasks, 0, redoTasks, 1, redoTaskCount);
    }

    // add the task to the end
    //redoTasks[redoTaskCount] = task;
    redoTasks[0] = task;
    redoTaskCount++;
  }

  /***********************************************************************************************
  Adds a <code>Task</code> to the <code>taskQueue</code>. Use this to run a <code>Task</code> in
  the correct order, but without changing the <code>redoTasks</code> or <code>undoTasks</code> arrays.
  @param task the standalone <code>Task</code> to run
  ***********************************************************************************************/
  public synchronized void addTask(Task task) {
    taskQueue.addTask(task);
  }

  /***********************************************************************************************
  Adds a <code>Task</code> to the <code>undoTasks</code> array.
  @param task the <code>Task</code> to add
  ***********************************************************************************************/
  synchronized void addUndoTask(Task task) {

    // UNDO TASKS ARE STORED IN THE OPPOSITE ORDER, SO THE NEWEST TASK IS AT THE TOP OF THE LIST index[0]

    int undoArrayLength = undoTasks.length;

    if (undoTaskCount >= undoArrayLength) {
      // the array is full, so need to move them along before adding the new task
      undoTaskCount = undoArrayLength - 1;
      System.arraycopy(undoTasks, 0, undoTasks, 1, undoTaskCount);
    }
    else {
      // move tasks along 1 position, so the newest task can be at the front
      System.arraycopy(undoTasks, 0, undoTasks, 1, undoTaskCount);
    }

    // add the task to the end
    //undoTasks[undoTaskCount] = task;
    undoTasks[0] = task;
    undoTaskCount++;
  }

  /***********************************************************************************************
   Are there any <code>Task</code>s in the <code>redoTasks</code> array?
   @return <b>true</b> if there are any redo <code>Task</code>s<br />
           <b>false</b> if there are no redo <code>Task</code>s
   ***********************************************************************************************/
  public boolean canRedo() {
    return redoTaskCount > 0;
  }

  /***********************************************************************************************
  Are there any <code>Task</code>s in the <code>undoTasks</code> array?
  @return <b>true</b> if there are any undo <code>Task</code>s<br />
          <b>false</b> if there are no undo <code>Task</code>s
  ***********************************************************************************************/
  public boolean canUndo() {
    return undoTaskCount > 0;
  }

  /***********************************************************************************************
   Clears all the redo and undo <code>Task</code>s
   ***********************************************************************************************/
  public synchronized void clear() {
    int taskArraySize = Settings.getInt("NumberOfTasks");
    if (taskArraySize <= 0) {
      taskArraySize = 20;
    }

    redoTasks = new Task[taskArraySize];
    undoTasks = new Task[taskArraySize];

    redoTaskCount = 0;
    undoTaskCount = 0;

    createTaskQueue();

    fireTaskListChanged();
  }

  /***********************************************************************************************
   Triggers a <code>WSEvent</code>. Alerts all monitors that the event has occurred.
   @param event the event that was triggered.
   ***********************************************************************************************/
  void fireEvent(WSEvent event) {
    int numMonitors = monitors.length;
    for (int i = 0; i < numMonitors; i++) {
      monitors[i].onEvent(event.getComponent(), event, event.getType());
    }
  }

  /***********************************************************************************************
   Triggers a <code>WSEvent.TASK_LIST_CHANGED</code> event
   ***********************************************************************************************/
  public void fireTaskListChanged() {
    fireEvent(new WSEvent(this, WSEvent.TASK_LIST_CHANGED));
  }

  /***********************************************************************************************
   Gets the <code>Task</code>s in the <code>redoTasks</code> array
   @return the redo <code>Task</code>s
   ***********************************************************************************************/
  public Task[] getRedoableTasks() {
    Task[] trimmedRedoTasks = new Task[redoTaskCount];
    System.arraycopy(redoTasks, 0, trimmedRedoTasks, 0, redoTaskCount);
    return trimmedRedoTasks;
  }

  /***********************************************************************************************
   Gets the number of <code>Task</code>s in the <code>redoTasks</code> array
   @return the number of redo <code>Task</code>s
   ***********************************************************************************************/
  public int getRedoCount() {
    return redoTaskCount;
  }

  /***********************************************************************************************
  Gets the <code>Task</code>s in the <code>undoTasks</code> array
  @return the undo <code>Task</code>s
  ***********************************************************************************************/
  public Task[] getUndoableTasks() {
    Task[] trimmedUndoTasks = new Task[undoTaskCount];
    System.arraycopy(undoTasks, 0, trimmedUndoTasks, 0, undoTaskCount);
    return trimmedUndoTasks;
  }

  /***********************************************************************************************
  Gets the number of <code>Task</code>s in the <code>undoTasks</code> array
  @return the number of undo <code>Task</code>s
  ***********************************************************************************************/
  public int getUndoCount() {
    return undoTaskCount;
  }

  /***********************************************************************************************
  Redo a single <code>Task</code> in the <code>redoTasks</code> array
  ***********************************************************************************************/
  public synchronized void redo() {
    redo(1);
  }

  /***********************************************************************************************
   Redo the next <code>redoCount</code> <code>Task</code>s in the <code>redoTasks</code> array
   @param redoCount the number of <code>Task</code>s to redo.
   ***********************************************************************************************/
  public synchronized void redo(int redoCount) {

    // invalid number of tasks to redo
    if (redoCount == 0) {
      return;
    }
    if (redoCount < 0) {
      try {
        throw new InvalidParameterException("The redoCount must be >= 0, but you specified " + redoCount);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
        return;
      }
    }

    /*
    //
    // REDO TASKS ARE IN NORMAL ORDER (newest at the back), SO NEED TO READ FROM index[n] TO THE FRONT
    //

    // setting the number of redos if there are too many given
    if (redoCount > redoTaskCount) {
      redoCount = redoTaskCount;
    }

    // There are multiple tasks to redo, so chain them together in the right order, then run them
    int firstTask = redoTaskCount - 1;
    //int lastTask = redoTaskCount - 1 - redoCount;
    int lastTask = redoTaskCount - redoCount;

    if (firstTask < 0) {
      return;
    }

    for (int i = firstTask; i >= lastTask; i--) {
      // prepare the task for redo
      Task task = redoTasks[i];

      if (task instanceof AbstractTask) {
        // Disable showing Task-specific popups, instead will show a "Redo Complete" popup at the end of the changes
        ((AbstractTask) task).setShowPopups(false);
      }

      task.setDirection(Task.DIRECTION_REDO);

      // add the task to the end of the queue
      taskQueue.addTask(task);

      // move the task into the undo array
      addUndoTask(task);
    }

    // decrease the counter now that the tasks have been moved (effectively drops the "redone" tasks off the end of the array)
    redoTaskCount -= redoCount;
    */

    //
    // REDO TASKS ARE IN REVERSE ORDER (newest at the front), SO NEED TO READ FROM index[0] TO THE BACK
    //

    // setting the number of redos if there are too many given
    if (redoCount > redoTaskCount) {
      redoCount = redoTaskCount;
    }

    // There are multiple tasks to redo, so chain them together in the right order, then run them
    int firstTask = 0;
    int lastTask = redoCount - 1;

    for (int i = firstTask; i <= lastTask; i++) {
      // prepare the task for redo
      Task task = redoTasks[i];

      if (task instanceof AbstractTask) {
        // Disable showing Task-specific popups, instead will show a "redo Complete" popup at the end of the changes
        ((AbstractTask) task).setShowPopups(false);
      }

      task.setDirection(Task.DIRECTION_REDO);

      // add the task to the end of the queue
      taskQueue.addTask(task);

      // move the task into the undo array
      addUndoTask(task);
    }

    // Move the items forward in the redo array so that the "redone" items are removed
    System.arraycopy(redoTasks, redoCount, redoTasks, 0, redoTaskCount - redoCount);

    // decrease the counter now that the tasks have been moved
    redoTaskCount -= redoCount;

    // fire the task list changed event before we do the tasks
    fireTaskListChanged();

    WSPopup.showMessageInNewThread("TaskManager_RedoComplete", true);

  }

  /***********************************************************************************************
   Finds the <code>task</code> in the <code>redoTasks</code> array, then runs all <code>Task</code>s
   up to and including the <code>task</code>
   @param task the <code>Task</code> to redo
   ***********************************************************************************************/
  public synchronized void redo(Task task) {
    int redoCount = 0;

    /*
    // REDO TASKS ARE IN NORMAL ORDER (newest at the back), SO NEED TO READ FROM index[n] TO THE FRONT
    for (int i = redoTaskCount - 1; i >= 0; i--) {
      redoCount++;
      if (redoTasks[i] == task) {
        redo(redoCount);
      }
    }
    */

    // REDO TASKS ARE IN REVERSE ORDER (newest at the front), SO NEED TO READ FROM index[0] TO THE BACK
    for (int i = 0; i < redoTaskCount; i++) {
      redoCount++;
      if (redoTasks[i] == task) {
        redo(redoCount);
      }
    }
  }

  /***********************************************************************************************
   Removes a <code>WSEventableInterface</code> event monitor
   @param monitor the monitor to remove
   ***********************************************************************************************/
  public void removeMonitor(WSEventableInterface monitor) {
    int numMonitors = monitors.length;
    for (int i = 0; i < numMonitors; i++) {
      if (monitors[i] == monitor) {
        // found the monitor, so lets remove it
        int numRemaining = numMonitors - i - 1;
        System.arraycopy(monitors, i + 1, monitors, i, numRemaining);
        monitors = resize(monitors, numMonitors - 1);
        return;
      }
    }
  }

  /***********************************************************************************************
   Resizes the <code>source</code array of type <code>Task</code> to a new size
   @param source the source <code>Task</code> array
   @param newSize the new size of the array
   @return the new array
   ***********************************************************************************************/
  Task[] resize(Task[] source, int newSize) {
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    Task[] target = new Task[newSize];
    System.arraycopy(source, 0, target, 0, copySize);

    return target;
  }

  /***********************************************************************************************
   Resizes the <code>source</code array of type <code>WSEventableInterface</code> to a new size
   @param source the source <code>WSEventableInterface</code> array
   @param newSize the new size of the array
   @return the new array
   ***********************************************************************************************/
  WSEventableInterface[] resize(WSEventableInterface[] source, int newSize) {
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    WSEventableInterface[] target = new WSEventableInterface[newSize];
    System.arraycopy(source, 0, target, 0, copySize);

    return target;
  }

  /***********************************************************************************************
  Undo a single <code>Task</code> in the <code>undoTasks</code> array
  ***********************************************************************************************/
  public synchronized void undo() {
    undo(1);
  }

  /***********************************************************************************************
   Undo the next <code>undoCount</code> <code>Task</code>s in the <code>undoTasks</code> array
   @param undoCount the number of <code>Task</code>s to undo.
   ***********************************************************************************************/
  public synchronized void undo(int undoCount) {

    // invalid number of tasks to undo
    if (undoCount == 0) {
      return;
    }
    if (undoCount < 0) {
      try {
        throw new InvalidParameterException("The undoCount must be >= 0, but you specified " + undoCount);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
        return;
      }
    }

    //
    // UNDO TASKS ARE IN REVERSE ORDER (newest at the front), SO NEED TO READ FROM index[0] TO THE BACK
    //

    // setting the number of undos if there are too many given
    if (undoCount > undoTaskCount) {
      undoCount = undoTaskCount;
    }

    // There are multiple tasks to undo, so chain them together in the right order, then run them
    int firstTask = 0;
    int lastTask = undoCount - 1;

    for (int i = firstTask; i <= lastTask; i++) {
      // prepare the task for undo
      Task task = undoTasks[i];

      if (task instanceof AbstractTask) {
        // Disable showing Task-specific popups, instead will show a "Undo Complete" popup at the end of the changes
        ((AbstractTask) task).setShowPopups(false);
      }

      task.setDirection(Task.DIRECTION_UNDO);

      // add the task to the end of the queue
      taskQueue.addTask(task);

      // move the task into the redo array
      addRedoTask(task);
    }

    // Move the items forward in the undo array so that the "undone" items are removed
    System.arraycopy(undoTasks, undoCount, undoTasks, 0, undoTaskCount - undoCount);

    // decrease the counter now that the tasks have been moved
    undoTaskCount -= undoCount;

    // fire the task list changed event before we do the tasks
    fireTaskListChanged();

    WSPopup.showMessageInNewThread("TaskManager_UndoComplete", true);

  }

  /***********************************************************************************************
   Finds the <code>task</code> in the <code>undoTasks</code> array, then runs all <code>Task</code>s
   up to and including the <code>task</code>
   @param task the <code>Task</code> to undo
   ***********************************************************************************************/
  public synchronized void undo(Task task) {
    int undoCount = 0;

    // UNDO TASKS ARE IN REVERSE ORDER (newest at the front), SO NEED TO READ FROM index[0] TO THE BACK
    for (int i = 0; i < undoTaskCount; i++) {
      undoCount++;
      if (undoTasks[i] == task) {
        undo(undoCount);
      }
    }

    /*
    for (int i = undoTaskCount - 1; i >= 0; i--) {
      undoCount++;
      if (undoTasks[i] == task) {
        undo(undoCount);
      }
    }
    */

  }

}