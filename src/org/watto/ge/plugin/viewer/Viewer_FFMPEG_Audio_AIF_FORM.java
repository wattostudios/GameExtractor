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
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Audio;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
Uses FFMPEG to convert a FLAC audio file into a WAV_RIFF audio file, then plays it
**********************************************************************************************
**/
public class Viewer_FFMPEG_Audio_AIF_FORM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_FFMPEG_Audio_AIF_FORM() {
    super("FFMPEG_Audio_AIF_FORM", "FFMPEG-supported AIFF Audio");
    setExtensions("aif", "aiff");
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
  Uses FFMPEG to convert the audio file from XWMA to WAV_RIFF format
  Outputs to 22050 sample rate and 1 channel (mono) for smaller output file size
  **********************************************************************************************
  **/
  public File convertAudio(File source) {
    try {

      String ffmpegPath = Settings.getString("FFMPEG_Path");

      File ffmpegFile = new File(ffmpegPath);

      if (ffmpegFile.exists() && ffmpegFile.isDirectory()) {
        // Path is a directory, append the filename to it
        ffmpegPath = ffmpegPath + File.separatorChar + "ffmpeg.exe";
        ffmpegFile = new File(ffmpegPath);
      }

      if (!ffmpegFile.exists()) {
        // ffmpeg path is invalid
        ErrorLogger.log("ffmpeg can't be found at the path " + ffmpegFile.getAbsolutePath());
        return null;
      }

      ffmpegPath = ffmpegFile.getAbsolutePath();

      String outputFilePath = source.getAbsolutePath() + ".conv.wav";
      if (new File(outputFilePath).exists()) {
        // already converted - previewed this file already
        return new File(outputFilePath);
      }
      ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-i", source.getAbsolutePath(), "-loglevel", "panic", "-ar", "22050", "-ac", "1", "-f", "wav", outputFilePath);

      // Progress dialog (only for files 1MB and larger)
      long fileLength = source.length();
      if (fileLength > 1000000) {
        TaskProgressManager.show(1, 0, Language.get("Progress_ConvertingFiles"));
        TaskProgressManager.setIndeterminate(true);

        // Start the task
        TaskProgressManager.startTask();
      }

      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for FFMPEG to finish

      // Stop the task (only for files 1MB and larger)
      if (fileLength > 1000000) {
        TaskProgressManager.stopTask();
      }

      if (returnCode == 0) {
        // successful conversion
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
          return outputFile;
        }
      }

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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      if (fm.readString(4).equals("FORM")) {
        rating += 20;
      }

      fm.skip(4);

      if (fm.readString(4).equals("AIFF")) {
        rating += 30; // 20+30=50
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

    File convertedFile = convertAudio(source);
    if (convertedFile == null || !convertedFile.exists()) {
      return null;
    }

    try {

      AudioInputStream stream = AudioSystem.getAudioInputStream(convertedFile);
      AudioFormat format = stream.getFormat();
      Info info = new Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
      Clip sound = (Clip) AudioSystem.getLine(info);
      sound.open(stream);

      PreviewPanel_Audio preview = new PreviewPanel_Audio(sound);

      return preview;

    }
    catch (UnsupportedAudioFileException e) {
      ErrorLogger.log("[Viewer_FFMPEG_Audio_AIF_FORM] Could not open the audio file.");
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
  public PreviewPanel read(FileManipulator fm) {
    return read(fm.getFile());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}
