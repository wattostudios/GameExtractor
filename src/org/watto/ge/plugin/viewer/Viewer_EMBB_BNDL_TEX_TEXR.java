/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_EMBB_BNDL;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_EMBB_BNDL_TEX_TEXR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_EMBB_BNDL_TEX_TEXR() {
    super("EMBB_BNDL_TEX_TEXR", "Middle Earth: Shadow Of Mordor TEX Image");
    setExtensions("tex");

    setGames("Middle Earth: Shadow Of Mordor");
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
      if (plugin instanceof Plugin_EMBB_BNDL) {
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

      // 4 - Header
      if (fm.readString(4).equals("TEXR")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // Version (3)
      if (fm.readInt() == 3) {
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

      // 4 - Header (TEXR)
      // 4 - Version (3)
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Number of Images? (1)
      fm.skip(16);

      // 2 - Filename Length
      short filenameLength = fm.readShort();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Filename
      fm.skip(filenameLength);

      // 3 - Unknown
      //fm.skip(ArchivePlugin.calculatePadding(filenameLength, 4));
      fm.skip(3);

      // X - File Data (DDS Image, starting with "DDS " header)
      return new Viewer_DDS_DDS().readThumbnail(fm);

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
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      ImageManipulator im = new ImageManipulator(ivp);
      int imageWidth = ivp.getImageWidth();
      int imageHeight = ivp.getImageHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Extract the original resource into a byte[] array, so we can reference it
      int srcLength = (int) resourceBeingReplaced.getDecompressedLength();
      if (srcLength > 1280) {
        srcLength = 1280; // allows enough reading for the header, but not much of the original image data
      }
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Header (TEXR)
      // 4 - Version (3)
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Number of Images? (1)
      fm.writeBytes(src.readBytes(16));

      // 2 - Filename Length
      short filenameLength = src.readShort();
      fm.writeShort(filenameLength);

      // X - Filename
      fm.writeBytes(src.readBytes(filenameLength));

      // 3 - Unknown
      //fm.skip(ArchivePlugin.calculatePadding(filenameLength, 4));
      fm.writeBytes(src.readBytes(3));

      // X - File Data (DDS Image, starting with "DDS " header)
      //new Viewer_DDS_DDS_Writer_DXT5().write(preview, fm); // can't use this because it has a SEEK in it. So just cut-paste from the writer instead

      // Generate all the mipmaps of the image
      ImageResource[] mipmaps = im.generateMipmaps();
      int mipmapCount = mipmaps.length;

      // Now try to get the property values from the ImageResource, if they exist
      if (preview instanceof PreviewPanel_Image) {
        ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

        if (imageResource != null) {
          mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        }
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
      fm.writeInt(imageWidth * imageHeight);

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
      fm.writeString("DXT5");

      // 4 - Color Bit Count
      // 4 - Red Bit Mask
      // 4 - Green Bit Mask
      // 4 - Blue Bit Mask
      // 4 - Alpha Bit Mask
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);

      // 16 - DDCAPS2
      // 4 - Texture Stage
      // X - Unknown
      fm.writeInt(0x1000);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        // X - Pixels
        ImageFormatWriter.writeDXT5(fm, mipmap);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}