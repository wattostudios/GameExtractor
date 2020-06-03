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
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.task.Task;
import org.watto.task.Task_PlayAudio_OggVorbis;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A preview panel for playing (streaming) Ogg Vorbis sound files Requires: - JOrbis 0.013+
 **********************************************************************************************
 **/
public class PreviewPanel_OggVorbisAudio extends PreviewPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  Task_PlayAudio_OggVorbis task;
  Thread thread;

  WSButton playbutton;
  WSButton stopbutton;

  File path;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PreviewPanel_OggVorbisAudio(File path) {
    this.path = path;

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
    stopbutton = new WSButton(XMLReader.read("<WSButton code=\"AudioPreview_Stop\" />"));

    JPanel buttonpanel = new JPanel(new GridLayout(1, 2, 5, 5));
    buttonpanel.add(playbutton);
    buttonpanel.add(stopbutton);

    WSPanel imagePanel = new WSPanel(XMLReader.read("<WSPanel border-width=\"8\" />"));
    imagePanel.add(new JLabel(new ImageIcon("images/General/Audio_Cover.png")), BorderLayout.CENTER);

    WSPanel overallPanel = new WSPanel(XMLReader.read("<WSPanel vertical-gap=\"8\" />"));
    overallPanel.add(new JPanel(), BorderLayout.NORTH);
    overallPanel.add(imagePanel, BorderLayout.CENTER);
    overallPanel.add(buttonpanel, BorderLayout.SOUTH);

    add(overallPanel, BorderLayout.NORTH);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {

    if (c == playbutton) {
      stopAudio();
      playAudio();
    }
    else if (c == stopbutton) {
      stopAudio();
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
    stopAudio();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void playAudio() {
    if (thread == null || !thread.isAlive()) {
      task = new Task_PlayAudio_OggVorbis(path);
      task.setDirection(Task.DIRECTION_REDO);
      thread = new Thread(task);
      thread.start();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void stopAudio() {
    if (thread != null) {
      task.stopAudio();
    }
  }

}
