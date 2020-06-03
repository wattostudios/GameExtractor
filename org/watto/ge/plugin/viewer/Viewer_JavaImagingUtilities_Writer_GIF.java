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
import org.watto.component.PreviewPanel_Image;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_JavaImagingUtilities_Writer_GIF extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_JavaImagingUtilities_Writer_GIF() {
    super("JavaImagingUtilities_Writer_GIF", "GIF Image Writer (JIU)");
    setExtensions("gif");

    try {
      Class.forName("net.sourceforge.jiu.codecs.ImageLoader");
      // NOTE - REPLACED BY THE STANDARD GIF WRITER!!!
      setEnabled(false);
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
    return 0;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator source) {
    return null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, File destination) {
    try {

      if (preview instanceof PreviewPanel_Image) {

        PreviewPanel_Image ivp = (PreviewPanel_Image) preview;

        net.sourceforge.jiu.color.quantization.MedianCutQuantizer quantizer = new net.sourceforge.jiu.color.quantization.MedianCutQuantizer();
        quantizer.setInputImage(net.sourceforge.jiu.gui.awt.ImageCreator.convertImageToRGB24Image(ivp.getImage()));
        quantizer.setPaletteSize(256);
        quantizer.process();
        net.sourceforge.jiu.data.PixelImage quantizedImage = quantizer.getOutputImage();

        net.sourceforge.jiu.codecs.GIFCodec codec = new net.sourceforge.jiu.codecs.GIFCodec();
        codec.setFile(destination, net.sourceforge.jiu.codecs.CodecMode.SAVE);
        codec.setImage(quantizedImage);
        codec.process();
        codec.close();

      }

    }
    catch (Throwable e) {
      logError(e);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator destination) {
    write(preview, destination.getFile());
  }

}
