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
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.task.Task;
import org.watto.task.Task_PlayAudio_JavaMediaFramework;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A preview panel for playing sound files from the Java Media Framework
 **********************************************************************************************
 **/
public class PreviewPanel_JavaMediaFrameworkAudio extends PreviewPanel implements WSClickableInterface {

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
  public PreviewPanel_JavaMediaFrameworkAudio(Player player) {
    this.player = player;

    setLayout(new BorderLayout(5, 5));

    constructInterface();

    if (Settings.getBoolean("PlayAudioOnLoad")) {
      playAudio();
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void constructInterface() {

    // 3.16 Added "codes" to every XML-built object, so that they're cleaned up when the object is destroyed (otherwise it was being retained in the ComponentRepository)

    playbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Play\" />"));
    pausebutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Pause\" />"));
    stopbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Stop\" />"));

    JPanel buttonpanel = new JPanel(new GridLayout(1, 3, 5, 5));
    buttonpanel.add(playbutton);
    buttonpanel.add(pausebutton);
    buttonpanel.add(stopbutton);

    JPanel toppanel = new JPanel(new BorderLayout(5, 5));
    toppanel.add(buttonpanel, BorderLayout.NORTH);

    WSPanel imagePanel = new WSPanel(XMLReader.read("<WSPanel code=\"AudioPreview_ImagePanelWrapper\" border-width=\"8\" />"));
    imagePanel.add(new JLabel(new ImageIcon("images/General/Audio_Cover.png")), BorderLayout.CENTER);

    WSPanel overallPanel = new WSPanel(XMLReader.read("<WSPanel code=\"AudioPreview_OverallPanelWrapper\" vertical-gap=\"8\" />"));
    overallPanel.add(new JPanel(), BorderLayout.NORTH);
    overallPanel.add(imagePanel, BorderLayout.CENTER);
    overallPanel.add(toppanel, BorderLayout.SOUTH);

    add(overallPanel, BorderLayout.NORTH);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {

    if (c == playbutton) {
      playAudio();
    }
    else if (c == stopbutton) {
      stopAudio();
    }
    else if (c == pausebutton) {
      pauseAudio();
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
  public void pauseAudio() {
    isPaused = true;

    player.stop();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void playAudio() {
    if (!isPaused) {
      stopAudio();
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
  public void stopAudio() {
    isPaused = false;

    player.stop();
    try {
      if (player.getState() != Controller.Unrealized) {
        player.setMediaTime(new Time(0));
      }
    }
    catch (Throwable e) {
    }

  }

}
