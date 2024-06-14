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
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RESOURCES_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RESOURCES_2_BIMAGE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RESOURCES_2_BIMAGE() {
    super("RESOURCES_2_BIMAGE", "RAGE BIMAGE Image");
    setExtensions("bimage");

    setGames("RAGE");
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
      if (plugin instanceof Plugin_RESOURCES_2) {
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

      fm.skip(12);

      // 4 - Image Width
      if (FieldValidator.checkWidth(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Number of Mipmaps
      if (FieldValidator.checkRange(IntConverter.changeFormat(fm.readInt()), 1, 20)) {
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
      // 4 - Unknown
      // 4 - null
      fm.skip(12);

      // 4 - Image Width
      int width = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkHeight(height);

      // 4 - null
      // 4 - Number of Mipmaps
      // 4 - null
      fm.skip(12);

      // 4 - Image Format? (10=DXT1, 11=DXT5) (LITTLE)
      int imageFormat = fm.readInt();

      // 4 - Unknown (5) (LITTLE)
      // 2 - Unknown (0/5)
      fm.skip(6);

      // 4 - Mipmap ID (incremental from 0)
      // 4 - null
      // 4 - Mipmap Width
      // 4 - Mipmap Height
      // 4 - Mipmap Data Length
      fm.skip(20);

      // X - Mipmap Data

      ImageResource imageResource = null;
      if (imageFormat == 10) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 11) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 5 || imageFormat == 8) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else if (imageFormat == 3) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_RESOURCES_2_BIMAGE] Unknown Image Format: " + imageFormat);
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
  public void write(PreviewPanel panel, FileManipulator destination) {
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
      if (srcLength > 62) {
        srcLength = 62; // allows enough reading for the header, but not much of the original image data
      }
      //byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      byte[] srcBytes = new byte[srcLength];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.writeBytes(src.readBytes(12));

      // 4 - Image Width
      fm.writeInt(IntConverter.changeFormat(width));
      src.skip(4);

      // 4 - Image Height
      fm.writeInt(IntConverter.changeFormat(height));
      src.skip(4);

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // 4 - Number of Mipmaps
      int numMipmaps = IntConverter.changeFormat(src.readInt());
      fm.writeInt(IntConverter.changeFormat(numMipmaps));

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // 4 - Image Format? (10=DXT1, 11=DXT5) (LITTLE)
      int imageFormat = src.readInt();
      fm.writeInt(imageFormat);

      // 4 - Unknown (5) (LITTLE)
      // 2 - Unknown (0/5)
      fm.writeBytes(src.readBytes(6));

      if (imageFormat == 5 || imageFormat == 8) {
        // Grayscale
        ImageManipulator im = new ImageManipulator(imageResource);
        im.changeColorCount(256);

        // change to grayscale palette
        int[] palette = PaletteGenerator.getGrayscale();
        im.setPalette(palette);

        ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
        for (int m = 0; m < numMipmaps; m++) {
          ImageManipulator mipmap = mipmaps[m];

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

          // 4 - Mipmap Data Length
          fm.writeInt(IntConverter.changeFormat(mipmapWidth * mipmapHeight));

          // X - Mipmap Data
          int[] pixels = mipmap.getPixels();
          int numPixels = pixels.length;

          for (int i = 0; i < numPixels; i++) {
            fm.writeByte(pixels[i]);
          }
        }

      }
      else {

        // X - Pixels
        ImageManipulator im = new ImageManipulator(imageResource);
        ImageResource[] mipmaps = im.generateMipmaps();

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

          if (imageFormat == 10) {
            // DXT1

            // 4 - Mipmap Data Length
            fm.writeInt(IntConverter.changeFormat(mipmapWidth * mipmapHeight / 2));

            // X - Mipmap Data
            ImageFormatWriter.writeDXT1(fm, mipmap);
          }
          else if (imageFormat == 11) {
            // DXT5

            // 4 - Mipmap Data Length
            fm.writeInt(IntConverter.changeFormat(mipmapWidth * mipmapHeight));

            // X - Mipmap Data
            ImageFormatWriter.writeDXT5(fm, mipmap);
          }
          else if (imageFormat == 3) {
            // BGRA

            // 4 - Mipmap Data Length
            fm.writeInt(IntConverter.changeFormat(mipmapWidth * mipmapHeight * 4));

            // X - Mipmap Data
            ImageFormatWriter.writeBGRA(fm, mipmap);
          }
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