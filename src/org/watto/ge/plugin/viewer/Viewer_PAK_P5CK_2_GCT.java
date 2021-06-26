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
import org.watto.ge.plugin.archive.Plugin_PAK_P5CK_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PAK_P5CK_2_GCT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PAK_P5CK_2_GCT() {
    super("PAK_P5CK_2_GCT", "TimeSplitters: Future Perfect GCT Paletted Image");
    setExtensions("gct");

    setGames("TimeSplitters: Future Perfect");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_PAK_P5CK_2) {
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

      boolean ps2Match = false;

      // 4 - Unknown (1/5)
      int headerInt1 = fm.readInt();
      if (headerInt1 == 1 || headerInt1 == 3 || headerInt1 == 5 || headerInt1 == 7) {
        // PS2
        rating += 5;
        ps2Match = true;
      }

      // 4 - Unknown (1)
      int headerInt2 = fm.readInt();
      if (headerInt2 == 1 || headerInt2 == 2 || headerInt2 == 3) {
        // PS2
        rating += 5;
        ps2Match = true;
      }

      if (ps2Match) {
        // 4 - Image Height
        if (FieldValidator.checkHeight(fm.readInt())) {
          rating += 5;
        }

        // 4 - Image Width
        if (FieldValidator.checkWidth(fm.readInt())) {
          rating += 5;
        }
      }
      else {

        // double-check for GameCube Format

        // 4 - Image Width
        int imageWidth = fm.readInt();

        // 4 - Image Height
        int imageHeight = fm.readInt();

        if (imageWidth == headerInt1 && FieldValidator.checkHeight(IntConverter.changeFormat(imageWidth))) {
          rating += 5;
        }

        if (imageHeight == headerInt2 && FieldValidator.checkHeight(IntConverter.changeFormat(imageHeight))) {
          rating += 5;
        }

        return 0; // WANT TO FORCE QUIT - DON'T SUPPORT GAMECUBE TEXTURES

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

      boolean ps2Match = false;

      // 4 - Unknown (1/3/5)
      int headerInt1 = fm.readInt();
      if (headerInt1 == 1 || headerInt1 == 3 || headerInt1 == 5 || headerInt1 == 7) {
        // PS2
        ps2Match = true;
      }

      // 4 - Number of Mipmaps (1/2)
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();

      // 4 - Image Height
      int height = fm.readInt();

      if (!ps2Match) {
        // note that width/height swap here for GameCube
        int realWidth = IntConverter.changeFormat(width);
        int realHeight = IntConverter.changeFormat(height);
        width = realWidth;
        height = realHeight;
      }

      FieldValidator.checkWidth(width);
      FieldValidator.checkHeight(height);

      ImageResource imageResource = null;

      if (ps2Match) {
        // PS2

        // X - Color Palette (256 colors)
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);

        // X - Pixels
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource = ImageFormatReader.doubleAlpha(imageResource);
        imageResource.setPixels(ImageFormatReader.unswizzlePS2(imageResource.getPixels(), width, height));
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else {
        // GameCube

        //
        // NOTE: DOESN'T WORK
        //

        // X - Pixels
        byte[] imageBytes = fm.readBytes(width * height / 2);
        imageBytes = ImageFormatReader.unswizzleSwitch(imageBytes, width, height);

        FileManipulator imageFM = new FileManipulator(new ByteBuffer(imageBytes));

        imageResource = ImageFormatReader.readDXT1(imageFM, width, height);

        imageFM.close();
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