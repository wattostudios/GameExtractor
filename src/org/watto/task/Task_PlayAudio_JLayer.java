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

import org.watto.ErrorLogger;
import org.watto.Language;
import javazoom.jl.player.Player;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_PlayAudio_JLayer extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Player sound;

  boolean running = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_PlayAudio_JLayer(Player sound) {
    this.sound = sound;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    if (running) {
      // don't want heaps of threads trying to play the same sound
      return;
    }

    try {
      running = true;
      sound.play();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
  }

}
