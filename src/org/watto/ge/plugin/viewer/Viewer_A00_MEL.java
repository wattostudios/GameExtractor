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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Audio;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_A00;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.stream.ManipulatorInputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_A00_MEL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_A00_MEL() {
    super("A00_MEL", "Steel Panthers MEL Audio");
    setExtensions("mel");

    setGames("Steel Panthers 2",
        "Steel Panthers 3");
    setPlatforms("PC");
    setStandardFileFormat(false);
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_A00) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      fm.skip(4);

      int frequency = fm.readInt();
      if (frequency == 11025 || frequency == 22050) {
        rating += 5;
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
      FileManipulator fm = new FileManipulator(source, false);
      PreviewPanel panel = read(fm);
      fm.close();
      return panel;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      // Get the audio details from the header
      // 4 - null
      fm.skip(4);

      // 4 - Frequency (11025 or 22050)
      int frequency = fm.readInt();

      // 8 - null
      // 4 - Channels? (1)
      // 4 - null
      fm.skip(16);

      // X - Raw Audio Data
      int dataLength = (int) (fm.getLength() - 24);
      byte[] dataBytes = fm.readBytes(dataLength);

      // Convert the audio file to a WAV format
      byte[] wavHeader = Exporter_Custom_WAV_RawAudio.getInstance().pcmwav_header(frequency, (short) 1, (short) 8, dataLength, (short) 0x0001, -1, (short) -1, null);
      int headerLength = wavHeader.length;

      int totalLength = headerLength + dataLength;
      byte[] audioData = new byte[totalLength];
      System.arraycopy(wavHeader, 0, audioData, 0, headerLength);
      System.arraycopy(dataBytes, 0, audioData, headerLength, dataLength);

      ManipulatorInputStream source = new ManipulatorInputStream(new FileManipulator(new ByteBuffer(audioData)));

      // Play the audio
      AudioInputStream stream = AudioSystem.getAudioInputStream(source);
      AudioFormat format = stream.getFormat();
      Info info = new Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
      Clip sound = (Clip) AudioSystem.getLine(info);
      sound.open(stream);

      PreviewPanel_Audio preview = new PreviewPanel_Audio(sound);

      return preview;

    }
    catch (UnsupportedAudioFileException e) {
      ErrorLogger.log("[Viewer_A00_MEL] Could not open audio file");
      return null;
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}
