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
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DDS_DDS_Writer_ARGB extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_DDS_DDS_Writer_ARGB() {
    super("DDS_DDS_Writer_ARGB", "DirectX DDS ARGB Image Writer");
    setExtensions("dds");
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
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    return null;
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
    return null;
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

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
      }

      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      int DDSD_CAPS = 0x0001;
      int DDSD_HEIGHT = 0x0002;
      int DDSD_WIDTH = 0x0004;
      int DDSD_PIXELFORMAT = 0x1000;
      int DDSD_MIPMAPCOUNT = 0x20000;
      int DDSD_LINEARSIZE = 0x80000;

      // Write the header

      // 4 - Header (DDS )
      fm.writeString("DDS ");

      // 4 - Header 1 Length (124)
      fm.writeInt(124);

      // 4 - Flags
      int flag = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT | DDSD_MIPMAPCOUNT | DDSD_LINEARSIZE;
      fm.writeInt(flag);

      // 4 - Height
      fm.writeInt(imageHeight);

      // 4 - Width
      fm.writeInt(imageWidth);

      // 4 - Linear Size
      fm.writeInt(imageWidth * imageHeight / 2);

      // 4 - Depth
      fm.writeInt(0);

      // 4 - Number Of MipMaps
      fm.writeInt(mipmapCount);

      // 4 - Alpha Bit Depth
      fm.writeInt(0);

      // 40 - Unknown
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);

      // 4 - Header 2 Length (32)
      fm.writeInt(32);

      // 4 - Flags 2
      fm.writeInt(0x0004);

      // 4 - Format Code (DXT1 - DXT5)
      fm.writeInt(0);

      // 4 - Color Bit Count
      // 4 - Red Bit Mask
      // 4 - Green Bit Mask
      // 4 - Blue Bit Mask
      // 4 - Alpha Bit Mask
      fm.writeInt(32);

      // Red
      fm.writeByte(0);
      fm.writeByte(0);
      fm.writeByte(255);
      fm.writeByte(0);

      // Green
      fm.writeByte(0);
      fm.writeByte(255);
      fm.writeByte(0);
      fm.writeByte(0);

      // Blue
      fm.writeByte(255);
      fm.writeByte(0);
      fm.writeByte(0);
      fm.writeByte(0);

      // Alpha
      fm.writeByte(0);
      fm.writeByte(0);
      fm.writeByte(0);
      fm.writeByte(255);

      // 16 - DDCAPS2
      // 4 - Texture Stage
      // X - Unknown
      fm.writeInt(0x1000);
      fm.writeInt(0);
      fm.seek(128);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        // X - Pixels
        ImageFormatWriter.writeARGB(fm, mipmap);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}