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
import org.watto.ge.plugin.archive.Plugin_ZIP_PK;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZIP_PK_SOUND extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZIP_PK_SOUND() {
    super("ZIP_PK_SOUND", "The Witness SOUND Audio File");
    setExtensions("sound");

    setGames("The Witness");
    setPlatforms("PC");
    setStandardFileFormat(false);

    //
    //
    // NOT ENABLED - NOT WORKING
    //
    //
    setEnabled(false);
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
      if (plugin instanceof Plugin_ZIP_PK) {
        rating += 20;
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

      // 4 - Unknown (11)
      if (fm.readInt() == 11) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      // 4 - Unknown (11)
      fm.skip(4);

      // 2 - Extra Header Records (0/1)
      short extraHeaderRecords = fm.readShort();

      // 2 - Unknown (7)
      fm.skip(2);

      int length = 0;

      if (extraHeaderRecords == 1) {
        // 4 - File Length [-4]
        // 2 - Unknown
        fm.skip(6);

        // 4 - File Length
        length = fm.readInt();
        FieldValidator.checkLength(length);
      }
      else if (extraHeaderRecords == 0) {
        // 4 - null
        fm.skip(4);

        // 4 - File Length
        length = fm.readInt();
        FieldValidator.checkLength(length);
      }
      else {
        ErrorLogger.log("[Viewer_ZIP_PK_SOUND] Unknown Header Length Size: " + extraHeaderRecords);
      }

      // X - WAV_RIFF Audio
      //ManipulatorInputStream fmStream = new ManipulatorInputStream(fm);
      String outputFilePath = fm.getFilePath() + ".audio_only.wav";
      File sourceFile = new File(outputFilePath);
      if (sourceFile.exists()) {
        // already converted - previewed this file already
      }
      else {
        // extract the file
        FileManipulator fmOut = new FileManipulator(sourceFile, true);
        fmOut.writeBytes(fm.readBytes(length));
        fmOut.close();
      }

      if (!sourceFile.exists()) {
        // error writing out the temp file
        return null;
      }

      AudioInputStream stream = AudioSystem.getAudioInputStream(sourceFile);
      AudioFormat format = stream.getFormat();
      Info info = new Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
      Clip sound = (Clip) AudioSystem.getLine(info);
      sound.open(stream);

      PreviewPanel_Audio preview = new PreviewPanel_Audio(sound);

      return preview;

    }
    catch (UnsupportedAudioFileException e) {
      ErrorLogger.log("[Viewer_ZIP_PK_SOUND] could not open the audio file.");
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