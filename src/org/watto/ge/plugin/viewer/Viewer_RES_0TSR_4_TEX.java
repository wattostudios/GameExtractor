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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RES_0TSR_4;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RES_0TSR_4_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RES_0TSR_4_TEX() {
    super("RES_0TSR_4_TEX", "Test Drive: Eve of Destruction TEX Image");
    setExtensions("tex", "tm0");

    setGames("Test Drive: Eve of Destruction");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_RES_0TSR_4) {
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

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Number of Mipmaps
      if (FieldValidator.checkRange(fm.readInt(), 1, 20)) {
        rating += 5;
      }

      fm.skip(12);

      // 4 - Color Palette Offset (128) (null if not a paletted image)
      int paletteOffset = fm.readInt();
      if (paletteOffset == 128 || paletteOffset == 0) {
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

      long arcSize = fm.getLength();

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Number of Mipmaps
      fm.skip(4);

      // 1 - Image Format? (0=4-bit Paletted, 1=8-bit Paletted, 2=RGB555)
      int imageFormat = fm.readByte();

      // 3 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.skip(11);

      // 4 - Color Palette Offset (128) (null if not a paletted image)
      int paletteOffset = fm.readInt();
      FieldValidator.checkOffset(paletteOffset, arcSize);

      // 4 - Image Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      int[] palette = null;
      if (imageFormat == 0 || imageFormat == 1) {
        // Paletted
        fm.relativeSeek(paletteOffset);

        palette = ImageFormatReader.readPaletteARGB(fm, 256);
      }

      fm.relativeSeek(dataOffset);

      ImageResource imageResource = null;

      if (imageFormat == 0) {
        // 4-bit Paletted
        /*
        int numBytes = width * height / 2;
        byte[] imageData = fm.readBytes(numBytes);
        imageData = ImageFormatReader.unswizzlePS2(imageData, width / 2, height / 2);
        
        fm.close();
        fm = new FileManipulator(new ByteBuffer(imageData));
        
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        */

        /*
        int swizzleWidth = width / 2;
        int swizzleHeight = height;
        int byteLength = swizzleWidth * swizzleHeight;
        
        byte[] sourceBytes = fm.readBytes(byteLength);
        sourceBytes = ImageFormatReader.unswizzlePS2(sourceBytes, swizzleWidth, swizzleHeight);
        
        fm.close();
        fm = new FileManipulator(new ByteBuffer(sourceBytes));
        
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        */

        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        imageResource.setPixels(ImageFormatReader.unswizzlePS2(imageResource.getImagePixels(), width, height));

      }
      else if (imageFormat == 1) {
        // 8-bit Paletted
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource.setPixels(ImageFormatReader.unswizzlePS2(imageResource.getImagePixels(), width, height));
      }
      else if (imageFormat == 2) {
        // RGB555
        imageResource = ImageFormatReader.readRGB555(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_RES_0TSR_4_TEX] Unknown Image Format: " + imageFormat);
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

}