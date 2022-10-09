/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_OggVorbisAudio;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
 **********************************************************************************************

 **********************************************************************************************
 **/
public class Viewer_FST_FAST_2_FSAMPLE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_FST_FAST_2_FSAMPLE() {
    super("FST_FAST_2_FSAMPLE", "Crazy Machines 2 FSample Audio");
    setExtensions("fsample");

    setGames("Crazy Machines 2");
    setPlatforms("PC");
    setStandardFileFormat(false);

    try {
      Class.forName("com.jcraft.jogg.Packet");
    }
    catch (Throwable e) {
      setEnabled(false);
    }

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

      fm.skip(40);

      // Header
      if (fm.readString(4).equals("OggS")) {
        rating += 25;
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

      // strip the first 40 characters from the file, then we're left with plain OGG audio
      File tempFile = new File(source.getAbsolutePath() + ".ogg");
      if (tempFile.exists()) {
        // probably already extracted
        source = tempFile;
      }
      else {
        // create the file, without the 40-byte header
        int readLength = (int) (source.length() - 40);
        if (readLength < 0) {
          readLength = 0;
        }

        FileManipulator fmIn = new FileManipulator(source, false);
        FileManipulator fmOut = new FileManipulator(tempFile, true);

        fmIn.skip(40);
        fmOut.writeBytes(fmIn.readBytes(readLength));

        fmIn.close();
        fmOut.close();

        source = tempFile;
      }

      PreviewPanel_OggVorbisAudio preview = new PreviewPanel_OggVorbisAudio(source);

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