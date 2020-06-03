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

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.media.Controller;
import javax.media.Player;
import javax.media.Time;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.task.Task;
import org.watto.task.Task_PlayAudio_JavaMediaFramework;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A preview panel for playing video files from the Java Media Framework
 **********************************************************************************************
 **/
public class PreviewPanel_JavaMediaFrameworkVideo extends PreviewPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  Player player;

  WSButton playbutton;
  WSButton pausebutton;
  WSButton stopbutton;

  boolean isPaused = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_JavaMediaFrameworkVideo(Player player) {
    this.player = player;

    setLayout(new BorderLayout(5, 5));

    constructInterface();

    if (Settings.getBoolean("PlayAudioOnLoad")) {
      playVideo();
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void constructInterface() {

    playbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Play\" />"));
    pausebutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Pause\" />"));
    stopbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Stop\" />"));

    JPanel buttonpanel = new JPanel(new GridLayout(1, 3, 5, 5));
    buttonpanel.add(playbutton);
    buttonpanel.add(pausebutton);
    buttonpanel.add(stopbutton);

    JPanel toppanel = new JPanel(new BorderLayout(5, 5));
    toppanel.add(player.getVisualComponent(), BorderLayout.CENTER);
    toppanel.add(buttonpanel, BorderLayout.SOUTH);

    add(toppanel, BorderLayout.NORTH);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {

    if (c == playbutton) {
      playVideo();
    }
    else if (c == stopbutton) {
      stopVideo();
    }
    else if (c == pausebutton) {
      pauseVideo();
    }
    else {
      return false;
    }

    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    try {
      player.close();
    }
    catch (Throwable t) {
    }
    player = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void pauseVideo() {
    isPaused = true;

    player.stop();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void playVideo() {
    if (!isPaused) {
      stopVideo();
    }

    isPaused = false;

    Task_PlayAudio_JavaMediaFramework task = new Task_PlayAudio_JavaMediaFramework(player, player.getMediaTime().getNanoseconds());
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopVideo() {
    isPaused = false;

    player.stop();
    //player.setFramePosition(0);
    try {
      if (player.getState() != Controller.Unrealized) {
        player.setMediaTime(new Time(0));
      }
    }
    catch (Throwable e) {
    }
    //position.setValue(0);
  }

}
