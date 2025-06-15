/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.component.PreviewPanel_JLayerAudio;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_VPK;
import org.watto.ge.plugin.archive.Plugin_VPK_2;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.stream.ManipulatorInputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VPK_2_VSNDC extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VPK_2_VSNDC() {
    super("VPK_2_VSNDC", "Valve VSND_C Audio");
    setExtensions("vsnd_c");

    setGames("Deadlock");
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

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_VPK || readPlugin instanceof Plugin_VPK_2) {
        rating += 50;
      }
      else if (!(readPlugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      fm.skip(16);

      if (fm.readString(4).equals("RED2")) {
        rating += 5;
      }

      fm.skip(8);

      if (fm.readString(4).equals("DATA")) {
        rating += 5;
      }

      fm.skip(8);

      if (fm.readString(4).equals("CTRL")) {
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

      long arcSize = fm.getLength();

      // 4 - Audio Data Offset
      int audioOffset = fm.readInt();
      FieldValidator.checkOffset(audioOffset, arcSize);

      // 2 - Version Major? (12)
      // 2 - Version Minor? (5)
      // 4 - Unknown (8)
      // 4 - Unknown (3)

      // RED2 AND DATA AND CONTROL BLOCKS
      // 4 - Header (RED2)
      // 4 - Unknown (44)
      // 4 - RED2 Block Length

      // 4 - Header (DATA)
      // 4 - DATA Block Length (not including these 3 header fields)
      // 4 - null

      // 4 - Header (CTRL)
      fm.skip(40);

      // 4 - CTRL Block Length (not including these 2 header fields)
      int blockLength = fm.readInt();
      FieldValidator.checkOffset(blockLength, arcSize);

      // X - CTRL Block
      fm.skip(blockLength);

      // AUDIO DESCRIPTOR
      // 16 - Hash?
      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown (25)
      // 4 - Unknown (1)
      // 4 - Unknown (199)
      // 2 - Unknown (2)
      // 2 - Unknown (2)
      // 4 - Unknown (319)
      // 4 - Unknown (283)
      // 4 - Unknown (1)
      // 12 - null
      // 1 - Unknown (98)
      // 4 - Unknown (15)
      // 2 - Unknown (2)
      // 2 - Unknown (1)
      // 4 - Unknown
      // 4 - Unknown
      // 1 - null
      // 1 - Unknown (0=PCM16, 11=MP3)
      // 2 - Unknown (3=PCM16, 20=MP3)
      // 2 - Unknown
      fm.skip(91);

      // 4 - Frequency (44100)
      int frequency = fm.readInt();
      FieldValidator.checkRange(frequency, 0, 48000);

      // 4 - Unknown (4)
      // 4 - Unknown (5)
      // X - Unknown
      int skipSize = audioOffset - (int) fm.getOffset();
      fm.skip(skipSize);

      // AUDIO DATA
      // X - Raw Audio Data
      int dataLength = (int) (fm.getLength() - audioOffset);
      byte[] dataBytes = fm.readBytes(dataLength);

      //if (audioFormat == 11) { // MP3
      // quick little check for MP3 data
      if (dataBytes[0] == -1 && dataBytes[1] == -5) { // MP3
        PreviewPanel_JLayerAudio preview = new PreviewPanel_JLayerAudio(new FileManipulator(new ByteBuffer(dataBytes)));
        return preview; // quick exit
      }

      // Convert the audio file to a WAV format
      byte[] wavHeader = Exporter_Custom_WAV_RawAudio.getInstance().pcmwav_header(frequency, (short) 1, (short) 16, dataLength, (short) 0x0001, -1, (short) -1, null);
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
      ErrorLogger.log("[Viewer_VPK_2_VSNDC] Could not open audio file");
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
