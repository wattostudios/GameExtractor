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
import javax.sound.midi.Sequencer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.task.Task;
import org.watto.task.Task_PlayAudio_Midi;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A panel for displaying Midi audio files
 **********************************************************************************************
 **/
public class PreviewPanel_MidiAudio extends PreviewPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  Sequencer sound;

  WSButton playbutton;
  WSButton pausebutton;
  WSButton stopbutton;

  JSlider position = new JSlider(JScrollBar.HORIZONTAL);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_MidiAudio(Sequencer sound) {
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

    playbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Play\" />"));
    pausebutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Pause\" />"));
    stopbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Stop\" />"));

    position.setMaximum((int) sound.getMicrosecondLength() / 1000000);
    position.setMinorTickSpacing((int) sound.getMicrosecondLength() / 1000000 / 50);
    position.setValue(0);
    position.setSnapToTicks(true);
    position.setPaintTicks(true);

    JPanel buttonpanel = new JPanel(new GridLayout(1, 3, 5, 5));
    buttonpanel.add(playbutton);
    buttonpanel.add(pausebutton);
    buttonpanel.add(stopbutton);

    JPanel toppanel = new JPanel(new GridLayout(2, 1, 5, 5));
    toppanel.add(position);
    toppanel.add(buttonpanel);

    add(toppanel, BorderLayout.NORTH);
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
    Task_PlayAudio_Midi task = new Task_PlayAudio_Midi(sound, position);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopAudio() {
    sound.stop();
    sound.setMicrosecondPosition(0);
    position.setValue(0);
  }

}
