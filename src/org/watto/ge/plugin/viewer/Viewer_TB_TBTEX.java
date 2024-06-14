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
import org.watto.ErrorLogger;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TB;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TB_TBTEX extends ViewerPlugin {

  //static int[] palette = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TB_TBTEX() {
    super("TB_TBTEX", "Die Hard Trilogy 2 TB_TEX Image");
    setExtensions("tb_tex");

    setGames("Die Hard Trilogy 2");
    setPlatforms("PC");
    setStandardFileFormat(false);
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
  public boolean canReplace(PreviewPanel panel) {
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_TB) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

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

      int height = 0;
      int width = 0;
      int numFrames = 0;
      int numMipmaps = 0;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
        numFrames = Integer.parseInt(resource.getProperty("FrameCount"));
        numMipmaps = Integer.parseInt(resource.getProperty("MipmapCount"));
      }
      catch (Throwable t) {
        //
      }

      if (height == 0 || width == 0) {
        return null;
      }

      if (numFrames > 100 || numFrames < 1) {
        numFrames = 1;
      }
      if (numMipmaps > 100 || numMipmaps < 1) {
        numMipmaps = 1;
      }

      ImageResource imageResource = null;
      if (numFrames == 1) {

        int mipmapWidth = width;
        int mipmapHeight = height;

        int offset = 0;

        for (int m = 0; m < numMipmaps - 1; m++) {
          offset += (mipmapWidth * mipmapHeight * 2);

          mipmapWidth *= 2;
          mipmapHeight *= 2;
        }

        fm.skip(offset);

        imageResource = ImageFormatReader.readRGB565(fm, mipmapWidth, mipmapHeight);
      }
      else {
        // animation

        ImageResource[] frames = new ImageResource[numFrames];
        for (int f = 0; f < numFrames; f++) {
          frames[f] = ImageFormatReader.readRGB565(fm, width, height);
        }

        imageResource = frames[0];

        for (int f = 0; f < numFrames - 1; f++) {
          frames[f].setNextFrame(frames[f + 1]);
        }
        for (int f = 1; f < numFrames; f++) {
          frames[f].setPreviousFrame(frames[f - 1]);
        }

        frames[0].setPreviousFrame(frames[numFrames - 1]);
        frames[numFrames - 1].setNextFrame(frames[0]);
      }

      fm.close();

      return imageResource;

    }
    catch (

    Throwable t) {
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

  /**
  **********************************************************************************************
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      Image image = ivp.getImage();
      int width = ivp.getImageWidth();
      int height = ivp.getImageHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // Try to get the existing ImageResource (if it was stored), otherwise build a new one
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      if (imageResource == null) {
        imageResource = new ImageResource(image, width, height);
      }

      // Get the source height and width, and mipmap/frame counts. None of these can be changed
      int srcHeight = 0;
      int srcWidth = 0;
      int numFrames = 0;
      int numMipmaps = 0;

      try {
        srcHeight = Integer.parseInt(resourceBeingReplaced.getProperty("Height"));
        srcWidth = Integer.parseInt(resourceBeingReplaced.getProperty("Width"));
        numFrames = Integer.parseInt(resourceBeingReplaced.getProperty("FrameCount"));
        numMipmaps = Integer.parseInt(resourceBeingReplaced.getProperty("MipmapCount"));
      }
      catch (Throwable t) {
        //
      }

      if (srcHeight == 0 || srcWidth == 0) {
        return;
      }

      int mipmapHeight = height;
      int mipmapWidth = width;
      for (int m = 1; m < numMipmaps; m++) {
        mipmapHeight /= 2;
        mipmapWidth /= 2;
      }

      if (mipmapHeight != srcHeight || mipmapWidth != srcWidth) {
        ErrorLogger.log("[TB_TBTEX] Trying to replace an image, but the height and/or width has changed, which isn't supported.");
        return;
      }

      if (numFrames > 100 || numFrames < 1) {
        numFrames = 1;
      }
      if (numMipmaps > 100 || numMipmaps < 1) {
        numMipmaps = 1;
      }

      if (numFrames == 1) {

        ImageManipulator im = new ImageManipulator(imageResource);
        ImageResource[] mipmaps = im.generateMipmaps();

        // we've generated all the mipmaps, but we only need some of them, starting from small to large
        for (int m = numMipmaps - 1; m >= 0; m--) {
          ImageFormatWriter.writeRGB565(fm, mipmaps[m]);
        }

      }
      else {
        // animation

        for (int f = 0; f < numFrames; f++) {
          if (f != 0) {
            imageResource = imageResource.getNextFrame();
          }

          ImageFormatWriter.writeRGB565(fm, imageResource);
        }

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}