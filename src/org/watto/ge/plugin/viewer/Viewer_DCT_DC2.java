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
public class Viewer_DCT_DC2 extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_DCT_DC2() {
    super("DCT_DC2", "Paris Chase DCT Image");
    setExtensions("dct");

    setGames("Paris Chase");
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
      if (fm.readString(3).equals("DC2")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Format
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // 4 - Unknown (1)
      // 4 - null
      // 4 - Unknown
      // 4 - Width/Height
      // 4 - Width/Height
      // 4 - Unknown (1)
      fm.skip(24);

      // 1 - Image Format (0/5/8/9)
      int imageFormat = fm.readByte();
      if (imageFormat == 9 || imageFormat == 8 || imageFormat == 5 || imageFormat == 0) {
        rating += 5;
      }
      else {
        rating = 0; // force-block, because we can't read the image data if we don't know what format it is
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

      // 3 - Header (DC2)
      // 4 - Format (2=Image, 0=Model, 3=Camera)
      // 4 - Unknown (1)
      // 4 - null
      // 4 - Unknown
      // 4 - Width/Height
      // 4 - Width/Height
      // 4 - Unknown (1)
      fm.skip(31);

      // 1 - Image Format (0/5/8/9)
      int imageFormat = fm.readByte();

      // 4 - Unknown
      fm.skip(4);

      // 4 - Width/Height
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Width/Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - File Length [+52]
      fm.skip(4);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 9) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (imageFormat == 8) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (imageFormat == 5) {
        imageResource = ImageFormatReader.readRGB(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB");
      }
      else if (imageFormat == 0) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
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
      String imageFormat = "DXT5";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        imageFormat = imageResource.getProperty("ImageFormat", "DXT5");
      }

      if (!(imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("RGB") || imageFormat.equals("RGBA"))) {
        // a different image format not allowed in this image - change to DXT3
        imageFormat = "DXT5";
      }
      if (imageFormat.equals("DXT3")) {
        imageFormat = "DXT5";
      }

      // work out the file length
      long fileLength = 52;
      if (imageFormat.equals("DXT1")) {
        fileLength += ((imageWidth * imageHeight) / 2); // DXT1 = 0.5bytes per pixel
      }
      else if (imageFormat.equals("DXT5")) {
        fileLength += (imageWidth * imageHeight); // DXT5 = 1 bytes per pixel
      }
      else if (imageFormat.equals("RGB")) {
        fileLength += (imageWidth * imageHeight) * 3; // RGB = 3 bytes per pixel
      }
      else if (imageFormat.equals("RGBA")) {
        fileLength += (imageWidth * imageHeight) * 4; // RGBA = 4 bytes per pixel
      }

      // 3 - Header (DC2)
      fm.writeString("DC2");

      // 4 - Format (2=Image, 0=Model, 3=Camera)
      fm.writeInt(2);

      // 4 - Unknown (1)
      fm.writeInt(1);

      // 4 - null
      fm.writeInt(0);

      // 4 - Unknown
      fm.writeInt(0);

      // 4 - Width/Height
      fm.writeInt(imageWidth);

      // 4 - Width/Height
      fm.writeInt(imageHeight);

      // 4 - Unknown (1)
      fm.writeInt(1);

      // 1 - Image Format (0/5/8/9)
      if (imageFormat.equals("DXT1")) {
        fm.writeByte(8);
      }
      else if (imageFormat.equals("DXT5")) {
        fm.writeByte(9);
      }
      else if (imageFormat.equals("RGB")) {
        fm.writeByte(5);
      }
      else {
        fm.writeByte(0);
      }

      // 4 - Unknown
      fm.writeInt(1);

      // 4 - Width/Height
      fm.writeInt(imageWidth);

      // 4 - Width/Height
      fm.writeInt(imageHeight);

      // 4 - Unknown
      fm.writeInt(fileLength);

      // X - Pixels
      if (imageFormat.equals("DXT1")) {
        ImageFormatWriter.writeDXT1(fm, imageResource);
      }
      else if (imageFormat.equals("DXT5")) {
        ImageFormatWriter.writeDXT5(fm, imageResource);
      }
      else if (imageFormat.equals("RGB")) {
        ImageFormatWriter.writeRGB(fm, imageResource);
      }
      else if (imageFormat.equals("RGBA")) {
        ImageFormatWriter.writeRGBA(fm, imageResource);
      }

      // 4 - null
      fm.writeInt(0);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}