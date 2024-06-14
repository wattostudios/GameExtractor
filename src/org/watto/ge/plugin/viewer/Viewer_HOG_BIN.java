/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import org.watto.SingletonManager;
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
import org.watto.ge.plugin.archive.Plugin_HOG;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_HOG_BIN extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_HOG_BIN() {
    super("HOG_BIN", "HOG BIN Image");
    setExtensions("bin");

    setGames("Syphon Filter");
    setPlatforms("PS1");
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
      if (plugin instanceof Plugin_HOG) {
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
  
  **********************************************************************************************
  **/
  public void loadPalettes() {
    // find the palette file

    Resource[] resources = Archive.getResources();
    int numFiles = resources.length;

    for (int i = 0; i < numFiles; i++) {
      if (resources[i].getName().equalsIgnoreCase("ZCLUT.BIN")) {
        // found the palette file

        Resource resource = resources[i];
        int resourceLength = (int) resource.getLength();

        ByteBuffer buffer = new ByteBuffer((int) resourceLength);
        FileManipulator fm = new FileManipulator(buffer);
        resource.extract(fm);

        fm.seek(0); // back to the beginning of the byte array

        int numPalettes = resourceLength / 512;

        for (int p = 0; p < numPalettes; p++) {
          ImageResource image = ImageFormatReader.readRGBA5551(fm, 1, 256);
          image = ImageFormatReader.swapRedAndBlue(image);
          int[] palette = image.getImagePixels();
          PaletteManager.addPalette(new Palette(palette));
        }

        break;
      }
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

      if (!PaletteManager.hasPalettes()) {
        loadPalettes();
      }

      if (!PaletteManager.hasPalettes()) {
        // still no palettes - so not the right type of BIN, or something like that
      }

      int width = 0;
      int height = 0;

      int length = (int) fm.getLength();
      if (length == 32768) {
        width = 128;
        height = 256;
      }

      // find which file this is in the archive, so we know what palette to assign
      Resource currentResource = (Resource) SingletonManager.get("CurrentResource");

      Resource[] resources = Archive.getResources();
      int numFiles = resources.length;

      int paletteNum = 0;
      for (int i = 0; i < numFiles; i++) {
        if (resources[i] == currentResource) {
          paletteNum = i;
          break;
        }
      }

      if (paletteNum >= PaletteManager.getNumPalettes()) {
        paletteNum = 0;
      }

      PaletteManager.setCurrentPalette(paletteNum);

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