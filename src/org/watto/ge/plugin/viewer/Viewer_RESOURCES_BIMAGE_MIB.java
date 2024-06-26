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

import java.awt.Image;

import org.watto.ErrorLogger;
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
import org.watto.ge.plugin.archive.Plugin_RESOURCES;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RESOURCES_BIMAGE_MIB extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RESOURCES_BIMAGE_MIB() {
    super("RESOURCES_BIMAGE_MIB", "Doom 3 BIMAGE Image");
    setExtensions("bimage");

    setGames("Doom 3");
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
      if (plugin instanceof Plugin_RESOURCES) {
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

      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(5);

      // 3 - Header
      if (fm.readString(3).equals("MIB")) {
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

      // 4 - null
      // 4 - Unknown
      // 4 - Header ((byte)10 + "MIB")
      // 4 - Unknown (1)
      fm.skip(16);

      // 4 - Image Format? (7=DXT1, 8=DXT5)
      int imageFormat = IntConverter.changeFormat(fm.readInt());

      // 4 - Unknown (0/3)
      fm.skip(4);

      // 4 - Image Width
      int width = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkHeight(height);

      // 4 - Number Of Mipmaps
      int numMipmaps = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numMipmaps, 1, 20);

      // 4 - null
      fm.skip(4);

      // 4 - null
      // 4 - Mipmap Width
      // 4 - Mipmap Height
      // 4 - Mipmap Data Length
      fm.skip(16);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 7) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 8) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_RESOURCES_BIMAGE_MIB] Unknown Image Format: " + imageFormat);
        return null;
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
  }

  /**
  **********************************************************************************************
  We REPLACE so that we can store it in the same format as the original  
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

      // Extract the original resource into a byte[] array, so we can reference it
      int srcLength = (int) resourceBeingReplaced.getDecompressedLength();
      if (srcLength > 40) {
        srcLength = 40; // allows enough reading for the header and color palette, but not much of the original image data
      }
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      ImageManipulator im = new ImageManipulator(imageResource);
      ImageResource[] mipmaps = im.generateMipmaps();

      // 4 - null
      // 4 - Unknown
      // 4 - Header ((byte)10 + "MIB")
      // 4 - Unknown (1)
      fm.writeBytes(src.readBytes(16));

      // 4 - Image Format? (7=DXT1, 8=DXT5)
      int imageFormat = IntConverter.changeFormat(src.readInt());
      fm.writeInt(IntConverter.changeFormat(imageFormat));

      // 4 - Unknown (0/3)
      fm.writeBytes(src.readBytes(4));

      // 4 - Image Width
      fm.writeInt(IntConverter.changeFormat(width));
      src.skip(4);

      // 4 - Image Height
      fm.writeInt(IntConverter.changeFormat(height));
      src.skip(4);

      // 4 - Number of Mipmaps
      int numMipmaps = IntConverter.changeFormat(src.readInt());
      if (mipmaps.length < numMipmaps) {
        numMipmaps = mipmaps.length;
      }
      fm.writeInt(IntConverter.changeFormat(numMipmaps));

      for (int m = 0; m < numMipmaps; m++) {
        ImageResource mipmap = mipmaps[m];
        int mipmapWidth = mipmap.getWidth();
        int mipmapHeight = mipmap.getHeight();

        // 4 - Mipmap ID (incremental from 0)
        fm.writeInt(IntConverter.changeFormat(m));

        // 4 - null
        fm.writeInt(0);

        // 4 - Mipmap Width
        fm.writeInt(IntConverter.changeFormat(mipmapWidth));

        // 4 - Mipmap Height
        fm.writeInt(IntConverter.changeFormat(mipmapHeight));

        if (imageFormat == 7) { // DXT1
          int mipmapLength = mipmapWidth * mipmapHeight / 2;

          // 4 - Mipmap Data Length
          fm.writeInt(IntConverter.changeFormat(mipmapLength));

          // X - Mipmap Data
          ImageFormatWriter.writeDXT1(fm, mipmap);
        }
        else if (imageFormat == 8) { // DXT5
          int mipmapLength = mipmapWidth * mipmapHeight;

          // 4 - Mipmap Data Length
          fm.writeInt(IntConverter.changeFormat(mipmapLength));

          // X - Mipmap Data
          ImageFormatWriter.writeDXT5(fm, mipmap);
        }
        else {
          ErrorLogger.log("[Viewer_RESOURCES_BIMAGE_MIB] Unknown Image Format: " + imageFormat);
          // store as DXT5, as a fallback

          int mipmapLength = mipmapWidth * mipmapHeight;

          // 4 - Mipmap Data Length
          fm.writeInt(IntConverter.changeFormat(mipmapLength));

          // X - Mipmap Data
          ImageFormatWriter.writeDXT5(fm, mipmap);
        }
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}