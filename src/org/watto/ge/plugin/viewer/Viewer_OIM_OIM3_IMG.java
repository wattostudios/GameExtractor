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
import org.watto.ge.plugin.archive.Plugin_OIM_OIM3;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_OIM_OIM3_IMG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_OIM_OIM3_IMG() {
    super("OIM_OIM3_IMG", "OIM Image");
    setExtensions("oim_img");

    setGames("Shadow Of Rome");
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
      if (plugin instanceof Plugin_OIM_OIM3) {
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
    catch (

    Throwable t) {
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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public int[] extractPalette(Resource paltResource, int paletteFormat) {
    try {
      int length = (int) paltResource.getLength();

      ByteBuffer buffer = new ByteBuffer(length);
      FileManipulator fm = new FileManipulator(buffer);
      paltResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      int[] palette = new int[0];

      if (paletteFormat == 19) {
        // 8-bit palette
        int numColors = length / 4;

        palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
          // 4 - BGRA (a is in the range -127 to 127)
          int rPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int bPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = fm.readByte();

          if (aPixel == -128) {
            aPixel = 255;
          }
          else {
            aPixel *= 2;
          }

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }

      }
      else if (paletteFormat == 20) {
        // 4-bit palette
        int numColors = length / 4;

        palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
          // 4 - BGRA (a is in the range -127 to 127)
          int rPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int bPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = fm.readByte();

          if (aPixel == -128) {
            aPixel = 255;
          }
          else {
            aPixel *= 2;
          }

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }

      }

      fm.close();

      return palette;
    }
    catch (Throwable t) {
      logError(t);
      return new int[0];
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

      int height = 0;
      int width = 0;
      int paletteID = -1;
      int paletteFormat = 0;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
        paletteID = Integer.parseInt(resource.getProperty("FileID"));
        paletteFormat = Integer.parseInt(resource.getProperty("PaletteFormat"));
      }
      catch (Throwable t) {
        //
      }

      if (height == 0 || width == 0 || paletteID == 0) {
        return null;
      }

      // read the image bytes first, before we grab the palette
      byte[] imageBytes = fm.readBytes((int) fm.getLength());

      // now grab the palette
      int[] palette = extractPalette(Archive.getResource(paletteID), paletteFormat);

      int numColors = palette.length;
      if (numColors <= 0) {
        ErrorLogger.log("[Viewer_OIM_OIM3_IMG] Invalid number of colors: " + numColors);
        return null;
      }

      // now process the image
      fm.close();
      fm = new FileManipulator(new ByteBuffer(imageBytes));

      ImageResource imageResource = null;
      if (paletteFormat == 19) {
        // 8-bit paletted
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      if (paletteFormat == 20) {
        // 4-bit paletted
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
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