/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.task;

import java.awt.Cursor;
import org.watto.ErrorLogger;
import org.watto.component.ComponentRepository;
import org.watto.component.WSOverlayProgressDialog;
import org.watto.component.WSPanel;
import org.watto.component.WSProgressDialog;
import org.watto.component.WSProgressDialogInterface;
import org.watto.ge.GameExtractor;

/**
**********************************************************************************************
Controls Tasks and the Progress bar display, such as making sure only 1 task can happen at a
time (ie Thread control)
**********************************************************************************************
**/
public class TaskProgressManager {

  static boolean taskRunning = false;

  /** The dialog showing the progress bars **/
  static WSProgressDialogInterface progress = null;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static boolean canDoTask() {
    return (taskRunning == false);
  }

  /***********************************************************************************************
  Sets this <code>WSProgressDialog</code> to show the main <code>JProgressBar</code> as indeterminate
  @param indeterminate <b>true</b> if the main <code>JProgressBar</code> is indeterminate<br />
                       <b>false</b> if it shows real values
  ***********************************************************************************************/
  public static void setIndeterminate(boolean indeterminate) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setIndeterminate(indeterminate);
  }

  /***********************************************************************************************
  Sets this <code>WSProgressDialog</code> to show indeterminate <code>JProgressBar</code>s
  @param indeterminate <b>true</b> if the <code>JProgressBar</code>s are indeterminate<br />
                       <b>false</b> if they show real values
  @param barNumber the <code>JProgressBar</code> to set as indeterminate
  ***********************************************************************************************/
  public static void setIndeterminate(boolean indeterminate, int barNumber) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setIndeterminate(indeterminate, barNumber);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void setMaximum(long maximum) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setMaximum(maximum);
  }

  /***********************************************************************************************
  Sets the maximum value of the given <code>JProgressBar</code>
  @param newMaximum the new maximum value
  @param barNumber the <code>JProgressBar</code> to set the maximum value of
  ***********************************************************************************************/
  public static void setMaximum(long newMaximum, int barNumber) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setMaximum(newMaximum, barNumber);
  }

  /***********************************************************************************************
  Sets the message shown on the <code>WSProgressDialog</code>
  @param newMessage the message to show
  ***********************************************************************************************/
  public static void setMessage(String newMessage) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setMessage(newMessage);
  }

  /***********************************************************************************************
  Sets the number of <code>JProgressBar</code>s to show on the <code>WSProgressDialog</code>
  @param newNumbars the number of <code>JProgressBar</code>s to show
  ***********************************************************************************************/
  public static void setNumberOfBars(int newNumBars) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setNumberOfBars(newNumBars);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void setTaskRunning(boolean running) {
    try {
      if (taskRunning && running) {
        throw new Exception("A task is already running!");
      }
      taskRunning = running;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void setValue(long value) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setValue(value);
  }

  /***********************************************************************************************
  Sets the current value of the given <code>JProgressBar</code>
  @param newValue the new current value
  @param barNumber the <code>JProgressBar</code> to set the current value of
  ***********************************************************************************************/
  public static void setValue(long newValue, int barNumber) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setValue(newValue, barNumber);
  }

  /***********************************************************************************************
  Shows or hides the <code>WSProgressDialog</code>
  @param visible <b>true</b> to show the <code>WSProgressDialog</code><br />
                 <b>false</b> to hide the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public static void setVisible(boolean visible) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.setVisible(visible);
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code> and a <i>Please
  Wait</i> message
  @param newMaximum the maximum value of the <code>JProgressBar</code>
  ***********************************************************************************************/
  public static void show(int newMaximum) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.show(newMaximum);
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with multiple <code>JProgressBar</code>s
  @param numBars the number of <code>JProgressBar</code>s to show
  @param newMaximum the maximum value of the <code>JProgressBar</code>s
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public static void show(int numBars, int newMaximum, String newMessage) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.show(numBars, newMaximum, newMessage);
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code>
  @param newMaximum the maximum value of the <code>JProgressBar</code>
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public static void show(int newMaximum, String newMessage) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.show(newMaximum, newMessage);
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code>
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public static void show(String newMessage) {
    if (progress == null) {
      return; // probably running from the command line
    }
    progress.show(newMessage);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void startTask() {
    setTaskRunning(true);

    if (progress == null) {
      return; // probably running from the command line
    }

    Cursor cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    progress.setVisible(true);
    progress.setCursor(cursor);

    GameExtractor.getInstance().setCursor(cursor);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void stopTask() {
    setTaskRunning(false);

    if (progress == null) {
      return; // probably running from the command line
    }

    Cursor cursor = Cursor.getDefaultCursor();

    // Remove the existing panel on the overlay
    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null) {
      overlayPanel.removeAll();
      overlayPanel.setVisible(false);

      overlayPanel.validate();
      overlayPanel.repaint();
    }

    progress.setVisible(false);
    progress.setCursor(cursor);

    GameExtractor.getInstance().setCursor(cursor);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public TaskProgressManager() {
    if (ComponentRepository.has("PopupOverlay")) {
      // use the OverlayProgressDialog
      progress = WSOverlayProgressDialog.getInstance();
    }
    else {
      // use the Popup ProgressDialog
      progress = WSProgressDialog.getInstance();
    }
  }

}
