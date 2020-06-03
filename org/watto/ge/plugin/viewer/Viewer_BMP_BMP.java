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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;
import com.sun.imageio.plugins.bmp.BMPImageReader;
import com.sun.imageio.plugins.bmp.BMPImageReaderSpi;
import com.sun.imageio.plugins.bmp.BMPImageWriter;
import com.sun.imageio.plugins.bmp.BMPImageWriterSpi;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BMP_BMP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BMP_BMP() {
    super("BMP_BMP", "Bitmap (BMP) Image");
    setExtensions("bmp");
    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
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

      if (fm.readString(2).equals("BM")) {
        rating += 51;
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
  public PreviewPanel read(File path) {
    try {

      ImageInputStream is = new FileImageInputStream(path);

      BMPImageReader pencoder = new BMPImageReader(new BMPImageReaderSpi());
      pencoder.setInput(is);
      BufferedImage image = pencoder.read(0, null);

      PreviewPanel_Image preview = new PreviewPanel_Image(image, image.getWidth(), image.getHeight());

      is.close();

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
  public PreviewPanel read(FileManipulator fm) {
    return read(fm.getFile());
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

      ImageInputStream is = new MemoryCacheImageInputStream(new ManipulatorInputStream(fm));

      BMPImageReader pencoder = new BMPImageReader(new BMPImageReaderSpi());
      pencoder.setInput(is);
      BufferedImage image = pencoder.read(0, null);

      int width = image.getWidth();
      int height = image.getHeight();

      PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, true);
      pixelGrabber.grabPixels();

      // get the pixels, and convert them to positive values in an int[] array
      try {
        int[] pixels = (int[]) pixelGrabber.getPixels();
        return new ImageResource(pixels, width, height);
      }
      catch (ClassCastException e) {
        byte[] pixelBytes = (byte[]) pixelGrabber.getPixels();

        int numPixels = pixelBytes.length;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = ByteConverter.unsign(pixelBytes[i]);
        }
        return new ImageResource(pixels, width, height);
      }

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;

      int imageWidth = ivp.getImageWidth();
      int imageHeight = ivp.getImageHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      ImageOutputStream out = new MemoryCacheImageOutputStream(new ManipulatorOutputStream(fm));

      BufferedImage bufImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
      Graphics g = bufImage.getGraphics();
      g.drawImage(ivp.getImage(), 0, 0, null);

      BMPImageWriter pencoder = new BMPImageWriter(new BMPImageWriterSpi());
      pencoder.setOutput(out);
      pencoder.write(bufImage);

      out.flush();
      out.close();

      g.dispose();

    }
    catch (Throwable e) {
      logError(e);
    }
  }

}