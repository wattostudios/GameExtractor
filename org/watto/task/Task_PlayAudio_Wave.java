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

import javax.sound.sampled.Clip;
import javax.swing.JSlider;
import org.watto.ErrorLogger;
import org.watto.Language;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_PlayAudio_Wave extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Clip sound;
  JSlider position;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_PlayAudio_Wave(Clip sound, JSlider position) {
    this.sound = sound;
    this.position = position;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void redo() {
    while (sound.isRunning()) {
      // don't want heaps of threads trying to play the same sound
      return;
    }

    if (position == null) {
      // just play the sound
      sound.stop();
      sound.setFramePosition(0);
      sound.start();
    }
    else {
      // play the sound
      sound.stop();
      sound.setFramePosition(position.getValue());
      sound.start();

      try {
        // keep updating the JSlider
        Thread.sleep(250); // so the sound has a chance to start running before we query it below
        while (sound.isRunning()) {
          position.setValue(sound.getFramePosition());
          Thread.sleep(250);
        }

        // reset when it reaches the end
        if (sound.getFramePosition() >= sound.getFrameLength()) {
          position.setValue(0);
          sound.setFramePosition(0);
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

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
