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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_IVT;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.XORRepeatingKeyBufferWrapper;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_IVT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_IVT() {
    super("IVT", "IVT Image");
    setExtensions("ivt");

    setGames("International Volleyball 2009");
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
      if (plugin instanceof Plugin_IVT) {
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

      fm.skip(1);

      if (FieldValidator.checkRange(fm.readByte(), 0, 3)) {
        rating += 5;
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

      long arcSize = fm.getLength();

      // 1 - Flags
      /*
      int flags = ByteConverter.unsign(fm.readByte());
      int flags1 = flags & 7;
      int flags2 = ((flags >> 6) & 3) + 1;
      */
      fm.skip(1);

      // 1 - Image Type
      int imageFormat = fm.readByte();

      // 6 - Unknown
      fm.skip(6);

      int numPixels = (int) arcSize - 8;

      // guess the width/height;
      int width = 256;
      int height = 256;
      int bytes = 4;
      if (numPixels == 1048576) {
        width = 512;
        height = 512;
        bytes = 4;
      }
      else if (numPixels == 786432) {
        width = 512;
        height = 512;
        bytes = 3;
      }
      else if (numPixels == 524288) {
        width = 512;
        height = 256;
        bytes = 4;
      }
      else if (numPixels == 393216) {
        width = 512;
        height = 256;
        bytes = 3;

        String name = fm.getFile().getName().toLowerCase();
        if (name.equals("sponsors.ivt")) {
          width = 256;
          height = 512;
        }
      }
      else if (numPixels == 262144) {
        width = 256;
        height = 256;
        bytes = 4;

        String name = fm.getFile().getName().toLowerCase();
        if (name.startsWith("pub2")) {
          width = 512;
          height = 128;
        }
      }
      else if (numPixels == 196608) {
        width = 256;
        height = 256;
        bytes = 3;
      }
      else if (numPixels == 131072) {
        width = 256;
        height = 128;
        bytes = 4;
      }
      else if (numPixels == 98304) {
        width = 256;
        height = 128;
        bytes = 3;

        String name = fm.getFile().getName().toLowerCase();
        if (name.startsWith("telon")) {
          width = 128;
          height = 256;
        }
      }
      else if (numPixels == 65536) {
        width = 128;
        height = 128;
        bytes = 4;

        String name = fm.getFile().getName().toLowerCase();
        if (name.equals("interface1.ivt") || name.equals("tournament5.ivt")) {
          width = 256;
          height = 64;
        }
        else if (name.equals("gallery.ivt")) {
          width = 512;
          height = 32;
        }
      }
      else if (numPixels == 49152) {
        width = 128;
        height = 128;
        bytes = 3;
      }
      else if (numPixels == 32768) {
        width = 128;
        height = 64;
        bytes = 4;
      }
      else if (numPixels == 16384) {
        width = 64;
        height = 64;
        bytes = 4;
      }
      else if (numPixels == 12288) {
        width = 64;
        height = 64;
        bytes = 3;
      }
      else if (numPixels == 8192) {
        width = 64;
        height = 32;
        bytes = 4;
      }
      else if (numPixels == 4096) {
        width = 64;
        height = 16;
        bytes = 4;

        String name = fm.getFile().getName().toLowerCase();
        if (name.startsWith("flag")) {
          width = 32;
          height = 32;
        }
      }
      else if (numPixels == 3072) {
        width = 32;
        height = 32;
        bytes = 3;
      }
      else if (numPixels == 2048) {
        width = 32;
        height = 16;
        bytes = 4;
      }
      else if (numPixels == 1024) {
        width = 16;
        height = 16;
        bytes = 4;
      }
      else {
        ErrorLogger.log("[Viewer_IVT] Unknown Image Dimensions for pixel count " + numPixels);
      }

      // set the XOR key
      int[] key = null;
      if (imageFormat == 0) {
        key = new int[] { 102, 187, 120 };
      }
      else if (imageFormat == 1) {
        key = new int[] { 69, 115, 84, 97, 102 }; // EsTaf
      }
      else if (imageFormat == 2) {
        key = new int[] { 79, 112, 65, 108, 101, 114, 77 }; // PpAlerM
      }
      else if (imageFormat == 3) {
        key = new int[] { 75, 73, 83, 84, 65, 98, 105, 101, 100, 97, 232 }; // KISTAbieda\xe8
      }

      fm.setBuffer(new XORRepeatingKeyBufferWrapper(fm.getBuffer(), key));

      // X - Pixels
      ImageResource imageResource = null;
      if (bytes == 4) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (bytes == 3) {
        imageResource = ImageFormatReader.readRGB(fm, width, height);
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
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
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
      if (srcLength > 8) {
        srcLength = 8; // allows enough reading for the header
      }
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // get the XOR format
      src.skip(1);
      int imageFormat = src.readByte();
      src.seek(0);

      // 1 - Flags
      // 1 - Image Type
      // 6 - Unknown
      fm.writeBytes(src.readBytes(8));

      // set the XOR key
      int[] key = null;
      if (imageFormat == 0) {
        key = new int[] { 102, 187, 120 };
      }
      else if (imageFormat == 1) {
        key = new int[] { 69, 115, 84, 97, 102 }; // EsTaf
      }
      else if (imageFormat == 2) {
        key = new int[] { 79, 112, 65, 108, 101, 114, 77 }; // PpAlerM
      }
      else if (imageFormat == 3) {
        key = new int[] { 75, 73, 83, 84, 65, 98, 105, 101, 100, 97, 232 }; // KISTAbieda\xe8
      }

      fm.setBuffer(new XORRepeatingKeyBufferWrapper(fm.getBuffer(), key));

      // now work out the image bytes (RGBA or RGB)
      int numPixels = (int) resourceBeingReplaced.getDecompressedLength() - 8;

      // guess the bytes
      int bytes = 4;
      if (numPixels == 1048576) {
        bytes = 4;
      }
      else if (numPixels == 786432) {
        bytes = 3;
      }
      else if (numPixels == 524288) {
        bytes = 4;
      }
      else if (numPixels == 393216) {
        bytes = 3;
      }
      else if (numPixels == 262144) {
        bytes = 4;
      }
      else if (numPixels == 196608) {
        bytes = 3;
      }
      else if (numPixels == 131072) {
        bytes = 4;
      }
      else if (numPixels == 98304) {
        bytes = 3;
      }
      else if (numPixels == 65536) {
        bytes = 4;
      }
      else if (numPixels == 49152) {
        bytes = 3;
      }
      else if (numPixels == 32768) {
        bytes = 4;
      }
      else if (numPixels == 16384) {
        bytes = 4;
      }
      else if (numPixels == 12288) {
        bytes = 3;
      }
      else if (numPixels == 8192) {
        bytes = 4;
      }
      else if (numPixels == 4096) {
        bytes = 4;
      }
      else if (numPixels == 3072) {
        bytes = 3;
      }
      else if (numPixels == 2048) {
        bytes = 4;
      }
      else if (numPixels == 1024) {
        bytes = 4;
      }
      else {
        ErrorLogger.log("[Viewer_IVT] Unknown Image Dimensions for pixel count " + numPixels);
      }

      // now write the image
      if (bytes == 3) {
        ImageFormatWriter.writeRGB(fm, imageResource);
      }
      else {
        ImageFormatWriter.writeRGBA(fm, imageResource);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}