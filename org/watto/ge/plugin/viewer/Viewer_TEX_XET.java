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
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_XET extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_XET() {
    super("TEX_XET", "Nitro Stunt Racing TEX Image");
    setExtensions("tex");

    setGames("Nitro Stunt Racing");
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("XET" + (char) 0)) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(16);

      // 4 - Number Of MipMaps
      if (FieldValidator.checkNumColors(fm.readInt())) {
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

      // 4 - Header ("XET" + null)
      // 4 - Unknown (590081)
      // 4 - File Path Offset [-4]
      // 4 - Unknown (128)
      // 4 - Unknown (2)
      fm.skip(20);

      // 4 - Number Of Mipmaps
      int mipmapCount = fm.readInt();
      FieldValidator.checkNumColors(mipmapCount);

      // 4 - Unknown (2/0)
      int smallerDimension = mipmapCount - fm.readInt();
      if (smallerDimension <= 0) {
        smallerDimension = mipmapCount;
      }

      // 4 - Unknown (22)
      // 4 - Timestamp? (1065353216)
      // 4 - Unknown (6/8)
      // 12 - null
      fm.skip(24);

      int width = (int) Math.pow(2, mipmapCount);
      FieldValidator.checkWidth(width);

      int height = (int) Math.pow(2, smallerDimension);
      FieldValidator.checkHeight(height);

      // X - Pixels
      ImageResource imageResource = ImageFormatReader.readDXT3(fm, width, height);
      imageResource.addProperty("ImageFormat", "DXT3");

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