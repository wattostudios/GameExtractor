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
import org.watto.ge.plugin.archive.Plugin_SPR_2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_SPR_2_BMP extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_SPR_2_BMP() {
    super("SPR_2_BMP", "Killer Tank BMP Image");
    setExtensions("bmp");

    setGames("Killer Tank");
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
      if (plugin instanceof Plugin_SPR_2) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        //return 0;
      }

      // 2 - Image Width/Height
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Width/Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

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

      // 2 - Image Width/Height
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Width/Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 4 - Color Format? (0/1/3)
      int colorFormat = fm.readInt();

      // 28 - null
      fm.skip(28);

      ImageResource imageResource = null;
      if (colorFormat == 1) {
        // RGBA5551
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA5551");
      }
      else if (colorFormat == 0) {
        // RGB565
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB565");
      }
      else if (colorFormat == 2) {
        // GBAR4444
        imageResource = ImageFormatReader.readGBAR4444(fm, width, height);
        imageResource.addProperty("ImageFormat", "GBAR4444");
      }
      else {
        //L8A8
        imageResource = ImageFormatReader.readL8A8(fm, width, height);
        imageResource.addProperty("ImageFormat", "L8A8");
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