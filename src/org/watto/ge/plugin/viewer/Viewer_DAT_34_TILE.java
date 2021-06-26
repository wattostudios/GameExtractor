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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_34;
import org.watto.ge.plugin.resource.Resource_DAT_34;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_34_TILE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_34_TILE() {
    super("DAT_34_TILE", "Nexus: The Kingdom Of The Winds [DAT_34] Tile Image");
    setExtensions("tile");

    setGames("Nexus: The Kingdom Of The Winds");
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
      if (plugin instanceof Plugin_DAT_34) {
        rating += 50;
      }
      else {
        // If that fails, see if the selected resource is an instance of Resource_DAT_34
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        if (selected != null && selected instanceof Resource_DAT_34) {
          rating += 50;
        }
        else {
          // Otherwise force-fail
          return 0;
        }
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

      // Relies on the Resource having the image details contained within it - if not, force-fail it!
      Resource selected = (Resource) SingletonManager.get("CurrentResource");
      if (selected == null || !(selected instanceof Resource_DAT_34)) {
        return null;
      }

      Resource_DAT_34 resource = (Resource_DAT_34) selected;

      Palette paletteObject = resource.getPalette();
      if (paletteObject == null) {
        ErrorLogger.log("[Viewer_DAT_34_TILE]: Missing Palette for file " + resource.getName());
        return null;
      }

      long arcSize = resource.getLength();

      int[] palette = paletteObject.getPalette();
      short width = resource.getWidth();
      short height = resource.getHeight();

      int numPixels = width * height;
      FieldValidator.checkLength(numPixels, arcSize);

      // X - Palette Indexes
      int[] pixels = new int[numPixels];
      for (int i = 0; i < numPixels; i++) {
        pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
      }

      // X - Stencil Data (TODO)

      ImageResource imageResource = new ImageResource(pixels, width, height);

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