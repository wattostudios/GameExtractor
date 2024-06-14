/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

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
import org.watto.ge.plugin.archive.Plugin_TEX_5;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_5_PXL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_5_PXL() {
    super("TEX_5_PXL", "Dino Crisis 2 Paletted Image");
    setExtensions("pxl");

    setGames("Dino Crisis 2");
    setPlatforms("PC");
    setStandardFileFormat(false);
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_TEX_5) {
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
    catch (Throwable t) {
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

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
      }
      catch (Throwable t) {
        //
      }

      if (height == 0 || width == 0) {
        return null;
      }

      // X - Pixels (palette indexes)
      // read blocks of 64x*32h*4, then read the next block
      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      for (int h = 0; h < height; h += 32) {
        for (int w = 0; w < width; w += 64) {

          for (int h2 = 0; h2 < 32; h2++) {
            for (int w2 = 0; w2 < 64; w2++) {
              pixels[((h + h2) * width) + w + w2] = ByteConverter.unsign(fm.readByte());
            }
          }

        }
      }

      // X - Color Palette
      int[] palette = ImageFormatReader.readBGR555(fm, 256, 1).getImagePixels();

      for (int i = 0; i < numPixels; i++) {
        pixels[i] = palette[pixels[i]];
      }

      fm.close();

      return new ImageResource(pixels, width, height);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int width = im.getWidth();
      int height = im.getHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // Generate all the mipmaps of the image
      im.convertToPaletted();
      im.changeColorCount(256);

      // X - Pixels
      int[] pixels = im.getPixels();
      for (int h = 0; h < height; h += 32) {
        for (int w = 0; w < width; w += 64) {

          for (int h2 = 0; h2 < 32; h2++) {
            for (int w2 = 0; w2 < 64; w2++) {
              fm.writeByte(pixels[((h + h2) * width) + w + w2]);
            }
          }

        }
      }

      // X - Palette
      ImageFormatWriter.writeBGR555(fm, new ImageResource(im.getPalette(), 256, 1));

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}