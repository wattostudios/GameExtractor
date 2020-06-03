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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WEBP_RIFF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WEBP_RIFF() {
    super("WEBP_RIFF", "WebP Image");
    setExtensions("webp");
    setStandardFileFormat(true);
    setEnabled(true);

    // Register the ImageIO plugin for reading this type of file
    /*
    try {
      IIORegistry r = javax.imageio.spi.IIORegistry.getDefaultInstance();
      WebPImageReaderSpi s = new WebPImageReaderSpi();
      r.registerServiceProvider(s);
    }
    catch (NoClassDefFoundError e) {
      ErrorLogger.log(e);
    }
    */

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
        fm.skip(4);
        if (fm.readString(4).equals("WEBP")) {
          rating += 50;
        }
        else {
          rating = 0;
        }
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
  public PreviewPanel read(File source) {

    File convertedFile = convertImage(source);
    if (convertedFile == null || !convertedFile.exists()) {
      return null;
    }

    return new Viewer_PNG_PNG().read(convertedFile);
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
  Uses dwebp to convert the WebP image into a PNG image
  **********************************************************************************************
  **/
  public File convertImage(File source) {
    try {

      String dwebpPath = Settings.getString("dwebp_Path");

      File dwebpFile = new File(dwebpPath);

      if (dwebpFile.exists() && dwebpFile.isDirectory()) {
        // Path is a directory, append the filename to it
        dwebpPath = dwebpPath + File.separatorChar + "dwebp.exe";
        dwebpFile = new File(dwebpPath);
      }

      if (!dwebpFile.exists()) {
        // dwebp path is invalid
        ErrorLogger.log("dwebp can't be found at the path " + dwebpFile.getAbsolutePath());
        return null;
      }

      dwebpPath = dwebpFile.getAbsolutePath();

      //String outputFilePath = source.getAbsolutePath() + ".conv.png";
      String outputFilePath = Settings.getString("TempDirectory") + File.separatorChar + source.getName() + ".conv.png";
      if (new File(outputFilePath).exists()) {
        // already converted - previewed this file already
        return new File(outputFilePath);
      }
      ProcessBuilder pb = new ProcessBuilder(dwebpPath, "-o", outputFilePath, source.getAbsolutePath());

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_ConvertingFiles"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      TaskProgressManager.startTask();

      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for dwebp to finish

      // Stop the task
      TaskProgressManager.stopTask();

      if (returnCode == 0) {
        // successful conversion
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
          return outputFile;
        }
      }

      return null;

    }
    catch (Throwable t) {
      logError(t);
      return null;
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

      File convertedFile = convertImage(fm.getFile());
      if (convertedFile == null || !convertedFile.exists()) {
        return null;
      }

      FileManipulator imageFM = new FileManipulator(convertedFile, false);
      ImageResource imageResource = new Viewer_PNG_PNG().readThumbnail(imageFM);
      imageFM.close();

      return imageResource;

      /*
      java.util.Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("WEBP");
      ImageReader imageReader = readers.next();
      //ImageInputStream iis = ImageIO.createImageInputStream(new ManipulatorInputStream(fm));
      //imageReader.setInput(iis, false);
      imageReader.setInput(new MemoryCacheImageInputStream(new ManipulatorInputStream(fm)), false);
      
      BufferedImage image = imageReader.read(0);
      int width = image.getWidth();
      int height = image.getHeight();
      
      PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, false);
      pixelGrabber.grabPixels();
      
      // get the pixels, and convert them to positive values in an int[] array
      int[] pixels = (int[]) pixelGrabber.getPixels();
      
      return new ImageResource(pixels, width, height);
      */

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