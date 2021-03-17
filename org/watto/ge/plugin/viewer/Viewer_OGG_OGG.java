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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_OggVorbisAudio;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
 **********************************************************************************************
 * Plugin for loading and previewing Ogg Vorbis audio files. Requires: - JOrbis 0.013+
 **********************************************************************************************
 **/
public class Viewer_OGG_OGG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_OGG_OGG() {
    super("OGG_OGG", "Ogg Vorbis Audio");
    setExtensions("ogg");

    try {
      Class.forName("com.jcraft.jogg.Packet");
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
    // NOTE: Write isn't normally triggered. Special case only for extracting OGGs from Unreal Engine 4 (PAK_38)
    if (panel instanceof PreviewPanel_OggVorbisAudio) {
      return true;
    }
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
      if (fm.readString(4).equals("OggS")) {
        rating += 50;
      }

      // null
      if (fm.readByte() == 0) {
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
    // NOTE: Write isn't normally triggered. Special case only for extracting OGGs from Unreal Engine 4 (PAK_38)
    if (!(preview instanceof PreviewPanel_OggVorbisAudio)) {
      return;
    }

    File oggFile = ((PreviewPanel_OggVorbisAudio) preview).getOggFilePath();
    if (!oggFile.exists() || !oggFile.isFile()) {
      return;
    }

    int length = (int) oggFile.length();

    FileManipulator oggFM = new FileManipulator(oggFile, false);
    for (int i = 0; i < length; i++) {
      fm.writeByte(oggFM.readByte());
    }
    oggFM.close();
  }

}