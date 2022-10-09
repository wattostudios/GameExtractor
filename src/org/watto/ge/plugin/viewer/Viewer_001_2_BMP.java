/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_001_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************
NOT CURRENTLY WORKING - PIXELS DON'T LOOK RIGHT
**********************************************************************************************
**/
public class Viewer_001_2_BMP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_001_2_BMP() {
    super("001_2_BMP", "Star Trek BMP Image");
    setExtensions("bmp");

    setGames("Star Trek: 25th Anniversary");
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
      if (plugin instanceof Plugin_001_2) {
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

      fm.skip(4);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
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
  public int[] extractPalette(Resource paletteResource) {
    try {
      int paletteLength = (int) paletteResource.getLength();

      ByteBuffer buffer = new ByteBuffer(paletteLength);
      FileManipulator fm = new FileManipulator(buffer);
      paletteResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      int numColors = 256;
      int[] palette = new int[numColors];

      for (int i = 0; i < numColors; i++) {
        // 3 - RGB
        int rPixel = ByteConverter.unsign(fm.readByte()) << 2;
        int gPixel = ByteConverter.unsign(fm.readByte()) << 2;
        int bPixel = ByteConverter.unsign(fm.readByte()) << 2;
        int aPixel = 255;

        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
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

      // 2 - X Co-ordinate
      // 2 - Y Co-ordinate
      fm.skip(4);

      // 2 - Image Width
      int width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = fm.readShort();
      FieldValidator.checkHeight(height);

      // get all the color Palettes in the archive
      if (PaletteManager.getNumPalettes() <= 0) {
        Resource[] resources = Archive.getResources();

        int numResources = resources.length;
        int defaultPaletteID = 0;
        for (int i = 0; i < numResources; i++) {
          Resource currentResource = resources[i];
          if (currentResource.getExtension().equalsIgnoreCase("pal")) {
            // found a color palette file - need to extract it and read the colors
            int[] palette = extractPalette(resources[i]);
            PaletteManager.addPalette(new Palette(palette));

            if (currentResource.getName().equalsIgnoreCase("PALETTE.PAL")) {
              defaultPaletteID = PaletteManager.getNumPalettes() - 1;
            }
          }
        }

        PaletteManager.setCurrentPalette(defaultPaletteID);
      }

      int[] palette = PaletteManager.getCurrentPalette().getPalette();

      int numColors = palette.length;
      if (numColors <= 0) {
        ErrorLogger.log("[Viewer_001_2_BMP] Invalid number of colors: " + numColors);
        return null;
      }

      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);

      /*
      // Read the color indexes
      int numPixels = width * height;
      int[] indexes = new int[numPixels];
      
      for (int i = 0; i < numPixels; i++) {
        indexes[i] = ByteConverter.unsign(fm.readByte());
      }
      
      
      PalettedImageResource imageResource = new PalettedImageResource(indexes, width, height, palette);
      */
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