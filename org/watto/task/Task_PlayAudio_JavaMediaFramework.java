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

import javax.media.Controller;
import javax.media.Player;
import javax.media.Time;
import org.watto.Language;

/**
 **********************************************************************************************
 * Play Audio (and video) using JavaMediaFramework
 **********************************************************************************************
 **/
public class Task_PlayAudio_JavaMediaFramework extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Player player;
  long position;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_PlayAudio_JavaMediaFramework(Player player, long position) {
    this.player = player;
    this.position = position;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    //while (sound.isRunning()){
    //  // don't want heaps of threads trying to play the same sound
    //  return;
    //  }

    player.stop();
    try {
      if (player.getState() != Controller.Unrealized) {
        player.setMediaTime(new Time(position));
      }
    }
    catch (Throwable t) {
    }
    player.start();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  @Override
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
