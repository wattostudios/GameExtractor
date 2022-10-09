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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_98;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_98_DATTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_98_DATTEX() {
    super("DAT_98_DATTEX", "Turok: Dinosaur Hunter DAT_TEX Image");
    setExtensions("dat_tex");

    setGames("Turok: Dinosaur Hunter");
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
      if (plugin instanceof Plugin_DAT_98) {
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

      // 4 - Unknown (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // 4 - Unknown (24)
      if (fm.readInt() == 24) {
        rating += 5;
      }

      // 4 - Unknown (48)
      if (fm.readInt() == 48) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
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

      // 4 - Unknown (3)
      // 4 - Unknown (24)
      // 4 - Unknown (48)
      // 4 - File Length [+8 or +48]
      // 4 - File Length
      // 4 - null
      // 4 - Unknown
      fm.skip(28);

      // 2 - Flip Flag? (0=No Flip, 255 = Flip)
      int flip = fm.readShort();

      // 2 - Unknown
      // 4 - Unknown
      fm.skip(6);

      // 2 - Image Width (Cropped)
      fm.skip(2);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 1 - Image Format (0=BGR555, 1=ABRG1555, 2=RGBA4444)
      int imageFormat = fm.readByte();

      // 3 - Unknown (usually null)
      fm.skip(3);

      // 2 - Image Width (Stored)
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - null
      fm.skip(2);

      int dataOffset = (int) fm.getOffset();

      // 4 - Number of Mipmaps
      int mipmapCount = fm.readInt();
      FieldValidator.checkRange(mipmapCount, 1, 100); // guess

      int[] mipmapOffsets = new int[mipmapCount];
      for (int i = 0; i < mipmapCount; i++) {
        // 4 - Mipmap Offset (relative to the start of the Mipmap Directory)
        int mipmapOffset = dataOffset + fm.readInt();
        FieldValidator.checkOffset(mipmapOffset, arcSize);
        mipmapOffsets[i] = mipmapOffset;
      }

      // 4 - File Length [+56]
      // 4 - Unknown (sometimes null)
      fm.skip(8);

      ImageResource[] imageResources = new ImageResource[mipmapCount];

      for (int i = 0; i < mipmapCount; i++) {
        fm.relativeSeek(mipmapOffsets[i]);

        // X - Pixels
        ImageResource imageResource = null;
        if (imageFormat == 0) {
          imageResource = ImageFormatReader.readBGR555(fm, width, height);
        }
        else if (imageFormat == 1) {
          imageResource = ImageFormatReader.readARGB1555(fm, width, height);
        }
        else if (imageFormat == 2) {
          imageResource = ImageFormatReader.readRGBA4444(fm, width, height);
        }
        else {
          ErrorLogger.log("[DAT_98_DATTEX] Unknown Image Format: " + imageFormat);
        }

        if (flip == 255) {
          imageResource = ImageFormatReader.flipVertically(imageResource);
        }

        imageResources[i] = imageResource;
      }

      for (int i = 1; i < mipmapCount; i++) {
        imageResources[i].setPreviousFrame(imageResources[i - 1]);
      }
      for (int i = 0; i < mipmapCount - 1; i++) {
        imageResources[i].setNextFrame(imageResources[i + 1]);
      }
      imageResources[0].setPreviousFrame(imageResources[mipmapCount - 1]);
      imageResources[mipmapCount - 1].setNextFrame(imageResources[0]);

      fm.close();

      return imageResources[0];

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