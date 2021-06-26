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

import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_POD_POD2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_POD_POD2_RAW extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_POD_POD2_RAW() {
    super("POD_POD2_RAW", "POD_POD2_RAW");
    setExtensions("raw");

    setGames("4x4 Evolution",
        "Nocturne");
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
      if (plugin instanceof Plugin_POD_POD2) {
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
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // Need to get the name of the current resource, so we can then go and find the color palette with the same name
      Resource selected = (Resource) SingletonManager.get("CurrentResource");
      if (selected == null) {
        return null;
      }

      String resourceName = selected.getName();
      int dotPos = resourceName.lastIndexOf(".");
      if (dotPos > 0) {
        resourceName = resourceName.substring(0, dotPos) + ".ACT"; // the name of the color palette
      }
      else {
        return null;
      }

      // Find the Color Palette Resource
      Resource[] resources = Archive.getResources();
      int resourceCount = resources.length;
      Resource paletteResource = null;
      for (int i = 0; i < resourceCount; i++) {
        if (resources[i].getName().equalsIgnoreCase(resourceName)) {
          // found the palette resource
          paletteResource = resources[i];
          break;
        }
      }

      if (paletteResource == null) {
        return null;
      }

      // work out the dimensions of the image

      int resourceLength = (int) selected.getLength();
      int width = 0;
      int height = 0;

      // First, try some common combinations (as in the game Nocturne)
      if (resourceLength == 307200) {
        width = 640;
        height = 480;
      }
      else if (resourceLength == 480000) {
        width = 800;
        height = 600;
      }
      else if (resourceLength == 76800) {
        width = 320;
        height = 240;
      }
      else if (resourceLength == 34928) {
        width = 148;
        height = 236;
      }
      else if (resourceLength == 176000) {
        width = 640;
        height = 275;
      }
      else if (resourceLength == 81840) {
        width = 248;
        height = 330;
      }
      else if (resourceLength == 143750) {
        width = 250;
        height = 575;
      }
      else if (resourceLength == 85312) {
        width = 248;
        height = 344;
      }
      else if (resourceLength == 133300) {
        width = 310;
        height = 430;
      }
      else if (resourceLength == 3840) {
        width = 40;
        height = 96;
      }
      else if (resourceLength == 1728) {
        width = 27;
        height = 64;
      }
      else {
        // Second, assume it's a square (as in the game 4x4 Evolution)
        width = (int) Math.sqrt(resourceLength);
        height = resourceLength / width;
      }

      if (width == 0 || height == 0) {
        return null; // couldn't work out an appropriate dimension
      }

      int numPixels = width * height;

      // NOTE: WE NEED TO READ THE IMAGE DATA FIRST, THEN READ THE COLOR PALETTE, OTHERWISE THE EXPORTERS OVERWRITE EACH OTHER!

      // X - Palette Indexes
      int[] pixels = new int[numPixels];
      for (int i = 0; i < numPixels; i++) {
        //pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
        pixels[i] = ByteConverter.unsign(fm.readByte());
      }

      // close the image resource so we can now open the palette and read it
      fm.close();

      // now extract the color palette file and build the palette array
      int numColors = (int) (paletteResource.getLength() / 3);
      if (numColors > 256) {
        numColors = 256;
      }
      int[] palette = new int[numColors];

      ExporterPlugin paletteExporter = paletteResource.getExporter();
      paletteExporter.open(paletteResource);
      for (int i = 0; i < numColors; i++) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (paletteExporter.available()) {
          r = ByteConverter.unsign((byte) paletteExporter.read());
        }
        if (paletteExporter.available()) {
          g = ByteConverter.unsign((byte) paletteExporter.read());
        }
        if (paletteExporter.available()) {
          b = ByteConverter.unsign((byte) paletteExporter.read());
        }
        palette[i] = (255 << 24 | r << 16 | g << 8 | b);
      }
      paletteExporter.close();

      // go through and change all the pixels to their palette colors
      for (int i = 0; i < numPixels; i++) {
        pixels[i] = palette[pixels[i]];
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

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