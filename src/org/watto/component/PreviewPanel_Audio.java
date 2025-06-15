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

import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.task.Task;
import org.watto.task.Task_PlayAudio_Wave;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A preview panel for playing sound files
 **********************************************************************************************
 **/
public class PreviewPanel_Audio extends PreviewPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  Clip sound;

  WSButton playbutton;
  WSButton pausebutton;
  WSButton stopbutton;

  JSlider position = new JSlider(JScrollBar.HORIZONTAL);

  /**
   **********************************************************************************************
   * For extended classes only!
   **********************************************************************************************
   **/
  public PreviewPanel_Audio() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_Audio(Clip sound) {
    this.sound = sound;

    setLayout(new BorderLayout(2, 2));

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

    position.setMaximum(sound.getFrameLength());
    position.setMinorTickSpacing(sound.getFrameLength() / 50);
    position.setValue(0);
    position.setSnapToTicks(true);
    position.setPaintTicks(true);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
    buttonPanel.add(playbutton);
    buttonPanel.add(pausebutton);
    buttonPanel.add(stopbutton);

    JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    topPanel.add(position);
    topPanel.add(buttonPanel);

    WSPanel imagePanel = new WSPanel(XMLReader.read("<WSPanel code=\"AudioPreview_ImagePanelWrapper\" border-width=\"8\" />"));
    imagePanel.add(new JLabel(new ImageIcon("images/General/Audio_Cover.png")), BorderLayout.CENTER);

    WSPanel overallPanel = new WSPanel(XMLReader.read("<WSPanel code=\"AudioPreview_OverallPanelWrapper\" vertical-gap=\"8\" />"));
    overallPanel.add(new JPanel(), BorderLayout.NORTH);
    overallPanel.add(imagePanel, BorderLayout.CENTER);
    overallPanel.add(topPanel, BorderLayout.SOUTH);

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
    if (sound != null) {
      sound.stop();
    }
    sound.close();
    sound = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void pauseAudio() {
    sound.stop();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void playAudio() {
    Task_PlayAudio_Wave task = new Task_PlayAudio_Wave(sound, position);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopAudio() {
    sound.stop();
    sound.setFramePosition(0);
    position.setValue(0);
  }

}
