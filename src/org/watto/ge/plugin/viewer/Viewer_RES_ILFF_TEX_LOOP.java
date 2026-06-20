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
import org.watto.ge.plugin.archive.Plugin_RES_ILFF;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RES_ILFF_TEX_LOOP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RES_ILFF_TEX_LOOP() {
    super("RES_ILFF_TEX_LOOP", "RES_ILFF_TEX_LOOP Image");
    setExtensions("tex", "spr");

    setGames("Project IGI: I'm Going In");
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
      if (plugin instanceof Plugin_RES_ILFF) {
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

      // 4 - Header
      if (fm.readString(4).equals("LOOP")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      int fileType = fm.readInt();
      if (fileType == 9 || fileType == 11) {
        rating += 5;
      }

      if (fileType == 11) {
        fm.skip(14);

        // 2 - Image Width
        if (FieldValidator.checkWidth(fm.readShort())) {
          rating += 5;
        }

        // 2 - Image Height
        if (FieldValidator.checkHeight(fm.readShort())) {
          rating += 5;
        }
      }
      else if (fileType == 9) {
        fm.skip(20);

        int arcSize = (int) fm.getLength();

        // 4 - Image Data Length
        if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
          rating += 5;
        }

        fm.skip(8);

        // 4 - Image Width
        if (FieldValidator.checkWidth(fm.readInt())) {
          rating += 5;
        }

        // 4 - Image Height
        if (FieldValidator.checkHeight(fm.readInt())) {
          rating += 5;
        }
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

      // 4 - Header (LOOP)
      fm.skip(4);

      // 4 - Unknown (11)
      int fileType = fm.readInt();

      if (fileType == 9) {
        // SPR (maybe an animation)
        long arcSize = fm.getLength();

        // 8 - null
        // 2 - null
        // 2 - Unknown (5)
        // 8 - null
        // 4 - Image Data Length
        fm.skip(24);

        // 4 - Number of Frames
        int numFrames = fm.readInt();
        FieldValidator.checkNumFiles(numFrames);

        // 4 - null
        // 4 - Image Width
        // 4 - Image Height
        // 4 - Image Format? (2=RGBA5551, 3 = BGRA)
        fm.skip(16);

        int[] offsets = new int[numFrames];
        int[] widths = new int[numFrames];
        int[] heights = new int[numFrames];
        int[] formats = new int[numFrames];
        for (int i = 0; i < numFrames; i++) {

          // 4 - Image Data Offset (84)
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Image Format? (2=RGBA5551, 3 = BGRA)
          int imageFormat = fm.readInt();

          // 2 - Flags (64/128/1024)
          fm.skip(2);

          // 2 - Image Width
          short width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          short height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - Unknown (0/1)
          // 16 - null
          fm.skip(18);

          offsets[i] = offset;
          widths[i] = width;
          heights[i] = height;
          formats[i] = imageFormat;
        }

        ImageResource[] imageResources = new ImageResource[numFrames];
        for (int i = 0; i < numFrames; i++) {
          fm.relativeSeek(offsets[i]);

          int width = widths[i];
          int height = heights[i];
          int imageFormat = formats[i];

          // X - Pixels
          ImageResource imageResource = null;
          if (imageFormat == 2) {
            imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
          }
          else if (imageFormat == 3) {
            imageResource = ImageFormatReader.readBGRA(fm, width, height);
          }
          else {
            ErrorLogger.log("[Viewer_RES_ILFF_TEX_LOOP] Unknown Image Format: " + imageFormat);
            return null;
          }

          // save the frame
          imageResources[i] = imageResource;

        }

        fm.close();

        // make it animated
        if (numFrames > 1) {
          for (int i = 0; i < numFrames; i++) {
            ImageResource image = imageResources[i];
            if (i == 0) {
              image.setNextFrame(imageResources[i + 1]);
              image.setPreviousFrame(imageResources[numFrames - 1]);
            }
            else if (i == numFrames - 1) {
              image.setNextFrame(imageResources[0]);
              image.setPreviousFrame(imageResources[i - 1]);
            }
            else {
              image.setNextFrame(imageResources[i + 1]);
              image.setPreviousFrame(imageResources[i - 1]);
            }
          }
        }

        return imageResources[0];

      }
      else { //else if (fileType == 11) {
        // TEX (we'll just assume everything else is a TEX

        // 4 - Unknown (2/67)
        // 8 - null
        // 2 - Unknown (5)
        fm.skip(14);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Image Width
        // 2 - Image Height
        fm.skip(4);

        // 2 - Image Format? (2=RGBA5551, 4 = BGRA)
        short imageFormat = fm.readShort();

        // X - Pixels
        ImageResource imageResource = null;
        if (imageFormat == 2) {
          imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
        }
        else if (imageFormat == 4) {
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
        }
        else {
          ErrorLogger.log("[Viewer_RES_ILFF_TEX_LOOP] Unknown Image Format: " + imageFormat);
          return null;
        }

        fm.close();

        return imageResource;
      }

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