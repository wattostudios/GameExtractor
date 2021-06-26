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
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorUnclosableInputStream;
import org.watto.task.Task;
import org.watto.task.Task_PlayAudio_JLayer;
import org.watto.xml.XMLReader;
import javazoom.jl.player.Player;

/**
 **********************************************************************************************
 * A preview panel for playing sound files
 **********************************************************************************************
 **/
public class PreviewPanel_JLayerAudio extends PreviewPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  Player sound = null;
  FileManipulator fm;

  WSButton playbutton;
  WSButton stopbutton;

  /**
   **********************************************************************************************
   * For extended classes only!
   **********************************************************************************************
   **/
  public PreviewPanel_JLayerAudio() {
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PreviewPanel_JLayerAudio(FileManipulator fm) {
    this.fm = fm;

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
    if (sound != null) {
      sound.close();
    }
    sound = null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void playAudio() {

    if (sound == null) {
      try {
        fm.seek(0);
        ManipulatorUnclosableInputStream stream = new ManipulatorUnclosableInputStream(fm);
        sound = new Player(stream);
      }
      catch (Throwable t) {
        logError(t);
      }
    }

    if (sound != null) {
      Task_PlayAudio_JLayer task = new Task_PlayAudio_JLayer(sound);
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void stopAudio() {
    sound.close();
    sound = null;
  }

}
