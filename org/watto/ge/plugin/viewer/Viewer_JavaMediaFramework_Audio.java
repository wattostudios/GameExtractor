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

package org.watto.ge.plugin.viewer;

import java.io.File;
import java.net.URL;
import javax.media.Controller;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_JavaMediaFrameworkAudio;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.MpegAudioDataSource;

/**
 **********************************************************************************************
 * Plugin for loading and previewing Java Media Framework audio files. Requires: - Java Media
 * Framework 2.11+
 **********************************************************************************************
 **/
public class Viewer_JavaMediaFramework_Audio extends ViewerPlugin {

  boolean mp3File = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_JavaMediaFramework_Audio() {
    super("JavaMediaFramework_Audio", "Java Media Framework Audio");
    //setExtensions("mp3", "aiff", "au", "gsm", "mp2", "rmf", "wav");
    setExtensions("aiff", "au", "gsm", "rmf", "wav");

    try {
      Class.forName("javax.media.Player");
    }
    catch (Throwable e) {
      setEnabled(false);
    }
    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      mp3File = false;
      byte[] header = fm.readBytes(4);
      if (new String(header).equals("RIFF")) {
        rating += 49; // so that it tries the Viewer_WAV_RIFF first (because it has a JSlider on it)
      }
      else if (header[0] == -1 && header[1] == -5) {
        // MP3
        //rating += 50;
        mp3File = true;
      }

      return rating;

    }
    catch (Throwable e) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(File source) {
    try {

      /*
       * FileManipulator fm = new FileManipulator(source,"r");
       *
       * boolean fakeNeeded = false;
       * if (fm.readByte() == -1 && fm.readByte() == -5) {
       * if (! FilenameSplitter.getExtension(fm.getFile()).equals("mp3")){
       *   // This is an MP3 file but it doesn't have the correct extension.
       *   // So, create a fake DataSource for it, so the Player generates correctly.
       *   fakeNeeded = true;
       *   }
       *}
       *
       * fm.close();
       */

      URL url = new URL("file:" + source);

      javax.media.Player player;
      if (mp3File) {
        player = javax.media.Manager.createPlayer(new MpegAudioDataSource(url));
      }
      else {
        player = javax.media.Manager.createPlayer(url);
      }

      if (player != null) {
        // check to see if the stream was loaded correctly - a quick simple test to rule out some incompatible stream formats
        player.realize();

        // wait for the player to realise, if it's a little delayed
        int sleepCount = 0;
        while (player.getState() == Controller.Realizing && sleepCount < 5) {
          Thread.sleep(100);
          sleepCount++;
        }

        if (player.getState() == Controller.Unrealized) {
          // failed to load the stream, so it's not a compatible format
          return null;
        }

      }

      PreviewPanel_JavaMediaFrameworkAudio preview = new PreviewPanel_JavaMediaFrameworkAudio(player);

      return preview;

    }
    catch (Throwable e) {
      logError(e);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator source) {
    return read(source.getFile());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}