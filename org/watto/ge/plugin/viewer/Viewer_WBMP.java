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

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import com.sun.imageio.plugins.wbmp.WBMPImageReader;
import com.sun.imageio.plugins.wbmp.WBMPImageReaderSpi;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WBMP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WBMP() {
    super("WBMP", "Wireless Bitmap (WBMP) Image");
    setExtensions("wbmp");
    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    //if (panel instanceof PreviewPanel_Image){
    //  return true;
    //  }
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

      WBMPImageReader pencoder = new WBMPImageReader(new WBMPImageReaderSpi());
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

      WBMPImageReader pencoder = new WBMPImageReader(new WBMPImageReaderSpi());
      pencoder.setInput(is);
      BufferedImage image = pencoder.read(0, null);

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
   * Doesn't write because there are speific formats that can be written only, and I don't know
   * what they are. For example, needs a specific color depth or something.
   **********************************************************************************************
   **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
    /*
     * try {
     *
     * if (! (preview instanceof PreviewPanel_Image)){ return; }
     *
     * PreviewPanel_Image ivp = (PreviewPanel_Image)preview;
     *
     * int imageWidth = ivp.getImageWidth(); int imageHeight = ivp.getImageHeight();
     *
     * if (imageWidth == -1 || imageHeight == -1){ return; }
     *
     * ImageOutputStream out = new MemoryCacheImageOutputStream(new
     * FileManipulatorOutputStream(fm));
     *
     * BufferedImage bufImage = new
     * BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_INT_RGB); Graphics g =
     * bufImage.getGraphics(); g.drawImage(ivp.getImage(), 0, 0, null);
     *
     * WBMPImageWriter pencoder = new WBMPImageWriter(new WBMPImageWriterSpi());
     * pencoder.setOutput(out); pencoder.write(bufImage);
     *
     * out.flush(); out.close();
     *
     * g.dispose();
     *
     * } catch (Throwable e){ logError(e); }
     */
  }

}