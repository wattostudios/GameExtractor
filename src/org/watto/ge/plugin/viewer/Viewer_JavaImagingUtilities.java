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

import java.awt.Image;
import java.io.File;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_JavaImagingUtilities extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_JavaImagingUtilities() {
    super("JavaImagingUtilities", "Java Imaging Utilities Images");
    setExtensions("pbm", "pgm", "ppm", "ras", "pcd", "tif", "tiff", "bmp", "iff", "lbm", "psd", "dib");

    try {
      Class.forName("net.sourceforge.jiu.codecs.ImageLoader");
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
      if (fm.readString(2).equals("BM")) {
        rating += 50;
      }

      // header
      //if (fm.readString(3).equals("GIF")){
      //  rating += 50;
      //  }
      //else {
      //  rating = 0;
      //  }
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

      net.sourceforge.jiu.data.PixelImage image = net.sourceforge.jiu.codecs.ImageLoader.load(source);
      Image normImage = net.sourceforge.jiu.gui.awt.ImageCreator.convertToAwtImage(image, net.sourceforge.jiu.gui.awt.ImageCreator.DEFAULT_ALPHA);
      PreviewPanel_Image preview = new PreviewPanel_Image(normImage, image.getWidth(), image.getHeight());

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
