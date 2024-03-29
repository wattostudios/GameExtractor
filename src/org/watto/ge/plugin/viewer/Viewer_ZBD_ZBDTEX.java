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
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ZBD;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZBD_ZBDTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZBD_ZBDTEX() {
    super("ZBD_ZBDTEX", "Recoil ZBD_TEX Image");
    setExtensions("zbd_tex");

    setGames("Recoil");
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
      if (plugin instanceof Plugin_ZBD) {
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

      // 4 - Image Format? (5=RGB565, 11=RGB565 followed by Alpha, 27=8bit followed by Alpha)
      int imageFormat = fm.readInt();

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 8 - null
      fm.skip(8);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 5 || imageFormat == 3) {
        // RGB565

        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }
      else if (imageFormat == 11) {
        // RGB565 followed by Alpha

        int[] pixels = ImageFormatReader.readRGB565(fm, width, height).getPixels();

        int numPixels = width * height;
        byte[] alpha = fm.readBytes(numPixels);

        // apply the alpha to the pixels
        for (int p = 0; p < numPixels; p++) {
          pixels[p] = (pixels[p] & 16777215) | (ByteConverter.unsign(alpha[p]) << 24);
        }

        imageResource = new ImageResource(pixels, width, height);
      }
      else if (imageFormat == 21 || imageFormat == 19) {
        // 8-Bit Paletted

        // Get the palette (stored as a property on the image);
        Object resourceObject = SingletonManager.get("CurrentResource");
        if (resourceObject == null || !(resourceObject instanceof Resource)) {
          return null;
        }
        Resource resource = (Resource) resourceObject;

        int paletteID = -1;
        try {
          paletteID = Integer.parseInt(resource.getProperty("ColorPaletteID"));
        }
        catch (Throwable t) {
          //
        }
        if (paletteID == -1) {
          ErrorLogger.log("[Viewer_ZBD_ZBDTEX] Color Palette ID Missing");
          return null;
        }
        else if (paletteID >= PaletteManager.getNumPalettes()) {
          ErrorLogger.log("[Viewer_ZBD_ZBDTEX] Color Palette ID is larger than the number of loaded Palettes");
          return null;
        }

        int[] palette = PaletteManager.getPalette(paletteID).getPalette();

        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (imageFormat == 27) {
        // 8-Bit Paletted followed by Alpha

        // Get the palette (stored as a property on the image);
        Object resourceObject = SingletonManager.get("CurrentResource");
        if (resourceObject == null || !(resourceObject instanceof Resource)) {
          return null;
        }
        Resource resource = (Resource) resourceObject;

        int paletteID = -1;
        try {
          paletteID = Integer.parseInt(resource.getProperty("ColorPaletteID"));
        }
        catch (Throwable t) {
          //
        }
        if (paletteID == -1) {
          ErrorLogger.log("[Viewer_ZBD_ZBDTEX] Color Palette ID Missing");
          return null;
        }
        else if (paletteID >= PaletteManager.getNumPalettes()) {
          ErrorLogger.log("[Viewer_ZBD_ZBDTEX] Color Palette ID is larger than the number of loaded Palettes");
          return null;
        }

        int[] palette = PaletteManager.getPalette(paletteID).getPalette();

        // 8-Bit Paletted followed by Alpha
        int[] pixels = ImageFormatReader.read8BitPaletted(fm, width, height, palette).getPixels();

        int numPixels = width * height;
        byte[] alpha = fm.readBytes(numPixels);

        // apply the alpha to the pixels
        for (int p = 0; p < numPixels; p++) {
          pixels[p] = (pixels[p] & 16777215) | (ByteConverter.unsign(alpha[p]) << 24);
        }

        imageResource = new ImageResource(pixels, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_ZBD_ZBDTEX] Unknown Image Format: " + imageFormat);
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