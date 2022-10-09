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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_XPR_XPR2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_XPR_XPR2_TX2D extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_XPR_XPR2_TX2D() {
    super("XPR_XPR2_TX2D", "XPR2 TX2D Image");
    setExtensions("tx2d");

    setGames("XPR2");
    setPlatforms("XBox 360");
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
      if (plugin instanceof Plugin_XPR_XPR2) {
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

      int height = 32;
      int width = 32;
      int imageFormat = -1;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
        imageFormat = Integer.parseInt(resource.getProperty("ImageFormat"));
      }
      catch (Throwable t) {
        //
      }

      if (imageFormat == -1) {
        return null;
      }

      // X - Pixels
      ImageResource imageResource = null;

      if (imageFormat == 2) {
        // L8 greyscale
        int length = (int) resource.getLength();

        byte[] imageBytes = fm.readBytes(length);
        int untileSize = 16;
        //int unswizzleSize = 4;
        imageBytes = untile(untileSize, width, imageBytes, length);
        //imageBytes = ImageFormatReader.unswizzle(imageBytes, width, height, unswizzleSize);

        /*
        byte[] outBytes = new byte[length];
        //imageBytes = ImageFormatReader.unswizzle(imageBytes, width, height, 16);
        int t = 4;
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            int off = XGAddress2DTiledOffset(x, y, width, t);
            if (off >= length) {
              continue;
            }
            try {
              System.arraycopy(imageBytes, off * t, outBytes, (x + y * width) * t, t);
            }
            catch (Throwable t2) {
            }
          }
        }
        */

        //byte[] imageBytes = fm.readBytes((int) resource.getLength());
        //imageBytes = ImageFormatReader.unswizzle(imageBytes, width, height, 16);

        fm.close();
        fm = new FileManipulator(new ByteBuffer(imageBytes));

        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);

        //int[] imagePixels = imageResource.getImagePixels();
        //imagePixels = ImageFormatReader.unswizzle(imagePixels, width, height, 16);
        //imageResource = new ImageResource(imagePixels, width, height);

        //imageResource = ImageFormatReader.reorderPixelBlocks(imageResource, 16, 1);
      }
      else if (imageFormat == 134) {
        // ARGB
        int length = (int) resource.getLength();

        byte[] imageBytes = fm.readBytes(length);
        int untileSize = 64;
        //int unswizzleSize = 4;

        imageBytes = untile(untileSize, width, imageBytes, length);
        //imageBytes = mortonize(width, height, imageBytes, length);
        //imageBytes = ImageFormatReader.unswizzle(imageBytes, width, height, unswizzleSize);

        /*
        int skipSize = 4096;
        int blockSize = 32;
        
        int numSkips = length / skipSize;
        int numBlocksPerSkip = skipSize / blockSize;
        
        byte[] outBytes = new byte[length];
        int outPos = 0;
        for (int i = 0; i < numBlocksPerSkip; i++) {
          for (int j = 0; j < numSkips; j++) {
            int readPos = (j * skipSize) + i * blockSize;
            System.arraycopy(imageBytes, readPos, outBytes, outPos, blockSize);
            outPos += blockSize;
          }
        }
        imageBytes = outBytes;
        */

        /*
        byte[] outBytes = new byte[length];
        //imageBytes = ImageFormatReader.unswizzle(imageBytes, width, height, 16);
        int t = 4;
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            int off = XGAddress2DTiledOffset(x, y, width, t);
            if (off >= length) {
              continue;
            }
            try {
              System.arraycopy(imageBytes, off * t, outBytes, (x + y * width) * t, t);
            }
            catch (Throwable t2) {
            }
          }
        }
        */

        fm.close();
        fm = new FileManipulator(new ByteBuffer(imageBytes));

        imageResource = ImageFormatReader.readARGB(fm, width, height);
        //imageResource = ImageFormatReader.reorderPixelBlocks(imageResource, 16, 16);//64,64
      }
      else {
        ErrorLogger.log("[Viewer_XPR_XPR2_TX2D] Unknown Image Format: " + imageFormat);
      }

      fm.close();

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /*
  public int XGAddress2DTiledOffset(int x, int y, int w, int texelPitch) {
  
    int alignedWidth = 0;
    int logBpp = 0;
    int Macro = 0;
    int Micro = 0;
    int Offset = 0;
  
    // alignedWidth = (w + 31) and not 31;
    if (w % 32 != 0) {
      alignedWidth = (w + 32) / 32;
    }
  
    //logBpp = (texelPitch shr 2) + ((texelPitch shr 1) shr (texelPitch shr 2));
    logBpp = (texelPitch >> 2) + ((texelPitch >> 1) >> (texelPitch >> 2));
  
    //Macro = ((x shr 5) + (y shr 5) * (alignedWidth shr 5)) shl (logBpp + 7);
    Macro = ((x >> 5) + (y >> 5) * (alignedWidth >> 5)) << (logBpp + 7);
  
    //Micro = (((x and 7) + ((y and 6) shl 2)) shl LogBpp);
    Micro = (((x & 7) + ((y & 6) << 2)) << logBpp);
  
    //Offset = Macro + ((Micro and not 15) shl 1) + (Micro and 15) + ((y and 8) shl (3 + logBpp)) + ((y and 1) shl 4);
    Offset = Macro + ((Micro & 0xFFFFFFF0) << 1) + (Micro & 15) + ((y & 8) << (3 + logBpp)) + ((y & 1) << 4);
  
    //Result= (((Offset and not 511) shl 3) + ((Offset and 448) shl 2) + (Offset and 63) + ((y and 16) shl 7) + (((((y and 8) shr 2) + (x shr 3)) and 3) shl 6)) shr logBpp;
    int result = (((Offset & 0xFFFFFE00) << 3) + ((Offset & 448) << 2) + (Offset & 63) + ((y & 16) << 7) + (((((y & 8) >> 2) + (x >> 3)) & 3) << 6)) >> logBpp;
    return result;
  }
  */

  public byte[] untile(int tile_size, int width, byte[] buf, int size) {
    int bytes_per_element = 1;
    //tile_size /= dds_bwh(format);
    //width /= dds_bwh(format);

    byte[] tmp_buf = new byte[size];

    for (int i = 0; i < size / bytes_per_element / tile_size / tile_size; i++) {
      int tile_row = i / (width / tile_size);
      int tile_column = i % (width / tile_size);
      int tile_start = tile_row * width * tile_size + tile_column * tile_size;
      for (int j = 0; j < tile_size; j++) {
        //memcpy (dest,source,size)
        //memcpy(&tmp_buf[bytes_per_element * (tile_start + j * width)], &buf[bytes_per_element * (i * tile_size * tile_size + j * tile_size)],   (size_t)tile_size * bytes_per_element);
        int destPos = bytes_per_element * (tile_start + j * width);
        int srcPos = bytes_per_element * (i * tile_size * tile_size + j * tile_size);
        int copyLength = tile_size * bytes_per_element;
        try {
          System.arraycopy(buf, srcPos, tmp_buf, destPos, copyLength);
        }
        catch (Throwable t) {
        }
      }
    }

    return tmp_buf;
    //memcpy(buf, tmp_buf, size);

  }

  public byte[] mortonize(int width, int height, byte[] buf, int size) {
    //int bits_per_element = dds_bpp(format) * dds_bwh(format) * dds_bwh(format) * wf;
    int bytes_per_element = 4;
    int tile_width = 4;

    //width /= dds_bwh(format) * wf;
    //height /= dds_bwh(format);
    int num_elements = size / bytes_per_element;

    int tile_size = tile_width * tile_width;
    byte[] tmp_buf = new byte[size];
    for (int i = 0; i < num_elements; i++) {
      int j, x, y;

      x = i % width;
      y = i / width;
      j = (int) ImageFormatReader.calculateMorton2D(x, y);
      // Now, apply tiling. This is accomplished by offseting our value
      // with the current tile position multiplied by the tile size.
      j += ((y / tile_width) * (width / tile_width) + (x / tile_width)) * tile_size;

      //memcpy (dest,source,size)
      //memcpy(&tmp_buf[j * bytes_per_element], &buf[], bytes_per_element);
      int destPos = j * bytes_per_element;
      int srcPos = i * bytes_per_element;
      int copyLength = bytes_per_element;
      try {
        System.arraycopy(buf, srcPos, tmp_buf, destPos, copyLength);
      }
      catch (Throwable t) {
      }
    }

    //memcpy(buf, tmp_buf, size);
    return tmp_buf;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}