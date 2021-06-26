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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.ImageIcon;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;
import com.sun.imageio.plugins.gif.GIFImageWriter;
import com.sun.imageio.plugins.gif.GIFImageWriterSpi;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GIF_GIF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GIF_GIF() {
    super("GIF_GIF", "GIF Image");
    setExtensions("gif");
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
    else if (panel instanceof PreviewPanel_3DModel) {
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

      // header
      if (fm.readString(3).equals("GIF")) {
        rating += 50;
      }
      else {
        rating = 0;
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

      ImageIcon icon = new ImageIcon(path.getAbsolutePath());
      PreviewPanel_Image preview = new PreviewPanel_Image(icon.getImage(), icon.getIconWidth(), icon.getIconHeight());

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

      BufferedImage image = ImageIO.read(new ManipulatorInputStream(fm));
      int width = image.getWidth();
      int height = image.getHeight();

      PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, false);
      pixelGrabber.grabPixels();

      IndexColorModel model = (IndexColorModel) pixelGrabber.getColorModel();
      int numColors = model.getMapSize();
      int[] palette = new int[numColors];
      for (int c = 0; c < numColors; c++) {
        palette[c] = model.getRGB(c);
      }

      // get the pixels, and convert them to positive values in an int[] array
      byte[] indexes = (byte[]) pixelGrabber.getPixels();
      int numPixels = indexes.length;
      int[] pixels = new int[numPixels];
      for (int p = 0; p < numPixels; p++) {
        int value = indexes[p];
        if (value < 0) {
          value = 256 + value;
        }
        pixels[p] = palette[value];
      }

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
    try {

      Image image = null;
      int imageWidth = -1;
      int imageHeight = -1;

      if (preview instanceof PreviewPanel_Image) {
        PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
        image = ivp.getImage();
        imageWidth = ivp.getImageWidth();
        imageHeight = ivp.getImageHeight();
      }
      else if (preview instanceof PreviewPanel_3DModel) {
        PreviewPanel_3DModel ivp = (PreviewPanel_3DModel) preview;
        image = ivp.getImage();
        imageWidth = ivp.getImageWidth();
        imageHeight = ivp.getImageHeight();
      }
      else {
        return;
      }

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      ImageOutputStream out = new MemoryCacheImageOutputStream(new ManipulatorOutputStream(fm));

      BufferedImage bufImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
      Graphics g = bufImage.getGraphics();
      g.drawImage(image, 0, 0, null);

      GIFImageWriter pencoder = new GIFImageWriter(new GIFImageWriterSpi());
      pencoder.setOutput(out);
      pencoder.write(bufImage);

      //out.flush();
      out.close();

      g.dispose();

    }
    catch (Throwable e) {
      logError(e);
    }
  }

}