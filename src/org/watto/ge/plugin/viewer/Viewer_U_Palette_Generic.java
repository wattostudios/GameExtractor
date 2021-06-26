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

import java.io.File;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.PluginGroup_U;
import org.watto.ge.plugin.archive.Plugin_U_Generic;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_U_Palette_Generic extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_U_Palette_Generic() {
    super("U_Palette_Generic", "Unreal Engine Color Palette");
    setExtensions("palette"); // MUST BE LOWER CASE

    setGames("Unreal Engine",
        "Tom Clancy's Rainbow Six 3: Black Arrow");
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
  @SuppressWarnings("static-access")
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (Archive.getReadPlugin() instanceof PluginGroup_U) {
        rating += 50;
      }

      if (Archive.getReadPlugin() instanceof Plugin_U_Generic) {
        rating += 10;
      }

      File paletteFile = Archive.getReadPlugin().getDirectoryFile(fm.getFile(), "Palette", false);
      if (paletteFile != null && paletteFile.exists()) {
        rating += 11;
      }
      else {
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        if (selected != null && selected instanceof Resource_Unreal) {

          Resource_Unreal resource = (Resource_Unreal) selected;
          //resource.setUnrealProperties(readPlugin.readProperties(readSource));
          UnrealProperty property = resource.getUnrealProperty("Palette");
          if (property != null) {
            rating += 22; // so the -11 actually makes this just +11
          }
        }
        rating -= 11;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      // 1 - Number Of Mipmaps (9)
      if (ByteConverter.unsign(fm.readByte()) < 50) {
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
   * Reads a DXT image
   **********************************************************************************************
   **/
  public ImageResource loadPaletted(FileManipulator fm, int width, int height, int[] palette) throws Exception {

    // X Bytes - Pixel Data
    int[] data = new int[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int byteValue = ByteConverter.unsign(fm.readByte());
        //data[x+y*width] = ((byteValue << 16) | (byteValue << 8) | byteValue | (255 << 24));
        data[x + y * width] = palette[byteValue];
      }
    }

    return new ImageResource(data, width, height);

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

      // 2 - Unknown
      fm.skip(2);

      int numColors = (int) (fm.getRemainingLength() / 4);
      if (numColors > 256) {
        numColors = 256;
      }

      // X - Palette
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());
        int a = ByteConverter.unsign(fm.readByte());
        a = 255;

        palette[i] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      int pixelSize = 10; // each color will show as a 10x10 box

      int width = 16 * pixelSize;
      int height = 16 * pixelSize;
      int numPixels = width * height;

      int[] pixels = new int[numPixels];

      int currentPixel = 0;
      for (int h = 0; h < height; h += pixelSize) {
        for (int w = 0; w < width; w += pixelSize) {
          int color = palette[currentPixel];

          for (int h2 = 0; h2 < pixelSize; h2++) {
            for (int w2 = 0; w2 < pixelSize; w2++) {
              int pixelPos = ((h + h2) * width) + (w + w2);
              //System.out.println(pixelPos);
              pixels[pixelPos] = color;
            }
          }

          currentPixel++;
        }
      }

      fm.close();

      return new ImageResource(pixels, width, height);

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