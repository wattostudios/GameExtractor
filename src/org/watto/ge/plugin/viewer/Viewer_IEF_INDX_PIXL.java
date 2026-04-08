/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_IEF_INDX;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_IEF_INDX_PIXL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_IEF_INDX_PIXL() {
    super("IEF_INDX_PIXL", "IEF_INDX_PIXL Image");
    setExtensions("pixl");

    setGames("Awesome Animated Monster Maker");
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
      if (plugin instanceof Plugin_IEF_INDX) {
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

      // 2 - Image Width
      if (FieldValidator.checkWidth(ShortConverter.changeFormat(fm.readShort()))) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(ShortConverter.changeFormat(fm.readShort()))) {
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
        // 256*4 - RGBA
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = ByteConverter.unsign(fm.readByte());
        aPixel = 255;

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

      // get all the color Palettes in the archive
      if (PaletteManager.getNumPalettes() <= 0) {
        Resource[] resources = Archive.getResources();

        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          Resource currentResource = resources[i];
          if (currentResource.getExtension().equalsIgnoreCase("pltt")) {
            // found a color palette file - need to extract it and read the colors
            int[] palette = extractPalette(resources[i]);
            PaletteManager.addPalette(new Palette(palette));
          }
        }

        PaletteManager.setCurrentPalette(0);
      }

      // 2 - Image Width
      short width = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkHeight(height);

      // X - Paletted Image Data
      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);

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