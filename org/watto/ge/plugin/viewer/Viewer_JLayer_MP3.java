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
import org.watto.component.PreviewPanel_JLayerAudio;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_JLayer_MP3 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_JLayer_MP3() {
    super("MP3", "MP3 Audio");
    setExtensions("mp3", "mp2");
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

      int byte1 = fm.readByte();
      int byte2 = fm.readByte();

      if (byte1 == -1 && byte2 == -5) {
        // MP3 header
        rating += 50;
      }
      else if (byte1 == 73 && byte2 == 68) {
        // ID3 header - probably a MP3 file
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
    return read(new FileManipulator(source, false));
    /*
    try {
      Player player = new Player(new FileInputStream(source));
      PreviewPanel_JLayerAudio preview = new PreviewPanel_JLayerAudio(player);

      return preview;
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
    */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    //return read(fm.getFile());

    try {
      PreviewPanel_JLayerAudio preview = new PreviewPanel_JLayerAudio(fm);
      return preview;
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
