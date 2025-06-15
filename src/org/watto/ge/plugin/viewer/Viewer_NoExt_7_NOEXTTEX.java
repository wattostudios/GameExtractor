/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_NoExt_7;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_NoExt_7_NOEXTTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_NoExt_7_NOEXTTEX() {
    super("NoExt_7_NOEXTTEX", "NoExt_7_NOEXTTEX Image");
    setExtensions("noext_tex");

    setGames("Human Resource Machine");
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
      if (plugin instanceof Plugin_NoExt_7) {
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

      // 
      int numImages = fm.readInt();
      if (FieldValidator.checkNumFiles(numImages)) {
        rating += 5;
      }

      // 
      if (fm.readInt() == (4 + (numImages * 4))) {
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

      long arcSize = fm.getLength();

      // 4 - Number of Sprites
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      long[] offsets = new long[numImages];
      for (int i = 0; i < numImages; i++) {
        // 4 - Sprite Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // force now to 1 image (they're just mipmaps, don't need them all rendered)
      numImages = 1;

      ImageResource[] images = new ImageResource[numImages];

      for (int i = 0; i < numImages; i++) {
        long offset = offsets[i];
        fm.relativeSeek(offset);

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Image Format?
        fm.skip(4);

        // X - Sprite Data (RGBA)
        ImageResource imageResource = ImageFormatReader.readRGBA(fm, width, height);
        images[i] = imageResource;
      }

      ImageResource firstImage = images[0];
      if (numImages > 1) {

        for (int i = 0; i < numImages; i++) {
          ImageResource image = images[i];
          if (i == 0) {
            image.setNextFrame(images[i + 1]);
            image.setPreviousFrame(images[numImages - 1]);
          }
          else if (i == numImages - 1) {
            image.setNextFrame(images[0]);
            image.setPreviousFrame(images[i - 1]);
          }
          else {
            image.setNextFrame(images[i + 1]);
            image.setPreviousFrame(images[i - 1]);
          }
        }

        firstImage.setManualFrameTransition(true);

      }

      fm.close();

      return firstImage;

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