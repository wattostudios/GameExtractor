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

package org.watto.component.task;

import org.watto.component.WSPopup;
import org.watto.task.AbstractTask;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ShowPopupInNewThread extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  String typeIn;
  String codeIn;
  boolean hidableIn;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_ShowPopupInNewThread(String typeIn, String codeIn, boolean hidableIn) {
    this.typeIn = typeIn;
    this.codeIn = codeIn;
    this.hidableIn = hidableIn;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void redo() {
    WSPopup.show(typeIn, codeIn, hidableIn);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void undo() {
  }

}
