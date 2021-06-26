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

import org.watto.ErrorLogger;
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
import org.watto.ge.plugin.archive.Plugin_RFA_REFRACTOR2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RFA_REFRACTOR2_RCT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RFA_REFRACTOR2_RCT() {
    super("RFA_REFRACTOR2_RCT", "RalliSport Challenge RCT Image");
    setExtensions("rct");

    setGames("RalliSport Challenge");
    setPlatforms("XBox");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_RFA_REFRACTOR2) {
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

      fm.skip(4);

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Mipmap Count
      if (FieldValidator.checkNumColors(fm.readInt())) {
        rating += 5;
      }

      // 4 - File Length
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

      // 4 - Unknown
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - null
      // 4 - Number of Mipmaps
      // 4 - Mipmap Data Length
      fm.skip(12);

      // 4 - Swizzle Flag (0=Swizzled if ImageFormat=2)
      int swizzle = fm.readInt();

      // 4 - Image Format (12=DXT1, 14=DXT3)
      int imageFormat = fm.readInt();

      // X - Pixels
      ImageResource imageResource = null;

      if (imageFormat == 12) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 14) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 16) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 2) {

        if (width % 16 != 0) {
          width += (16 - (width % 16));
        }

        imageResource = ImageFormatReader.readBGRA(fm, width, height);

        if (swizzle == 0) {
          imageResource.setPixels(ImageFormatReader.unswizzle(imageResource.getPixels(), width, height, 1));
        }
      }
      else {
        ErrorLogger.log("[Viewer_RFA_REFRACTOR2_RCT] Unknown Image Format: " + imageFormat);
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

      // Generate all the mipmaps of the image
      ImageResource[] mipmaps = im.generateMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      int fileID = 0;
      int hash = 0;
      String filename = "";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        fileID = imageResource.getProperty("FileID", 0);
        hash = imageResource.getProperty("Hash", 0);
        filename = imageResource.getProperty("Filename", "");
      }

      if (filename.equals("")) {
        filename = fm.getFile().getName();
      }
      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      // work out the file length
      long fileLength = 28 + filename.length() + 1 + (mipmapCount * 4);
      for (int i = 0; i < mipmapCount; i++) {
        // ABGR is 4 bytes per pixel
        int byteCount = mipmaps[i].getNumPixels() * 4;
        fileLength += byteCount;
      }

      // 4 - Header (ABGR)
      fm.writeString("ABGR");

      // 4 - File Length (including all these header fields)
      fm.writeInt(fileLength);

      // 4 - File ID
      fm.writeInt(fileID);

      // 2 - Image Height
      fm.writeShort((short) imageHeight);

      // 2 - Image Width
      fm.writeShort((short) imageWidth);

      // 4 - Number Of Mipmaps
      fm.writeInt(mipmapCount);

      // 4 - File Type? (28)
      fm.writeInt(28);

      // 4 - Hash?
      fm.writeInt(hash);

      // X - Filename
      // 1 - null Filename Terminator
      fm.writeString(filename);
      fm.writeByte(0);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        int pixelCount = mipmap.getNumPixels();

        // 4 - Data Length
        fm.writeInt(pixelCount * 4); // ABGR is 4 bytes per pixel

        // X - Pixels
        ImageFormatWriter.writeBGRA(fm, mipmap);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}