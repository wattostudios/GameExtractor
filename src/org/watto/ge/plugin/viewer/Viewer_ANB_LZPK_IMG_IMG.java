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
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.PalettedImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ANB_LZPK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ANB_LZPK_IMG_IMG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ANB_LZPK_IMG_IMG() {
    super("ANB_LZPK_IMG_IMG", "Addiction Pinball IMG Image");
    setExtensions("img");

    setGames("Addiction Pinball");
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
      if (plugin instanceof Plugin_ANB_LZPK) {
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
      if (fm.readString(4).equals("IMG ")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public int[] extractPalette(File paletteFile) {
    try {

      FileManipulator fm = new FileManipulator(paletteFile, false);

      int numColors = 256;
      int[] palette = new int[numColors];

      // 2 - Header (CM)
      fm.skip(2);

      // 2 - Palette Length
      short paletteLength = fm.readShort();

      fm.skip(2);

      if (paletteLength == 768) {
        for (int i = 0; i < numColors; i++) {
          // 3 - RGB
          int rPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int bPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = 255;

          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }
      else {
        ErrorLogger.log("[Viewer_ANB_LZPK_IMG_IMG] Unsuitable palette length: " + paletteLength);
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

      // 4 - Header ("IMG ")
      fm.skip(4);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown
      fm.skip(2);

      // 2 - Color Palette Filename
      String paletteFilename = "" + fm.readShort();

      // 2 - Unknown
      // 2 - Unknown
      fm.skip(4);

      // find the appropriate palette based on the filename of the archive
      String arcName = Archive.getArchiveName().toLowerCase();
      String optimalPaletteFilename = "";
      if (arcName.startsWith("tbl_")) {
        optimalPaletteFilename = paletteFilename + "_" + arcName.substring(4, 5) + ".pal";
      }

      // Load palettes from the filesystem
      File archiveDirectory = Archive.getBasePath().getParentFile();
      File[] palFiles = archiveDirectory.listFiles();
      int numFiles = palFiles.length;
      PaletteManager.clear();

      int optimalPaletteIndex = -1;
      int currentPaletteIndex = 0;
      for (int i = 0; i < numFiles; i++) {
        File file = palFiles[i];
        String filename = file.getName().toLowerCase();
        if (filename.startsWith(paletteFilename) && filename.endsWith("pal")) {
          // found a color palette file - need to extract it and read the colors
          int[] palette = extractPalette(file);
          PaletteManager.addPalette(new Palette(palette));

          currentPaletteIndex++;

          if (filename.equals(optimalPaletteFilename)) {
            optimalPaletteIndex = currentPaletteIndex;
          }

        }
      }
      // Also add the default Grayscale palette, for the Depth images
      PaletteManager.addPalette(PaletteGenerator.getGrayscalePalette());

      if (optimalPaletteIndex != -1) {
        PaletteManager.setCurrentPalette(optimalPaletteIndex);
      }

      if (fm.getFile().getName().contains("depth")) {
        // show the grayscale palette
        PaletteManager.setCurrentPalette(currentPaletteIndex);
      }

      int[] palette = PaletteManager.getCurrentPalette().getPalette();

      // X - Paletted Image Data
      // Read the color indexes
      int numPixels = width * height;
      int[] indexes = new int[numPixels];

      for (int i = 0; i < numPixels; i++) {
        indexes[i] = ByteConverter.unsign(fm.readByte());
      }

      PalettedImageResource imageResource = new PalettedImageResource(indexes, width, height, palette);

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