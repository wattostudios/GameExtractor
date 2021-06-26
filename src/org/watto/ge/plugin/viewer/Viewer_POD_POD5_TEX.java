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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_POD_POD5;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_POD_POD5_TEX extends ViewerPlugin {

  /** If false, we don't run the PVRTC decompression (as it's a thumbnail) but if true we do run it (it's a full preview) **/
  boolean doingFullPreview = false;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_POD_POD5_TEX() {
    super("POD_POD5_TEX", "Silent Hill: Book of Memories (TEX) Image [POD_POD5_TEX]");
    setExtensions("tex");

    setGames("Silent Hill: Book of Memories");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_POD_POD5) {
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

      // 4 - Unknown (8)
      // 16 - Hash?
      // 4 - null
      fm.skip(24);

      // 4 - Image Format (72=DXT5 Swizzled)
      if (fm.readInt() == 72) {
        rating += 5;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Width/Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Unknown (8)
      fm.skip(4);

      // 4 - Number of Mipmaps [+1] (ie "2" means there are 3 mipmaps)
      if (FieldValidator.checkNumColors(fm.readInt() + 1, 256)) {
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

      doingFullPreview = true;
      ImageResource imageResource = readThumbnail(fm);
      doingFullPreview = false;

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

      // 4 - Unknown (8)
      // 16 - Hash?
      // 4 - null
      fm.skip(24);

      // 4 - Image Format (72=DXT5 Swizzled)
      int formatCode = fm.readInt();

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (8)
      fm.skip(4);

      // 4 - Number of Mipmaps [+1] (ie "2" means there are 3 mipmaps)
      int mipmapCount = fm.readInt() + 1;
      FieldValidator.checkNumColors(mipmapCount, 256);

      // 8 - null
      fm.skip(8);

      // X - Image Data
      ImageResource imageResource = null;
      if (formatCode == 72) { // Swizzled DXT5
        //imageResource = ImageFormatReader.readDXT5Swizzled(fm, width, height);

        byte[] bytes = fm.readBytes(width * height);
        bytes = ImageFormatReader.unswizzle(bytes, width, height, 16);

        FileManipulator swizzledBuffer = new FileManipulator(new ByteBuffer(bytes));
        imageResource = ImageFormatReader.readDXT5(swizzledBuffer, width, height);
        swizzledBuffer.close();

        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (formatCode == 88) { // Swizzled BGRA
        byte[] bytes = fm.readBytes(width * height * 4);
        bytes = ImageFormatReader.unswizzle(bytes, width, height, 4);

        FileManipulator swizzledBuffer = new FileManipulator(new ByteBuffer(bytes));
        imageResource = ImageFormatReader.readBGRA(swizzledBuffer, width, height);
        swizzledBuffer.close();

        imageResource.addProperty("ImageFormat", "BGRA");
      }
      else if (formatCode == 67) { // PVRT-4bpp
        if (doingFullPreview) { // only run the converter if showing a full preview, not a thumbnail (too slow!)
          imageResource = ImageFormatReader.readPVRTC4bpp(fm, width, height);
          if (imageResource != null) {
            imageResource.addProperty("ImageFormat", "PVRTC4bpp");
          }
        }
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

}