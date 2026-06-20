/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_UOO_2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_UOO_2_UOOANIM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_UOO_2_UOOANIM() {
    super("UOO_2_UOOANIM", "UOO_2_UOOANIM Image");
    setExtensions("uoo_anim");

    setGames("Ultima Online: Outlands");
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
      if (plugin instanceof Plugin_UOO_2) {
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
  //
  NOTE: THIS PLUGIN LOADS THE FULL ANIMATION FOR THE PREVIEW, BUT NOT FOR THE THUMBNAIL
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource[] frames = new ImageResource[256]; // guess max 256 frames
      int numFrames = 0;

      long arcSize = fm.getLength();

      while (fm.getOffset() < arcSize - 12) {

        // 2 - X Offset
        //short xOffset = (short) (128 - fm.readShort());
        int xOffset = fm.readShort();

        // 2 - Y Offset
        //short yOffset = (short) (128 + fm.readShort());
        int yOffset = fm.readShort();

        // 2 - Frame Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Frame Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Number of Pixels (Frame Length = numPixels*2)
        fm.skip(4);

        //System.out.println(xOffset + "\t" + yOffset + "\t" + width + "\t" + height);

        // X - Pixels
        ImageResource imageResource = ImageFormatReader.readRGB555(fm, width, height);

        xOffset = 200 - xOffset;
        yOffset = 200 - ((height + yOffset) / 2); // approximately right

        imageResource = ImageFormatReader.placeOnStage(imageResource, 400, 400, xOffset, yOffset, 255 << 24); // 255<<24 is Black

        frames[numFrames] = imageResource;
        numFrames++;
      }

      fm.close();

      if (numFrames > 1) {
        for (int i = 0; i < numFrames; i++) {
          ImageResource image = frames[i];
          if (i == 0) {
            image.setNextFrame(frames[i + 1]);
            image.setPreviousFrame(frames[numFrames - 1]);
          }
          else if (i == numFrames - 1) {
            image.setNextFrame(frames[0]);
            image.setPreviousFrame(frames[i - 1]);
          }
          else {
            image.setNextFrame(frames[i + 1]);
            image.setPreviousFrame(frames[i - 1]);
          }

          image.setManualFrameTransition(false); // it's an animation
          image.setAnimationSpeed(200);
        }
      }

      ImageResource imageResource = frames[0];

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
  //
  NOTE: THIS PLUGIN ONLY RETURNS FRAME NUMBER 1, FOR THE THUMBNAIL
  **********************************************************************************************
  **/
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 2 - X Offset
      //short xOffset = (short) (128 - fm.readShort());
      int xOffset = fm.readShort();

      // 2 - Y Offset
      //short yOffset = (short) (128 + fm.readShort());
      int yOffset = fm.readShort();

      // 2 - Frame Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Frame Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 4 - Number of Pixels (Frame Length = numPixels*2)
      fm.skip(4);

      //System.out.println(xOffset + "\t" + yOffset + "\t" + width + "\t" + height);

      // X - Pixels
      ImageResource imageResource = ImageFormatReader.readRGB555(fm, width, height);

      xOffset = 200 - xOffset;
      yOffset = 200 - ((height + yOffset) / 2); // approximately right

      imageResource = ImageFormatReader.placeOnStage(imageResource, 400, 400, xOffset, yOffset, 255 << 24); // 255<<24 is Black

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