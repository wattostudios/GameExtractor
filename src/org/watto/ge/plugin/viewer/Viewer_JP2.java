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

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import com.github.jpeg2000.J2KFile;
import com.github.jpeg2000.J2KReader;
import jj2000.j2k.io.BEBufferedFileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_JP2 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_JP2() {
    super("JP2", "JPEG 2000 (JP2) Image");
    setExtensions("jp2");
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

      /*
      fm.skip(6);
      
      // Header
      String header = fm.readString(4);
      if (header.equals("JFIF")) {
        rating += 50;
      }
      else if (header.equals("Exif")) {
        rating += 25;
      }
      */

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

      J2KFile file = new J2KFile();
      //file.read(new BEBufferedRandomAccessFile(path, "r", 8192));
      file.read(new BEBufferedFileManipulator(fm));
      J2KReader reader = new J2KReader(file);
      BufferedImage image = reader.getBufferedImage();
      reader.close();

      PreviewPanel_Image preview = new PreviewPanel_Image(image, image.getWidth(), image.getHeight());

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return read(fm.getFile());
    }
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

      J2KFile file = new J2KFile();
      //file.read(new BEBufferedRandomAccessFile(path, "r", 8192));
      file.read(new BEBufferedFileManipulator(fm));
      J2KReader reader = new J2KReader(file);
      BufferedImage image = reader.getBufferedImage();
      reader.close();

      int width = image.getWidth();
      int height = image.getHeight();

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