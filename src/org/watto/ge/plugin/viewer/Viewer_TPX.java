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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TPX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TPX() {
    super("TPX", "TPX Image");
    setExtensions("tpx");

    setGames("Hero of the Kingdom 2");
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
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - File Length
      if (fm.readInt() + 8 == fm.getLength()) {
        rating += 5;
      }

      if (fm.readLong() == 1) {
        rating += 5;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
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
  
  THIS IS A LONG FORMAT, NOT JUST REDIRECTING TO readThumbnail();
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      int numImages = 0;
      ImageResource[] imageResources = new ImageResource[100];// guess max frames in an image

      while (fm.getOffset() < arcSize) {
        long offset = fm.getOffset();

        // 4 - File Length [+8] (including all these header fields)
        int length = fm.readInt() + 8;
        FieldValidator.checkLength(length, arcSize);

        if (length < 32) {
          fm.seek(offset + length);
          continue;
        }

        // 4 - Unknown (1)
        // 4 - null
        fm.skip(8);

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width + 1); // +1 to allow nulls

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height + 1); // +1 to allow nulls

        if (width == 0 && height == 0) {
          fm.seek(offset + length);
          continue;
        }

        // 4 - Unknown (21)
        // 4 - Unknown (4)
        // 4 - Flags? (3936=Single Image, 1592=Animation)
        fm.skip(12);

        // X - Image Data (BGRA)
        ImageResource imageResource = ImageFormatReader.readBGRA(fm, width, height);
        imageResources[numImages] = imageResource;
        numImages++;

        fm.seek(offset + length);
      }

      fm.close();

      ImageResource firstImage = imageResources[0];

      if (numImages > 1) {
        // animation, link the frames together
        for (int i = 0; i < numImages; i++) {
          ImageResource imageResource = imageResources[i];
          if (i == 0) {
            imageResource.setNextFrame(imageResources[i + 1]);
            imageResource.setPreviousFrame(imageResources[numImages - 1]);
          }
          else if (i == numImages - 1) {
            imageResource.setNextFrame(imageResources[0]);
            imageResource.setPreviousFrame(imageResources[i - 1]);
          }
          else {
            imageResource.setNextFrame(imageResources[i + 1]);
            imageResource.setPreviousFrame(imageResources[i - 1]);
          }
        }
      }

      if (firstImage == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(firstImage);

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
  
  THIS IS A SHORT ONE, TO ONLY RENDER THE FIRST IMAGE FOR A THUMBNAIL
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - File Length [+8] (including all these header fields)
      int length = fm.readInt() + 8;
      FieldValidator.checkLength(length, arcSize);

      if (length < 32) {
        return null;
      }

      // 4 - Unknown (1)
      // 4 - null
      fm.skip(8);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width + 1); // +1 to allow nulls

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height + 1); // +1 to allow nulls

      if (width == 0 && height == 0) {
        return null;
      }

      // 4 - Unknown (21)
      // 4 - Unknown (4)
      // 4 - Flags? (3936=Single Image, 1592=Animation)
      fm.skip(12);

      // X - Image Data (BGRA)
      ImageResource imageResource = ImageFormatReader.readBGRA(fm, width, height);

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