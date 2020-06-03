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
import org.watto.ge.plugin.archive.Plugin_BF_BANKFILEV113;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BF_BANKFILEV113_IMAGE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BF_BANKFILEV113_IMAGE() {
    super("BF_BANKFILEV113_IMAGE", "Ultimate Race Pro Image");
    setExtensions("");

    setGames("Ultimate Race Pro");
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
      if (plugin instanceof Plugin_BF_BANKFILEV113) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
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

      long arcSize = fm.getLength();

      ImageResource firstFile = null;
      ImageResource previousFile = null;

      // read each image
      while (fm.getOffset() < arcSize) {
        // 4 - Unknown
        // 12 - null
        // 4 - Unknown
        // 64 - Filename/Description (null terminated, filled with nulls)
        // 4 - Unknown (90)
        // 8 - null
        // 2 - Unknown
        // 2 - Unknown
        // 4 - null
        fm.skip(100);

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Image Width
        // 4 - Image Height
        // 22 - null
        // 4 - Unknown
        // 4 - null
        // 4 - Unknown
        // 4 - null
        // 6 - null
        fm.skip(52);

        // X - Image Data (RGB565)
        ImageResource imageResource = ImageFormatReader.readRGB565(fm, width, height);

        if (firstFile == null) {
          firstFile = imageResource;

        }

        if (previousFile == null) {
          previousFile = imageResource;
        }
        else {
          previousFile.setNextFrame(imageResource);
          previousFile = imageResource;
        }

      }

      if (previousFile != null) {
        previousFile.setNextFrame(firstFile);
      }

      if (previousFile != firstFile) {
        firstFile.setManualFrameTransition(true);
      }

      return firstFile;

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