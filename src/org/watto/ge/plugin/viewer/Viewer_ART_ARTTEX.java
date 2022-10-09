/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ART;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ART_ARTTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ART_ARTTEX() {
    super("ART_ARTTEX", "ART_TEX Image");
    setExtensions("art_tex");

    setGames("Requiem: Avenging Angel");
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
      if (plugin instanceof Plugin_ART) {
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

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      fm.skip(8);

      if (fm.readShort() == 256) {
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

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Animation Image Width
      short animationWidth = fm.readShort();
      FieldValidator.checkWidth(animationWidth + 1); // +1 to allow nulls

      // 2 - Animation Image Height
      short animationHeight = fm.readShort();
      FieldValidator.checkHeight(animationHeight + 1); // +1 to allow nulls

      // 2 - Static Image Width
      short staticWidth = fm.readShort();
      FieldValidator.checkWidth(staticWidth + 1); // +1 to allow nulls

      // 2 - Static Image Height
      short staticHeight = fm.readShort();
      FieldValidator.checkHeight(staticHeight + 1); // +1 to allow nulls

      // 1 - Number of Frames
      int numFrames = fm.readByte();

      // 1 - Unknown
      // 2 - Unknown
      fm.skip(3);

      // 1 - Image Format (0=Paletted, 96=ABGR4444)
      int imageFormat = fm.readByte();

      // 1 - Unknown (254)
      fm.skip(1);

      // X - Color Palette
      int[] palette = ImageFormatReader.readPaletteBGRA(fm, 256);

      // X - Pixels
      ImageResource imageResource = null;

      if (numFrames == 0) {
        // normal image

        if (imageFormat == 0) {
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          imageResource = ImageFormatReader.flipVertically(imageResource);
        }
        else if (imageFormat == 96) {
          imageResource = ImageFormatReader.readABGR4444(fm, width, height);
          imageResource = ImageFormatReader.flipVertically(imageResource);
        }
        else {
          ErrorLogger.log("[Viewer_ART_ARTTEX] Unknown Image Format: " + imageFormat);
        }
      }
      else {
        // animation, and perhaps also a static image with it

        // read the animation first
        ImageResource[] imageResources = new ImageResource[2];

        // read all the frames in 1 go
        animationHeight *= numFrames;

        imageResource = ImageFormatReader.read8BitPaletted(fm, animationWidth, animationHeight, palette);
        imageResource = ImageFormatReader.flipVertically(imageResource);
        imageResources[0] = imageResource;

        // now read the static image
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource = ImageFormatReader.flipVertically(imageResource);
        imageResources[1] = imageResource;

        // now set the transitions
        imageResources[0].setNextFrame(imageResources[1]);
        imageResources[0].setPreviousFrame(imageResources[1]);

        imageResources[1].setNextFrame(imageResources[0]);
        imageResources[1].setPreviousFrame(imageResources[0]);

        imageResource = imageResources[0];
        imageResource.setManualFrameTransition(true);
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