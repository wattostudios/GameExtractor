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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_IMG_GMI extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_IMG_GMI() {
    super("IMG_GMI", "Jetboat Superchamps IMG Image");
    setExtensions("img");

    setGames("Jetboat Superchamps",
        "Jetboat Superchamps 2");
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals(" GMI")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Bits Per Pixel
      if (FieldValidator.checkNumColors(fm.readInt())) {
        rating += 5;
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

      // 4 - Header ( GMI)
      fm.skip(4);

      // 4 - Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (1)
      // 4 - Bits Per Pixel (16)
      fm.skip(8);

      // 4 - Alpha Bit Mask (5-6-5 = 0,     4-4-4-4 = 61440)
      int alphaMask = fm.readInt();

      // 4 - Red Bit Mask   (5-6-5 = 63488, 4-4-4-4 = 3840)
      // 4 - Green Bit Mask (5-6-5 = 2016,  4-4-4-4 = 240)
      // 4 - Blue Bit Mask  (5-6-5 = 31,    4-4-4-4 = 15)
      fm.skip(12);

      // X - Pixels
      ImageResource imageResource = null;
      if (alphaMask == 0) {
        // 5-6-5 RGB
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB565");
      }
      else {
        // 4-4-4-4 ARGB
        imageResource = ImageFormatReader.readGBAR4444(fm, width, height);
        imageResource.addProperty("ImageFormat", "GBAR4444");
      }

      fm.close();

      return imageResource;

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

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String imageFormat = "RGB565";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        imageFormat = imageResource.getProperty("ImageFormat", "RGB565");
      }

      if (!(imageFormat.equals("RGB565") || imageFormat.equals("GBAR4444"))) {
        imageFormat = "RGB565";
      }

      // 4 - Header ( GMI)
      fm.writeString(" GMI");

      // 4 - Width
      fm.writeInt(imageWidth);

      // 4 - Height
      fm.writeInt(imageHeight);

      // 4 - Unknown (1)
      fm.writeInt(1);

      // 4 - Bits Per Pixel (16)
      fm.writeInt(16);

      if (imageFormat.equals("RGB565")) {
        // 4 - Alpha Bit Mask (5-6-5 = 0,     4-4-4-4 = 61440)
        // 4 - Blue Bit Mask  (5-6-5 = 63488, 4-4-4-4 = 3840)
        // 4 - Green Bit Mask (5-6-5 = 2016,  4-4-4-4 = 240)
        // 4 - Red Bit Mask   (5-6-5 = 31,    4-4-4-4 = 15)
        fm.writeInt(0);
        fm.writeInt(63488);
        fm.writeInt(2016);
        fm.writeInt(31);

        // X - Pixels
        ImageFormatWriter.writeRGB565(fm, imageResource);
      }
      else {
        // 4 - Alpha Bit Mask (5-6-5 = 0,     4-4-4-4 = 61440)
        // 4 - Blue Bit Mask  (5-6-5 = 63488, 4-4-4-4 = 3840)
        // 4 - Green Bit Mask (5-6-5 = 2016,  4-4-4-4 = 240)
        // 4 - Red Bit Mask   (5-6-5 = 31,    4-4-4-4 = 15)
        fm.writeInt(61440);
        fm.writeInt(3840);
        fm.writeInt(240);
        fm.writeInt(15);

        // X - Pixels
        ImageFormatWriter.writeGBAR4444(fm, imageResource);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}