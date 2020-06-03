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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_JavaMediaFrameworkVideo;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
 **********************************************************************************************
 * Plugin for loading and previewing Java Media Framework video files. Requires: - Java Media
 * Framework 2.11+
 **********************************************************************************************
 **/
public class Viewer_JavaMediaFramework_Video extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_JavaMediaFramework_Video() {
    super("JavaMediaFramework_Video", "Java Media Framework Video");
    setExtensions("avi", "mpeg", "mpg", "qt", "mov");

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
      if (fm.readString(4).equals("RIFF")) {
        rating += 48; // So RIFF-WAV tries to use the other ones - only come here for RIFF-AVI
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

      URL url = new URL("file:" + source);
      javax.media.Player player = javax.media.Manager.createRealizedPlayer(url);
      PreviewPanel_JavaMediaFrameworkVideo preview = new PreviewPanel_JavaMediaFrameworkVideo(player);

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
