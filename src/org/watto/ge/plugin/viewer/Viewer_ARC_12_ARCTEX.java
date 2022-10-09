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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARC_12;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_12_ARCTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_12_ARCTEX() {
    super("ARC_12_ARCTEX", "Rockman X4 (Mega Man X4) ARC_TEX Image");
    setExtensions("arc_tex");

    setGames("Rockman X4",
        "Mega Man X4");
    setPlatforms("PC", "PSX");
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
      if (plugin instanceof Plugin_ARC_12) {
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

      int arcSize = (int) fm.getLength();
      int width = 128;
      int height = arcSize / 128;
      if (height * width == arcSize) {
        rating += 5;
      }
      else {
        rating = 0;
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
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      int width = 128;
      int height = (int) (arcSize / 128);

      /*
      // load the palettes (external DAT files)
      File arcFile = Archive.getBasePath();
      String basePath = arcFile.getParent() + File.separatorChar;
      
      String filename = arcFile.getName();
      int underscorePos = filename.indexOf('_');
      
      String paletteChar = "0";
      if (underscorePos > 0) {
        paletteChar = filename.substring(underscorePos - 1, underscorePos);
      }
      String paletteFilename = "PAL" + paletteChar + "00.DAT";
      
      File paletteFile = new File(basePath + paletteFilename);
      if (!paletteFile.exists()) {
        ErrorLogger.log("[Viewer_ARC_12_ARCTEX] Missing palette file: " + paletteFilename);
        return null;
      }
      
      FileManipulator palFM = new FileManipulator(paletteFile, false);
      
      int numPalettes = (int) (paletteFile.length() / 512);
      PaletteManager.clear();
      for (int i = 0; i < numPalettes; i++) {
        int[] palette = ImageFormatReader.readABGR4444(palFM, 256, 1).getPixels();
        PaletteManager.addPalette(new Palette(palette));
      }
      
      palFM.close();
      
      // X - Image Data
      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);
      */

      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);

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