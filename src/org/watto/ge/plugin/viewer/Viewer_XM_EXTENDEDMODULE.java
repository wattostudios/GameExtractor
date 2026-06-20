/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.stream.ManipulatorBufferInputStream;
import org.watto.io.stream.ManipulatorBufferOutputStream;

import ibxm.IBXM;
import ibxm.Module;
import ibxm.WavInputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_XM_EXTENDEDMODULE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_XM_EXTENDEDMODULE() {
    super("XM_EXTENDEDMODULE", "XM and S3M Audio");
    setExtensions("mod", "xm", "s3m");
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

      if (fm.readString(15).equals("Extended module")) {
        rating += 50;
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

      IBXM ibxm = new IBXM(new Module(new java.io.FileInputStream(source)), 48000);
      WavInputStream in = new WavInputStream(ibxm);

      ManipulatorBufferOutputStream out = new ManipulatorBufferOutputStream(new ByteBuffer(in.getBytesRemaining()));

      // Convert the file to a WAV
      try {
        byte[] buf = new byte[ibxm.getMixBufferLength() * 2];
        int remain = in.getBytesRemaining();
        while (remain > 0) {
          int count = remain > buf.length ? buf.length : remain;
          count = in.read(buf, 0, count);
          out.write(buf, 0, count);
          remain -= count;
        }
      }
      finally {
        out.close();
      }

      in.close();

      ManipulatorBufferInputStream inStream = new ManipulatorBufferInputStream(out.getManipulatorBuffer());
      inStream.seek(0);

      AudioInputStream stream = AudioSystem.getAudioInputStream(inStream);
      AudioFormat format = stream.getFormat();
      Info info = new Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
      Clip sound = (Clip) AudioSystem.getLine(info);
      sound.open(stream);

      PreviewPanel_Audio preview = new PreviewPanel_Audio(sound);

      return preview;

    }
    catch (UnsupportedAudioFileException e) {
      ErrorLogger.log("Viewer_WAV_RIFF could not open the audio file.");
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
