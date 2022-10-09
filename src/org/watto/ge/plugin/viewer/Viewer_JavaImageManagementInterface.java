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
import java.awt.image.PixelGrabber;
import java.io.File;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_JavaImageManagementInterface extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_JavaImageManagementInterface() {
    super("JavaImageManagementInterface", "Java Image Management Interface");
    setExtensions("tif", "tiff", "pict", "pct", "pic", "psd", "bmp", "ddb", "dib", "tga", /*"ico",*/ "cur", "ras", "xbm", "xpm", "pcx");

    try {
      Class.forName("com.sun.jimi.core.Jimi");
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

      Image image = com.sun.jimi.core.Jimi.getImage(source.getAbsolutePath());

      if (image.getWidth(null) < 1 || image.getHeight(null) < 1) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(image, image.getWidth(null), image.getHeight(null));

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
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      Image image = com.sun.jimi.core.Jimi.getImage(new ManipulatorInputStream(fm));

      int width = image.getWidth(null);
      int height = image.getHeight(null);

      if (width < 1 || height < 1) {
        return null;
      }

      PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, false);
      pixelGrabber.grabPixels();

      // get the pixels, and convert them to positive values in an int[] array
      int[] pixels = (int[]) pixelGrabber.getPixels();

      return new ImageResource(pixels, width, height);

    }
    catch (Throwable t) {
      logError(t);
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}