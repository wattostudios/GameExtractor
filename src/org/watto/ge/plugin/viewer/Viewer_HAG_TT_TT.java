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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_HAG;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_HAG_TT_TT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_HAG_TT_TT() {
    super("HAG_TT_TT", "The Riddle Of Master Lu TT Image");
    setExtensions("tt");

    setGames("Orion Burger",
        "Ripley's Believe It or Not!: The Riddle Of Master Lu");
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
      if (plugin instanceof Plugin_HAG) {
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
      if (fm.readString(4).equals("  TT")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - File Length [+8]
      if (fm.readInt() + 8 == fm.getLength()) {
        rating += 5;
      }

      // 4 - Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Height
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

      // 4 - Header ("  TT")
      // 4 - File Length [+8]
      fm.skip(8);

      // 4 - Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - XBlocks
      int numXBlocks = fm.readInt();
      FieldValidator.checkRange(numXBlocks, 1, width);

      // 4 - YBlocks
      int numYBlocks = fm.readInt();
      FieldValidator.checkRange(numYBlocks, 1, height);

      // 4 - XBlockSize
      int xSize = fm.readInt();
      FieldValidator.checkRange(xSize, 1, width);

      // 4 - YBlockSize
      int ySize = fm.readInt();
      FieldValidator.checkRange(ySize, 1, height);

      // X - Palette (BGRA)
      int[] palette = new int[256];

      for (int c = 0; c < 256; c++) {
        // 3 - RGB
        int bPixel = ByteConverter.unsign(fm.readByte()) << 2;
        int gPixel = ByteConverter.unsign(fm.readByte()) << 2;
        int rPixel = ByteConverter.unsign(fm.readByte()) << 2;
        int aPixel = 255;
        fm.skip(1); // the 'a' exists in the file, it's just not used

        //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
        int color = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

        // 1 - Palette Index
        //int paletteIndex = ByteConverter.unsign(fm.readByte());
        palette[c] = color;
      }

      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      for (int y = 0; y < numYBlocks; y++) {
        for (int x = 0; x < numXBlocks; x++) {

          int xPos = x * xSize;
          int yPos = y * ySize;

          int xToCopy = xSize;
          if (xPos + xToCopy > width) {
            xToCopy = width % xSize;
          }
          int xToSkip = xSize - xToCopy;

          int yToCopy = ySize;
          if (yPos + yToCopy > height) {
            yToCopy = height % ySize;
          }
          int yToSkip = ySize - yToCopy;

          for (int y2 = 0; y2 < yToCopy; y2++) {
            for (int x2 = 0; x2 < xToCopy; x2++) {
              int outPos = ((yPos + y2) * width) + xPos + x2;
              pixels[outPos] = palette[ByteConverter.unsign(fm.readByte())];
            }
            fm.skip(xToSkip);
          }
          fm.skip(yToSkip * xSize);
        }
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

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