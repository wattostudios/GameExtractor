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
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_MUK_MUKFILE_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_MUK_MUKFILE_2_TEX_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_MUK_MUKFILE_2_TEX_TEX() {
    super("MUK_MUKFILE_2_TEX_TEX", "Mall Tycoon TEX Image");
    setExtensions("tex");

    setGames("Mall Tycoon");
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
      if (plugin instanceof Plugin_MUK_MUKFILE_2) {
        rating += 25;
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

      // 4 - Header (_TEX)
      if (fm.readString(4).equals("_TEX")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      fm.skip(8);

      // 2 - Pixel Format (768/1024)
      short pixelFormat = fm.readShort();
      if (pixelFormat == 768 || pixelFormat == 1024) {
        rating += 5;
      }
      else {
        return 0; // don't want to support it if it doesn't have a valid pixelFormat value,
                 // cause we don't know what format the pixels would be in!
      }

      // 2 - Image Width/Height
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Width/Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      fm.skip(1);

      // 4 - Image Data Length
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
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

      // 4 - Header (_TEX)
      // 4 - Version (1)
      // 4 - Unknown (3329)
      fm.skip(12);

      // 2 - Pixel Format (768/1024)
      short pixelFormat = fm.readShort();

      // 2 - Image Width/Height
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Width/Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 1 - Number Of Mipmaps
      int numMipmaps = ByteConverter.unsign(fm.readByte());

      // 4 - Image Data Length
      fm.skip(4);

      ImageResource imageResource = null;
      if (pixelFormat == 768) { // 8-8-8 FORMAT
        // X - Pixels (RGB)
        imageResource = ImageFormatReader.readRGB(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB");
      }

      else if (pixelFormat == 1024) { // 8-8-8-8 FORMAT
        // X - Pixels (RGBA)
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
      }

      fm.close();

      imageResource.addProperty("MipmapCount", "" + numMipmaps);

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

      // Generate all the mipmaps of the image
      ImageResource[] mipmaps = im.generateMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String imageFormat = "RGBA";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        imageFormat = imageResource.getProperty("ImageFormat", "RGBA");
      }

      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      if (!(imageFormat.equals("RGBA") || imageFormat.equals("RGB"))) {
        // a different image format not allowed in this image
        imageFormat = "RGBA";
      }

      int pixelFormat = 1024;
      if (imageFormat.equals("RGB")) {
        pixelFormat = 768;
      }

      // work out the image data length
      long imageDataLength = 0;
      for (int i = 0; i < mipmapCount; i++) {
        int byteCount = mipmaps[i].getNumPixels();
        if (pixelFormat == 768) {
          imageDataLength += (byteCount * 3); // RGB is 3 bytes per pixel
        }
        else {
          imageDataLength += (byteCount * 4); // RGBA is 4 bytes per pixel
        }
      }

      // 4 - Header (_TEX)
      fm.writeString("_TEX");

      // 4 - Version (1)
      fm.writeInt(1);

      // 4 - Unknown (3329)
      fm.writeInt(3329);

      // 2 - Pixel Format (768/1024)
      fm.writeShort((short) pixelFormat);

      // 2 - Image Width/Height
      fm.writeShort((short) imageWidth);

      // 2 - Image Width/Height
      fm.writeShort((short) imageHeight);

      // 1 - Number Of Mipmaps
      fm.writeByte(mipmapCount);

      // 4 - Image Data Length
      fm.writeInt(imageDataLength);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];
        // X - Pixels
        if (pixelFormat == 768) { // RGB 8-8-8 FORMAT
          ImageFormatWriter.writeRGB(fm, mipmap);
        }
        else { // RGBA 8-8-8-8 FORMAT
          ImageFormatWriter.writeRGBA(fm, mipmap);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}