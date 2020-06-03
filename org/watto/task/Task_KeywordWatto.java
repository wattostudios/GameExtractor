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

import java.awt.Color;
import org.watto.Language;
import org.watto.ge.GameExtractor;
import org.watto.plaf.LookAndFeelManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_KeywordWatto extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  GameExtractor ge;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_KeywordWatto(GameExtractor ge) {
    this.ge = ge;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {

    Color startColor = LookAndFeelManager.getPropertyColor("COLOR_DARK");

    int orig_r = startColor.getRed();
    int orig_g = startColor.getGreen();
    int orig_b = startColor.getBlue();

    int r = orig_r;
    int g = orig_g;
    int b = orig_b;

    int startValue_r = orig_r / 10;
    int startValue_g = (255 - orig_g) / 10;
    int startValue_b = orig_b / 10;

    while (r > 10 || b > 10 || g < 245) {
      if (r > 10) {
        r -= startValue_r;
      }
      if (b > 10) {
        b -= startValue_b;
      }
      if (g < 245) {
        g += startValue_g;
      }

      LookAndFeelManager.setMidColor(new Color(r, g, b));
      ge.reload();
      try {
        Thread.sleep(50);
      }
      catch (Throwable t) {
        return;
      }
    }

    g = 255;
    r = 0;
    b = 0;

    for (int i = 0; i < 245; i += 10) {
      r += 10;
      g -= 10;

      LookAndFeelManager.setMidColor(new Color(r, g, b));
      ge.reload();
      try {
        Thread.sleep(50);
      }
      catch (Throwable t) {
        return;
      }
    }

    g = 0;
    r = 255;
    b = 0;

    for (int i = 0; i < 245; i += 10) {
      b += 10;
      r -= 10;

      LookAndFeelManager.setMidColor(new Color(r, g, b));
      ge.reload();
      try {
        Thread.sleep(50);
      }
      catch (Throwable t) {
        return;
      }
    }

    g = 0;
    r = 0;
    b = 255;

    int endValue_r = orig_r / 10;
    int endValue_g = orig_g / 10;
    int endValue_b = (255 - orig_b) / 10;

    while (r < orig_r || b > orig_b || g < orig_g) {
      if (r < orig_r) {
        r += endValue_r;
      }
      if (b > orig_b) {
        b -= endValue_b;
      }
      if (g < orig_g) {
        g += endValue_g;
      }

      LookAndFeelManager.setMidColor(new Color(r, g, b));
      ge.reload();
      try {
        Thread.sleep(50);
      }
      catch (Throwable t) {
        return;
      }
    }

    LookAndFeelManager.setMidColor(new Color(orig_r, orig_g, orig_b));

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
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
