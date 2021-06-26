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

import javax.swing.ImageIcon;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.WSLabel;
import org.watto.datatype.ImageResource;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ImagePreviewAnimation extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the image being displayed (the frame) **/
  ImageResource imageResource;

  /** the label where the image is drawn **/
  WSLabel label;

  /** The preview panel is being destroyed, so it asks to stop the animation **/
  boolean stopRequested = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ImagePreviewAnimation(ImageResource imageResource, WSLabel label) {
    this.imageResource = imageResource;
    this.label = label;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    try {
      while (true) {
        Thread.sleep(100);

        if (stopRequested) {
          break;
        }
        label.setIcon(new ImageIcon(imageResource.getImage()));
        imageResource = imageResource.getNextFrame();
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public synchronized void stop() {
    this.stopRequested = true;
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
